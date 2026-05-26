import os
import json
import io
import pandas as pd
import numpy as np
from supabase import create_client
from dotenv import load_dotenv
from sklearn.ensemble import GradientBoostingRegressor, RandomForestRegressor
from sklearn.linear_model import Ridge
from sklearn.metrics import mean_absolute_error
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

load_dotenv()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_KEY")
supabase = create_client(SUPABASE_URL, SUPABASE_KEY)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

FEATURES = [
    "route_id_enc",   # katera linija
    "stop_sequence",  # katera postaja na liniji
    "hour",           # ura
    "day_of_week",    # dan v tednu
    "is_weekend",     # vikend flag
    "temperature",    # temperatura
    "rain",           # padavine
    "wind_speed",     # hitrost vetra
    "direction",      # smer linije (0, 1, 2, ...)
]
TARGET = "delay_seconds"


def load_silver():
    data = supabase.storage.from_("silver").download("buses/latest.parquet")
    return pd.read_parquet(io.BytesIO(data))


def load_direction_mapping():
    with open(os.path.join(SCRIPT_DIR, "stop_direction_mapping.json")) as f:
        raw = json.load(f)
    lookup = {}
    for key, direction in raw.items():
        route_id, stop_id = key.split("_", 1)
        lookup[(str(route_id), str(stop_id))] = direction
    return lookup


def build_features(df, dir_lookup):
    df = df.copy()
    df["recorded_at_local"] = pd.to_datetime(df["recorded_at_local"])
    df["recorded_at"]  = pd.to_datetime(df["recorded_at"], utc=True)
    df["hour"]         = df["recorded_at_local"].dt.hour
    df["day_of_week"]  = df["recorded_at_local"].dt.dayofweek
    df["is_weekend"]   = (df["day_of_week"] >= 5).astype(int)
    df["direction"]    = df.apply(
        lambda row: dir_lookup.get((str(row["route_id"]), str(row["current_stop_id"])), -1),
        axis=1,
    )
    df["route_id_enc"] = df["route_id"].astype(int)
    return df


def clean(df):
    return df[
        (df["delay_seconds"] >= -900) &
        (df["delay_seconds"] <= 3600) &
        (df["direction"] != -1) &
        (df["hour"] != 0)
    ].copy()


def upload_to_gold(onnx_bytes, filename):
    path = f"models/{filename}"
    try:
        supabase.storage.from_("gold").remove([path])
    except Exception:
        pass
    supabase.storage.from_("gold").upload(path, onnx_bytes, {"content-type": "application/octet-stream"})
    print(f"Naloženo → gold/{path}")


def train_and_export(df_model):
    # razdelimo po datumu — ne naključno, da preprečimo data leakage
    df_sorted = df_model.sort_values("recorded_at")
    split_idx = int(len(df_sorted) * 0.8)
    train_df  = df_sorted.iloc[:split_idx]
    test_df   = df_sorted.iloc[split_idx:]

    print(f"Trening: {len(train_df)} vrstic  (do  {train_df['recorded_at'].max()})")
    print(f"Test:    {len(test_df)} vrstic  (od  {test_df['recorded_at'].min()})")

    X_train = train_df[FEATURES].astype(np.float32)
    y_train = train_df[TARGET].astype(np.float32)
    X_test  = test_df[FEATURES].astype(np.float32)
    y_test  = test_df[TARGET].astype(np.float32)

    models = {
        "Ridge":            Ridge(),
        "RandomForest":     RandomForestRegressor(
                                n_estimators=200, max_depth=20,
                                max_features="log2", random_state=42, n_jobs=-1),
        "GradientBoosting": GradientBoostingRegressor(
                                n_estimators=100, max_depth=4,
                                learning_rate=0.1, random_state=42),
    }

    mae_results = {}
    for name, model in models.items():
        print(f"Treniram {name}...")
        model.fit(X_train, y_train)
        mae = mean_absolute_error(y_test, model.predict(X_test))
        mae_results[name] = mae
        print(f"  {name:25s}  MAE = {mae:.1f}s")

    best_name  = min(mae_results, key=mae_results.get)
    best_model = models[best_name]
    print(f"\nZmagovalec: {best_name}  (MAE = {mae_results[best_name]:.1f}s)")

    # izvoz v ONNX
    initial_type = [("float_input", FloatTensorType([None, len(FEATURES)]))]
    onnx_bytes   = convert_sklearn(best_model, initial_types=initial_type).SerializeToString()

    print(f"Vrstni red značilk: {FEATURES}")
    return onnx_bytes


if __name__ == "__main__":
    print("Nalaganje silver podatkov...")
    df = load_silver()
    print(f"Vrstic: {len(df)}")

    print("Nalaganje direction mappinga...")
    dir_lookup = load_direction_mapping()

    print("Gradnja značilk...")
    df = build_features(df, dir_lookup)
    df = clean(df)
    print(f"Vrstic za trening: {len(df)}")

    print("\nTrening modelov...")
    onnx_bytes = train_and_export(df)

    print("\nNalaganje modela v Supabase gold...")
    upload_to_gold(onnx_bytes, "model_bus_delay.onnx")

    print("\nKončano.")
