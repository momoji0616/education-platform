import json

import pymysql


DB_NAME = "education_legacy_piclass"
PROGRAM_LANGUAGE_MAP = {
    1: "未知语言",
    2: "C/C++",
    3: "Java",
    4: "Python",
    5: "C#",
}


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


def normalize_text(value):
    if value is None:
        return ""
    return str(value).replace("\r\n", "\n").replace("\r", "\n").strip()


def to_json(data):
    return json.dumps(data, ensure_ascii=False)


def truncate_batch2_staging(conn):
    with conn.cursor() as cur:
        cur.execute("DELETE FROM legacy_staging_student_answer WHERE answer_type = 'program'")
        cur.execute("DELETE FROM legacy_staging_question_bank WHERE question_type = 'program'")
        cur.execute("TRUNCATE TABLE legacy_staging_assignment_score")
        cur.execute("TRUNCATE TABLE legacy_staging_assignment_question")
        cur.execute("TRUNCATE TABLE legacy_staging_assignment")
    conn.commit()


def sync_program_questions(conn):
    read_sql = """
        SELECT time, id, type, lang, chapter, title, content, code, jx, input, output, username, sample
        FROM tk_program
        ORDER BY id ASC
    """
    insert_sql = """
        INSERT INTO legacy_staging_question_bank
        (source_system, source_question_id, source_catalog_id, source_question_no, question_type,
         course_name, chapter_code, chapter_name, question_stem, standard_answer, analysis,
         program_language, sample_input, sample_output, reference_code, owner_username, raw_content, raw_payload)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    stats = {"read": 0, "inserted": 0}
    with conn.cursor() as cur:
        cur.execute(read_sql)
        rows = cur.fetchall()
        stats["read"] = len(rows)
        for row in rows:
            chapter_code = str(row["chapter"]) if row["chapter"] is not None else ""
            payload = {
                "time": str(row["time"]) if row["time"] else None,
                "id": row["id"],
                "type": row["type"],
                "lang": row["lang"],
                "chapter": row["chapter"],
                "title": row["title"],
                "content": row["content"],
                "code": row["code"],
                "jx": row["jx"],
                "input": row["input"],
                "output": row["output"],
                "username": row["username"],
                "sample": row["sample"],
            }
            cur.execute(
                insert_sql,
                (
                    "piclass2",
                    row["id"],
                    row["chapter"],
                    row["id"],
                    "program",
                    "程序设计",
                    chapter_code,
                    f"编程题章节{chapter_code}" if chapter_code else "编程题",
                    normalize_text(row["title"]),
                    normalize_text(row["code"]) or None,
                    normalize_text(row["jx"]) or None,
                    PROGRAM_LANGUAGE_MAP.get(int(row["lang"] or 0), f"Lang-{row['lang']}"),
                    normalize_text(row["input"]) or None,
                    normalize_text(row["output"]) or None,
                    normalize_text(row["code"]) or None,
                    normalize_text(row["username"]) or None,
                    normalize_text(row["content"]) or None,
                    to_json(payload),
                ),
            )
            stats["inserted"] += 1
    conn.commit()
    return stats


def sync_program_answers(conn):
    read_sql = """
        SELECT time, id, id1, username, score, notice, code, zy_id
        FROM dt_program
        ORDER BY id ASC
    """
    insert_sql = """
        INSERT INTO legacy_staging_student_answer
        (source_system, source_record_id, student_no, answer_type, source_catalog_id, source_question_no,
         source_question_id, assignment_source_id, answer_content, raw_code, standardized_score, is_correct,
         teacher_feedback, submit_time, raw_payload)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    stats = {"read": 0, "inserted": 0, "skipped": 0}
    with conn.cursor() as cur:
        cur.execute(read_sql)
        rows = cur.fetchall()
        stats["read"] = len(rows)
        for row in rows:
            student_no = normalize_text(row["username"])
            if not student_no or row["id1"] is None:
                stats["skipped"] += 1
                continue
            score = float(row["score"] or 0)
            payload = {
                "time": str(row["time"]) if row["time"] else None,
                "id": row["id"],
                "id1": row["id1"],
                "username": row["username"],
                "score": row["score"],
                "notice": row["notice"],
                "code": row["code"],
                "zy_id": row["zy_id"],
            }
            cur.execute(
                insert_sql,
                (
                    "piclass2",
                    row["id"],
                    student_no,
                    "program",
                    row["id1"],
                    row["id1"],
                    row["id1"],
                    row["zy_id"],
                    normalize_text(row["notice"]) or None,
                    normalize_text(row["code"]) or None,
                    score,
                    1 if score > 0 else 0,
                    normalize_text(row["notice"]) or None,
                    row["time"],
                    to_json(payload),
                ),
            )
            stats["inserted"] += 1
    conn.commit()
    return stats


