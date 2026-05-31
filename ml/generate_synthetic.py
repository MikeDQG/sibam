"""
Generiranje sintetičnih podatkov o zamudah avtobusov.

Pristop:
- Iz realnih podatkov naučimo porazdelitev zamud po (route_id, direction, hour, is_weekend)
- Generiramo nove vrstice za zimske mesece (okt 2025 – apr 2026), ki jih v realnih podatkih nimamo
- Dodamo realistično zimsko vreme za Maribor
- Sintetični podatki se shranijo kot parquet in jih train_buses.py doda k realnim
"""

import os
import json
import io
import pandas as pd
import numpy as np
from datetime import date, timedelta
from supabase import create_client
from dotenv import load_dotenv

load_dotenv()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_KEY")
supabase = create_client(SUPABASE_URL, SUPABASE_KEY)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))


# 1. Nalaganje realnih podatkov in direction mappinga
def load_silver():
    data = supabase.storage.from_("silver").download("buses/latest.parquet")
    df = pd.read_parquet(io.BytesIO(data))
    df["recorded_at_local"] = pd.to_datetime(df["recorded_at_local"])
    df["hour"]       = df["recorded_at_local"].dt.hour
    df["day_of_week"]= df["recorded_at_local"].dt.dayofweek
    df["is_weekend"] = (df["day_of_week"] >= 5).astype(int)
    return df

def load_direction_mapping():
    with open(os.path.join(SCRIPT_DIR, "stop_direction_mapping.json")) as f:
        raw = json.load(f)
    # raw: "route_id_stopId" -> direction_int
    # preoblikujemo v: route_id -> {direction -> [stop_ids]}
    route_stop_dir = {}
    for key, direction in raw.items():
        route_id, stop_id = key.split("_", 1)
        route_id = int(route_id)
        if route_id not in route_stop_dir:
            route_stop_dir[route_id] = {}
        if direction not in route_stop_dir[route_id]:
            route_stop_dir[route_id][direction] = []
        route_stop_dir[route_id][direction].append(stop_id)
    return route_stop_dir


# 2. Zimsko vreme za Maribor (realistične vrednosti)
MONTHLY_WEATHER = {
    #  mesec: (povp_temp, std_temp, rain_prob, avg_rain, avg_wind)
    10: (10.0, 4.0, 0.35, 0.8,  2.5),  # oktober
    11: ( 4.0, 4.0, 0.40, 1.0,  3.0),  # november
    12: ( 0.0, 4.0, 0.30, 0.6,  2.8),  # december
     1: (-1.0, 5.0, 0.25, 0.5,  2.5),  # januar
     2: ( 1.0, 5.0, 0.25, 0.5,  2.5),  # februar
     3: ( 6.0, 4.0, 0.35, 0.7,  3.0),  # marec
     4: (11.0, 4.0, 0.40, 1.2,  2.8),  # april
}

def sample_weather(month, n):
    avg_t, std_t, rain_prob, avg_rain, avg_wind = MONTHLY_WEATHER[month]
    temperature = np.random.normal(avg_t, std_t, n)
    rain_mask   = np.random.random(n) < rain_prob
    rain        = np.where(rain_mask, np.random.exponential(avg_rain, n), 0.0).clip(0, 12)
    wind_speed  = np.random.exponential(avg_wind, n).clip(0, 10)
    return temperature, rain, wind_speed


# 3. Naučimo se porazdelitve zamud iz realnih podatkov
def compute_delay_stats(df, direction_mapping):
    """
    Za vsako kombinacijo (route_id, direction, stop_sequence, hour, is_weekend)
    izračunamo povprečje in std zamude.
    """
    # dodaj direction iz current_stop_id
    dir_lookup = {}
    for route_id, dirs in direction_mapping.items():
        for direction, stops in dirs.items():
            for stop_id in stops:
                dir_lookup[(str(route_id), str(stop_id))] = direction

    df = df.copy()
    df["direction"] = df.apply(
        lambda row: dir_lookup.get((str(row["route_id"]), str(row["current_stop_id"])), -1),
        axis=1
    )
    df = df[df["direction"] != -1]
    df = df[(df["delay_seconds"] >= -900) & (df["delay_seconds"] <= 3600)]
    df = df[df["hour"] != 0]

    stats = df.groupby(["route_id", "direction", "stop_sequence", "hour", "is_weekend"])[
        "delay_seconds"
    ].agg(["mean", "std", "count"]).reset_index()

    # za majhne vzorce std postane nestabilen — privzeto 60s
    stats["std"] = stats["std"].fillna(60).clip(lower=20)

    return stats, dir_lookup


