package com.ruoyi.student.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.student.mapper.EduLegacyAnalyticsMapper;
import com.ruoyi.student.mapper.EduPadMapper;

@Service
public class EduAiStudentProfileService
{
    private static final String ROLE_STUDENT = "student";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EduPadMapper eduPadMapper;

    @Autowired
    private EduLegacyAnalyticsMapper eduLegacyAnalyticsMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void ensurePredictionSnapshotTable()
    {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS edu_ai_prediction_snapshot ("
                        + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "user_id BIGINT NOT NULL,"
                        + "user_name VARCHAR(64) NOT NULL,"
                        + "display_name VARCHAR(64) NULL,"
                        + "role_key VARCHAR(32) NOT NULL,"
                        + "predicted_score DECIMAL(10,2) NOT NULL,"
                        + "score_level VARCHAR(32) NULL,"
                        + "input_snapshot MEDIUMTEXT NULL,"
                        + "prediction_message VARCHAR(255) NULL,"
                        + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "INDEX idx_prediction_user(user_id, create_time)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    public void saveCurrentUserPredictionSnapshot(Map<String, Object> requestBody, Map<String, Object> predictionResult)
    {
        SysUser user = currentUser();
        if (user == null || user.getUserId() == null || predictionResult == null)
        {
            return;
        }
        BigDecimal predictedScore = toDecimal(predictionResult.get("predicted_score"));
        if (predictedScore == null)
        {
            return;
        }

        Object inputData = null;
        if (requestBody != null)
        {
            Object nestedInput = requestBody.get("input_data");
            inputData = nestedInput != null ? nestedInput : requestBody;
        }
        jdbcTemplate.update(
                "INSERT INTO edu_ai_prediction_snapshot("
                        + "user_id, user_name, display_name, role_key, predicted_score, score_level, input_snapshot, prediction_message"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                user.getUserId(),
                StringUtils.defaultIfEmpty(user.getUserName(), ""),
                StringUtils.defaultIfEmpty(user.getNickName(), user.getUserName()),
                resolveCurrentRoleKey(),
                predictedScore,
                resolveScoreLevel(predictedScore),
                toJson(inputData),
                trim(predictionResult.get("message")));
    }

    public Map<String, Object> buildCurrentStudentRagProfile(String courseName, String chapterCode, String chapterName)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!ROLE_STUDENT.equals(resolveCurrentRoleKey()))
        {
            result.put("predictionReady", false);
            result.put("dataReady", false);
            result.put("bound", false);
            result.put("latestPrediction", null);
            result.put("predictionTip", "");
            result.put("businessContext", "");
            return result;
        }
        Long userId = SecurityUtils.getUserId();
        result.put("predictionReady", false);
        result.put("dataReady", false);
        result.put("bound", false);
        result.put("latestPrediction", null);
        result.put("predictionTip", "当前还没有成绩预测结果，请先去成绩预测模块完成最新预测。");

        if (userId == null)
        {
            result.put("businessContext", "");
            return result;
        }

        Map<String, Object> latestPrediction = getLatestPrediction(userId);
        if (latestPrediction != null && !latestPrediction.isEmpty())
        {
            result.put("predictionReady", true);
            result.put("latestPrediction", latestPrediction);
            result.put("predictionTip", "当前已接入最新一次成绩预测结果，RAG 会优先按这次预测进行分析。");
        }

        String studentNo = StringUtils.trimToEmpty(eduLegacyAnalyticsMapper.selectMappedStudentNoByCurrentUserId(userId));
        if (StringUtils.isEmpty(studentNo))
        {
            result.put("missingReason", "当前账号尚未绑定真实历史作答数据，暂时无法生成完整学情画像。");
            result.put("businessContext", buildBusinessContext(result, courseName, chapterCode, chapterName));
            return result;
        }

        result.put("bound", true);

        Map<String, Object> diagnosisOverview = defaultMap(eduLegacyAnalyticsMapper.selectStudentDiagnosisOverview(studentNo));
        List<Map<String, Object>> chapterDiagnosis = defaultList(eduLegacyAnalyticsMapper.selectStudentChapterDiagnosis(studentNo));
        List<Map<String, Object>> weakKnowledgePoints = defaultList(eduLegacyAnalyticsMapper.selectStudentWeakKnowledgePoints(studentNo, 5));
        List<Map<String, Object>> wrongQuestions = defaultList(eduLegacyAnalyticsMapper.selectStudentWrongQuestions(studentNo, 5));
        Map<String, Object> programOverview = defaultMap(eduLegacyAnalyticsMapper.selectStudentProgramOverview(studentNo));
        List<Map<String, Object>> weakAssignments = defaultList(eduLegacyAnalyticsMapper.selectStudentProgramWeakAssignments(studentNo, 5));
        List<Map<String, Object>> recentAnswers = defaultList(eduLegacyAnalyticsMapper.selectStudentAnswerHistory(
                studentNo,
                StringUtils.trimToEmpty(courseName),
                StringUtils.trimToEmpty(chapterCode),
                "",
                12));

        String targetCourseName = StringUtils.trimToEmpty(courseName);
        String targetChapterCode = StringUtils.trimToEmpty(chapterCode);
        String targetChapterName = StringUtils.trimToEmpty(chapterName);
        Map<String, Object> weakestChapter = chapterDiagnosis.isEmpty() ? Collections.emptyMap() : chapterDiagnosis.get(0);
        if (StringUtils.isEmpty(targetCourseName))
        {
            targetCourseName = trim(weakestChapter.get("courseName"));
        }
        if (StringUtils.isEmpty(targetChapterCode))
        {
            targetChapterCode = trim(weakestChapter.get("chapterCode"));
        }
        if (StringUtils.isEmpty(targetChapterName))
        {
            targetChapterName = trim(weakestChapter.get("chapterName"));
        }
        List<Map<String, Object>> practiceRecommendations = defaultList(
                eduLegacyAnalyticsMapper.selectPracticeRecommendationCandidates(
                        studentNo,
                        targetCourseName,
                        targetChapterCode,
                        targetChapterName,
                        5));

        List<Map<String, Object>> examScores = defaultList(eduPadMapper.selectExamScoreByStudentId(userId));
        List<Map<String, Object>> selfScores = defaultList(eduPadMapper.selectStudentPerformanceByStudentId(userId));

        result.put("dataReady", true);
        result.put("diagnosisOverview", diagnosisOverview);
        result.put("weakestChapter", weakestChapter);
        result.put("chapterDiagnosis", chapterDiagnosis.subList(0, Math.min(5, chapterDiagnosis.size())));
        result.put("weakKnowledgePoints", weakKnowledgePoints);
        result.put("wrongQuestions", wrongQuestions);
        result.put("practiceRecommendations", practiceRecommendations);
        result.put("programOverview", programOverview);
        result.put("weakProgramAssignments", weakAssignments);
        result.put("recentAnswers", recentAnswers);
        result.put("recentAnswerSummary", buildRecentAnswerSummary(recentAnswers));
        result.put("examScoreSummary", buildExamScoreSummary(examScores));
        result.put("selfScoreSummary", buildSelfScoreSummary(selfScores));
        result.put("analysisTarget", buildAnalysisTarget(targetCourseName, targetChapterName));
        result.put("businessContext", buildBusinessContext(result, targetCourseName, targetChapterCode, targetChapterName));
        return result;
    }

