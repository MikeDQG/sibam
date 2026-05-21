import os
import pandas as pd
import psycopg2
from supabase import create_client
from datetime import date
from dotenv import load_dotenv

load_dotenv()

DB_URL = os.getenv("SUPABASE_DB_URL")
SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_KEY")

supabase = create_client(SUPABASE_URL, SUPABASE_KEY)

BIKES_QUERY = """
SELECT *
FROM bike_station_snapshots
ORDER BY recorded_at
"""

WEATHER_QUERY = """
SELECT *
FROM weather_snapshots
ORDER BY recorded_at
"""

BIKE_STATIONS_QUERY = """
SELECT *
FROM bike_stations
"""

TRIP_SNAPSHOTS_QUERY = """
SELECT *
FROM trip_snapshots
ORDER BY recorded_at
"""

STOP_DELAY_QUERY = """
SELECT *
FROM stop_delay_snapshots
"""


def export_table(query, bucket_folder, filename):
    connection = psycopg2.connect(DB_URL)
    df = pd.read_sql(query, connection)
    connection.close()

    print(f"Loaded {len(df)} rows")
    
    local_path = f"{filename}.parquet"
    df.to_parquet(local_path, index=False)

    with open(local_path, "rb") as f:
        try:
            supabase.storage.from_("bronze").remove([f"{bucket_folder}/{local_path}"])
        except:
            pass
        supabase.storage.from_("bronze").upload(f"{bucket_folder}/{local_path}", f)

    print(f"Uploaded to bronze/{bucket_folder}/{local_path}")
    os.remove(local_path)


if __name__ == "__main__":
    today = date.today()
    export_table(BIKE_STATIONS_QUERY, "bikes",   "stations")
    export_table(BIKES_QUERY,         "bikes",   f"snapshots_{today}")
    export_table(WEATHER_QUERY,       "weather", f"weather_{today}")
    export_table(TRIP_SNAPSHOTS_QUERY, "buses",  f"trips_{today}")
    export_table(STOP_DELAY_QUERY,     "buses",  f"delays_{today}")
