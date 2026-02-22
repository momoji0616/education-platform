package com.ruoyi.student.service.impl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.student.domain.StudentPerformance;
import com.ruoyi.student.mapper.StudentPerformanceMapper;
import com.ruoyi.student.service.IStudentPerformanceService;

/**
 * 学生成绩表现服务实现
 * 
 * @author ruoyi
 */
@Service
public class StudentPerformanceServiceImpl implements IStudentPerformanceService
{
    private static final Logger log = LoggerFactory.getLogger(StudentPerformanceServiceImpl.class);
    
    @Autowired
    private StudentPerformanceMapper studentPerformanceMapper;

    @Override
    public List<StudentPerformance> selectStudentPerformanceList(StudentPerformance studentPerformance) {
        // 直接返回完整列表，分页逻辑由Controller层通过PageHelper统一处理
        // 这样可以避免分页逻辑冲突
        return studentPerformanceMapper.selectStudentPerformanceList(studentPerformance);
    }

    @Override
    public StudentPerformance selectStudentPerformanceById(Long id) {
        return studentPerformanceMapper.selectStudentPerformanceById(id);
    }

    @Override
    public int insertStudentPerformance(StudentPerformance studentPerformance) {
        try {
            return studentPerformanceMapper.insertStudentPerformance(studentPerformance);
        } catch (Exception e) {
            log.error("新增学生成绩数据失败", e);
            return 0;
        }
    }

    @Override
    public int updateStudentPerformance(StudentPerformance studentPerformance) {
        try {
            return studentPerformanceMapper.updateStudentPerformance(studentPerformance);
        } catch (Exception e) {
            log.error("更新学生成绩数据失败", e);
            return 0;
        }
    }

    @Override
    public int deleteStudentPerformanceById(Long id) {
        try {
            return studentPerformanceMapper.deleteStudentPerformanceById(id);
        } catch (Exception e) {
            log.error("删除学生成绩数据失败", e);
            return 0;
        }
    }

    @Override
    public int deleteStudentPerformanceByIds(Long[] ids) {
        try {
            return studentPerformanceMapper.deleteStudentPerformanceByIds(ids);
        } catch (Exception e) {
            log.error("批量删除学生成绩数据失败", e);
            return 0;
        }
    }

    @Override
    public String importStudentPerformance(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return "导入失败：文件不存在";
            }

            int successCount = 0;
            int failureCount = 0;
            StringBuilder failureMsg = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        // 跳过标题行
                        isFirstLine = false;
                        continue;
                    }
                    
                    try {
                        String[] fields = line.split(",");
                        if (fields.length < 20) {
                            log.warn("CSV行数据字段不足: {}", line);
                            failureCount++;
                            failureMsg.append(line).append("\n");
                            continue;
                        }

                        StudentPerformance student = new StudentPerformance();
                        // 注意：数据库自增ID，不需要手动设置
                        student.setHoursStudied(Convert.toInt(fields[0]));
                        student.setAttendance(Convert.toInt(fields[1]));
                        student.setParentalInvolvement(fields[2]);
                        student.setAccessToResources(fields[3]);
                        student.setExtracurricularActivities(fields[4]);
                        student.setSleepHours(Convert.toInt(fields[5]));
                        student.setPreviousScores(Convert.toInt(fields[6]));
                        student.setMotivationLevel(fields[7]);
                        student.setInternetAccess(fields[8]);
                        student.setTutoringSessions(Convert.toInt(fields[9]));
                        student.setFamilyIncome(fields[10]);
                        student.setTeacherQuality(fields[11]);
                        student.setSchoolType(fields[12]);
                        student.setPeerInfluence(fields[13]);
                        student.setPhysicalActivity(Convert.toInt(fields[14]));
                        student.setLearningDisabilities(fields[15]);
                        student.setParentalEducationLevel(fields[16]);
                        student.setDistanceFromHome(fields[17]);
                        student.setGender(fields[18]);
                        student.setExamScore(Convert.toInt(fields[19]));
                        
                        // 直接插入到数据库
                        int result = studentPerformanceMapper.insertStudentPerformance(student);
                        if (result > 0) {
                            successCount++;
                        } else {
                            failureCount++;
                            failureMsg.append(line).append("\n");
                        }
                    } catch (Exception e) {
                        log.error("解析CSV行数据失败: {}", line, e);
                        failureCount++;
                        failureMsg.append(line).append("\n");
                    }
                }
            }

            return String.format("导入成功 %d 条，失败 %d 条。%s", 
                successCount, failureCount, 
                failureCount > 0 ? "失败信息：" + failureMsg : "");
        } catch (Exception e) {
            log.error("导入学生成绩数据失败", e);
            return "导入失败：" + e.getMessage();
        }
    }

    @Override
    public List<StudentPerformance> exportStudentPerformance(StudentPerformance studentPerformance) {
        // 直接调用查询方法获取数据用于导出
        return studentPerformanceMapper.selectStudentPerformanceList(studentPerformance);
    }
}