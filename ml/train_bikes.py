import os
import pandas as pd
import numpy as np
from supabase import create_client
from dotenv import load_dotenv
import io
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType


load_dotenv()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_KEY")
supabase = create_client(SUPABASE_URL, SUPABASE_KEY)

def load_silver():
    data = supabase.storage.from_("silver").download("bikes/latest.parquet")
    return pd.read_parquet(io.BytesIO(data))
    
def build_features(df):
    df["recorded_at_local"] = pd.to_datetime(df["recorded_at_local"])
    df["hour"]        = df["recorded_at_local"].dt.hour
    df["day_of_week"] = df["recorded_at_local"].dt.dayofweek
    df["is_weekend"]  = (df["day_of_week"] >= 5).astype(int)
    return df

FEATURES = ["number", "hour", "day_of_week", "is_weekend",
            "temperature", "rain", "wind_speed"]

TARGET_BIKES  = "bikes"
TARGET_STANDS = "stands"


def train_and_export(df, target, output_path):
    X = df[FEATURES].astype(np.float32)
    y = df[target].astype(np.float32)

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    model= GradientBoostingRegressor(n_estimators=200, max_depth=4, learning_rate=0.05)

    model.fit(X_train, y_train)

    mae = mean_absolute_error(y_test, model.predict(X_test))
    print(f"{target}: MAE = {mae:.2f} (test set of {len(X_test)} samples)")

    initial_type = [("float_input", FloatTensorType([None, len(FEATURES)]))]
    onnx_model = convert_sklearn(model, initial_types=initial_type)
    with open(output_path, "wb") as f:
        f.write(onnx_model.SerializeToString())
    print(f"Saved -> {output_path}")



if __name__ == "__main__":
    out_dir = "../backend/src/main/resources/models"
    os.makedirs(out_dir, exist_ok=True)

    print("Loading silver...")
    df = load_silver()
    df = build_features(df)
    print(f"Rows: {len(df)}")

    train_and_export(df, TARGET_BIKES,  f"{out_dir}/model_bikes.onnx")
    train_and_export(df, TARGET_STANDS, f"{out_dir}/model_stands.onnx")
