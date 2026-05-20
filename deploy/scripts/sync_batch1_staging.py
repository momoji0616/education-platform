import json
import re
from collections import defaultdict

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


def normalize_text(value):
    if value is None:
        return ""
    return str(value).replace("\r\n", "\n").replace("\r", "\n").strip()


def parse_options(question_text):
    text = normalize_text(question_text)
    if not text:
        return "", None, False

    lines = [line.strip() for line in text.split("\n") if line.strip()]
    option_pattern = re.compile(r"^[\(（]?([A-H])[\)）]?[\.:：．、]?\s*(.*)$")
    stem_lines = []
    options = []
    found_option = False

    for line in lines:
        match = option_pattern.match(line)
        if match:
            found_option = True
            options.append({"label": match.group(1), "content": match.group(2).strip()})
        else:
            if found_option:
                # option continuation line
                if options:
                    options[-1]["content"] = f"{options[-1]['content']} {line}".strip()
            else:
                stem_lines.append(line)

    stem = "\n".join(stem_lines).strip() if stem_lines else text
    if not options:
        return stem or text, None, False
    return stem, options, True


def truncate_staging_tables(conn):
    tables = [
        "legacy_staging_student_answer",
        "legacy_staging_question_bank",
        "legacy_staging_question_catalog",
    ]
    with conn.cursor() as cur:
        for table in tables:
            cur.execute(f"TRUNCATE TABLE `{table}`")
    conn.commit()


def sync_catalogs(conn):
    read_sql = """
        SELECT id, id1, chapter, title, username, count, type, `limit`, time
        FROM tk_choice1
        ORDER BY id ASC
    """
    insert_sql = """
        INSERT INTO legacy_staging_question_catalog
        (source_system, source_id, source_catalog_id, source_chapter_code, catalog_name,
         question_count, question_type, owner_username, raw_type, raw_limit, raw_payload)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    stats = {"read": 0, "inserted": 0, "skipped": 0}
    with conn.cursor() as cur:
        cur.execute(read_sql)
        rows = cur.fetchall()
        stats["read"] = len(rows)
        for row in rows:
            catalog_name = normalize_text(row["title"])
            if not row["id1"] or not normalize_text(row["chapter"]) or not catalog_name:
                stats["skipped"] += 1
                continue
            payload = {
                "id": row["id"],
                "id1": row["id1"],
                "chapter": row["chapter"],
                "title": row["title"],
                "username": row["username"],
                "count": row["count"],
                "type": row["type"],
                "limit": row["limit"],
                "time": str(row["time"]) if row["time"] else None,
            }
            cur.execute(
                insert_sql,
                (
                    "piclass2",
                    row["id"],
                    row["id1"],
                    normalize_text(row["chapter"]),
                    catalog_name,
                    int(row["count"] or 0),
                    "choice",
                    normalize_text(row["username"]) or None,
                    str(row["type"]) if row["type"] is not None else None,
                    normalize_text(row["limit"]) or None,
                    to_json(payload),
                ),
            )
            stats["inserted"] += 1
    conn.commit()
    return stats


def build_catalog_maps(conn):
    catalog_map = {}
    course_map = defaultdict(list)
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT source_catalog_id, source_chapter_code, catalog_name
            FROM legacy_staging_question_catalog
            """
        )
        for row in cur.fetchall():
            key = (int(row["source_catalog_id"]), str(row["source_chapter_code"]))
            catalog_map[key] = row["catalog_name"]
            course_map[int(row["source_catalog_id"])].append(
                (str(row["source_chapter_code"]), row["catalog_name"])
            )

    course_name_map = {}
    for catalog_id, entries in course_map.items():
        preferred = None
        for chapter_code, name in entries:
            if chapter_code in {"0", "*"}:
                preferred = name
                break
        course_name_map[catalog_id] = preferred or entries[0][1]
    return catalog_map, course_name_map


