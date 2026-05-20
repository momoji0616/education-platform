import request from '@/utils/request'

export function listChatContacts() {
  return request({ url: '/education/pad/chat/contacts', method: 'get' })
}

export function listChatMessages(peerUserId) {
  return request({ url: '/education/pad/chat/messages', method: 'get', params: { peerUserId } })
}

export function sendChatMessage(data) {
  return request({ url: '/education/pad/chat/send', method: 'post', data })
}

export function listChatGroups() {
  return request({ url: '/education/pad/chat/groups', method: 'get' })
}

export function listGroupChatMessages(groupId) {
  return request({ url: '/education/pad/chat/group/messages', method: 'get', params: { groupId } })
}

export function sendGroupChatMessage(data) {
  return request({ url: '/education/pad/chat/group/send', method: 'post', data })
}
