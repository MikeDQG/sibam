import os
import pandas as pd
import numpy as np
from supabase import create_client
from dotenv import load_dotenv
import io
from sklearn.ensemble import GradientBoostingRegressor, GradientBoostingClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, roc_auc_score
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

load_dotenv()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_KEY")
supabase = create_client(SUPABASE_URL, SUPABASE_KEY)

FEATURES = ["number", "hour", "day_of_week", "is_weekend",
            "temperature", "rain", "wind_speed"]

TARGET_BIKES           = "bikes"
TARGET_STANDS          = "stands"
TARGET_AVAILABLE_BIKE  = "bike_available"
TARGET_AVAILABLE_STAND = "stand_available"


def load_silver():
    data = supabase.storage.from_("silver").download("bikes/latest.parquet")
    return pd.read_parquet(io.BytesIO(data))


def build_features(df):
    df["recorded_at_local"] = pd.to_datetime(df["recorded_at_local"])
    df["hour"]          = df["recorded_at_local"].dt.hour
    df["day_of_week"]   = df["recorded_at_local"].dt.dayofweek
    df["is_weekend"]    = (df["day_of_week"] >= 5).astype(int)
    df["bike_available"]  = (df["bikes"] >= 2).astype(int)
    df["stand_available"] = (df["stands"] >= 2).astype(int)
    return df


def upload_to_gold(onnx_bytes, filename):
    path = f"models/{filename}"
    try:
        supabase.storage.from_("gold").remove([path])
    except Exception:
        pass
    supabase.storage.from_("gold").upload(path, onnx_bytes, {"content-type": "application/octet-stream"})
    print(f"Naloženo → gold/{path}")


def train_regressor(df, target):
    X = df[FEATURES].astype(np.float32)
    y = df[target].astype(np.float32)

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    model = GradientBoostingRegressor(n_estimators=200, max_depth=4, learning_rate=0.05)
    model.fit(X_train, y_train)

    mae = mean_absolute_error(y_test, model.predict(X_test))
    print(f"{target}: MAE = {mae:.2f}  (test set: {len(X_test)} samples)")

    initial_type = [("float_input", FloatTensorType([None, len(FEATURES)]))]
    return convert_sklearn(model, initial_types=initial_type).SerializeToString()


def train_classifier(df, target):
    X = df[FEATURES].astype(np.float32)
    y = df[target].astype(int)

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    model = GradientBoostingClassifier(n_estimators=200, max_depth=4, learning_rate=0.05)
    model.fit(X_train, y_train)

    auc = roc_auc_score(y_test, model.predict_proba(X_test)[:, 1])
    print(f"{target}: ROC-AUC = {auc:.3f}")

    initial_type = [("float_input", FloatTensorType([None, len(FEATURES)]))]
    return convert_sklearn(model, initial_types=initial_type).SerializeToString()


if __name__ == "__main__":
    print("Nalaganje silver podatkov...")
    df = load_silver()
    df = build_features(df)
    print(f"Vrstic: {len(df)}")

    print("\nTrening modelov...")
    upload_to_gold(train_regressor(df, TARGET_BIKES),           "model_bikes.onnx")
    upload_to_gold(train_regressor(df, TARGET_STANDS),          "model_stands.onnx")
    upload_to_gold(train_classifier(df, TARGET_AVAILABLE_STAND), "model_available_stand.onnx")
    upload_to_gold(train_classifier(df, TARGET_AVAILABLE_BIKE),  "model_available_bike.onnx")

    print("\nKončano.")
