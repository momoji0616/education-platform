export {
  listManagerScores,
  createTeacherTask,
  createManagerHomework
} from '@/api/education/admin'

export {
  createHomework,
  listTeacherHomework,
  uploadHomeworkAttachment,
  listTeacherHomeworkSubmissions,
  scoreHomework,
  createExam,
  listTeacherExam,
  scoreExam,
  listTeacherExamScore,
  listTeacherTasks,
  listTeacherScores,
  aiSuggestReview
} from '@/api/education/teacher'

export {
  listStudentHomework,
  submitHomework,
  listStudentHomeworkSubmissions,
  listStudentExam,
  submitExam,
  listStudentExamScore,
  listStudentSelfScores
} from '@/api/education/student'
