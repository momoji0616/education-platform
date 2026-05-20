package com.ruoyi.student.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.student.mapper.EduLegacyAnalyticsMapper;
import com.ruoyi.student.mapper.EduPadMapper;

@Service
public class EduAiTeacherProfileService
{
    private static final String ROLE_TEACHER = "teacher";
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_MANAGER = "manager";
    private static final String MAJOR_DATA_SCIENCE_KEY = "data-science";
    private static final String MAJOR_NETWORK_ENGINEERING_KEY = "network-engineering";

    @Autowired
    private EduLegacyAnalyticsMapper eduLegacyAnalyticsMapper;

    @Autowired
    private EduPadMapper eduPadMapper;

    @Autowired
    private EduAiInteractionService eduAiInteractionService;

    public Map<String, Object> buildCurrentTeacherRagProfile(String studentNo, String studentName, String courseName, String chapterCode, String chapterName)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("teacherReady", false);
        result.put("bound", false);
        result.put("studentScoped", false);
        result.put("businessContext", "");
        if (!ROLE_TEACHER.equals(resolveCurrentRoleKey()))
        {
            return result;
        }

        List<String> legacyClassCodes = getCurrentLegacyClassCodesForMajor();
        if (legacyClassCodes.isEmpty())
        {
            result.put("missingReason", "当前教师账号尚未绑定可用于展示的真实历史作答范围，暂时无法生成教师学情画像。");
            result.put("businessContext", buildTeacherBusinessContext(result, studentNo, studentName, courseName, chapterCode, chapterName));
            return result;
        }

        result.put("teacherReady", true);
        result.put("bound", true);
        result.put("majorKey", getCurrentMajorKey());

        String targetStudentNo = StringUtils.trimToEmpty(studentNo);
        String targetStudentName = StringUtils.trimToEmpty(studentName);
        if (StringUtils.isNotEmpty(targetStudentNo))
        {
            Map<String, Object> scopedStudent = defaultMap(
                    eduLegacyAnalyticsMapper.selectScopedStudentBaseInfo(legacyClassCodes, targetStudentNo));
            if (!scopedStudent.isEmpty())
            {
                result.put("studentScoped", true);
                result.put("selectedStudent", scopedStudent);
                if (StringUtils.isEmpty(targetStudentName))
                {
                    targetStudentName = trim(scopedStudent.get("studentName"));
                }
                fillStudentScopedProfile(result, targetStudentNo, targetStudentName, courseName, chapterCode, chapterName);
            }
            else
            {
                result.put("missingReason", "当前教师无权查看该学生，或该学生不在当前教师可分析的真实数据范围内。");
            }
        }

        if (!Boolean.TRUE.equals(result.get("studentScoped")))
        {
            fillClassScopedProfile(result, courseName, chapterCode, chapterName);
        }

