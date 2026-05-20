import json

import pymysql


DB_NAME = "education_legacy_piclass"


def get_connection():
    conn = pymysql.connect(
        host="localhost",
        user="root",
        password="123456",
        database=DB_NAME,
        charset="utf8mb4",
        autocommit=False,
        cursorclass=pymysql.cursors.DictCursor,
    )
    with conn.cursor() as cur:
        cur.execute("SET SESSION sql_mode=''")
        cur.execute("SET SESSION foreign_key_checks=0")
    conn.commit()
    return conn


def to_json(data):
    return json.dumps(data, ensure_ascii=False)


def normalize(value):
    if value is None:
        return ""
    return str(value).strip()


def truncate_tables(conn):
    with conn.cursor() as cur:
        cur.execute("TRUNCATE TABLE legacy_staging_student_profile")
        cur.execute("TRUNCATE TABLE legacy_staging_class")
    conn.commit()


def sync_classes(conn):
    stats = {"read": 0, "inserted": 0, "skipped": 0}
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id, username, teacher, class_name, class_time, class_address, class, type, notice
            FROM class
            ORDER BY id ASC
            """
        )
        rows = cur.fetchall()
        stats["read"] = len(rows)
        for row in rows:
            class_code = normalize(row["class"])
            if not class_code:
                stats["skipped"] += 1
                continue
            payload = {k: row[k] for k in row}
            cur.execute(
                """
                INSERT INTO legacy_staging_class
                (source_system, source_id, class_code, class_name, teacher_no, creator_no,
                 class_time, class_address, class_type, notice, raw_payload)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    "piclass2",
                    row["id"],
                    class_code,
                    normalize(row["class_name"]) or class_code,
                    normalize(row["teacher"]) or None,
                    normalize(row["username"]) or None,
                    normalize(row["class_time"]) or None,
                    normalize(row["class_address"]) or None,
                    normalize(row["type"]) or None,
                    normalize(row["notice"]) or None,
                    to_json(payload),
                ),
            )
            stats["inserted"] += 1
    conn.commit()
    return stats


def sync_students(conn):
    stats = {"read": 0, "inserted": 0, "skipped": 0, "duplicate_student_no": 0}
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id, username, name, major, class, class2, seat, notice
            FROM student
            ORDER BY id ASC
            """
        )
        rows = cur.fetchall()
        stats["read"] = len(rows)
        dedup = {}
        for row in rows:
            student_no = normalize(row["username"])
            class_code = normalize(row["class"])
            if not student_no or not class_code:
                stats["skipped"] += 1
                continue
            current = dedup.get(student_no)
            if current is None:
                dedup[student_no] = row
                continue
            stats["duplicate_student_no"] += 1
            current_score = (
                (1 if normalize(current["major"]) else 0),
                (1 if normalize(current["class2"]) else 0),
                (1 if current["seat"] is not None else 0),
                int(current["id"] or 0),
            )
            next_score = (
                (1 if normalize(row["major"]) else 0),
                (1 if normalize(row["class2"]) else 0),
                (1 if row["seat"] is not None else 0),
                int(row["id"] or 0),
            )
            if next_score > current_score:
                dedup[student_no] = row
        for row in dedup.values():
            student_no = normalize(row["username"])
            class_code = normalize(row["class"])
            payload = {k: row[k] for k in row}
            cur.execute(
                """
                INSERT INTO legacy_staging_student_profile
                (source_system, source_id, student_no, student_name, major_name,
                 class_code, class_code_ext, seat_no, remark, raw_payload)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    "piclass2",
                    row["id"],
                    student_no,
                    normalize(row["name"]) or None,
                    normalize(row["major"]) or None,
                    class_code,
                    normalize(row["class2"]) or None,
                    row["seat"],
                    normalize(row["notice"]) or None,
                    to_json(payload),
                ),
            )
            stats["inserted"] += 1
    conn.commit()
    return stats


def verify(conn):
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) AS c FROM legacy_staging_class")
        c1 = cur.fetchone()["c"]
        cur.execute("SELECT COUNT(*) AS c FROM legacy_staging_student_profile")
        c2 = cur.fetchone()["c"]
    return {"legacy_staging_class": c1, "legacy_staging_student_profile": c2}


if __name__ == "__main__":
    conn = get_connection()
    try:
        truncate_tables(conn)
        class_stats = sync_classes(conn)
        student_stats = sync_students(conn)
        counts = verify(conn)
        print("CLASS_STATS", class_stats)
        print("STUDENT_STATS", student_stats)
        print("STAGING_COUNTS", counts)
    finally:
        conn.close()
