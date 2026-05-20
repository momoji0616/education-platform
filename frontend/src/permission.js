import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isHttp, isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register', '/education/auth']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

const normalizeRoles = (roles = []) => roles.map(role => String(role || '').toLowerCase())
const isManagerRole = (roles = []) => roles.includes('admin') || roles.includes('manager')
const isPadOnlyPath = (path = '') => path === '/education' || path.startsWith('/education/')
const shouldForcePad = (path = '') => {
  if (isWhiteList(path)) return false
  if (path === '/401' || path === '/404') return false
  return !isPadOnlyPath(path)
}

const demoTeacherEntry = '/education/auth?role=teacher&redirect=/education/teacher/pad&demo=teacher'

function resolveAuthEntry(path = '') {
  if (path.startsWith('/education/student')) {
    return `/education/auth?role=student&redirect=${encodeURIComponent(path)}`
  }
  if (path.startsWith('/education/teacher')) {
    return `/education/auth?role=teacher&redirect=${encodeURIComponent(path)}`
  }
  if (path.startsWith('/education/admin')) {
    return `/education/auth?role=admin&redirect=${encodeURIComponent(path)}`
  }
  return demoTeacherEntry
}

router.beforeEach((to, from, next) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title)
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else if (isWhiteList(to.path)) {
      next()
    } else {
      if (useUserStore().roles.length === 0) {
        isRelogin.show = true
        useUserStore().getInfo().then(() => {
          isRelogin.show = false
          const userRoles = useUserStore().roles || []
          usePermissionStore().generateRoutes(userRoles).then(accessRoutes => {
            accessRoutes.forEach(route => {
              if (!isHttp(route.path) && (!route.name || !router.hasRoute(route.name))) {
                router.addRoute(route)
              }
            })
            const normalizedRoles = normalizeRoles(userRoles)
            if (!isManagerRole(normalizedRoles) && shouldForcePad(to.path)) {
              next({ path: '/education/pad', replace: true })
              return
            }
            next({ ...to, replace: true })
          })
        }).catch(err => {
          useUserStore().logOut().then(() => {
            ElMessage.error(err)
            next({ path: demoTeacherEntry })
          })
        })
      } else {
        const normalizedRoles = normalizeRoles(useUserStore().roles || [])
        if (usePermissionStore().addRoutes.length === 0) {
          usePermissionStore().generateRoutes(useUserStore().roles || []).then(accessRoutes => {
            accessRoutes.forEach(route => {
              if (!isHttp(route.path) && (!route.name || !router.hasRoute(route.name))) {
                router.addRoute(route)
              }
            })
            if (!isManagerRole(normalizedRoles) && shouldForcePad(to.path)) {
              next({ path: '/education/pad', replace: true })
              return
            }
            next({ ...to, replace: true })
          })
          return
        }
        if (!isManagerRole(normalizedRoles) && shouldForcePad(to.path)) {
          next({ path: '/education/pad', replace: true })
          return
        }
        next()
      }
    }
  } else {
    if (isWhiteList(to.path)) {
      next()
    } else {
      next(resolveAuthEntry(to.path))
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
