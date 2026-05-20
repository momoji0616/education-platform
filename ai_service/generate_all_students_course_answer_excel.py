from pathlib import Path
from datetime import datetime

import pymysql
from openpyxl import Workbook


DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "123456",
    "database": "ry-vue",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
}


SUMMARY_SQL = """
SELECT
    sp.student_no AS studentNo,
    sp.student_name AS studentName,
    sp.class_code AS classCode,
    qb.course_name AS courseName,
    IFNULL(NULLIF(qb.chapter_code, ''), qb.chapter_name) AS chapterCode,
    qb.chapter_name AS chapterName,
    COUNT(1) AS answerCount,
    SUM(CASE WHEN ar.is_correct = '1' THEN 1 ELSE 0 END) AS correctCount,
    SUM(CASE WHEN ar.is_correct = '0' THEN 1 ELSE 0 END) AS wrongCount,
    ROUND(IFNULL(AVG(CASE WHEN ar.is_correct = '1' THEN 100 ELSE 0 END), 0), 2) AS correctRate,
    MAX(ar.submit_time) AS latestSubmitTime
FROM edu_student_answer_record ar
JOIN education_legacy_piclass.legacy_staging_student_profile sp
    ON ar.student_no = sp.student_no
LEFT JOIN edu_question_bank qb
    ON ar.question_id = qb.id
WHERE qb.course_name IS NOT NULL
  AND qb.course_name != ''
  AND qb.chapter_name IS NOT NULL
  AND qb.chapter_name != ''
GROUP BY
    sp.student_no,
    sp.student_name,
    sp.class_code,
    qb.course_name,
    qb.chapter_code,
    qb.chapter_name
ORDER BY
    sp.student_name ASC,
    qb.course_name ASC,
    chapterCode ASC
"""


DETAIL_SQL = """
SELECT
    sp.student_no AS studentNo,
    sp.student_name AS studentName,
    sp.class_code AS classCode,
    ar.id AS answerRecordId,
    ar.question_id AS questionId,
    ar.question_type AS questionType,
    ar.answer_content AS answerContent,
    ar.score AS score,
    ar.is_correct AS isCorrect,
    ar.submit_time AS submitTime,
    qb.course_name AS courseName,
    IFNULL(NULLIF(qb.chapter_code, ''), qb.chapter_name) AS chapterCode,
    qb.chapter_name AS chapterName,
    qb.knowledge_point AS knowledgePoint,
    qb.question_stem AS questionStem,
    qb.standard_answer AS standardAnswer,
    qb.analysis AS analysis
FROM edu_student_answer_record ar
JOIN education_legacy_piclass.legacy_staging_student_profile sp
    ON ar.student_no = sp.student_no
LEFT JOIN edu_question_bank qb
    ON ar.question_id = qb.id
ORDER BY ar.submit_time DESC, ar.id DESC
"""


OVERVIEW_SQL = """
SELECT
    qb.course_name AS courseName,
    IFNULL(NULLIF(qb.chapter_code, ''), qb.chapter_name) AS chapterCode,
    qb.chapter_name AS chapterName,
    COUNT(DISTINCT sp.student_no) AS studentCount,
    COUNT(1) AS answerCount,
    SUM(CASE WHEN ar.is_correct = '1' THEN 1 ELSE 0 END) AS correctCount,
    SUM(CASE WHEN ar.is_correct = '0' THEN 1 ELSE 0 END) AS wrongCount,
    ROUND(IFNULL(AVG(CASE WHEN ar.is_correct = '1' THEN 100 ELSE 0 END), 0), 2) AS correctRate
FROM edu_student_answer_record ar
JOIN education_legacy_piclass.legacy_staging_student_profile sp
    ON ar.student_no = sp.student_no
LEFT JOIN edu_question_bank qb
    ON ar.question_id = qb.id
WHERE qb.course_name IS NOT NULL
  AND qb.course_name != ''
  AND qb.chapter_name IS NOT NULL
  AND qb.chapter_name != ''
GROUP BY qb.course_name, qb.chapter_code, qb.chapter_name
ORDER BY qb.course_name ASC, correctRate ASC, answerCount DESC
"""


def fetch_rows(connection, sql):
    with connection.cursor() as cursor:
        cursor.execute(sql)
        return cursor.fetchall()


def normalize_value(value):
    if value is None:
        return ""
    if isinstance(value, datetime):
        return value.strftime("%Y-%m-%d %H:%M:%S")
    return value


def write_sheet(workbook, title, rows, headers):
    sheet = workbook.create_sheet(title)
    sheet.append(headers)
    for row in rows:
        sheet.append([normalize_value(row.get(header, "")) for header in headers])
    for column_cells in sheet.columns:
        max_length = 0
        column_letter = column_cells[0].column_letter
        for cell in column_cells:
            cell_value = "" if cell.value is None else str(cell.value)
            max_length = max(max_length, min(len(cell_value), 60))
        sheet.column_dimensions[column_letter].width = max(14, max_length + 2)


def main():
    output_dir = Path(r"E:\education-platform\ai_service\data")
    output_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_path = output_dir / f"all_students_course_answer_profile_{timestamp}.xlsx"

    connection = pymysql.connect(**DB_CONFIG)
    try:
        summary_rows = fetch_rows(connection, SUMMARY_SQL)
        detail_rows = fetch_rows(connection, DETAIL_SQL)
        overview_rows = fetch_rows(connection, OVERVIEW_SQL)
    finally:
        connection.close()

    workbook = Workbook()
    default_sheet = workbook.active
    workbook.remove(default_sheet)

    write_sheet(
        workbook,
        "学生章节汇总",
        summary_rows,
        [
            "studentNo",
            "studentName",
            "classCode",
            "courseName",
            "chapterCode",
            "chapterName",
            "answerCount",
            "correctCount",
            "wrongCount",
            "correctRate",
            "latestSubmitTime",
        ],
    )
    write_sheet(
        workbook,
        "学生作答明细",
        detail_rows,
        [
            "studentNo",
            "studentName",
            "classCode",
            "answerRecordId",
            "questionId",
            "questionType",
            "courseName",
            "chapterCode",
            "chapterName",
            "knowledgePoint",
            "isCorrect",
            "score",
            "submitTime",
            "answerContent",
            "questionStem",
            "standardAnswer",
            "analysis",
        ],
    )
    write_sheet(
        workbook,
        "课程章节总览",
        overview_rows,
        [
            "courseName",
            "chapterCode",
            "chapterName",
            "studentCount",
            "answerCount",
            "correctCount",
            "wrongCount",
            "correctRate",
        ],
    )

    workbook.save(output_path)
    print(str(output_path))
    print(f"summary_rows={len(summary_rows)}")
    print(f"detail_rows={len(detail_rows)}")
    print(f"overview_rows={len(overview_rows)}")


if __name__ == "__main__":
    main()
