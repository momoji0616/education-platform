package com.ruoyi.student.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface EduLegacyAnalyticsMapper
{
    String selectMappedStudentNoByCurrentUserId(@Param("userId") Long userId);

    String selectMappedClassCodeByCurrentClassName(@Param("currentClassName") String currentClassName);

    List<Map<String, Object>> selectQuestionCatalogs(@Param("courseName") String courseName);

    List<Map<String, Object>> selectQuestionCatalogsFromBank(@Param("courseName") String courseName);

    List<Map<String, Object>> selectTeacherCatalogsByScope(@Param("legacyClassCodes") List<String> legacyClassCodes,
                                                           @Param("courseNames") List<String> courseNames,
                                                           @Param("courseName") String courseName);

    List<Map<String, Object>> selectQuestionBankCandidates(@Param("courseName") String courseName,
                                                           @Param("chapterCode") String chapterCode,
                                                           @Param("chapterName") String chapterName,
                                                           @Param("questionType") String questionType,
                                                           @Param("difficultyLevel") String difficultyLevel,
                                                           @Param("limit") Integer limit);

    Map<String, Object> selectStudentDiagnosisOverview(@Param("studentNo") String studentNo);

    List<Map<String, Object>> selectStudentChapterDiagnosis(@Param("studentNo") String studentNo);

    List<Map<String, Object>> selectStudentWrongQuestions(@Param("studentNo") String studentNo, @Param("limit") Integer limit);

    List<Map<String, Object>> selectStudentWeakKnowledgePoints(@Param("studentNo") String studentNo, @Param("limit") Integer limit);

    Map<String, Object> selectStudentProgramOverview(@Param("studentNo") String studentNo);

    List<Map<String, Object>> selectStudentProgramWeakAssignments(@Param("studentNo") String studentNo, @Param("limit") Integer limit);

    List<Map<String, Object>> selectPracticeRecommendationCandidates(
            @Param("studentNo") String studentNo,
            @Param("courseName") String courseName,
            @Param("chapterCode") String chapterCode,
            @Param("chapterName") String chapterName,
            @Param("limit") Integer limit);

    List<Map<String, Object>> selectStudentAnswerHistory(@Param("studentNo") String studentNo,
                                                         @Param("courseName") String courseName,
                                                         @Param("chapterCode") String chapterCode,
                                                         @Param("questionType") String questionType,
                                                         @Param("limit") Integer limit);

    List<Map<String, Object>> selectStudentHistoryCatalogs(@Param("studentNo") String studentNo,
                                                           @Param("courseName") String courseName);

    Map<String, Object> selectTeacherAnalysisOverviewByScope(@Param("legacyClassCodes") List<String> legacyClassCodes,
                                                             @Param("courseNames") List<String> courseNames);

    List<Map<String, Object>> selectTeacherAnalysisChaptersByScope(@Param("legacyClassCodes") List<String> legacyClassCodes,
                                                                   @Param("courseNames") List<String> courseNames);

    List<Map<String, Object>> selectTeacherHotWrongQuestions(@Param("legacyClassCode") String legacyClassCode, @Param("limit") Integer limit);

    List<Map<String, Object>> selectTeacherKnowledgePoints(@Param("legacyClassCode") String legacyClassCode, @Param("limit") Integer limit);

    Map<String, Object> selectTeacherAssignmentOverview(@Param("legacyClassCode") String legacyClassCode);

    List<Map<String, Object>> selectTeacherAssignmentSummaries(@Param("legacyClassCode") String legacyClassCode, @Param("limit") Integer limit);

    List<Map<String, Object>> selectTeacherStudentModulePerformanceByScope(@Param("legacyClassCodes") List<String> legacyClassCodes,
                                                                    @Param("courseNames") List<String> courseNames,
                                                                    @Param("limit") Integer limit);

    List<Map<String, Object>> selectTeacherStudentManagementPageByScope(@Param("legacyClassCodes") List<String> legacyClassCodes,
                                                                        @Param("courseNames") List<String> courseNames,
                                                                        @Param("studentName") String studentName,
                                                                        @Param("courseName") String courseName,
                                                                        @Param("chapterCode") String chapterCode);

    List<Map<String, Object>> selectTeacherStudentAnswerHistoryByScope(@Param("legacyClassCodes") List<String> legacyClassCodes,
                                                                       @Param("courseNames") List<String> courseNames,
                                                                       @Param("studentNo") String studentNo,
                                                                       @Param("limit") Integer limit);

    Map<String, Object> selectScopedStudentBaseInfo(@Param("legacyClassCodes") List<String> legacyClassCodes,
                                                    @Param("studentNo") String studentNo);
}
