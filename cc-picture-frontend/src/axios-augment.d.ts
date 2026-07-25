// 模块增强：openapi 自动生成的 controller 会用到 `requestType: 'form'`，
// 而 axios 的 AxiosRequestConfig 类型里没有这个字段。
// 在这里补上声明，既消除 TS2353 类型报错，也连带修复上传接口返回类型
// 被推断成裸泛型 T 的问题（否则业务代码里 res.data.code 会报错）。
import 'axios'

declare module 'axios' {
  export interface AxiosRequestConfig {
    requestType?: 'form' | 'json' | 'multipart'
  }
}
