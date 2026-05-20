<template>
  <div :class="['assistant-shell', isTeacher ? 'teacher-shell' : 'student-shell']">
    <aside :class="[isTeacher ? 'teacher-nav' : 'student-nav']">
      <div class="nav-title">{{ isTeacher ? '教师导航' : '学生导航' }}</div>
      <button
        v-for="item in currentNavItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: item.path === currentNavPath }"
        @click="router.push(item.path)"
      >
        <strong>{{ item.title }}</strong>
        <span>{{ item.desc }}</span>
      </button>
    </aside>

    <section class="assistant-main">
      <header class="shell-header">
        <div>
          <p class="eyebrow">{{ eyebrow }}</p>
          <h1>{{ title }}</h1>
          <p class="subtitle">{{ subtitle }}</p>
        </div>
        <div class="header-actions">
          <el-button plain @click="router.push(homePath)">返回{{ roleLabel }}首页</el-button>
          <el-button type="primary" plain @click="router.push(ragPath)">进入智能问答</el-button>
        </div>
      </header>

      <section class="shell-body">
        <aside class="side-panel">
          <div class="tab-row">
            <button
              type="button"
              :class="['tab-btn', { active: activeMode === 'group' }]"
              @click="switchMode('group')"
            >
              群聊
            </button>
            <button
              type="button"
              :class="['tab-btn', { active: activeMode === 'private' }]"
              @click="switchMode('private')"
            >
              私聊
            </button>
          </div>

          <div class="toolbar-block">
            <el-input
              v-model="searchKeyword"
              clearable
              :placeholder="activeMode === 'private' ? '搜索联系人姓名' : '搜索群组名称'"
            />
          </div>

          <div v-if="activeMode === 'group'" class="toolbar-block toolbar-actions">
            <div class="group-actions">
              <el-button v-if="isTeacher" type="primary" plain @click="openCreateGroupDialog">新建群组</el-button>
              <el-button v-if="isTeacher" plain @click="openAddStudentDialog">拉学生进群</el-button>
              <el-button plain @click="loadGroups">刷新群组</el-button>
            </div>
          </div>

          <div class="conversation-list">
            <template v-if="activeMode === 'group'">
              <div
                v-for="item in filteredGroupConversations"
                :key="item.id"
                :class="['conversation-card', { active: item.id === activeConversationId }]"
                @click="selectConversation(item)"
              >
                <div class="conversation-avatar group-avatar">群</div>
                <div class="conversation-main">
                  <strong>{{ item.name }}</strong>
                  <p>{{ item.desc }}</p>
                </div>
              </div>

              <el-empty
                v-if="!filteredGroupConversations.length"
                description="当前暂无群聊会话"
                :image-size="84"
              />
            </template>

            <template v-else>
              <div
                v-for="item in pagedPrivateConversations"
                :key="item.id"
                :class="['conversation-card', { active: item.id === activeConversationId }]"
                @click="selectConversation(item)"
              >
                <div class="conversation-avatar">{{ item.avatarText }}</div>
                <div class="conversation-main">
                  <strong>{{ item.name }}</strong>
                  <p>{{ item.desc }}</p>
                </div>
              </div>

              <el-empty
                v-if="!pagedPrivateConversations.length"
                description="当前暂无可私聊联系人"
                :image-size="84"
              />
            </template>
          </div>

          <div v-if="activeMode === 'private' && filteredPrivateConversations.length" class="pager-row">
            <el-button plain :disabled="privatePage <= 1" @click="privatePage -= 1">上一页</el-button>
            <span>{{ privatePage }} / {{ privatePageCount }}</span>
            <el-button plain :disabled="privatePage >= privatePageCount" @click="privatePage += 1">下一页</el-button>
          </div>
        </aside>

        <section class="chat-panel">
          <div class="chat-header">
            <div>
              <h2>{{ activeConversationTitle }}</h2>
              <p>{{ activeConversationDesc }}</p>
            </div>
            <div class="chat-actions">
              <el-button plain @click="reloadCurrentConversation">刷新会话</el-button>
              <el-button plain @click="clearComposer">清空输入</el-button>
            </div>
          </div>

          <div ref="messageBodyRef" class="message-body">
            <el-empty v-if="!messages.length" description="当前暂无聊天消息" :image-size="96" />
            <div
              v-for="message in messages"
              :key="message.messageId || message.message_id || message.postId || message.id"
              :class="['message-row', { self: isSelfMessage(message) }]"
            >
              <div class="message-bubble">
                <div class="message-author">{{ displayAuthorName(message) }}</div>
                <div class="message-content">{{ displayMessageContent(message) }}</div>
                <div class="message-time">{{ formatDateTime(message.createTime || message.create_time) }}</div>
              </div>
            </div>
            <div ref="messageBottomRef" class="message-bottom-anchor"></div>
          </div>

          <div class="composer">
            <el-input
              v-model="chatInput"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              resize="none"
              placeholder="输入消息内容，按 Ctrl + Enter 可快速发送"
              @keydown.ctrl.enter.prevent="sendMessage"
            />
            <div class="composer-actions">
              <span>{{ composerHint }}</span>
              <el-button type="primary" :loading="sending" :disabled="!canSend" @click="sendMessage">
                发送
              </el-button>
            </div>
          </div>
        </section>
      </section>
    </section>

    <el-dialog v-model="createGroupVisible" title="新建群组" width="420px">
      <el-form label-width="76px">
        <el-form-item label="群组名称">
          <el-input v-model="createGroupForm.groupName" maxlength="30" show-word-limit placeholder="请输入群组名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createGroupVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateGroup">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addStudentVisible" title="拉学生进群" width="560px">
      <el-form label-width="86px">
        <el-form-item label="目标群组">
          <el-select v-model="addStudentForm.groupId" placeholder="请选择群组" style="width: 100%">
            <el-option
              v-for="item in groupConversations"
              :key="item.id"
              :label="item.name"
              :value="item.groupId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择学生">
          <el-select
            v-model="addStudentForm.studentIds"
            multiple
            collapse-tags
            placeholder="请选择学生"
            style="width: 100%"
          >
            <el-option
              v-for="item in privateConversations"
              :key="item.id"
              :label="item.name"
              :value="item.peerUserId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addStudentVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddStudent">确认拉群</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onActivated, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'
