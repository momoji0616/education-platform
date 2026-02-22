package com.ruoyi.student.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.student.domain.StudentPerformance;

/**
 * 学生成绩表现数据层
 * 
 * @author ruoyi
 */
public interface StudentPerformanceMapper
{
    /**
     * 查询学生成绩表现列表
     * 
     * @param studentPerformance 学生成绩表现
     * @return 学生成绩表现集合
     */
    public List<StudentPerformance> selectStudentPerformanceList(StudentPerformance studentPerformance);

    /**
     * 分页查询学生成绩表现列表
     * 
     * @param studentPerformance 学生成绩表现
     * @param offset 偏移量
     * @param limit 每页记录数
     * @return 学生成绩表现集合
     */
    public List<StudentPerformance> selectStudentPerformanceListByPage(@Param("studentPerformance") StudentPerformance studentPerformance, @Param("offset") Integer offset, @Param("limit") Integer limit);

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
     * 删除学生成绩表现
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
}