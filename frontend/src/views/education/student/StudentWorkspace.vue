<template>
  <el-tabs :model-value="activeTab" @tab-change="handleTabChange">
    <el-tab-pane label="我的考试成绩" name="exam">
      <el-card>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>我的考试（合并视图）</span>
          <el-button @click="refreshStudentExamMerged">刷新</el-button>
        </div>
        <el-table :data="studentExamMerged" size="small" height="560" style="margin-top: 12px;">
          <el-table-column prop="examId" label="ID" width="70" />
          <el-table-column prop="title" label="考试" min-width="180" />
          <el-table-column prop="className" label="专业" width="160" />
          <el-table-column prop="totalScore" label="总分" width="80" />
          <el-table-column label="我的分数" width="90">
            <template #default="{ row }">
              {{ row.myScore === null || row.myScore === undefined ? '--' : row.myScore }}
            </template>
          </el-table-column>
          <el-table-column label="历史均分" width="90">
            <template #default>
              {{ classScoreStats.avgText }}
            </template>
          </el-table-column>
          <el-table-column label="历史最高" width="90">
            <template #default>
              {{ classScoreStats.maxText }}
            </template>
          </el-table-column>
          <el-table-column label="历史最低" width="90">
            <template #default>
              {{ classScoreStats.minText }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="批改结果" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.done" type="success" effect="light">已完成</el-tag>
              <el-tag v-else type="info" effect="light">未完成</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="!row.done" link type="primary" @click="openExamSubmit(row)">作答</el-button>
              <span v-else>已完成</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-tab-pane>

    <el-tab-pane label="数据可视化" name="visual">
      <div class="visual-grid">
        <el-card class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>我的考试趋势</span>
              <el-button link type="primary" @click="refreshStudentVisual">刷新</el-button>
            </div>
          </template>
          <div :ref="studentExamTrendRefSetter" class="chart-box"></div>
        </el-card>

        <el-card class="chart-card">
          <template #header>学习画像雷达</template>
          <div :ref="studentRadarRefSetter" class="chart-box"></div>
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
  studentExamMerged: { type: Array, required: true },
  classScoreStats: { type: Object, required: true },
  studentExamTrendRefSetter: { type: Function, required: true },
  studentRadarRefSetter: { type: Function, required: true },
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
  refreshStudentExamMerged: { type: Function, required: true },
  openExamSubmit: { type: Function, required: true },
  refreshStudentVisual: { type: Function, required: true },
  loadChatData: { type: Function, required: true },
  handleChatModeChange: { type: Function, required: true },
  selectChatContact: { type: Function, required: true },
  selectChatGroup: { type: Function, required: true },
  isSelfChatMessage: { type: Function, required: true },
  openPrivateFromGroup: { type: Function, required: true },
  sendChat: { type: Function, required: true }
})

const emit = defineEmits(['update:activeTab', 'update:chatMode', 'update:chatKeyword', 'update:chatInput'])

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
