package com.ruoyi.student.domain;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 学生成绩因素对象 student_performance
 * 
 * @author ruoyi
 */
public class StudentPerformance extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    
    /** 序号 */
    private Long id;
    
    /** 学习时长（小时） */
    @Excel(name = "学习时长")
    private Integer hoursStudied;

    /** 出勤率（%） */
    @Excel(name = "出勤率")
    private Integer attendance;

    /** 家长参与度 */
    @Excel(name = "家长参与度", readConverterExp = "Low=低,Medium=中,High=高")
    private String parentalInvolvement;

    /** 资源获取情况 */
    @Excel(name = "资源获取", readConverterExp = "Low=低,Medium=中,High=高")
    private String accessToResources;

    /** 是否参加课外活动 */
    @Excel(name = "课外活动", readConverterExp = "Yes=是,No=否")
    private String extracurricularActivities;

    /** 睡眠时间（小时） */
    @Excel(name = "睡眠时间")
    private Integer sleepHours;

    /** 之前成绩 */
    @Excel(name = "之前成绩")
    private Integer previousScores;

    /** 学习动力水平 */
    @Excel(name = "学习动力", readConverterExp = "Low=低,Medium=中,High=高")
    private String motivationLevel;

    /** 是否有互联网访问 */
    @Excel(name = "互联网访问", readConverterExp = "Yes=是,No=否")
    private String internetAccess;

    /** 辅导课程数量 */
    @Excel(name = "辅导课程数")
    private Integer tutoringSessions;

    /** 家庭收入水平 */
    @Excel(name = "家庭收入", readConverterExp = "Low=低,Medium=中,High=高")
    private String familyIncome;

    /** 教师质量 */
    @Excel(name = "教师质量", readConverterExp = "Low=低,Medium=中,High=高")
    private String teacherQuality;

    /** 学校类型 */
    @Excel(name = "学校类型", readConverterExp = "Public=公立,Private=私立")
    private String schoolType;

    /** 同伴影响 */
    @Excel(name = "同伴影响", readConverterExp = "Negative=负面,Neutral=中性,Positive=正面")
    private String peerInfluence;

    /** 体育活动时长（小时/周） */
    @Excel(name = "体育活动")
    private Integer physicalActivity;

    /** 是否有学习障碍 */
    @Excel(name = "学习障碍", readConverterExp = "Yes=是,No=否")
    private String learningDisabilities;

    /** 父母教育水平 */
    @Excel(name = "父母教育水平")
    private String parentalEducationLevel;

    /** 家离学校距离 */
    @Excel(name = "家校距离", readConverterExp = "Near=近,Moderate=中,Far=远")
    private String distanceFromHome;

    /** 性别 */
    @Excel(name = "性别", readConverterExp = "Male=男,Female=女")
    private String gender;

    /** 考试成绩 */
    @Excel(name = "考试成绩")
    private Integer examScore;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Integer getHoursStudied()
    {
        return hoursStudied;
    }

    public void setHoursStudied(Integer hoursStudied)
    {
        this.hoursStudied = hoursStudied;
    }

    public Integer getAttendance()
    {
        return attendance;
    }

    public void setAttendance(Integer attendance)
    {
        this.attendance = attendance;
    }

    public String getParentalInvolvement()
    {
        return parentalInvolvement;
    }

    public void setParentalInvolvement(String parentalInvolvement)
    {
        this.parentalInvolvement = parentalInvolvement;
    }

    public String getAccessToResources()
    {
        return accessToResources;
    }

    public void setAccessToResources(String accessToResources)
    {
        this.accessToResources = accessToResources;
    }

    public String getExtracurricularActivities()
    {
        return extracurricularActivities;
    }

    public void setExtracurricularActivities(String extracurricularActivities)
    {
        this.extracurricularActivities = extracurricularActivities;
    }

    public Integer getSleepHours()
    {
        return sleepHours;
    }

    public void setSleepHours(Integer sleepHours)
    {
        this.sleepHours = sleepHours;
    }

    public Integer getPreviousScores()
    {
        return previousScores;
    }

    public void setPreviousScores(Integer previousScores)
    {
        this.previousScores = previousScores;
    }

    public String getMotivationLevel()
    {
        return motivationLevel;
    }

    public void setMotivationLevel(String motivationLevel)
    {
        this.motivationLevel = motivationLevel;
    }

    public String getInternetAccess()
    {
        return internetAccess;
    }

    public void setInternetAccess(String internetAccess)
    {
        this.internetAccess = internetAccess;
    }

    public Integer getTutoringSessions()
    {
        return tutoringSessions;
    }

    public void setTutoringSessions(Integer tutoringSessions)
    {
        this.tutoringSessions = tutoringSessions;
    }

    public String getFamilyIncome()
    {
        return familyIncome;
    }

    public void setFamilyIncome(String familyIncome)
    {
        this.familyIncome = familyIncome;
    }

    public String getTeacherQuality()
    {
        return teacherQuality;
    }

    public void setTeacherQuality(String teacherQuality)
    {
        this.teacherQuality = teacherQuality;
    }

    public String getSchoolType()
    {
        return schoolType;
    }

    public void setSchoolType(String schoolType)
    {
        this.schoolType = schoolType;
    }

    public String getPeerInfluence()
    {
        return peerInfluence;
    }

    public void setPeerInfluence(String peerInfluence)
    {
        this.peerInfluence = peerInfluence;
    }

    public Integer getPhysicalActivity()
    {
        return physicalActivity;
    }

    public void setPhysicalActivity(Integer physicalActivity)
    {
        this.physicalActivity = physicalActivity;
    }

    public String getLearningDisabilities()
    {
        return learningDisabilities;
    }

    public void setLearningDisabilities(String learningDisabilities)
    {
        this.learningDisabilities = learningDisabilities;
    }

    public String getParentalEducationLevel()
    {
        return parentalEducationLevel;
    }

    public void setParentalEducationLevel(String parentalEducationLevel)
    {
        this.parentalEducationLevel = parentalEducationLevel;
    }

    public String getDistanceFromHome()
    {
        return distanceFromHome;
    }

    public void setDistanceFromHome(String distanceFromHome)
    {
        this.distanceFromHome = distanceFromHome;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public Integer getExamScore()
    {
        return examScore;
    }

    public void setExamScore(Integer examScore)
    {
        this.examScore = examScore;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("hoursStudied", getHoursStudied())
                .append("attendance", getAttendance())
                .append("parentalInvolvement", getParentalInvolvement())
                .append("accessToResources", getAccessToResources())
                .append("extracurricularActivities", getExtracurricularActivities())
                .append("sleepHours", getSleepHours())
                .append("previousScores", getPreviousScores())
                .append("motivationLevel", getMotivationLevel())
                .append("internetAccess", getInternetAccess())
                .append("tutoringSessions", getTutoringSessions())
                .append("familyIncome", getFamilyIncome())
                .append("teacherQuality", getTeacherQuality())
                .append("schoolType", getSchoolType())
                .append("peerInfluence", getPeerInfluence())
                .append("physicalActivity", getPhysicalActivity())
                .append("learningDisabilities", getLearningDisabilities())
                .append("parentalEducationLevel", getParentalEducationLevel())
                .append("distanceFromHome", getDistanceFromHome())
                .append("gender", getGender())
                .append("examScore", getExamScore())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}