def sync_assignments(conn):
    read_sql = """
        SELECT id, time, username, class, title, notice, program, choice, office, file, deadline, state, `limit`
        FROM zy_assign
        ORDER BY id ASC
    """
    insert_sql = """
        INSERT INTO legacy_staging_assignment
        (source_system, source_id, teacher_no, class_code, title, description, program_count, choice_count,
         office_count, file_count, deadline, status, raw_limit, raw_payload)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    stats = {"read": 0, "inserted": 0}
    with conn.cursor() as cur:
        cur.execute(read_sql)
        rows = cur.fetchall()
        stats["read"] = len(rows)
        for row in rows:
            payload = {
                "id": row["id"],
                "time": str(row["time"]) if row["time"] else None,
                "username": row["username"],
                "class": row["class"],
                "title": row["title"],
                "notice": row["notice"],
                "program": row["program"],
                "choice": row["choice"],
                "office": row["office"],
                "file": row["file"],
                "deadline": str(row["deadline"]) if row["deadline"] else None,
                "state": row["state"],
                "limit": row["limit"],
            }
            cur.execute(
                insert_sql,
                (
                    "piclass2",
                    row["id"],
                    normalize_text(row["username"]) or None,
                    normalize_text(row["class"]) or None,
                    normalize_text(row["title"]) or None,
                    normalize_text(row["notice"]) or None,
                    int(row["program"] or 0),
                    int(row["choice"] or 0),
                    int(row["office"] or 0),
                    int(row["file"] or 0),
                    row["deadline"],
                    str(row["state"]) if row["state"] is not None else None,
                    normalize_text(row["limit"]) or None,
                    to_json(payload),
                ),
            )
            stats["inserted"] += 1
    conn.commit()
    return stats


def sync_assignment_questions(conn):
    read_sql = """
        SELECT id, time, zy_id, id1, id2, type, notice, huping
        FROM zy_detail
        ORDER BY id ASC
    """
    insert_sql = """
        INSERT INTO legacy_staging_assignment_question
        (source_system, source_id, assignment_source_id, question_source_id_1, question_source_id_2,
         question_type_code, remark, peer_review_flag, raw_payload)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    stats = {"read": 0, "inserted": 0}
    with conn.cursor() as cur:
        cur.execute(read_sql)
        rows = cur.fetchall()
        stats["read"] = len(rows)
        for row in rows:
            payload = {
                "id": row["id"],
                "time": str(row["time"]) if row["time"] else None,
                "zy_id": row["zy_id"],
                "id1": row["id1"],
                "id2": row["id2"],
                "type": row["type"],
                "notice": row["notice"],
                "huping": row["huping"],
            }
            cur.execute(
                insert_sql,
                (
                    "piclass2",
                    row["id"],
                    row["zy_id"],
                    row["id1"],
                    row["id2"],
                    str(row["type"]) if row["type"] is not None else None,
                    normalize_text(row["notice"]) or None,
                    int(row["huping"] or 0),
                    to_json(payload),
                ),
            )
            stats["inserted"] += 1
    conn.commit()
    return stats


def sync_assignment_scores(conn):
    read_sql = """
        SELECT time, id, username, zy_id, program, choice, office, file
        FROM zy_score
        ORDER BY id ASC
    """
    insert_sql = """
        INSERT INTO legacy_staging_assignment_score
        (source_system, source_id, student_no, assignment_source_id, program_score, choice_score,
         office_score, file_score, total_score, score_time, raw_payload)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    stats = {"read": 0, "inserted": 0, "skipped": 0}
    with conn.cursor() as cur:
        cur.execute(read_sql)
        rows = cur.fetchall()
        stats["read"] = len(rows)
        for row in rows:
            student_no = normalize_text(row["username"])
            if not student_no:
                stats["skipped"] += 1
                continue
            program_score = float(row["program"] or 0)
            choice_score = float(row["choice"] or 0)
            office_score = float(row["office"] or 0)
            file_score = float(row["file"] or 0)
            total_score = program_score + choice_score + office_score + file_score
            payload = {
                "time": str(row["time"]) if row["time"] else None,
                "id": row["id"],
                "username": row["username"],
                "zy_id": row["zy_id"],
                "program": row["program"],
                "choice": row["choice"],
                "office": row["office"],
                "file": row["file"],
            }
            cur.execute(
                insert_sql,
                (
                    "piclass2",
                    row["id"],
                    student_no,
                    row["zy_id"],
                    program_score,
                    choice_score,
                    office_score,
                    file_score,
                    total_score,
                    row["time"],
                    to_json(payload),
                ),
            )
            stats["inserted"] += 1
    conn.commit()
    return stats


def verify_counts(conn):
    tables = [
        "legacy_staging_question_bank",
        "legacy_staging_student_answer",
        "legacy_staging_assignment",
        "legacy_staging_assignment_question",
        "legacy_staging_assignment_score",
    ]
    result = {}
    with conn.cursor() as cur:
        for table in tables:
            if table in {"legacy_staging_question_bank", "legacy_staging_student_answer"}:
                if table == "legacy_staging_question_bank":
                    cur.execute("SELECT COUNT(*) AS c FROM legacy_staging_question_bank WHERE question_type = 'program'")
                else:
                    cur.execute("SELECT COUNT(*) AS c FROM legacy_staging_student_answer WHERE answer_type = 'program'")
            else:
                cur.execute(f"SELECT COUNT(*) AS c FROM `{table}`")
            result[table] = cur.fetchone()["c"]
    return result


def main():
    conn = get_connection()
    try:
        truncate_batch2_staging(conn)
        program_question_stats = sync_program_questions(conn)
        program_answer_stats = sync_program_answers(conn)
        assignment_stats = sync_assignments(conn)
        assignment_question_stats = sync_assignment_questions(conn)
        assignment_score_stats = sync_assignment_scores(conn)
        counts = verify_counts(conn)
        print("PROGRAM_QUESTION_STATS", program_question_stats)
        print("PROGRAM_ANSWER_STATS", program_answer_stats)
        print("ASSIGNMENT_STATS", assignment_stats)
        print("ASSIGNMENT_QUESTION_STATS", assignment_question_stats)
        print("ASSIGNMENT_SCORE_STATS", assignment_score_stats)
        print("BATCH2_STAGING_COUNTS", counts)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