import {
  listChatContacts,
  listChatGroups,
  listChatMessages,
  listGroupChatMessages,
  sendChatMessage,
  sendGroupChatMessage
} from '@/api/education/chat'

const PRIVATE_PAGE_SIZE = 6

const props = defineProps({
  role: {
    type: String,
    default: 'teacher'
  }
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMode = ref('group')
const searchKeyword = ref('')
const contacts = ref([])
const groups = ref([])
const activeConversationId = ref('')
const messages = ref([])
const chatInput = ref('')
const sending = ref(false)
const privatePage = ref(1)
const messageBodyRef = ref(null)
const messageBottomRef = ref(null)

const createGroupVisible = ref(false)
const addStudentVisible = ref(false)
const createGroupForm = ref({ groupName: '' })
const addStudentForm = ref({ groupId: '', studentIds: [] })

const isTeacher = computed(() => props.role === 'teacher')
const roleLabel = computed(() => (isTeacher.value ? '教师端' : '学生端'))
const eyebrow = computed(() => (isTeacher.value ? 'Teacher Assistant' : 'Student Assistant'))
const title = computed(() => '师生AI助手')
const subtitle = computed(() =>
  activeMode.value === 'group'
    ? '群聊用于课堂统一沟通与集中互动，支持查看历史消息，并围绕当前教学场景继续追问。'
    : '私聊支持按联系人搜索和分页浏览，避免一次性铺满整个列表，每条消息都保留时间信息。'
)
const homePath = computed(() => (isTeacher.value ? '/education/teacher/pad' : '/education/student/pad'))
const ragPath = computed(() => (isTeacher.value ? '/education/teacher/rag' : '/education/rag'))
const currentNavPath = computed(() => route.path)

const teacherNavItems = [
  { path: '/education/teacher/students', title: '学生管理', desc: '查看学生做题与近期学习表现' },
  { path: '/education/teacher/analysis', title: '学情分析', desc: '查看课程与模块层面的真实学情' },
  { path: '/education/teacher/paper', title: '智能组卷', desc: '按课程、模块与题型生成试卷草案' },
  { path: '/education/teacher/qa', title: '成绩预测', desc: '结合训练模型查看成绩趋势和影响因素' },
  { path: '/education/teacher/assistant', title: '师生AI助手', desc: '查看群聊、私聊与互动记录' },
  { path: '/education/teacher/rag', title: 'RAG智能问答', desc: '结合知识库和场景数据进行智能问答' }
]

const studentNavItems = [
  { path: '/education/student/history', title: '历史做题', desc: '查看每次真实作答记录与得分情况' },
  { path: '/education/student/report', title: '学生诊断', desc: '查看真实作答支撑下的薄弱点分析' },
  { path: '/education/student/plan', title: '学习规划', desc: '把诊断结果转成可执行的学习任务' },
  { path: '/education/student/practice', title: '智能刷题', desc: '基于题库与薄弱模块做针对性训练' },
  { path: '/education/student/prediction', title: '成绩预测', desc: '结合多维数据查看成绩预测结果' },
  { path: '/education/student/assistant', title: '师生AI助手', desc: '进入群聊、私聊与课堂互动问答' },
  { path: '/education/rag', title: 'RAG智能问答', desc: '基于知识库与前序数据进行智能问答' }
]

const currentNavItems = computed(() => (isTeacher.value ? teacherNavItems : studentNavItems))

function currentUserId() {
  return Number(userStore.id || userStore.userId || 0)
}

function normalizeText(value) {
  return String(value || '').trim()
}

function formatDateTime(value) {
  if (!value) return '--'
  const time = new Date(value)
  if (Number.isNaN(time.getTime())) return String(value)
  const y = time.getFullYear()
  const m = String(time.getMonth() + 1).padStart(2, '0')
  const d = String(time.getDate()).padStart(2, '0')
  const hh = String(time.getHours()).padStart(2, '0')
  const mm = String(time.getMinutes()).padStart(2, '0')
  const ss = String(time.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
}

function buildAvatarText(name) {
  const text = normalizeText(name)
  return text ? text.slice(0, 1).toUpperCase() : '?'
}

function displayAuthorName(message) {
  return (
    normalizeText(
      message.authorName ||
      message.author_name ||
      message.senderName ||
      message.sender_name
    ) || '课堂成员'
  )
}

function displayMessageContent(message) {
  return normalizeText(message.content || message.postContent || message.post_content) || '暂无内容'
}

function isSelfMessage(message) {
  const authorId = Number(
    message.authorId ||
    message.author_id ||
    message.senderId ||
    message.sender_id ||
    0
  )
  return authorId && authorId === currentUserId()
}

const privateConversations = computed(() =>
  contacts.value.map(item => ({
    id: `private-${item.userId || item.user_id}`,
    mode: 'private',
    peerUserId: Number(item.userId || item.user_id),
    name: normalizeText(item.nickName || item.nick_name) || '未命名联系人',
    desc: isTeacher.value
      ? `${item.className || item.class_name || '--'} · 学生`
      : `${item.className || item.class_name || '--'} · 教师`,
    avatarText: buildAvatarText(item.nickName || item.nick_name)
  }))
)

const groupConversations = computed(() =>
  groups.value.map(item => ({
    id: `group-${item.groupId || item.group_id}`,
    mode: 'group',
    groupId: String(item.groupId || item.group_id),
    name: normalizeText(item.groupName || item.group_name || item.className || item.class_name) || '默认群聊',
    desc: `${item.className || item.class_name || ''} 群组互动`
  }))
)

const filteredPrivateConversations = computed(() => {
  const keyword = normalizeText(searchKeyword.value).toLowerCase()
  if (!keyword) return privateConversations.value
  return privateConversations.value.filter(item => item.name.toLowerCase().includes(keyword))
})

const filteredGroupConversations = computed(() => {
  const keyword = normalizeText(searchKeyword.value).toLowerCase()
  if (!keyword) return groupConversations.value
  return groupConversations.value.filter(item => item.name.toLowerCase().includes(keyword))
})

const privatePageCount = computed(() =>
  Math.max(1, Math.ceil(filteredPrivateConversations.value.length / PRIVATE_PAGE_SIZE))
)

const pagedPrivateConversations = computed(() => {
  const start = (privatePage.value - 1) * PRIVATE_PAGE_SIZE
  return filteredPrivateConversations.value.slice(start, start + PRIVATE_PAGE_SIZE)
})

const rawConversationList = computed(() =>
  activeMode.value === 'group' ? groupConversations.value : privateConversations.value
)

const activeConversation = computed(() =>
  rawConversationList.value.find(item => item.id === activeConversationId.value) || null
)

const activeConversationTitle = computed(() => {
  if (!activeConversation.value) return activeMode.value === 'group' ? '未选择群组' : '未选择私聊联系人'
  return activeConversation.value.name
})

const activeConversationDesc = computed(() => {
  if (!activeConversation.value) {
    return activeMode.value === 'group'
      ? '请先从左侧选择一个群组。'
      : '请先通过搜索或分页选择一个联系人。'
  }
  return activeConversation.value.desc
})

const composerHint = computed(() =>
  activeMode.value === 'group'
    ? '群聊消息按时间顺序展示，方便围绕课堂话题继续沟通。'
    : '私聊消息按联系人独立展示，每条消息都带有明确时间。'
)

const canSend = computed(() => Boolean(activeConversation.value && chatInput.value.trim()))

watch(searchKeyword, () => {
  if (activeMode.value === 'private') privatePage.value = 1
})

watch(activeMode, () => {
  searchKeyword.value = ''
  if (activeMode.value === 'private') privatePage.value = 1
})

async function loadContacts() {
  try {
    const response = await listChatContacts()
    contacts.value = Array.isArray(response?.data) ? response.data : []
  } catch (error) {
    contacts.value = []
    ElMessage.error('加载私聊联系人失败，请稍后重试')
  }
}

async function loadGroups() {
  try {
    const response = await listChatGroups()
    groups.value = Array.isArray(response?.data) ? response.data : []
  } catch (error) {
    groups.value = []
    ElMessage.error('加载群聊列表失败，请稍后重试')
  }
}

async function ensureConversationSelected() {
  const current = rawConversationList.value
  if (!current.length) {
    activeConversationId.value = ''
    messages.value = []
    return
  }
  if (!current.some(item => item.id === activeConversationId.value)) {
    activeConversationId.value = current[0].id
  }
  await loadMessages()
}

async function loadMessages() {
  if (!activeConversation.value) {
    messages.value = []
    return
  }
  try {
    const response = activeConversation.value.mode === 'group'
      ? await listGroupChatMessages(activeConversation.value.groupId)
      : await listChatMessages(activeConversation.value.peerUserId)
    messages.value = Array.isArray(response?.data) ? response.data : []
    await forceScrollToBottom()
  } catch (error) {
    messages.value = []
    ElMessage.error('加载聊天消息失败，请稍后重试')
  }
}

function scrollToBottom() {
  const el = messageBodyRef.value
  if (!el) return
  if (messageBottomRef.value?.scrollIntoView) {
    messageBottomRef.value.scrollIntoView({ block: 'end', behavior: 'auto' })
  }
  el.scrollTop = el.scrollHeight
}

async function forceScrollToBottom() {
  await nextTick()
  scrollToBottom()
  requestAnimationFrame(() => {
    scrollToBottom()
    setTimeout(() => {
      scrollToBottom()
    }, 120)
  })
}

async function switchMode(mode) {
  if (activeMode.value === mode) return
  activeMode.value = mode
  await ensureConversationSelected()
}

async function selectConversation(item) {
  if (!item || item.id === activeConversationId.value) return
  activeConversationId.value = item.id
  await loadMessages()
}

async function reloadCurrentConversation() {
  await loadMessages()
}

function clearComposer() {
  chatInput.value = ''
}

async function sendMessage() {
  if (!canSend.value) return
  try {
    sending.value = true
    if (activeConversation.value.mode === 'group') {
      await sendGroupChatMessage({
        groupId: activeConversation.value.groupId,
        content: chatInput.value.trim()
      })
    } else {
      await sendChatMessage({
        peerUserId: activeConversation.value.peerUserId,
        content: chatInput.value.trim()
      })
    }
    chatInput.value = ''
    await loadMessages()
  } catch (error) {
    ElMessage.error('发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

function openCreateGroupDialog() {
  createGroupForm.value = { groupName: '' }
  createGroupVisible.value = true
}

function openAddStudentDialog() {
  addStudentForm.value = {
    groupId: activeConversation.value?.mode === 'group' ? activeConversation.value.groupId : '',
    studentIds: []
  }
  addStudentVisible.value = true
}

function submitCreateGroup() {
  if (!normalizeText(createGroupForm.value.groupName)) {
    ElMessage.warning('请先输入群组名称')
    return
  }
  createGroupVisible.value = false
  ElMessage.warning('当前后端还未开放“新建群组”接口，前端入口和弹窗结构已预留完成。')
}

function submitAddStudent() {
  if (!addStudentForm.value.groupId) {
    ElMessage.warning('请先选择目标群组')
    return
  }
  if (!addStudentForm.value.studentIds.length) {
    ElMessage.warning('请至少选择一名学生')
    return
  }
  addStudentVisible.value = false
  ElMessage.warning('当前后端还未开放“拉学生进群”接口，前端入口和选择流程已预留完成。')
}

onMounted(async () => {
  await Promise.all([loadContacts(), loadGroups()])
  if (groupConversations.value.length) {
    activeMode.value = 'group'
  } else {
    activeMode.value = 'private'
  }
  await ensureConversationSelected()
  await forceScrollToBottom()
})

onActivated(async () => {
  await forceScrollToBottom()
})

watch(
  () => messages.value.length,
  async () => {
    await forceScrollToBottom()
  },
  { flush: 'post' }
)

watch(
  () => route.path,
  async () => {
    await forceScrollToBottom()
  },
  { flush: 'post' }
)

watch(
  () => activeConversationId.value,
  async () => {
    await forceScrollToBottom()
  },
  { flush: 'post' }
)
</script>

<style scoped lang="scss">
.assistant-shell {
  min-height: 100vh;
  padding: 28px 28px 40px;
  display: grid;
  grid-template-columns: 284px minmax(0, 1fr);
  gap: 20px;
}

.teacher-shell {
  background:
    radial-gradient(circle at top left, rgba(249, 115, 22, 0.14), transparent 24%),
    linear-gradient(180deg, #fffaf4 0%, #eef6ff 100%);
}

.student-shell {
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.14), transparent 24%),
    linear-gradient(180deg, #f6fbff 0%, #fffdf5 100%);
}

.assistant-main {
  min-width: 0;
}

.teacher-nav,
.student-nav,
.side-panel,
.chat-panel {
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.teacher-nav,
.student-nav {
  padding: 20px;
  height: fit-content;
}

.nav-title {
  margin-bottom: 14px;
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
}

.nav-item {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: #fff;
  border-radius: 18px;
  padding: 18px;
  margin-bottom: 12px;
  cursor: pointer;
}

.nav-item strong {
  display: block;
  color: #0f172a;
  font-size: 21px;
  line-height: 1.25;
}

.nav-item span {
  display: block;
  margin-top: 6px;
  color: #64748b;
  font-size: 17px;
  line-height: 1.7;
}

.teacher-shell .nav-item.active {
  border-color: rgba(249, 115, 22, 0.5);
  background: rgba(255, 247, 237, 0.95);
}

.student-shell .nav-item.active {
  border-color: rgba(14, 165, 233, 0.5);
  background: rgba(240, 249, 255, 0.95);
}

.shell-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  margin-bottom: 24px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.teacher-shell .eyebrow {
  color: #c2410c;
}

.student-shell .eyebrow {
  color: #0369a1;
}

h1,
h2 {
  margin: 0;
  color: #0f172a;
}

h1 {
  font-size: 46px;
  line-height: 1.14;
}

h2 {
  font-size: 28px;
}

.subtitle {
  margin-top: 10px;
  max-width: 900px;
  color: #475569;
  font-size: 17px;
  line-height: 1.9;
}

.header-actions,
.chat-actions,
.group-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.shell-body {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 22px;
  min-height: calc(100vh - 180px);
}

.side-panel {
  padding: 20px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.tab-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.tab-btn {
  width: 100%;
  text-align: center;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: #fff;
  border-radius: 18px;
  padding: 14px 16px;
  cursor: pointer;
  color: #0f172a;
  font-size: 17px;
  font-weight: 700;
}

.teacher-shell .tab-btn.active {
  border-color: rgba(249, 115, 22, 0.5);
  background: rgba(255, 247, 237, 0.95);
}

.student-shell .tab-btn.active {
  border-color: rgba(14, 165, 233, 0.5);
  background: rgba(240, 249, 255, 0.95);
}

.toolbar-block {
  margin-top: 16px;
}

.toolbar-block :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 14px;
  padding: 0 14px;
}

.toolbar-block :deep(.el-input__inner) {
  font-size: 16px;
}

.toolbar-actions {
  padding: 2px 0 4px;
}

.group-actions {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.group-actions :deep(.el-button) {
  width: 100%;
  height: 44px;
  margin-left: 0;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
}

.group-actions :deep(.el-button.is-plain) {
  background: #fff;
  color: #334155;
  border-color: rgba(148, 163, 184, 0.34);
}

.teacher-shell .group-actions :deep(.el-button--primary.is-plain) {
  background: #fff7ed;
  color: #c2410c;
  border-color: rgba(249, 115, 22, 0.35);
}

.student-shell .group-actions :deep(.el-button--primary.is-plain) {
  background: rgba(240, 249, 255, 0.95);
  color: #0369a1;
  border-color: rgba(14, 165, 233, 0.35);
}

.group-actions :deep(.el-button:last-child:nth-child(odd)) {
  grid-column: 1 / -1;
}

.conversation-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.conversation-card {
  display: flex;
  gap: 12px;
  align-items: center;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: #fff;
  border-radius: 18px;
  padding: 14px;
  cursor: pointer;
}

.teacher-shell .conversation-card.active {
  border-color: rgba(249, 115, 22, 0.5);
  background: rgba(255, 247, 237, 0.95);
}

.student-shell .conversation-card.active {
  border-color: rgba(14, 165, 233, 0.5);
  background: rgba(240, 249, 255, 0.95);
}

.conversation-avatar {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.teacher-shell .conversation-avatar {
  background: rgba(255, 237, 213, 1);
  color: #c2410c;
}

.student-shell .conversation-avatar {
  background: rgba(224, 242, 254, 1);
  color: #0369a1;
}

.group-avatar {
  background: rgba(226, 232, 240, 1) !important;
  color: #334155 !important;
}

.conversation-main {
  min-width: 0;
}

.conversation-main strong {
  display: block;
  color: #0f172a;
  font-size: 16px;
}

.conversation-main p {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.7;
  font-size: 14px;
}

.pager-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  color: #64748b;
}

.chat-panel {
  padding: 22px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.chat-header p,
.composer-actions span {
  color: #64748b;
}

.chat-header p {
  margin: 10px 0 0;
  font-size: 16px;
  line-height: 1.8;
}

.message-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.message-bottom-anchor {
  width: 100%;
  height: 1px;
}

.message-row {
  display: flex;
  margin-bottom: 16px;
}

.message-row.self {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 72%;
  padding: 14px 16px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.teacher-shell .message-row.self .message-bubble {
  background: rgba(255, 247, 237, 0.95);
  border-color: rgba(249, 115, 22, 0.24);
}

.student-shell .message-row.self .message-bubble {
  background: rgba(240, 249, 255, 0.95);
  border-color: rgba(14, 165, 233, 0.24);
}

.message-author {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.message-content {
  margin-top: 8px;
  color: #0f172a;
  font-size: 17px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-time {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}

.composer {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
}

.composer :deep(.el-textarea__inner) {
  min-height: 112px !important;
  padding: 16px 18px;
  border-radius: 16px;
  font-size: 16px;
  line-height: 1.8;
}

.composer-actions {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.composer-actions span {
  font-size: 14px;
  line-height: 1.7;
}

@media (max-width: 1200px) {
  .assistant-shell,
  .shell-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .assistant-shell {
    padding: 16px;
  }

  .shell-header,
  .chat-header,
  .composer-actions {
    flex-direction: column;
  }

  .message-bubble {
    max-width: 100%;
  }
}
</style>