# 4. Generiranje sintetičnih vrstic
def generate_synthetic(stats, direction_mapping, n_days=120, rows_per_combo=2):
    """
    Za vsako (route_id, direction, stop_sequence, hour, is_weekend) kombinacijo
    generiramo rows_per_combo novih vrstic na dan.
    """
    rng = np.random.default_rng(42)

    # naključni zimski datumi: okt 2025 – apr 2026
    start_date = date(2025, 10, 1)
    end_date   = date(2026, 4, 30)
    all_dates  = [start_date + timedelta(days=i)
                  for i in range((end_date - start_date).days)]
    chosen_dates = rng.choice(all_dates, size=n_days, replace=False)

    synthetic_rows = []

    # za vsak datum
    for d in chosen_dates:
        month      = d.month
        is_weekend = int(d.weekday() >= 5)

        # samo is_weekend == d.weekday() kombinacije
        group = stats[stats["is_weekend"] == is_weekend]
        if group.empty:
            continue

        n_combos = len(group)
        temps, rains, winds = sample_weather(month, n_combos)

        for idx, (_, row) in enumerate(group.iterrows()):
            route_id    = row["route_id"]
            direction   = row["direction"]
            stop_seq    = row["stop_sequence"]
            hour        = int(row["hour"])
            mean_delay  = float(row["mean"])
            std_delay   = float(row["std"])

            # sintetična zamuda — vzorčimo iz normalne porazdelitve
            # pozimi malo povečamo zamude (zimski faktor ~10%)
            winter_factor = 1.1 if month in [11, 12, 1, 2] else 1.0
            # dež dodaja zamudo
            rain_bonus = float(rains[idx]) * 15

            adj_mean = mean_delay * winter_factor + rain_bonus
            delays = rng.normal(adj_mean, std_delay, rows_per_combo).clip(-900, 3600)

            # naključna ura v tem bloku
            for delay in delays:
                minute  = int(rng.integers(0, 60))
                second  = int(rng.integers(0, 60))
                dt_local = pd.Timestamp(
                    year=d.year, month=d.month, day=d.day,
                    hour=hour, minute=minute, second=second,
                    tz="Europe/Ljubljana"
                )
                dt_utc = dt_local.tz_convert("UTC")

                # izberemo naključen stop_id za to kombinacijo (route, direction)
                dirs = direction_mapping.get(int(route_id), {})
                stop_ids = dirs.get(direction, [])
                if not stop_ids:
                    continue
                current_stop_id = str(rng.choice(stop_ids))

                synthetic_rows.append({
                    "route_id":        str(route_id),
                    "stop_sequence":   int(stop_seq),
                    "delay_seconds":   int(delay),
                    "bearing":         float(rng.integers(0, 360)),
                    "current_stop_id": current_stop_id,
                    "recorded_at":     dt_utc,
                    "recorded_at_local": dt_local,
                    "temperature":     float(temps[idx]),
                    "wind_speed":      float(winds[idx]),
                    "rain":            float(rains[idx]),
                })

    df_syn = pd.DataFrame(synthetic_rows)
    print(f"Generirano: {len(df_syn)} sintetičnih vrstic")
    return df_syn


# 5. Shranjevanje
def save_synthetic(df_syn):
    out_path = os.path.join(SCRIPT_DIR, "synthetic_buses.parquet")
    df_syn.to_parquet(out_path, index=False)
    print(f"Shranjeno → {out_path}")
    return out_path


if __name__ == "__main__":
    print("Nalaganje realnih podatkov...")
    df_real = load_silver()
    print(f"Realnih vrstic: {len(df_real)}")

    print("Nalaganje direction mappinga...")
    direction_mapping = load_direction_mapping()

    print("Računanje statistike zamud...")
    stats, _ = compute_delay_stats(df_real, direction_mapping)
    print(f"Kombinacij (route/dir/stop/hour/weekend): {len(stats)}")

    print("Generiranje sintetičnih podatkov...")
    df_syn = generate_synthetic(stats, direction_mapping, n_days=120, rows_per_combo=2)

    save_synthetic(df_syn)
    print("Končano.")
