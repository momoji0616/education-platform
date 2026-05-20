import re
import sys
from pathlib import Path

import pymysql


ROOT = Path(__file__).resolve().parents[2]
SQL_FILE = ROOT / "backend" / "sql" / "piclass2.sql"
TARGET_DB = "education_legacy_piclass"
TARGET_TABLES = {"tk_choice1", "tk_choice2", "dt_choice"}
DEPENDENCY_TABLES = {"user"}
IMPORT_TABLES = TARGET_TABLES | DEPENDENCY_TABLES


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


def ensure_database():
    conn = get_connection()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "CREATE DATABASE IF NOT EXISTS education_legacy_piclass "
                "DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
            )
        conn.commit()
    finally:
        conn.close()


def import_selected_tables():
    ensure_database()
    conn = get_connection(TARGET_DB)
    stats = {
        "drop": 0,
        "create": 0,
        "insert": 0,
    }

    drop_re = re.compile(r"DROP TABLE IF EXISTS `([^`]+)`;")
    create_re = re.compile(r"CREATE TABLE `([^`]+)`")
    insert_re = re.compile(r"INSERT INTO `([^`]+)` VALUES ")
    drops = {}
    creates = {}
    inserts = {table: [] for table in IMPORT_TABLES}
    current_create = []
    capture_create = False
    current_table = None
    create_order = ["user", "tk_choice1", "tk_choice2", "dt_choice"]

    try:
        with SQL_FILE.open("r", encoding="utf-8", errors="ignore") as f:
            for raw_line in f:
                line = raw_line.rstrip("\n")

                drop_match = drop_re.search(line)
                if drop_match and drop_match.group(1) in IMPORT_TABLES:
                    drops[drop_match.group(1)] = line
                    continue

                if not capture_create:
                    create_match = create_re.search(line)
                    if create_match and create_match.group(1) in IMPORT_TABLES:
                        capture_create = True
                        current_table = create_match.group(1)
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

                insert_match = insert_re.search(line)
                if insert_match and insert_match.group(1) in IMPORT_TABLES:
                    inserts[insert_match.group(1)].append(line)

        with conn.cursor() as cur:
            for table in reversed(create_order):
                stmt = drops.get(table)
                if stmt:
                    cur.execute(stmt)
                    stats["drop"] += 1

            for table in create_order:
                stmt = creates.get(table)
                if stmt:
                    cur.execute(stmt)
                    stats["create"] += 1

            for table in create_order:
                for stmt in inserts.get(table, []):
                    cur.execute(stmt)
                    stats["insert"] += 1

        conn.commit()
        return stats
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def verify():
    conn = get_connection(TARGET_DB)
    try:
        result = {}
        with conn.cursor() as cur:
            for table in sorted(IMPORT_TABLES):
                cur.execute(f"SELECT COUNT(*) FROM `{table}`")
                result[table] = cur.fetchone()[0]
        return result
    finally:
        conn.close()


def main():
    if not SQL_FILE.exists():
        print(f"SQL file not found: {SQL_FILE}")
        sys.exit(1)

    stats = import_selected_tables()
    counts = verify()
    print("IMPORT_STATS", stats)
    print("ROW_COUNTS", counts)


if __name__ == "__main__":
    main()