        result.put("businessContext", buildTeacherBusinessContext(result, targetStudentNo, targetStudentName, courseName, chapterCode, chapterName));
        return result;
    }

    public String buildCurrentTeacherBusinessContext(String studentNo, String studentName, String courseName, String chapterCode, String chapterName)
    {
        return trim(buildCurrentTeacherRagProfile(studentNo, studentName, courseName, chapterCode, chapterName).get("businessContext"));
    }

    public Map<String, Object> buildCurrentTeacherSceneDataset(String sourceScene, String studentNo, String studentName, String courseName, String chapterCode, String chapterName)
    {
        Map<String, Object> profile = buildCurrentTeacherRagProfile(studentNo, studentName, courseName, chapterCode, chapterName);
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> lines = new ArrayList<>();
        lines.add("教育平台教师场景综合画像");
        lines.add("生成时间：" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, new Date()));
        if (StringUtils.isNotEmpty(trim(sourceScene)))
        {
            lines.add("来源场景：" + trim(sourceScene));
        }
        if (StringUtils.isNotEmpty(trim(studentName)) || StringUtils.isNotEmpty(trim(studentNo)))
        {
            lines.add("分析学生：" + StringUtils.defaultIfEmpty(trim(studentName), trim(studentNo)));
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
        lines.add(buildTeacherBusinessContext(profile, studentNo, studentName, courseName, chapterCode, chapterName));
        result.put("fileName", buildSceneDatasetFileName(sourceScene, studentName, courseName, chapterName));
        result.put("content", String.join("\n", lines));
        result.put("profile", profile);
        return result;
    }

    private void fillStudentScopedProfile(Map<String, Object> result, String studentNo, String studentName, String courseName, String chapterCode, String chapterName)
    {
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
        List<Map<String, Object>> practiceRecommendations = defaultList(
                eduLegacyAnalyticsMapper.selectPracticeRecommendationCandidates(
                        studentNo,
                        StringUtils.trimToEmpty(courseName),
                        StringUtils.trimToEmpty(chapterCode),
                        StringUtils.trimToEmpty(chapterName),
                        5));

        result.put("analysisMode", "student");
        result.put("selectedStudentName", studentName);
        result.put("diagnosisOverview", diagnosisOverview);
        result.put("weakestChapter", chapterDiagnosis.isEmpty() ? Collections.emptyMap() : chapterDiagnosis.get(0));
        result.put("chapterDiagnosis", chapterDiagnosis.subList(0, Math.min(5, chapterDiagnosis.size())));
        result.put("weakKnowledgePoints", weakKnowledgePoints);
        result.put("wrongQuestions", wrongQuestions);
        result.put("practiceRecommendations", practiceRecommendations);
        result.put("programOverview", programOverview);
        result.put("weakProgramAssignments", weakAssignments);
        result.put("recentAnswers", recentAnswers);
        result.put("recentAnswerSummary", buildRecentAnswerSummary(recentAnswers));
    }

    private void fillClassScopedProfile(Map<String, Object> result, String courseName, String chapterCode, String chapterName)
    {
        List<String> legacyClassCodes = getCurrentLegacyClassCodesForMajor();
        List<String> courseNames = Collections.emptyList();
        Map<String, Object> overview = defaultMap(eduLegacyAnalyticsMapper.selectTeacherAnalysisOverviewByScope(legacyClassCodes, courseNames));
        List<Map<String, Object>> weakChapters = defaultList(eduLegacyAnalyticsMapper.selectTeacherAnalysisChaptersByScope(legacyClassCodes, courseNames));
        List<Map<String, Object>> studentModules = defaultList(eduLegacyAnalyticsMapper.selectTeacherStudentModulePerformanceByScope(legacyClassCodes, courseNames, 12));
        Map<String, Object> aiSummary = defaultMap(eduAiInteractionService.buildTeacherSummary(getCurrentMajorKey(), 6, 6));
        String legacyClassCode = getMappedLegacyClassCode();

        result.put("analysisMode", "class");
        result.put("classOverview", overview);
        result.put("weakChapters", weakChapters.subList(0, Math.min(5, weakChapters.size())));
        result.put("studentModules", filterStudentModules(studentModules, courseName, chapterCode, chapterName));
        result.put("aiAssistantSummary", aiSummary);
        if (StringUtils.isNotEmpty(legacyClassCode))
        {
            result.put("assignmentOverview", defaultMap(eduLegacyAnalyticsMapper.selectTeacherAssignmentOverview(legacyClassCode)));
            result.put("assignmentSummaries", defaultList(eduLegacyAnalyticsMapper.selectTeacherAssignmentSummaries(legacyClassCode, 5)));
            result.put("hotWrongQuestions", defaultList(eduLegacyAnalyticsMapper.selectTeacherHotWrongQuestions(legacyClassCode, 5)));
            result.put("knowledgePoints", defaultList(eduLegacyAnalyticsMapper.selectTeacherKnowledgePoints(legacyClassCode, 5)));
        }
    }

    private String buildTeacherBusinessContext(Map<String, Object> profile, String studentNo, String studentName, String courseName, String chapterCode, String chapterName)
    {
        List<String> lines = new ArrayList<>();
        lines.add("以下是当前教师侧可见范围内的最新学情画像。回答教师提问时，必须优先引用这些真实数据，不能只给空泛建议。");

        String targetStudentName = StringUtils.defaultIfEmpty(trim(studentName), trim(studentNo));
        String analysisTarget = buildAnalysisTarget(courseName, chapterName);
        if (Boolean.TRUE.equals(profile.get("studentScoped")))
        {
            lines.add("当前分析对象：学生 " + targetStudentName + "。");
            if (StringUtils.isNotEmpty(analysisTarget))
            {
                lines.add("当前优先分析范围：" + analysisTarget + "。");
            }

            Map<String, Object> diagnosisOverview = defaultMap(profile.get("diagnosisOverview"));
            if (!diagnosisOverview.isEmpty())
            {
                lines.add("学生刷题总览：累计作答 " + valueOrDash(diagnosisOverview.get("answerCount"))
                        + " 题，累计正确率 " + valueOrDash(diagnosisOverview.get("correctRate"))
                        + "%；近7天作答 " + valueOrDash(diagnosisOverview.get("recentAnswerCount"))
                        + " 题，近7天正确率 " + valueOrDash(diagnosisOverview.get("recentCorrectRate")) + "%。");
            }

            Map<String, Object> weakestChapter = defaultMap(profile.get("weakestChapter"));
            if (!weakestChapter.isEmpty())
            {
                lines.add("学生最薄弱章节：" + trim(weakestChapter.get("courseName")) + " / " + trim(weakestChapter.get("chapterName"))
                        + "，正确率 " + valueOrDash(weakestChapter.get("correctRate")) + "%，作答 "
                        + valueOrDash(weakestChapter.get("answerCount")) + " 题。");
            }

            List<Map<String, Object>> chapterDiagnosis = defaultList(profile.get("chapterDiagnosis"));
            if (!chapterDiagnosis.isEmpty())
            {
                lines.add("学生章节对比：" + buildChapterSummary(chapterDiagnosis));
            }

            List<Map<String, Object>> weakKnowledgePoints = defaultList(profile.get("weakKnowledgePoints"));
            if (!weakKnowledgePoints.isEmpty())
            {
                lines.add("学生薄弱知识点：" + weakKnowledgePoints.stream()
                        .limit(3)
                        .map(item -> trim(item.get("knowledgePoint")) + "（正确率 " + valueOrDash(item.get("correctRate")) + "%）")
                        .filter(StringUtils::isNotEmpty)
                        .collect(Collectors.joining("；")) + "。");
            }

            List<Map<String, Object>> wrongQuestions = defaultList(profile.get("wrongQuestions"));
            if (!wrongQuestions.isEmpty())
            {
                lines.add("学生高频错题线索：" + wrongQuestions.stream()
                        .limit(2)
                        .map(item -> abbreviate(trim(item.get("questionStem")), 36) + "（错题次数 " + valueOrDash(item.get("wrongCount")) + "）")
                        .collect(Collectors.joining("；")) + "。");
            }

            List<Map<String, Object>> practiceRecommendations = defaultList(profile.get("practiceRecommendations"));
            if (!practiceRecommendations.isEmpty())
            {
                lines.add("建议教师优先安排的练习方向：" + practiceRecommendations.stream()
                        .limit(3)
                        .map(item -> trim(item.get("chapterName")) + "/" + trim(item.get("knowledgePoint")))
                        .filter(StringUtils::isNotEmpty)
                        .collect(Collectors.joining("；")) + "。");
            }

            Map<String, Object> programOverview = defaultMap(profile.get("programOverview"));
            if (!programOverview.isEmpty())
            {
                lines.add("学生编程题表现：共提交 " + valueOrDash(programOverview.get("submitCount")) + " 次，平均分 "
                        + valueOrDash(programOverview.get("averageScore")) + "，最近提交时间 "
                        + valueOrDash(programOverview.get("latestSubmitTime")) + "。");
            }

            Map<String, Object> recentAnswerSummary = defaultMap(profile.get("recentAnswerSummary"));
            if (!recentAnswerSummary.isEmpty())
            {
                lines.add("学生最近答题表现：最近 " + valueOrDash(recentAnswerSummary.get("answerCount")) + " 题，正确率 "
                        + valueOrDash(recentAnswerSummary.get("correctRate")) + "%。");
            }

            lines.add("回答要求：如果教师问某个学生哪里薄弱、该怎么教、该布置什么练习，必须结合以上学生画像数据做针对性分析。");
            return String.join("\n", lines);
        }

        if (StringUtils.isNotEmpty(analysisTarget))
        {
            lines.add("当前优先分析范围：" + analysisTarget + "。");
        }

        Map<String, Object> overview = defaultMap(profile.get("classOverview"));
        if (!overview.isEmpty())
        {
            lines.add("班级整体概览：覆盖学生 " + valueOrDash(overview.get("studentCount"))
                    + " 人，累计作答 " + valueOrDash(overview.get("answerCount"))
                    + " 次，平均正确率 " + valueOrDash(overview.get("correctRate"))
                    + "%，覆盖知识点 " + valueOrDash(overview.get("knowledgePointCount")) + " 个。");
        }

        List<Map<String, Object>> weakChapters = defaultList(profile.get("weakChapters"));
        if (!weakChapters.isEmpty())
        {
            lines.add("班级薄弱章节：" + buildChapterSummary(weakChapters));
        }

        List<Map<String, Object>> studentModules = defaultList(profile.get("studentModules"));
        if (!studentModules.isEmpty())
        {
            lines.add("重点关注学生/模块：" + studentModules.stream()
                    .limit(4)
                    .map(item -> trim(item.get("studentName")) + " - " + trim(item.get("chapterName"))
                            + "（正确率 " + valueOrDash(item.get("correctRate")) + "%，作答 " + valueOrDash(item.get("answerCount")) + " 题）")
                    .collect(Collectors.joining("；")) + "。");
        }

        Map<String, Object> aiSummary = defaultMap(profile.get("aiAssistantSummary"));
        Map<String, Object> aiOverview = defaultMap(aiSummary.get("overview"));
        if (!aiOverview.isEmpty())
        {
            lines.add("AI 互动概览：互动总数 " + valueOrDash(aiOverview.get("interactionCount"))
                    + "，涉及学生 " + valueOrDash(aiOverview.get("studentCount"))
                    + " 人，涉及模块 " + valueOrDash(aiOverview.get("chapterCount")) + " 个。");
        }

        List<Map<String, Object>> hotWrongQuestions = defaultList(profile.get("hotWrongQuestions"));
        if (!hotWrongQuestions.isEmpty())
        {
            lines.add("班级高频错题：" + hotWrongQuestions.stream()
                    .limit(2)
                    .map(item -> abbreviate(trim(item.get("questionStem")), 36) + "（错题次数 " + valueOrDash(item.get("wrongCount")) + "）")
                    .collect(Collectors.joining("；")) + "。");
        }

        List<Map<String, Object>> knowledgePoints = defaultList(profile.get("knowledgePoints"));
        if (!knowledgePoints.isEmpty())
        {
            lines.add("班级薄弱知识点：" + knowledgePoints.stream()
                    .limit(3)
                    .map(item -> trim(item.get("knowledgePoint")) + "（正确率 " + valueOrDash(item.get("correctRate")) + "%）")
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.joining("；")) + "。");
        }

        Map<String, Object> assignmentOverview = defaultMap(profile.get("assignmentOverview"));
        if (!assignmentOverview.isEmpty())
        {
            lines.add("作业表现概览：作业数 " + valueOrDash(assignmentOverview.get("assignmentCount"))
                    + "，覆盖学生 " + valueOrDash(assignmentOverview.get("studentCount"))
                    + " 人，平均总分 " + valueOrDash(assignmentOverview.get("averageTotalScore"))
                    + "，平均编程分 " + valueOrDash(assignmentOverview.get("averageProgramScore")) + "。");
        }

        lines.add("回答要求：如果教师问班级整体问题、不同模块表现、教学改进方向或如何针对不同学生布置训练，必须先引用以上教师画像，再给教学建议。");
        return String.join("\n", lines);
    }

    private List<Map<String, Object>> filterStudentModules(List<Map<String, Object>> rows, String courseName, String chapterCode, String chapterName)
    {
        if (rows == null || rows.isEmpty())
        {
            return Collections.emptyList();
        }
        String targetCourseName = StringUtils.trimToEmpty(courseName);
        String targetChapterCode = StringUtils.trimToEmpty(chapterCode);
        String targetChapterName = StringUtils.trimToEmpty(chapterName);
        return rows.stream()
                .filter(item -> StringUtils.isEmpty(targetCourseName) || targetCourseName.equals(trim(item.get("courseName"))))
                .filter(item -> StringUtils.isEmpty(targetChapterCode)
                        || targetChapterCode.equals(trim(item.get("chapterCode")))
                        || targetChapterCode.equals(trim(item.get("chapterName"))))
                .filter(item -> StringUtils.isEmpty(targetChapterName) || targetChapterName.equals(trim(item.get("chapterName"))))
                .limit(8)
                .collect(Collectors.toList());
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
        summary.put("correctRate", recentAnswers.isEmpty() ? "--" : String.format("%.2f", correctCount * 100D / recentAnswers.size()));
        return summary;
    }

    private String buildChapterSummary(List<Map<String, Object>> rows)
    {
        return rows.stream()
                .limit(3)
                .map(item -> trim(item.get("courseName")) + " / " + trim(item.get("chapterName"))
                        + "（正确率 " + valueOrDash(item.get("correctRate")) + "%，作答 " + valueOrDash(item.get("answerCount")) + " 题）")
                .collect(Collectors.joining("；"));
    }

    private String buildAnalysisTarget(String courseName, String chapterName)
    {
        return java.util.stream.Stream.of(StringUtils.trimToEmpty(courseName), StringUtils.trimToEmpty(chapterName))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(" / "));
    }

    private String buildSceneDatasetFileName(String sourceScene, String studentName, String courseName, String chapterName)
    {
        List<String> parts = new ArrayList<>();
        parts.add("teacher_scene_profile");
        if (StringUtils.isNotEmpty(trim(sourceScene))) parts.add(sanitizeFilePart(sourceScene));
        if (StringUtils.isNotEmpty(trim(studentName))) parts.add(sanitizeFilePart(studentName));
        if (StringUtils.isNotEmpty(trim(courseName))) parts.add(sanitizeFilePart(courseName));
        if (StringUtils.isNotEmpty(trim(chapterName))) parts.add(sanitizeFilePart(chapterName));
        parts.add(DateUtils.dateTimeNow());
        return String.join("_", parts) + ".txt";
    }

    private String sanitizeFilePart(String value)
    {
        return trim(value).replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private String getMappedLegacyClassCode()
    {
        Map<String, Object> profile = eduPadMapper.selectUserClassProfileByUserId(SecurityUtils.getUserId());
        String className = profile == null ? "" : trim(profile.get("class_name"));
        if (StringUtils.isEmpty(className))
        {
            return "";
        }
        return StringUtils.trimToEmpty(eduLegacyAnalyticsMapper.selectMappedClassCodeByCurrentClassName(className));
    }

    private String getCurrentMajorKey()
    {
        Map<String, Object> profile = eduPadMapper.selectUserClassProfileByUserId(SecurityUtils.getUserId());
        if (profile == null || profile.isEmpty())
        {
            return "";
        }
        Object gradeNo = profile.get("grade_no");
        if (gradeNo instanceof Number)
        {
            int currentGradeNo = ((Number) gradeNo).intValue();
            if (currentGradeNo == 1)
            {
                return MAJOR_DATA_SCIENCE_KEY;
            }
            if (currentGradeNo == 2)
            {
                return MAJOR_NETWORK_ENGINEERING_KEY;
            }
        }
        return "";
    }

    private List<String> getCurrentLegacyClassCodesForMajor()
    {
        if (MAJOR_DATA_SCIENCE_KEY.equals(getCurrentMajorKey()))
        {
            return Arrays.asList("1002", "1005", "1008", "1009", "1010", "1011", "1012", "1013", "1014", "1015", "1016");
        }
        return Collections.emptyList();
    }

    private String resolveCurrentRoleKey()
    {
        if (hasRole(ROLE_TEACHER) || hasRole(ROLE_ADMIN) || hasRole(ROLE_MANAGER))
        {
            return ROLE_TEACHER;
        }
        return "";
    }

    private boolean hasRole(String roleKey)
    {
        try
        {
            return SecurityUtils.hasRole(roleKey);
        }
        catch (Exception ignore)
        {
            return false;
        }
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

    private String abbreviate(String text, int maxLength)
    {
        if (StringUtils.isEmpty(text) || text.length() <= maxLength)
        {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 1)) + "...";
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
