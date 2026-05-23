package com.ruoyi.student.service;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.student.mapper.EduLegacyAnalyticsMapper;

@Service
public class EduAiInteractionService
{
    private static final String ROLE_TEACHER = "teacher";
    private static final String ROLE_STUDENT = "student";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EduLegacyAnalyticsMapper eduLegacyAnalyticsMapper;

    @PostConstruct
    public void ensureInteractionLogTable()
    {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS edu_ai_interaction_log ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "user_id BIGINT NOT NULL,"
                        + "user_name VARCHAR(64) NOT NULL,"
                        + "display_name VARCHAR(64) NULL,"
                        + "role_key VARCHAR(32) NOT NULL,"
                        + "major_key VARCHAR(32) NULL,"
                        + "legacy_class_code VARCHAR(32) NULL,"
                        + "student_no VARCHAR(64) NULL,"
                        + "course_name VARCHAR(128) NULL,"
                        + "chapter_code VARCHAR(64) NULL,"
                        + "chapter_name VARCHAR(128) NULL,"
                        + "knowledge_point VARCHAR(255) NULL,"
                        + "question_id BIGINT NULL,"
                        + "source_scene VARCHAR(64) NULL,"
                        + "asked_question TEXT NOT NULL,"
                        + "answer_snapshot MEDIUMTEXT NULL,"
                        + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "INDEX idx_ai_log_user(user_id),"
                        + "INDEX idx_ai_log_major(major_key),"
                        + "INDEX idx_ai_log_student(student_no),"
                        + "INDEX idx_ai_log_create(create_time)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    public Map<String, Object> logInteraction(Map<String, Object> body)
    {
        SysUser user = SecurityUtils.getLoginUser() == null ? null : SecurityUtils.getLoginUser().getUser();
        if (user == null || user.getUserId() == null)
        {
            return Collections.emptyMap();
        }

        String askedQuestion = trim(body.get("askedQuestion"));
        if (StringUtils.isEmpty(askedQuestion))
        {
            return Collections.emptyMap();
        }

        String roleKey = resolveCurrentRoleKey();
        String majorKey = selectCurrentMajorKey(user.getUserId());
        String studentNo = ROLE_STUDENT.equals(roleKey)
                ? StringUtils.trimToEmpty(eduLegacyAnalyticsMapper.selectMappedStudentNoByCurrentUserId(user.getUserId()))
                : "";
        String legacyClassCode = StringUtils.isEmpty(studentNo) ? "" : selectLegacyClassCode(studentNo);

        jdbcTemplate.update(
                "INSERT INTO edu_ai_interaction_log("
                        + "user_id, user_name, display_name, role_key, major_key, legacy_class_code, student_no, "
                        + "course_name, chapter_code, chapter_name, knowledge_point, question_id, source_scene, asked_question, answer_snapshot"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                user.getUserId(),
                user.getUserName(),
                StringUtils.defaultIfEmpty(user.getNickName(), user.getUserName()),
                roleKey,
                majorKey,
                legacyClassCode,
                studentNo,
                trim(body.get("courseName")),
                trim(body.get("chapterCode")),
                trim(body.get("chapterName")),
                trim(body.get("knowledgePoint")),
                toLong(body.get("questionId")),
                trim(body.get("sourceScene")),
                askedQuestion,
                trim(body.get("answerSnapshot")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("logged", true);
        return result;
    }

    public Map<String, Object> buildTeacherSummary(String majorKey, int hotQuestionLimit, int recentLimit)
    {
        String scopeMajor = StringUtils.trimToEmpty(majorKey);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overview", buildOverview(scopeMajor));
        result.put("hotQuestions", listHotQuestions(scopeMajor, hotQuestionLimit));
        result.put("recentInteractions", listRecentInteractions(scopeMajor, recentLimit));
        result.put("moduleInteractions", listModuleInteractions(scopeMajor));
        return result;
    }

    private Map<String, Object> buildOverview(String majorKey)
    {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT COUNT(1) AS interactionCount,"
                        + " COUNT(DISTINCT student_no) AS studentCount,"
                        + " COUNT(DISTINCT chapter_name) AS chapterCount,"
                        + " MAX(create_time) AS latestAskTime"
                        + " FROM edu_ai_interaction_log"
                        + " WHERE role_key = ?"
                        + " AND (? = '' OR major_key = ?)",
                ROLE_STUDENT,
                majorKey,
                majorKey);
        return rows.isEmpty() ? Collections.emptyMap() : rows.get(0);
    }

    private List<Map<String, Object>> listHotQuestions(String majorKey, int limit)
    {
        return jdbcTemplate.queryForList(
                "SELECT asked_question AS askedQuestion,"
                        + " COUNT(1) AS askCount,"
                        + " COUNT(DISTINCT student_no) AS studentCount,"
                        + " MAX(course_name) AS courseName,"
                        + " MAX(chapter_name) AS chapterName,"
                        + " MAX(create_time) AS latestAskTime"
                        + " FROM edu_ai_interaction_log"
                        + " WHERE role_key = ?"
                        + " AND (? = '' OR major_key = ?)"
                        + " GROUP BY asked_question"
                        + " ORDER BY askCount DESC, latestAskTime DESC"
                        + " LIMIT ?",
                ROLE_STUDENT,
                majorKey,
                majorKey,
                Math.max(1, Math.min(limit, 20)));
    }

    private List<Map<String, Object>> listRecentInteractions(String majorKey, int limit)
    {
        return jdbcTemplate.queryForList(
                "SELECT display_name AS displayName,"
                        + " student_no AS studentNo,"
                        + " course_name AS courseName,"
                        + " chapter_name AS chapterName,"
                        + " asked_question AS askedQuestion,"
                        + " answer_snapshot AS answerSnapshot,"
                        + " create_time AS createTime"
                        + " FROM edu_ai_interaction_log"
                        + " WHERE role_key = ?"
                        + " AND (? = '' OR major_key = ?)"
                        + " ORDER BY create_time DESC, id DESC"
                        + " LIMIT ?",
                ROLE_STUDENT,
                majorKey,
                majorKey,
                Math.max(1, Math.min(limit, 20)));
    }

    private List<Map<String, Object>> listModuleInteractions(String majorKey)
    {
        return jdbcTemplate.queryForList(
                "SELECT course_name AS courseName,"
                        + " chapter_name AS chapterName,"
                        + " COUNT(1) AS askCount,"
                        + " COUNT(DISTINCT student_no) AS studentCount,"
                        + " MAX(create_time) AS latestAskTime"
                        + " FROM edu_ai_interaction_log"
                        + " WHERE role_key = ?"
                        + " AND (? = '' OR major_key = ?)"
                        + " AND chapter_name IS NOT NULL"
                        + " AND chapter_name <> ''"
                        + " GROUP BY course_name, chapter_name"
                        + " ORDER BY askCount DESC, latestAskTime DESC"
                        + " LIMIT 50",
                ROLE_STUDENT,
                majorKey,
                majorKey);
    }

    private String resolveCurrentRoleKey()
    {
        if (SecurityUtils.getLoginUser() == null || SecurityUtils.getLoginUser().getAuthorities() == null)
        {
            return ROLE_STUDENT;
        }
        return SecurityUtils.getLoginUser().getAuthorities().stream()
                .map(item -> item.getAuthority())
                .anyMatch(item -> item != null && item.contains(ROLE_TEACHER))
                ? ROLE_TEACHER : ROLE_STUDENT;
    }

    private String selectCurrentMajorKey(Long userId)
    {
        List<String> rows = jdbcTemplate.query(
                "SELECT CASE "
                        + "WHEN grade_no = 1 THEN 'data-science' "
                        + "WHEN grade_no = 2 THEN 'network-engineering' "
                        + "ELSE '' END AS major_key "
                        + "FROM edu_user_class_profile WHERE user_id = ? LIMIT 1",
                (rs, rowNum) -> rs.getString(1),
                userId);
        return rows.isEmpty() ? "" : StringUtils.trimToEmpty(rows.get(0));
    }

    private String selectLegacyClassCode(String studentNo)
    {
        List<String> rows = jdbcTemplate.query(
                "SELECT class_code FROM legacy_staging_student_profile"
                        + " WHERE student_no = ? LIMIT 1",
                (rs, rowNum) -> rs.getString(1),
                studentNo);
        return rows.isEmpty() ? "" : StringUtils.trimToEmpty(rows.get(0));
    }

    private String trim(Object value)
    {
        return StringUtils.trimToEmpty(value == null ? "" : String.valueOf(value));
    }

    private Long toLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        try
        {
            return Long.parseLong(String.valueOf(value));
        }
        catch (Exception ignore)
        {
            return null;
        }
    }
}
