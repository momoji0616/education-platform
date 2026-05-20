import re
from pathlib import Path

import pymysql


ROOT = Path(__file__).resolve().parents[2]
SQL_FILE = ROOT / "backend" / "sql" / "piclass2.sql"
TARGET_DB = "education_legacy_piclass"
IMPORT_TABLES = {"user", "class", "student"}


def get_connection(database=None):
    conn = pymysql.connect(
        host="localhost",
        user="root",
        password="123456",
        database=database,
        charset="utf8mb4",
        autocommit=False,
    )
    with conn.cursor() as cur:
        cur.execute("SET SESSION sql_mode=''")
        cur.execute("SET SESSION foreign_key_checks=0")
    conn.commit()
    return conn


def import_tables():
    drop_re = re.compile(r"DROP TABLE IF EXISTS `([^`]+)`;")
    create_re = re.compile(r"CREATE TABLE `([^`]+)`")
    insert_re = re.compile(r"INSERT INTO `([^`]+)` VALUES ")

    drops = {}
    creates = {}
    inserts = {table: [] for table in IMPORT_TABLES}
    capture_create = False
    current_create = []
    current_table = None
    order = ["user", "class", "student"]

    conn = get_connection(TARGET_DB)
    try:
        with SQL_FILE.open("r", encoding="utf-8", errors="ignore") as f:
            for raw_line in f:
                line = raw_line.rstrip("\n")
                m = drop_re.search(line)
                if m and m.group(1) in IMPORT_TABLES:
                    drops[m.group(1)] = line
                    continue
                if not capture_create:
                    m = create_re.search(line)
                    if m and m.group(1) in IMPORT_TABLES:
                        capture_create = True
                        current_table = m.group(1)
                        current_create = [line]
                        continue
                else:
                    current_create.append(line)
                    if line.strip().endswith(";"):
                        creates[current_table] = "\n".join(current_create)
                        capture_create = False
                        current_create = []
                        current_table = None
                    continue

                m = insert_re.search(line)
                if m and m.group(1) in IMPORT_TABLES:
                    inserts[m.group(1)].append(line)

        stats = {"drop": 0, "create": 0, "insert": 0}
        with conn.cursor() as cur:
            for table in reversed(order):
                if table in drops:
                    cur.execute(drops[table])
                    stats["drop"] += 1
            for table in order:
                if table in creates:
                    cur.execute(creates[table])
                    stats["create"] += 1
            for table in order:
                for stmt in inserts[table]:
                    cur.execute(stmt)
                    stats["insert"] += 1
        conn.commit()

        counts = {}
        with conn.cursor() as cur:
            for table in order:
                cur.execute(f"SELECT COUNT(*) FROM `{table}`")
                counts[table] = cur.fetchone()[0]
        return stats, counts
    finally:
        conn.close()


if __name__ == "__main__":
    stats, counts = import_tables()
    print("IMPORT_STATS", stats)
    print("ROW_COUNTS", counts)
