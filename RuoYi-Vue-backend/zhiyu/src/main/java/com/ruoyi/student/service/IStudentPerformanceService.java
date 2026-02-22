package com.ruoyi.student.service;

import java.util.List;
import com.ruoyi.student.domain.StudentPerformance;

/**
 * 学生成绩表现服务层
 * 
 * @author ruoyi
 */
public interface IStudentPerformanceService
{
    /**
     * 查询学生成绩表现列表
     * 
     * @param studentPerformance 学生成绩表现
     * @return 学生成绩表现集合
     */
    public List<StudentPerformance> selectStudentPerformanceList(StudentPerformance studentPerformance);

    /**
     * 通过ID查询学生成绩表现
     * 
     * @param id 学生成绩表现ID
     * @return 学生成绩表现
     */
    public StudentPerformance selectStudentPerformanceById(Long id);

    /**
     * 新增学生成绩表现
     * 
     * @param studentPerformance 学生成绩表现
     * @return 结果
     */
    public int insertStudentPerformance(StudentPerformance studentPerformance);

    /**
     * 修改学生成绩表现
     * 
     * @param studentPerformance 学生成绩表现
     * @return 结果
     */
    public int updateStudentPerformance(StudentPerformance studentPerformance);

    /**
     * 删除学生成绩表现信息
     * 
     * @param id 学生成绩表现ID
     * @return 结果
     */
    public int deleteStudentPerformanceById(Long id);

    /**
     * 批量删除学生成绩表现
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteStudentPerformanceByIds(Long[] ids);

    /**
     * 从CSV文件导入学生成绩表现数据
     * 
     * @param filePath CSV文件路径
     * @return 结果
     */
    public String importStudentPerformance(String filePath);

    /**
     * 导出学生成绩表现数据
     * 
     * @param studentPerformance 学生成绩表现
     * @return 数据集合
     */
    public List<StudentPerformance> exportStudentPerformance(StudentPerformance studentPerformance);
}