def sync_questions(conn):
    catalog_map, course_name_map = build_catalog_maps(conn)
    read_sql = """
        SELECT id, id1, id2, chapter, question, ans, jx, notice, kb, link
        FROM tk_choice2
        ORDER BY id ASC
    """
    insert_sql = """
        INSERT INTO legacy_staging_question_bank
        (source_system, source_question_id, source_catalog_id, source_question_no, question_type,
         course_name, chapter_code, chapter_name, question_stem, options_json, standard_answer,
         analysis, knowledge_point, owner_username, raw_content, raw_payload)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    stats = {"read": 0, "inserted": 0, "parse_failed": 0, "missing_answer": 0}
    with conn.cursor() as cur:
        cur.execute(read_sql)
        rows = cur.fetchall()
        stats["read"] = len(rows)
        for row in rows:
            question_text = normalize_text(row["question"])
            answer = normalize_text(row["ans"])
            if not answer:
                stats["missing_answer"] += 1
                continue
            stem, options, parsed = parse_options(question_text)
            if not parsed:
                stats["parse_failed"] += 1
            source_catalog_id = int(row["id1"])
            chapter_code = normalize_text(row["chapter"])
            payload = {
                "id": row["id"],
                "id1": row["id1"],
                "id2": row["id2"],
                "chapter": row["chapter"],
                "question": row["question"],
                "ans": row["ans"],
                "jx": row["jx"],
                "notice": row["notice"],
                "kb": row["kb"],
                "link": row["link"],
            }
            cur.execute(
                insert_sql,
                (
                    "piclass2",
                    row["id"],
                    source_catalog_id,
                    row["id2"],
                    "choice",
                    course_name_map.get(source_catalog_id),
                    chapter_code,
                    catalog_map.get((source_catalog_id, chapter_code)),
                    stem,
                    to_json(options) if options else None,
                    answer,
                    normalize_text(row["jx"]) or None,
                    normalize_text(row["kb"]) or None,
                    None,
                    question_text,
                    to_json(payload),
                ),
            )
            stats["inserted"] += 1
    conn.commit()
    return stats


def build_question_lookup(conn):
    lookup = {}
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT source_question_id, source_catalog_id, source_question_no
            FROM legacy_staging_question_bank
            """
        )
        for row in cur.fetchall():
            lookup[(int(row["source_catalog_id"]), int(row["source_question_no"]))] = int(row["source_question_id"])
    return lookup


def sync_answers(conn):
    question_lookup = build_question_lookup(conn)
    read_sql = """
        SELECT id, id1, id2, username, ans, score, zy_id, time
        FROM dt_choice
        ORDER BY id ASC
    """
    insert_sql = """
        INSERT INTO legacy_staging_student_answer
        (source_system, source_record_id, student_no, answer_type, source_catalog_id,
         source_question_no, source_question_id, assignment_source_id, answer_content,
         standardized_score, is_correct, submit_time, raw_payload)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    stats = {"read": 0, "inserted": 0, "skipped": 0, "missing_question": 0}
    with conn.cursor() as cur:
        cur.execute(read_sql)
        rows = cur.fetchall()
        stats["read"] = len(rows)
        for row in rows:
            student_no = normalize_text(row["username"])
            if not student_no or row["id1"] is None or row["id2"] is None:
                stats["skipped"] += 1
                continue
            question_key = (int(row["id1"]), int(row["id2"]))
            source_question_id = question_lookup.get(question_key)
            if source_question_id is None:
                stats["missing_question"] += 1
            score = float(row["score"] or 0)
            payload = {
                "id": row["id"],
                "id1": row["id1"],
                "id2": row["id2"],
                "username": row["username"],
                "ans": row["ans"],
                "score": row["score"],
                "zy_id": row["zy_id"],
                "time": str(row["time"]) if row["time"] else None,
            }
            cur.execute(
                insert_sql,
                (
                    "piclass2",
                    row["id"],
                    student_no,
                    "choice",
                    int(row["id1"]),
                    int(row["id2"]),
                    source_question_id,
                    row["zy_id"],
                    normalize_text(row["ans"]) or None,
                    score,
                    1 if score > 0 else 0,
                    row["time"],
                    to_json(payload),
                ),
            )
            stats["inserted"] += 1
    conn.commit()
    return stats


def verify_counts(conn):
    tables = [
        "legacy_staging_question_catalog",
        "legacy_staging_question_bank",
        "legacy_staging_student_answer",
    ]
    result = {}
    with conn.cursor() as cur:
        for table in tables:
            cur.execute(f"SELECT COUNT(*) AS c FROM `{table}`")
            result[table] = cur.fetchone()["c"]
    return result


def main():
    conn = get_connection()
    try:
        truncate_staging_tables(conn)
        catalog_stats = sync_catalogs(conn)
        question_stats = sync_questions(conn)
        answer_stats = sync_answers(conn)
        counts = verify_counts(conn)
        print("CATALOG_STATS", catalog_stats)
        print("QUESTION_STATS", question_stats)
        print("ANSWER_STATS", answer_stats)
        print("STAGING_COUNTS", counts)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
