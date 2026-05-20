package com.ruoyi.student.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.student.domain.EduExam;
import com.ruoyi.student.domain.EduExamScore;
import com.ruoyi.student.domain.EduHomework;
import com.ruoyi.student.domain.EduHomeworkSubmission;
import com.ruoyi.student.service.EduAiTeacherProfileService;

@RestController
@RequestMapping("/education/pad")
public class EduPadTeacherController extends EduPadSupport
{
    @org.springframework.beans.factory.annotation.Autowired
    private EduAiTeacherProfileService eduAiTeacherProfileService;

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('admin') or @ss.hasRole('manager')")
    @PostMapping("/homework")
    public AjaxResult createHomework(@Validated @RequestBody EduHomework homework)
    {
        fillTeacherInfo(homework);
        if (isTeacherRole())
        {
            String className = getCurrentClassName();
            if (StringUtils.isEmpty(className)) return error("老师未绑定班级，请先在登录页选择班级后登录");
            homework.setClassName(className);
        }
        if (homework.getStatus() == null) homework.setStatus(STATUS_PUBLISHED);
        return toAjax(eduPadMapper.insertHomework(homework));
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/homework/teacher")
    public AjaxResult listTeacherHomework()
    {
        return success(eduPadMapper.selectHomeworkByTeacherId(SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/homework/submissions")
    public AjaxResult listHomeworkSubmissionsByTeacher()
    {
        List<Map<String, Object>> list = eduPadMapper.selectHomeworkSubmissionByTeacherId(SecurityUtils.getUserId());
        if (list == null) list = Collections.emptyList();
        int hasImageCount = 0;
        for (Map<String, Object> item : list)
        {
            String feedback = String.valueOf(item == null ? "" : item.getOrDefault("feedback", ""));
            if (StringUtils.isNotEmpty(extractReviewImageUrlFromFeedback(feedback))) hasImageCount++;
        }
        log.info("查询教师作业提交: teacherId={}, total={}, withReviewImage={}", SecurityUtils.getUserId(), list.size(), hasImageCount);
        return success(list);
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @PostMapping("/homework/score")
    public AjaxResult scoreHomework(@RequestBody EduHomeworkSubmission submission)
    {
        if (submission == null || submission.getSubmissionId() == null) return error("提交记录 ID 不能为空");
        if (submission.getScore() == null || submission.getScore() < HOMEWORK_MIN_SCORE || submission.getScore() > HOMEWORK_MAX_SCORE)
            return error("作业分数必须在 " + HOMEWORK_MIN_SCORE + "-" + HOMEWORK_MAX_SCORE + " 之间");
        Map<String, Object> current = eduPadMapper.selectHomeworkSubmissionById(submission.getSubmissionId());
        if (current == null || current.isEmpty()) return error("提交记录不存在");
        Object teacherIdObj = current.get("teacher_id");
        if (!(teacherIdObj instanceof Number) || ((Number) teacherIdObj).longValue() != currentUserId()) return error("只能批改自己发布作业的提交");
        String requestFeedback = StringUtils.trimToEmpty(submission.getFeedback());
        String requestReviewImage = extractReviewImageUrlFromFeedback(requestFeedback);
        log.info("作业批改提交: submissionId={}, teacherId={}, score={}, feedbackLength={}, reviewImage={}", submission.getSubmissionId(), currentUserId(), submission.getScore(), requestFeedback.length(), requestReviewImage);
        int rows = eduPadMapper.updateHomeworkSubmissionScore(submission);
        if (rows > 0)
        {
            Map<String, Object> updated = eduPadMapper.selectHomeworkSubmissionById(submission.getSubmissionId());
            String savedFeedback = String.valueOf(updated == null ? "" : updated.getOrDefault("feedback", ""));
            String savedReviewImage = extractReviewImageUrlFromFeedback(savedFeedback);
            log.info("作业批改保存完成: submissionId={}, rows={}, savedFeedbackLength={}, savedReviewImage={}", submission.getSubmissionId(), rows, savedFeedback.length(), savedReviewImage);
            notifyHomeworkReviewed(current, submission);
        }
        return toAjax(rows);
    }

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('admin') or @ss.hasRole('manager')")
    @PostMapping("/exam")
    public AjaxResult createExam(@Validated @RequestBody EduExam exam)
    {
        exam.setTeacherId(currentUserId());
        exam.setTeacherName(currentUserNickName());
        if (isTeacherRole())
        {
            String className = getCurrentClassName();
            if (StringUtils.isEmpty(className)) return error("老师未绑定班级，请先在登录页选择班级后登录");
            exam.setClassName(className);
        }
        return toAjax(eduPadMapper.insertExam(exam));
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/exam/teacher")
    public AjaxResult listTeacherExam()
    {
        return success(eduPadMapper.selectExamByTeacherId(SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('admin') or @ss.hasRole('manager')")
    @PostMapping("/exam/score")
    public AjaxResult scoreExam(@Validated @RequestBody EduExamScore examScore)
    {
        if (isTeacherRole())
        {
            String className = getCurrentClassName();
            if (StringUtils.isEmpty(className)) return error("老师未绑定班级");
            Integer matched = eduPadMapper.countStudentInClassByUserId(examScore.getStudentId(), className);
            if (matched == null || matched <= 0) return error("只能为自己班级的学生评分");
        }
        if (examScore.getStudentName() == null || examScore.getStudentName().isEmpty()) examScore.setStudentName("student-" + examScore.getStudentId());
        return toAjax(eduPadMapper.upsertExamScore(examScore));
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/exam/score/teacher")
    public AjaxResult listTeacherExamScore()
    {
        return success(eduPadMapper.selectExamScoreByTeacherId(SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/tasks")
    public AjaxResult listTeacherTasks()
    {
        return success(eduPadMapper.selectTeacherTaskByTeacherId(SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/scores")
    public AjaxResult listTeacherStudentScores()
    {
        String className = getCurrentClassName();
        if (StringUtils.isEmpty(className)) return emptyListSuccess();
        return success(eduPadMapper.selectStudentPerformanceByClassName(className));
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/analysis/overview")
    public AjaxResult getTeacherAnalysisOverview()
    {
        List<String> legacyClassCodes = getCurrentLegacyClassCodesForMajor();
        List<String> courseNames = Collections.emptyList();
        if (legacyClassCodes.isEmpty()) return legacyBoundMapEmptyResult("当前专业尚未绑定可用于比赛展示的真实历史数据。");
        Map<String, Object> overview = eduLegacyAnalyticsMapper.selectTeacherAnalysisOverviewByScope(legacyClassCodes, courseNames);
        Map<String, Object> aiAssistantSummary = eduAiInteractionService.buildTeacherSummary(getCurrentMajorKey(), 8, 6);
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", overview);
        result.put("aiAssistantSummary", aiAssistantSummary);
        return result;
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/analysis/chapters")
    public AjaxResult getTeacherAnalysisChapters()
    {
        List<String> legacyClassCodes = getCurrentLegacyClassCodesForMajor();
        List<String> courseNames = Collections.emptyList();
        if (legacyClassCodes.isEmpty()) return legacyBoundEmptyResult("当前专业尚未绑定可用于比赛展示的真实历史数据。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectTeacherAnalysisChaptersByScope(legacyClassCodes, courseNames));
        return result;
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/analysis/hot-wrong-questions")
    public AjaxResult getTeacherHotWrongQuestions(@RequestParam(defaultValue = "10") Integer limit)
    {
        String legacyClassCode = getMappedLegacyClassCode();
        if (StringUtils.isEmpty(legacyClassCode)) return legacyBoundEmptyResult("当前教师班级尚未绑定历史做题数据，暂时无法查看高频错题。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectTeacherHotWrongQuestions(legacyClassCode, Math.max(1, Math.min(limit, 20))));
        return result;
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/analysis/knowledge-points")
    public AjaxResult getTeacherKnowledgePoints(@RequestParam(defaultValue = "10") Integer limit)
    {
        String legacyClassCode = getMappedLegacyClassCode();
        if (StringUtils.isEmpty(legacyClassCode)) return legacyBoundEmptyResult("当前教师班级尚未绑定历史做题数据，暂时无法查看知识点热力。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectTeacherKnowledgePoints(legacyClassCode, Math.max(1, Math.min(limit, 20))));
        return result;
    }
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/analysis/assignment-overview")
    public AjaxResult getTeacherAssignmentOverview()
    {
        String legacyClassCode = getMappedLegacyClassCode();
        if (StringUtils.isEmpty(legacyClassCode)) return legacyBoundMapEmptyResult("当前教师班级尚未绑定历史做题数据，暂时无法查看作业分析概览。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectTeacherAssignmentOverview(legacyClassCode));
        return result;
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/analysis/assignment-summaries")
    public AjaxResult getTeacherAssignmentSummaries(@RequestParam(defaultValue = "5") Integer limit)
    {
        String legacyClassCode = getMappedLegacyClassCode();
        if (StringUtils.isEmpty(legacyClassCode)) return legacyBoundEmptyResult("当前教师班级尚未绑定历史做题数据，暂时无法查看作业表现详情。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectTeacherAssignmentSummaries(legacyClassCode, Math.max(1, Math.min(limit, 10))));
        return result;
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/analysis/student-modules")
    public AjaxResult getTeacherStudentModulePerformance(@RequestParam(defaultValue = "80") Integer limit)
    {
        List<String> legacyClassCodes = getCurrentLegacyClassCodesForMajor();
        List<String> courseNames = Collections.emptyList();
        if (legacyClassCodes.isEmpty()) return legacyBoundEmptyResult("当前专业尚未绑定可用于比赛展示的真实历史数据。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectTeacherStudentModulePerformanceByScope(
                legacyClassCodes,
                courseNames,
                Math.max(10, Math.min(limit, 200))));
        return result;
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/question/catalogs")
    public AjaxResult listTeacherQuestionCatalogs(@RequestParam(required = false) String courseName)
    {
        List<String> legacyClassCodes = getCurrentLegacyClassCodesForMajor();
        List<String> courseNames = Collections.emptyList();
        if (legacyClassCodes.isEmpty())
        {
            return legacyBoundEmptyResult("当前教师端暂未绑定可展示的真实历史作答目录数据。");
        }
        List<Map<String, Object>> data = cleanCatalogs(eduLegacyAnalyticsMapper.selectTeacherCatalogsByScope(
                legacyClassCodes,
                courseNames,
                StringUtils.trimToEmpty(courseName)));
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", data);
        return result;
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/paper/questions")
    public AjaxResult listTeacherPaperQuestions(@RequestParam(required = false) String courseName,
                                                @RequestParam(required = false) String chapterCode,
                                                @RequestParam(required = false) String chapterName,
                                                @RequestParam(required = false) String questionType,
                                                @RequestParam(required = false) String difficultyLevel,
                                                @RequestParam(defaultValue = "20") Integer limit)
    {
        String targetCourseName = StringUtils.trimToEmpty(courseName);
        String targetChapterCode = StringUtils.trimToEmpty(chapterCode);
        String targetChapterName = StringUtils.trimToEmpty(chapterName);
        String targetDifficultyLevel = StringUtils.trimToEmpty(difficultyLevel);
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Map<String, Object>> data = eduLegacyAnalyticsMapper.selectQuestionBankCandidates(
                targetCourseName,
                targetChapterCode,
                targetChapterName,
                StringUtils.trimToEmpty(questionType),
                targetDifficultyLevel,
                safeLimit);
        if ((data == null || data.isEmpty()) && StringUtils.isNotEmpty(targetCourseName))
        {
            // 旧题库里部分课程名称和章节编码并不稳定，兜底放宽课程条件，优先保证页面有题可用
            data = eduLegacyAnalyticsMapper.selectQuestionBankCandidates(
                    "",
                    targetChapterCode,
                    targetChapterName,
                    StringUtils.trimToEmpty(questionType),
                    targetDifficultyLevel,
                    safeLimit);
        }
        if (data == null || data.isEmpty())
        {
            data = eduLegacyAnalyticsMapper.selectQuestionBankCandidates(
                    "",
                    "",
                    "",
                    StringUtils.trimToEmpty(questionType),
                    targetDifficultyLevel,
                    safeLimit);
        }
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", data == null ? Collections.emptyList() : data);
        return result;
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/students/page")
    public TableDataInfo getTeacherStudentManagementPage(@RequestParam(required = false) String studentName,
                                                         @RequestParam(required = false) String courseName,
                                                         @RequestParam(required = false) String chapterCode)
    {
        List<String> legacyClassCodes = getCurrentLegacyClassCodesForMajor();
        List<String> courseNames = Collections.emptyList();
        if (legacyClassCodes.isEmpty())
        {
            return getDataTable(Collections.emptyList());
        }
        startPage();
        List<Map<String, Object>> list = eduLegacyAnalyticsMapper.selectTeacherStudentManagementPageByScope(
                legacyClassCodes,
                courseNames,
                StringUtils.trimToEmpty(studentName),
                StringUtils.trimToEmpty(courseName),
                StringUtils.trimToEmpty(chapterCode));
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/students/history")
    public AjaxResult getTeacherStudentHistory(@RequestParam String studentNo,
                                               @RequestParam(defaultValue = "30") Integer limit)
    {
        List<String> legacyClassCodes = getCurrentLegacyClassCodesForMajor();
        List<String> courseNames = Collections.emptyList();
        if (legacyClassCodes.isEmpty()) return legacyBoundEmptyResult("当前专业暂无可展示的真实历史做题数据。");
        if (StringUtils.isEmpty(studentNo)) return error("学生学号不能为空");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectTeacherStudentAnswerHistoryByScope(
                legacyClassCodes,
                courseNames,
                StringUtils.trimToEmpty(studentNo),
                Math.max(10, Math.min(limit, 80))));
        return result;
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping({"/teacher/ai-assistant/summary", "/teacher/assistantSummary"})
    public AjaxResult getTeacherAiAssistantSummary(@RequestParam(defaultValue = "8") Integer questionLimit,
                                                   @RequestParam(defaultValue = "6") Integer recentLimit,
                                                   @RequestParam(defaultValue = "5") Integer weakChapterLimit)
    {
        List<String> legacyClassCodes = getCurrentLegacyClassCodesForMajor();
        List<String> courseNames = Collections.emptyList();
        if (legacyClassCodes.isEmpty())
        {
            return legacyBoundMapEmptyResult("当前专业暂无可展示的真实历史数据。");
        }
        Map<String, Object> data = eduAiInteractionService.buildTeacherSummary(
                getCurrentMajorKey(),
                Math.max(1, Math.min(questionLimit, 20)),
                Math.max(1, Math.min(recentLimit, 20)));
        List<Map<String, Object>> weakChapters = eduLegacyAnalyticsMapper.selectTeacherAnalysisChaptersByScope(legacyClassCodes, courseNames);
        data.put("weakChapters", weakChapters == null
                ? Collections.emptyList()
                : weakChapters.subList(0, Math.min(Math.max(1, Math.min(weakChapterLimit, 10)), weakChapters.size())));
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", data);
        return result;
    }

    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/teacher/rag/profile")
    public AjaxResult getTeacherRagProfile(@RequestParam(required = false) String studentNo,
                                           @RequestParam(required = false) String studentName,
                                           @RequestParam(required = false) String courseName,
                                           @RequestParam(required = false) String chapterCode,
                                           @RequestParam(required = false) String chapterName)
    {
        return success(eduAiTeacherProfileService.buildCurrentTeacherRagProfile(
                studentNo,
                studentName,
                courseName,
                chapterCode,
                chapterName));
    }
}
