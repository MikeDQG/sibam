import os
import pandas as pd
import numpy as np
import psycopg2
from dotenv import load_dotenv

load_dotenv()
DB_URL = os.getenv("SUPABASE_DB_URL")

QUERY = """
    SELECT
"""