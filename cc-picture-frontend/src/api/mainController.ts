// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** doLogin POST /api/sa-token/main/doLogin */
export async function doLoginUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.doLoginUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<string>('/api/sa-token/main/doLogin', {
    method: 'POST',
    params: {
      ...params
    },
    ...(options || {})
  })
}

/** health GET /api/sa-token/main/health */
export async function healthUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseString_>('/api/sa-token/main/health', {
    method: 'GET',
    ...(options || {})
  })
}

/** isLogin GET /api/sa-token/main/isLogin */
export async function isLoginUsingGet(options?: { [key: string]: any }) {
  return request<string>('/api/sa-token/main/isLogin', {
    method: 'GET',
    ...(options || {})
  })
}

/** logout GET /api/sa-token/main/logout */
export async function logoutUsingGet(options?: { [key: string]: any }) {
  return request<Record<string, any>>('/api/sa-token/main/logout', {
    method: 'GET',
    ...(options || {})
  })
}

/** tokenInfo GET /api/sa-token/main/tokenInfo */
export async function tokenInfoUsingGet(options?: { [key: string]: any }) {
  return request<Record<string, any>>('/api/sa-token/main/tokenInfo', {
    method: 'GET',
    ...(options || {})
  })
}
