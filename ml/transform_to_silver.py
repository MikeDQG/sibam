import os
import pandas as pd
from supabase import create_client
from dotenv import load_dotenv
import io

load_dotenv()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_KEY")

supabase = create_client(SUPABASE_URL, SUPABASE_KEY)

# downloadanje parquet datotek iz bronze bucketa 
def download_all_parquets(folder, prefix=""):
    files = supabase.storage.from_("bronze").list(folder)
    dfs = []
    for file in files:
        name = file["name"]
        if name.endswith(".parquet") and name.startswith(prefix):
            data = supabase.storage.from_("bronze").download(f"{folder}/{name}")
            dfs.append(pd.read_parquet(io.BytesIO(data)))
    return pd.concat(dfs, ignore_index=True).drop_duplicates(subset=["id"])

def load_bronze():
    snapshots = download_all_parquets("bikes",   prefix="snapshots_")
    stations  = download_all_parquets("bikes",   prefix="stations")
    weather   = download_all_parquets("weather", prefix="weather_")
    return snapshots, stations, weather


# PECIKLI
def transform(snapshots, stations, weather):
    # zdruzi postaje pa njihove snapshote
    df = snapshots.merge(stations[["id", "number", "name", "latitude", "longitude", "capacity"]],
                         left_on="station_id", right_on="id",
                         how="left")
    
    # popravi timezone
    df["recorded_at"] = pd.to_datetime(df["recorded_at"], utc=True)
    df["recorded_at_local"] = df["recorded_at"].dt.tz_convert("Europe/Ljubljana")

    # zdruzi vreme z najbljizjim timestampom
    weather["recorded_at"] = pd.to_datetime(weather["recorded_at"], utc=True)
    df = pd.merge_asof(
        df.sort_values("recorded_at"),
        weather[["recorded_at", "temperature", "wind_speed", "rain", "condition"]].sort_values("recorded_at"),
        on="recorded_at",
        direction="nearest"
    )

    df["rain"] = df["rain"].fillna(0)

    return df



def upload_silver(df, folder):
    local_path = "latest.parquet"
    df.to_parquet(local_path, index=False)

    with open(local_path, "rb") as f:
        try:
            supabase.storage.from_("silver").remove([f"{folder}/latest.parquet"])
        except:
            pass
        supabase.storage.from_("silver").upload(f"{folder}/latest.parquet", f)

    os.remove(local_path)
    print(f"Uploaded silver/{folder}/latest.parquet with {len(df)} rows")


# AVTOBUSI
def transform_buses():
    trips  = download_all_parquets("buses", prefix="trips_")
    delays = download_all_parquets("buses", prefix="delays_")
    weather = download_all_parquets("weather", prefix="weather_")

    trips = trips[trips["trip_id"].notna()]

    # join delays z trips da dobis route_id in recorded_at
    df = delays.merge(trips[["id", "route_id", "bearing", "current_stop_id", "recorded_at"]],
                  left_on="trip_snapshot_id", right_on="id",
                  how="left")
    
    df = df.dropna(subset=["recorded_at"])

    # fix timezone
    df["recorded_at"] = pd.to_datetime(df["recorded_at"], utc=True)
    df["recorded_at_local"] = df["recorded_at"].dt.tz_convert("Europe/Ljubljana")

    df = df[["route_id", "stop_sequence", "delay_seconds", "bearing", "current_stop_id", "recorded_at", "recorded_at_local"]]

    # pridruzimo vreme po najblizjem casu
    weather["recorded_at"] = pd.to_datetime(weather["recorded_at"], utc=True)
    df = pd.merge_asof(
        df.sort_values("recorded_at"),
        weather[["recorded_at", "temperature", "wind_speed", "rain"]].sort_values("recorded_at"),
        on="recorded_at",
        direction="nearest"
    )

    df["rain"] = df["rain"].fillna(0)
    return df



if __name__ == "__main__":
    print("Loading bronze bikes...")
    snapshots, stations, weather = load_bronze()
    print(f"Snapshots: {len(snapshots)}, Stations: {len(stations)}, Weather: {len(weather)}")
    df_bikes = transform(snapshots, stations, weather)
    upload_silver(df_bikes, "bikes")

    print("Loading bronze buses...")
    df_buses = transform_buses()
    print(f"Bus rows: {len(df_buses)}")
    upload_silver(df_buses, "buses")