    public String buildCurrentStudentBusinessContext(String courseName, String chapterCode, String chapterName)
    {
        return trim(buildCurrentStudentRagProfile(courseName, chapterCode, chapterName).get("businessContext"));
    }

    public Map<String, Object> buildCurrentStudentSceneDataset(String sourceScene, String courseName, String chapterCode, String chapterName)
    {
        Map<String, Object> profile = buildCurrentStudentRagProfile(courseName, chapterCode, chapterName);
        Map<String, Object> result = new LinkedHashMap<>();
        String fileName = buildSceneDatasetFileName(sourceScene, courseName, chapterName);
        List<String> lines = new ArrayList<>();
        lines.add("教育平台学生场景综合画像");
        lines.add("生成时间：" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, new Date()));
        if (StringUtils.isNotEmpty(trim(sourceScene)))
        {
            lines.add("来源场景：" + trim(sourceScene));
        }
        if (StringUtils.isNotEmpty(trim(courseName)))
        {
            lines.add("课程：" + trim(courseName));
        }
        if (StringUtils.isNotEmpty(trim(chapterCode)))
        {
            lines.add("章节编码：" + trim(chapterCode));
        }
        if (StringUtils.isNotEmpty(trim(chapterName)))
        {
            lines.add("章节名称：" + trim(chapterName));
        }
        lines.add("");
        lines.add(buildBusinessContext(profile, courseName, chapterCode, chapterName));
        result.put("fileName", fileName);
        result.put("content", String.join("\n", lines));
        result.put("profile", profile);
        return result;
    }

    private String buildBusinessContext(Map<String, Object> profile, String courseName, String chapterCode, String chapterName)
    {
        List<String> lines = new ArrayList<>();
        lines.add("以下是当前学生的最新综合学习画像，回答学习建议、学情分析、提分规划、复盘建议时必须优先使用这些数据。");
        SysUser user = currentUser();
        if (user != null)
        {
            String displayName = StringUtils.defaultIfEmpty(trim(user.getNickName()), trim(user.getUserName()));
            if (StringUtils.isNotEmpty(displayName))
            {
                lines.add("当前学生姓名：" + displayName + "。");
            }
        }

        String target = buildAnalysisTarget(courseName, chapterName);
        if (StringUtils.isNotEmpty(target))
        {
            lines.add("当前优先分析范围：" + target + "。");
        }

        Map<String, Object> latestPrediction = defaultMap(profile.get("latestPrediction"));
        if (!latestPrediction.isEmpty())
        {
            lines.add("最新成绩预测：" + trim(latestPrediction.get("predictedScore")) + " 分，预测时间 " + trim(latestPrediction.get("createTime")) + "。");
        }
        else
        {
            lines.add("最新成绩预测：暂无，请先提醒学生去成绩预测模块完成最新预测，再结合预测结果细化方案。");
        }

        if (Boolean.TRUE.equals(profile.get("dataReady")))
        {
            Map<String, Object> diagnosisOverview = defaultMap(profile.get("diagnosisOverview"));
            lines.add("刷题总览：累计作答 " + valueOrDash(diagnosisOverview.get("answerCount"))
                    + " 题，累计正确率 " + valueOrDash(diagnosisOverview.get("correctRate"))
                    + "%；近7天作答 " + valueOrDash(diagnosisOverview.get("recentAnswerCount"))
                    + " 题，近7天正确率 " + valueOrDash(diagnosisOverview.get("recentCorrectRate")) + "%。");

            Map<String, Object> weakestChapter = defaultMap(profile.get("weakestChapter"));
            if (!weakestChapter.isEmpty())
            {
                lines.add("最薄弱章节：" + trim(weakestChapter.get("courseName")) + " / " + trim(weakestChapter.get("chapterName"))
                        + "，正确率 " + valueOrDash(weakestChapter.get("correctRate")) + "%，作答 "
                        + valueOrDash(weakestChapter.get("answerCount")) + " 题。");
            }

            List<Map<String, Object>> chapterDiagnosis = defaultList(profile.get("chapterDiagnosis"));
            if (!chapterDiagnosis.isEmpty())
            {
                lines.add("章节对比：" + buildChapterDiagnosisSummary(chapterDiagnosis));
            }

            List<Map<String, Object>> weakKnowledgePoints = defaultList(profile.get("weakKnowledgePoints"));
            if (!weakKnowledgePoints.isEmpty())
            {
                lines.add("薄弱知识点：" + weakKnowledgePoints.stream()
                        .limit(3)
                        .map(item -> trim(item.get("knowledgePoint")) + "（正确率 " + valueOrDash(item.get("correctRate")) + "%）")
                        .collect(Collectors.joining("；")) + "。");
            }

            List<Map<String, Object>> wrongQuestions = defaultList(profile.get("wrongQuestions"));
            if (!wrongQuestions.isEmpty())
            {
                lines.add("高频错题线索：" + wrongQuestions.stream()
                        .limit(2)
                        .map(item -> abbreviate(trim(item.get("questionStem")), 36) + "（错题次数 " + valueOrDash(item.get("wrongCount")) + "）")
                        .collect(Collectors.joining("；")) + "。");
                lines.add("错题复盘重点：" + buildWrongQuestionSummary(wrongQuestions));
            }

            List<Map<String, Object>> practiceRecommendations = defaultList(profile.get("practiceRecommendations"));
            if (!practiceRecommendations.isEmpty())
            {
                lines.add("当前推荐练习方向：" + practiceRecommendations.stream()
                        .limit(3)
                        .map(item -> trim(item.get("chapterName")) + "/" + trim(item.get("knowledgePoint")))
                        .filter(StringUtils::isNotEmpty)
                        .collect(Collectors.joining("；")) + "。");
            }
            else if (!chapterDiagnosis.isEmpty())
            {
                lines.add("当前推荐练习方向：可优先围绕正确率最低的 2-3 个章节安排专项练习，先补基础题，再补综合题。");
            }

            Map<String, Object> programOverview = defaultMap(profile.get("programOverview"));
            if (!programOverview.isEmpty())
            {
                lines.add("编程题表现：共提交 " + valueOrDash(programOverview.get("submitCount")) + " 次，平均分 "
                        + valueOrDash(programOverview.get("averageScore")) + "，最近提交时间 " + valueOrDash(programOverview.get("latestSubmitTime")) + "。");
            }

            List<Map<String, Object>> weakAssignments = defaultList(profile.get("weakProgramAssignments"));
            if (!weakAssignments.isEmpty())
            {
                lines.add("编程薄弱作业：" + weakAssignments.stream()
                        .limit(2)
                        .map(item -> trim(item.get("assignmentTitle")) + "（平均分 " + valueOrDash(item.get("averageScore")) + "）")
                        .collect(Collectors.joining("；")) + "。");
            }

            Map<String, Object> recentAnswerSummary = defaultMap(profile.get("recentAnswerSummary"));
            if (!recentAnswerSummary.isEmpty())
            {
                lines.add("最近答题表现：最近 " + valueOrDash(recentAnswerSummary.get("answerCount")) + " 题，正确率 "
                        + valueOrDash(recentAnswerSummary.get("correctRate")) + "%。");
            }

            List<Map<String, Object>> recentAnswers = defaultList(profile.get("recentAnswers"));
            if (!recentAnswers.isEmpty())
            {
                lines.add("最近作答明细：" + buildRecentAnswerDetails(recentAnswers));
            }

            Map<String, Object> examScoreSummary = defaultMap(profile.get("examScoreSummary"));
            if (!examScoreSummary.isEmpty())
            {
                lines.add("考试成绩：最近一次考试 " + valueOrDash(examScoreSummary.get("latestScore")) + " 分，历史均分 "
                        + valueOrDash(examScoreSummary.get("averageScore")) + " 分。");
            }
            else
            {
                lines.add("考试成绩：当前场景暂无考试表现数据，本次分析暂以刷题、错题、编程题和系统成绩档案为主。");
            }

            Map<String, Object> selfScoreSummary = defaultMap(profile.get("selfScoreSummary"));
            if (!selfScoreSummary.isEmpty())
            {
                lines.add("系统成绩档案：当前记录均分 " + valueOrDash(selfScoreSummary.get("averageScore")) + " 分。");
            }

            lines.add("分析提示：回答时不要只复述数据，要根据以上数据判断问题成因、优先级和短期提分动作。");
        }
        else if (StringUtils.isNotEmpty(trim(profile.get("missingReason"))))
        {
            lines.add("学情画像状态：" + trim(profile.get("missingReason")));
        }

        lines.add("回答要求：");
        lines.add("1. 如果用户问学情分析、如何提高、复盘建议、学习规划，必须明确引用以上画像数据作为依据。");
        lines.add("2. 如果缺少最新成绩预测，第一句先提醒去做预测，但仍要结合已有刷题、错题、编程题、考试数据继续给建议。");
        lines.add("3. 如果知识库命中了课程知识点，先按知识库规定的顺序回答知识点，再衔接学生画像给出个性化建议。");
        return String.join("\n", lines);
    }

    private Map<String, Object> getLatestPrediction(Long userId)
    {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT predicted_score, score_level, input_snapshot, prediction_message, create_time "
                        + "FROM edu_ai_prediction_snapshot WHERE user_id = ? ORDER BY create_time DESC, id DESC LIMIT 1",
                userId);
        if (rows.isEmpty())
        {
            return Collections.emptyMap();
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("predictedScore", valueOrDash(row.get("predicted_score")));
        result.put("scoreLevel", trim(row.get("score_level")));
        result.put("predictionMessage", trim(row.get("prediction_message")));
        result.put("createTime", formatTimestamp(row.get("create_time")));
        result.put("inputData", parseJsonObject(row.get("input_snapshot")));
        return result;
    }

    private Map<String, Object> buildRecentAnswerSummary(List<Map<String, Object>> recentAnswers)
    {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (recentAnswers == null || recentAnswers.isEmpty())
        {
            return summary;
        }
        int correctCount = 0;
        for (Map<String, Object> item : recentAnswers)
        {
            if ("1".equals(trim(item.get("isCorrect"))))
            {
                correctCount++;
            }
        }
        summary.put("answerCount", recentAnswers.size());
        summary.put("correctCount", correctCount);
        summary.put("correctRate", formatPercent(correctCount, recentAnswers.size()));
        summary.put("latestSubmitTime", trim(recentAnswers.get(0).get("submitTime")));
        return summary;
    }

    private String buildChapterDiagnosisSummary(List<Map<String, Object>> chapterDiagnosis)
    {
        return chapterDiagnosis.stream()
                .limit(3)
                .map(item -> trim(item.get("chapterName")) + "（正确率 " + valueOrDash(item.get("correctRate"))
                        + "%，作答 " + valueOrDash(item.get("answerCount")) + " 题）")
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining("；"));
    }

    private String buildWrongQuestionSummary(List<Map<String, Object>> wrongQuestions)
    {
        return wrongQuestions.stream()
                .limit(3)
                .map(item -> {
                    String stem = abbreviate(trim(item.get("questionStem")), 24);
                    String knowledgePoint = trim(item.get("knowledgePoint"));
                    StringBuilder builder = new StringBuilder();
                    if (StringUtils.isNotEmpty(stem))
                    {
                        builder.append(stem);
                    }
                    if (StringUtils.isNotEmpty(knowledgePoint))
                    {
                        if (builder.length() > 0)
                        {
                            builder.append("，");
                        }
                        builder.append("涉及 ").append(knowledgePoint);
                    }
                    if (builder.length() == 0)
                    {
                        builder.append("错题");
                    }
                    builder.append("，累计错 ").append(valueOrDash(item.get("wrongCount"))).append(" 次");
                    return builder.toString();
                })
                .collect(Collectors.joining("；"));
    }

    private String buildRecentAnswerDetails(List<Map<String, Object>> recentAnswers)
    {
        return recentAnswers.stream()
                .limit(5)
                .map(item -> {
                    String chapter = trim(item.get("chapterName"));
                    String knowledgePoint = trim(item.get("knowledgePoint"));
                    String correct = "1".equals(trim(item.get("isCorrect"))) ? "正确" : "错误";
                    StringBuilder builder = new StringBuilder();
                    if (StringUtils.isNotEmpty(chapter))
                    {
                        builder.append(chapter);
                    }
                    if (StringUtils.isNotEmpty(knowledgePoint))
                    {
                        if (builder.length() > 0)
                        {
                            builder.append("/");
                        }
                        builder.append(knowledgePoint);
                    }
                    if (builder.length() == 0)
                    {
                        builder.append("未标注知识点题目");
                    }
                    builder.append("（").append(correct).append("）");
                    return builder.toString();
                })
                .collect(Collectors.joining("；"));
    }

    private Map<String, Object> buildExamScoreSummary(List<Map<String, Object>> examScores)
    {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (examScores == null || examScores.isEmpty())
        {
            return summary;
        }
        double total = 0D;
        int count = 0;
        String latestScore = "";
        for (Map<String, Object> item : examScores)
        {
            BigDecimal score = toDecimal(item.get("score"));
            if (score == null)
            {
                continue;
            }
            if (StringUtils.isEmpty(latestScore))
            {
                latestScore = DECIMAL_FORMAT.format(score);
            }
            total += score.doubleValue();
            count++;
        }
        if (count == 0)
        {
            return summary;
        }
        summary.put("scoreCount", count);
        summary.put("latestScore", latestScore);
        summary.put("averageScore", DECIMAL_FORMAT.format(total / count));
        return summary;
    }

    private Map<String, Object> buildSelfScoreSummary(List<Map<String, Object>> selfScores)
    {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (selfScores == null || selfScores.isEmpty())
        {
            return summary;
        }
        double total = 0D;
        int count = 0;
        for (Map<String, Object> item : selfScores)
        {
            BigDecimal score = toDecimal(item.get("exam_score"));
            if (score == null)
            {
                continue;
            }
            total += score.doubleValue();
            count++;
        }
        if (count == 0)
        {
            return summary;
        }
        summary.put("scoreCount", count);
        summary.put("averageScore", DECIMAL_FORMAT.format(total / count));
        return summary;
    }

    private String buildAnalysisTarget(String courseName, String chapterName)
    {
        String target = java.util.stream.Stream.of(StringUtils.trimToEmpty(courseName), StringUtils.trimToEmpty(chapterName))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(" / "));
        return StringUtils.defaultIfEmpty(target, "");
    }

    private String resolveCurrentRoleKey()
    {
        if (SecurityUtils.getLoginUser() == null || SecurityUtils.getLoginUser().getAuthorities() == null)
        {
            return ROLE_STUDENT;
        }
        return SecurityUtils.getLoginUser().getAuthorities().stream()
                .map(item -> item.getAuthority())
                .anyMatch(item -> item != null && item.contains("teacher")) ? "teacher" : ROLE_STUDENT;
    }

    private SysUser currentUser()
    {
        return SecurityUtils.getLoginUser() == null ? null : SecurityUtils.getLoginUser().getUser();
    }

    private String resolveScoreLevel(BigDecimal score)
    {
        if (score == null)
        {
            return "";
        }
        double value = score.doubleValue();
        if (value >= 90) return "优秀";
        if (value >= 80) return "良好";
        if (value >= 70) return "中等";
        if (value >= 60) return "及格";
        return "待提升";
    }

    private String toJson(Object value)
    {
        if (value == null)
        {
            return "";
        }
        try
        {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException ignore)
        {
            return String.valueOf(value);
        }
    }

    private Map<String, Object> parseJsonObject(Object value)
    {
        String text = trim(value);
        if (StringUtils.isEmpty(text))
        {
            return Collections.emptyMap();
        }
        try
        {
            Object parsed = objectMapper.readValue(text, Object.class);
            if (parsed instanceof Map)
            {
                return (Map<String, Object>) parsed;
            }
        }
        catch (Exception ignore)
        {
        }
        return Collections.emptyMap();
    }

    private BigDecimal toDecimal(Object value)
    {
        if (value == null)
        {
            return null;
        }
        try
        {
            return new BigDecimal(String.valueOf(value).trim());
        }
        catch (Exception ignore)
        {
            return null;
        }
    }

    private String formatTimestamp(Object value)
    {
        if (value instanceof Timestamp)
        {
            return value.toString();
        }
        return trim(value);
    }

    private String trim(Object value)
    {
        return StringUtils.trimToEmpty(value == null ? "" : String.valueOf(value));
    }

    private String valueOrDash(Object value)
    {
        String text = trim(value);
        return StringUtils.isEmpty(text) ? "--" : text;
    }

    private String formatPercent(int part, int total)
    {
        if (total <= 0)
        {
            return "--";
        }
        return DECIMAL_FORMAT.format(part * 100D / total);
    }

    private String abbreviate(String text, int maxLength)
    {
        if (StringUtils.isEmpty(text) || text.length() <= maxLength)
        {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private String buildSceneDatasetFileName(String sourceScene, String courseName, String chapterName)
    {
        String scene = sanitizeFilePart(trim(sourceScene));
        String course = sanitizeFilePart(trim(courseName));
        String chapter = sanitizeFilePart(trim(chapterName));
        List<String> parts = new ArrayList<>();
        parts.add("student_scene_profile");
        if (StringUtils.isNotEmpty(scene))
        {
            parts.add(scene);
        }
        if (StringUtils.isNotEmpty(course))
        {
            parts.add(course);
        }
        if (StringUtils.isNotEmpty(chapter))
        {
            parts.add(chapter);
        }
        parts.add(DateUtils.dateTimeNow());
        return String.join("_", parts) + ".txt";
    }

    private String sanitizeFilePart(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return "";
        }
        return value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private Map<String, Object> defaultMap(Object value)
    {
        if (value instanceof Map)
        {
            return new LinkedHashMap<>((Map<String, Object>) value);
        }
        return new LinkedHashMap<>();
    }

    private List<Map<String, Object>> defaultList(Object value)
    {
        if (value instanceof List)
        {
            List<?> source = (List<?>) value;
            List<Map<String, Object>> list = new ArrayList<>();
            for (Object item : source)
            {
                if (item instanceof Map)
                {
                    list.add(new LinkedHashMap<>((Map<String, Object>) item));
                }
            }
            return list;
        }
        return Collections.emptyList();
    }
}
