<template>
  <el-tabs :model-value="activeTab" @tab-change="handleTabChange">
    <el-tab-pane label="发布任务" name="publish">
      <el-row :gutter="16">
        <el-col :span="10">
          <el-card>
            <el-form label-width="80px">
              <el-form-item label="发布类型">
                <el-select :model-value="publishType" @update:model-value="emit('update:publishType', $event)" @change="loadTeacherPublishData">
                  <el-option label="发布作业" value="homework" />
                  <el-option label="发布考试" value="exam" />
                </el-select>
              </el-form-item>

              <template v-if="publishType === 'homework'">
                <el-form-item label="标题"><el-input v-model="homeworkForm.title" /></el-form-item>
                <el-form-item label="专业">
                  <el-select v-model="homeworkForm.className" placeholder="请选择专业" filterable>
                    <el-option v-for="item in classOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
                <el-form-item label="形式">
                  <el-select v-model="homeworkForm.publishMode">
                    <el-option label="纯文本" value="text" />
                    <el-option label="Word" value="word" />
                    <el-option label="PDF" value="pdf" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="homeworkForm.publishMode === 'text'" label="内容">
                  <el-input v-model="homeworkForm.content" type="textarea" />
                </el-form-item>
                <el-form-item v-else label="附件">
                  <FileUpload
                    v-model="homeworkForm.fileUrl"
                    :limit="1"
                    :file-size="20"
                    :file-type="homeworkUploadTypes"
                  />
                </el-form-item>
                <el-form-item><el-button type="primary" @click="handleCreateHomework">发布作业</el-button></el-form-item>
              </template>

              <template v-else>
                <el-form-item label="考试名称"><el-input v-model="examForm.title" /></el-form-item>
                <el-form-item label="专业">
                  <el-select v-model="examForm.className" placeholder="请选择专业" filterable>
                    <el-option v-for="item in classOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
                <el-form-item label="总分"><el-input-number v-model="examForm.totalScore" :min="1" :max="200" /></el-form-item>
                <el-form-item><el-button type="primary" @click="handleCreateExam">发布考试</el-button></el-form-item>
              </template>
            </el-form>
          </el-card>
        </el-col>
        <el-col :span="14">
          <el-card>
            <el-table v-if="publishType === 'homework'" :data="teacherHomework" size="small" height="320">
              <el-table-column prop="homeworkId" label="ID" width="70" />
              <el-table-column prop="title" label="标题" />
              <el-table-column prop="className" label="专业" />
              <el-table-column label="附件" width="96">
                <template #default="{ row }">
                  <el-button
                    v-if="hasHomeworkAttachment(row.content)"
                    link
                    type="primary"
                    @click="openHomeworkAttachment(row.content)"
                  >
                    查看
                  </el-button>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100" />
            </el-table>
            <el-table v-else :data="teacherExam" size="small" height="320">
              <el-table-column prop="examId" label="ID" width="70" />
              <el-table-column prop="title" label="考试名称" />
              <el-table-column prop="className" label="专业" />
              <el-table-column prop="totalScore" label="总分" width="80" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </el-tab-pane>

    <el-tab-pane label="批改作业" name="review">
      <el-card>
        <el-button @click="loadTeacherHomeworkSubmissions">刷新提交</el-button>
        <el-table :data="teacherSubmissions" size="small" height="380" style="margin-top: 12px;">
          <el-table-column prop="homework_title" label="作业" />
          <el-table-column prop="student_id" label="学生ID" width="90" />
          <el-table-column prop="student_name" label="学生" width="120" />
          <el-table-column prop="answer_content" label="作答" show-overflow-tooltip />
          <el-table-column prop="score" label="分数" width="80" />
          <el-table-column label="评语" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              {{ extractTeacherReasonFromFeedback(row.feedback) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="批改图" width="86">
            <template #default="{ row }">
              <el-button
                v-if="extractReviewImageFromFeedback(row.feedback)"
                link
                type="primary"
                @click="openReviewImage(extractReviewImageFromFeedback(row.feedback), 'teacher-list')"
              >
                查看
              </el-button>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="openHomeworkReview(row)">批改</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-tab-pane>

    <el-tab-pane label="考试评分" name="score">
      <el-card>
        <el-button @click="loadTeacherExamScores">刷新考试作答</el-button>
        <el-table :data="teacherExamScores" size="small" height="380" style="margin-top: 12px;">
          <el-table-column prop="exam_title" label="考试" />
          <el-table-column prop="class_name" label="专业" width="160" />
          <el-table-column prop="student_id" label="学生ID" width="90" />
          <el-table-column prop="student_name" label="学生" width="110" />
          <el-table-column prop="remark" label="作答" show-overflow-tooltip />
          <el-table-column prop="score" label="分数" width="80" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="openExamReview(row)">批改</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-tab-pane>

    <el-tab-pane label="我的管理任务" name="task">
      <el-card>
        <el-table :data="teacherTasks" size="small" height="380">
          <el-table-column prop="taskId" label="ID" width="70" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="content" label="内容" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100" />
        </el-table>
      </el-card>
    </el-tab-pane>

    <el-tab-pane label="查看学生成绩" name="studentScore">
      <el-card>
        <el-button @click="loadTeacherScores">刷新成绩</el-button>
        <el-table :data="teacherScores" size="small" height="380" style="margin-top: 12px;">
          <el-table-column prop="student_id" label="学生ID" width="90" />
          <el-table-column prop="exam_score" label="总成绩" width="90" />
          <el-table-column prop="gender" label="性别" width="90" />
          <el-table-column prop="school_type" label="学校类型" />
        </el-table>
      </el-card>
    </el-tab-pane>

    <el-tab-pane label="数据可视化" name="visual">
      <div class="visual-grid">
        <el-card class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>考试发布趋势</span>
              <el-button link type="primary" @click="refreshTeacherVisual">刷新</el-button>
            </div>
          </template>
          <div :ref="teacherExamTrendRefSetter" class="chart-box"></div>
        </el-card>

        <el-card class="chart-card">
          <template #header>作业批改分布</template>
          <div :ref="teacherHomeworkDistRefSetter" class="chart-box"></div>
        </el-card>

        <el-card class="chart-card">
          <template #header>教师任务状态占比</template>
          <div :ref="teacherTaskPieRefSetter" class="chart-box"></div>
        </el-card>

        <el-card class="chart-card">
          <template #header>教学指标雷达</template>
          <div :ref="teacherRadarRefSetter" class="chart-box"></div>
        </el-card>
      </div>
    </el-tab-pane>

    <el-tab-pane :label="`消息中心${chatContactCount > 0 ? `（${chatContactCount}）` : ''}`" name="message">
      <el-card>
        <template #header>
          <div class="chat-header-line">
            <span>专业私聊</span>
            <el-button link type="primary" @click="loadChatData">刷新</el-button>
          </div>
        </template>
        <div class="chat-layout">
          <aside class="chat-contact-pane">
            <div class="chat-mode-bar">
              <el-segmented v-model="chatModeProxy" :options="chatModeOptions" @change="handleChatModeChange" />
            </div>
            <el-input v-model="chatKeywordProxy" placeholder="搜索姓名或群名" clearable class="chat-search-input" />
            <el-empty
              v-if="chatModeProxy === 'dm' ? filteredChatContacts.length === 0 : filteredChatGroups.length === 0"
              :description="chatModeProxy === 'dm' ? '暂无可联系对象' : '暂无群聊'"
              :image-size="60"
            />
            <div
              v-for="contact in (chatModeProxy === 'dm' ? filteredChatContacts : filteredChatGroups)"
              :key="chatModeProxy === 'dm' ? `dm-${contact.user_id}` : `gp-${contact.groupId || contact.group_id}`"
              :class="['chat-contact-item', {
                active: chatModeProxy === 'dm'
                  ? (activeChatTargetType === 'dm' && String(contact.user_id) === activeChatPeerId)
                  : (activeChatTargetType === 'group' && String(contact.groupId || contact.group_id) === activeChatGroupId)
              }]"
              @click="chatModeProxy === 'dm' ? selectChatContact(contact) : selectChatGroup(contact)"
            >
              <div class="chat-avatar">
                {{ String(chatModeProxy === 'dm' ? (contact.nick_name || '?') : (contact.groupName || contact.group_name || '群')).slice(0, 1) }}
              </div>
              <div class="chat-contact-text">
                <strong>{{ chatModeProxy === 'dm' ? (contact.nick_name || `用户${contact.user_id}`) : (contact.groupName || contact.group_name || '专业群聊') }}</strong>
                <span v-if="chatModeProxy === 'dm'">{{ contact.class_name || '' }} · {{ contact.role_key === 'teacher' ? '教师' : '学生' }}</span>
                <span v-else>{{ contact.className || contact.class_name || '' }} · 群聊</span>
              </div>
            </div>
          </aside>
          <section class="chat-main-pane">
            <div v-if="!activeChatTargetLabel" class="chat-empty">请先选择左侧会话开始聊天</div>
            <template v-else>
              <div class="chat-main-top">{{ activeChatTargetLabel }}</div>
              <div :ref="chatBodyRefSetter" class="chat-message-list" v-loading="chatListLoading">
                <el-empty v-if="chatMessages.length === 0 && !chatListLoading" description="暂无消息，发送第一条吧" :image-size="64" />
                <div
                  v-for="item in chatMessages"
                  :key="item.message_id"
                  :class="['chat-message-row', { self: isSelfChatMessage(item) }]"
                >
                  <div class="chat-bubble">
                    <strong v-if="activeChatTargetType === 'group'" class="chat-sender-name" @click="openPrivateFromGroup(item)">
                      {{ item.sender_name || `用户${item.sender_id}` }}
                    </strong>
                    <p>{{ item.content }}</p>
                    <span>{{ item.create_time }}</span>
                  </div>
                </div>
              </div>
              <div class="chat-editor">
                <el-input
                  v-model="chatInputProxy"
                  type="textarea"
                  :rows="2"
                  resize="none"
                  placeholder="输入消息，回车发送"
                  @keyup.enter.exact.prevent="sendChat"
                />
                <el-button type="primary" :loading="chatSending" @click="sendChat">发送</el-button>
              </div>
            </template>
          </section>
        </div>
      </el-card>
    </el-tab-pane>
  </el-tabs>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  activeTab: { type: String, required: true },
  publishType: { type: String, required: true },
  homeworkForm: { type: Object, required: true },
  examForm: { type: Object, required: true },
  classOptions: { type: Array, required: true },
  homeworkUploadTypes: { type: Array, required: true },
  teacherHomework: { type: Array, required: true },
  teacherExam: { type: Array, required: true },
  teacherSubmissions: { type: Array, required: true },
  teacherExamScores: { type: Array, required: true },
  teacherTasks: { type: Array, required: true },
  teacherScores: { type: Array, required: true },
  teacherExamTrendRefSetter: { type: Function, required: true },
  teacherHomeworkDistRefSetter: { type: Function, required: true },
  teacherTaskPieRefSetter: { type: Function, required: true },
  teacherRadarRefSetter: { type: Function, required: true },
  chatContactCount: { type: Number, required: true },
  chatMode: { type: String, required: true },
  chatKeyword: { type: String, required: true },
  filteredChatContacts: { type: Array, required: true },
  filteredChatGroups: { type: Array, required: true },
  activeChatTargetType: { type: String, required: true },
  activeChatPeerId: { type: String, required: true },
  activeChatGroupId: { type: String, required: true },
  activeChatTargetLabel: { type: String, default: '' },
  chatListLoading: { type: Boolean, required: true },
  chatMessages: { type: Array, required: true },
  chatSending: { type: Boolean, required: true },
  chatInput: { type: String, required: true },
  chatBodyRefSetter: { type: Function, required: true },
  loadTeacherPublishData: { type: Function, required: true },
  handleCreateHomework: { type: Function, required: true },
  handleCreateExam: { type: Function, required: true },
  hasHomeworkAttachment: { type: Function, required: true },
  openHomeworkAttachment: { type: Function, required: true },
  loadTeacherHomeworkSubmissions: { type: Function, required: true },
  extractTeacherReasonFromFeedback: { type: Function, required: true },
  extractReviewImageFromFeedback: { type: Function, required: true },
  openReviewImage: { type: Function, required: true },
  openHomeworkReview: { type: Function, required: true },
  loadTeacherExamScores: { type: Function, required: true },
  openExamReview: { type: Function, required: true },
  loadTeacherScores: { type: Function, required: true },
  refreshTeacherVisual: { type: Function, required: true },
  loadChatData: { type: Function, required: true },
  handleChatModeChange: { type: Function, required: true },
  selectChatContact: { type: Function, required: true },
  selectChatGroup: { type: Function, required: true },
  isSelfChatMessage: { type: Function, required: true },
  openPrivateFromGroup: { type: Function, required: true },
  sendChat: { type: Function, required: true }
})

const emit = defineEmits(['update:activeTab', 'update:publishType', 'update:chatMode', 'update:chatKeyword', 'update:chatInput'])

const chatModeOptions = [
  { label: '私聊', value: 'dm' },
  { label: '群聊', value: 'group' }
]

const chatModeProxy = computed({
  get: () => props.chatMode,
  set: (value) => emit('update:chatMode', value)
})

const chatKeywordProxy = computed({
  get: () => props.chatKeyword,
  set: (value) => emit('update:chatKeyword', value)
})

const chatInputProxy = computed({
  get: () => props.chatInput,
  set: (value) => emit('update:chatInput', value)
})

function handleTabChange(name) {
  emit('update:activeTab', name)
}
</script>
