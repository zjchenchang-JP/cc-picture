# 2026/07/18

## userController.ts 逐行讲解(逻辑 / 基本语法 / 泛型)

### 附:完整源码

#### userController.ts

```ts
// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addUser POST /api/user/add */
export async function addUserUsingPost(
  body: API.UserAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/user/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** deleteUser POST /api/user/delete */
export async function deleteUserUsingPost(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** getUserById GET /api/user/get */
export async function getUserByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUser_>('/api/user/get', {
    method: 'GET',
    params: {
      ...params
    },
    ...(options || {})
  })
}

/** getLoginUser GET /api/user/get/login */
export async function getLoginUserUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO_>('/api/user/get/login', {
    method: 'GET',
    ...(options || {})
  })
}

/** getUserVOById GET /api/user/get/vo */
export async function getUserVoByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserVOByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserVO_>('/api/user/get/vo', {
    method: 'GET',
    params: {
      ...params
    },
    ...(options || {})
  })
}

/** listUserVOByPage POST /api/user/list/page/vo */
export async function listUserVoByPageUsingPost(
  body: API.UserQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserVO_>('/api/user/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** userLogin POST /api/user/login */
export async function userLoginUsingPost(
  body: API.UserLoginRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoginUserVO_>('/api/user/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** userLogout POST /api/user/logout */
export async function userLogoutUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/user/logout', {
    method: 'POST',
    ...(options || {})
  })
}

/** userRegister POST /api/user/register */
export async function userRegisterUsingPost(
  body: API.UserRegisterRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/user/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}

/** updateUser POST /api/user/update */
export async function updateUserUsingPost(
  body: API.UserUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}
```

#### typings.d.ts

```ts
declare namespace API {
  // ...
  type SpaceUserVO = {
    createTime?: string
    id?: number
    space?: SpaceVO
    spaceId?: number
    spaceRole?: string
    updateTime?: string
    user?: UserVO
    userId?: number
  }

  type SpaceVO = {
    createTime?: string
    editTime?: string
    id?: number
    maxCount?: number
    maxSize?: number
    permissionList?: string[]
    spaceLevel?: number
    spaceName?: string
    spaceType?: number
    totalCount?: number
    totalSize?: number
    updateTime?: string
    user?: UserVO
    userId?: number
  }

  type TaskMetrics = {
    failed?: number
    succeeded?: number
    total?: number
  }

  type testDownloadFileUsingGETParams = {
    /** filepath */
    filepath?: string
  }

  type uploadPictureUsingPOSTParams = {
    fileUrl?: string
    id?: number
    picColor?: string
    picName?: string
    spaceId?: number
  }

  type User = {
    createTime?: string
    editTime?: string
    id?: number
    isDelete?: number
    updateTime?: string
    userAccount?: string
    userAvatar?: string
    userName?: string
    userPassword?: string
    userProfile?: string
    userRole?: string
  }

  type UserAddRequest = {
    userAccount?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    current?: number
    id?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    userAccount?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    checkPassword?: string
    userAccount?: string
    userPassword?: string
  }

  type UserUpdateRequest = {
    id?: number
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type UserVO = {
    createTime?: string
    id?: number
    userAccount?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type View = {
    contentType?: string
  }
}
```

---

### 一、文件整体定位

这个文件做的事很简单:**把后端 `/api/user/*` 这一组 REST 接口,封装成一个个类型安全的 TypeScript 函数,供 Vue 组件调用。** 调用者只需 `await addUserUsingPost({...})`,不用关心 URL、请求方法、请求头这些细节。

它是一份由 OpenAPI 工具根据后端 Swagger 文档**自动生成**的前端 API 调用层(从 git 里的 `openapi.config.js` / `default_OpenAPI.json` 可以印证),所以 10 个函数长得几乎一样——理解一个就等于理解全部。

它依赖两样东西:

- `src/request.ts` → 提供 `request`(其实是 axios 实例)
- `src/api/typings.d.ts` → 提供 `API.*` 一堆类型

### 二、开头三行:注释与导入

```ts
// @ts-ignore
/* eslint-disable */
import request from '@/request'
```

| 行 | 作用 |
|---|---|
| `// @ts-ignore` | 告诉 TypeScript 编译器**忽略下一行的类型检查**。这里下一行是 eslint 注释,本身不会触发 TS 报错,所以这行基本是生成器"保险起见"加的,实际作用不大。 |
| `/* eslint-disable */` | 一次性关闭本文件**所有 ESLint 规则**(比如命名规范、未使用变量等)。因为自动生成的代码不可能满足项目的人工规范,所以整文件豁免。 |
| `import request from '@/request'` | 引入默认导出的 axios 实例。`@/` 是 Vite/脚手架配的路径别名,指 `src/` 目录。 |

> ⚠️ **关键点(很多人误解)**:这里导入的 `request` **不是**一个叫 `request` 的函数,而是 `request.ts` 里 `export default myAxios` 导出的那个 **axios 实例**。axios 实例本身是一个**可被调用的对象(callable)**,所以下面才能写成 `request<T>(url, config)`。

### 三、彻底讲透「函数模板」(以 addUser 为例)

```ts
/** addUser POST /api/user/add */
export async function addUserUsingPost(
  body: API.UserAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/user/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    data: body,
    ...(options || {})
  })
}
```

这是整文件的「模板」,逐行拆解。

#### 第 1 行:`/** ... */` —— 文档注释(JSDoc)

```ts
/** addUser POST /api/user/add */
```

- 这是 **JSDoc 注释**。IDE(VSCode/WebStorm)会在你鼠标悬停到函数名上时,把这行文字作为提示显示出来。
- 内容是生成器拼出来的:`addUser`(后端方法名)+ `POST`(HTTP 方法)+ `/api/user/add`(路径)。**纯展示用,不影响运行。**

#### 第 2~5 行:函数签名(参数 + 泛型在这一层最密集)

```ts
export async function addUserUsingPost(
  body: API.UserAddRequest,
  options?: { [key: string]: any }
) {
```

逐词拆:

- `export` —— 把这个函数作为**模块的命名导出**。其他文件可以 `import { addUserUsingPost } from '@/api/userController'`。
- `async` —— 把函数标记为异步函数。两个作用:
  1. 让函数**永远返回 Promise**(即使你 return 一个普通值,也会被包成 `Promise.resolve(...)`)。
  2. 允许函数体内使用 `await`(本文件里其实没用到 `await`,所以这里的 `async` **严格说是多余的**,但生成器统一加,便于以后扩展,也语义上更清楚——这是个异步请求)。
- `function addUserUsingPost` —— 普通函数声明。命名规则 `xxxUsingPost` / `xxxUsingGet` 是生成器约定,后缀表示 HTTP 方法,便于一眼区分。
- 第一个参数 `body: API.UserAddRequest`:
  - `API.UserAddRequest` 是定义在 `typings.d.ts` 里的类型,长这样:
    ```ts
    type UserAddRequest = {
      userAccount?: string
      userAvatar?: string
      userName?: string
      userProfile?: string
      userRole?: string
    }
    ```
  - 每个字段后面的 `?` 表示**可选属性**——创建用户时不是每个字段都必须填。
  - **类型约束**:你传给 `body` 的对象必须长成这个形状,否则编译报错。这就是 TypeScript 提供的"调用前类型保护"。
- 第二个参数 `options?: { [key: string]: any }`:
  - 外层 `?` 表示**整个参数可传可不传**。
  - `{ [key: string]: any }` 是 **索引签名(index signature)**:表示"一个对象,它的键是字符串,值是任意类型"。这是给调用者留的**逃生口**,比如想额外传一个 `timeout: 5000` 或自定义 `headers`,都能塞进来。

#### 第 6~13 行:函数体(请求配置 + 泛型真正发挥作用)

```ts
return request<API.BaseResponseLong_>('/api/user/add', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  data: body,
  ...(options || {})
})
```

**① `return request<API.BaseResponseLong_>(...)` —— 泛型调用,全文件最精髓的一行**

这一行同时做了三件事:

1. **调用 axios 实例**:`request(url, config)` 这种"两个参数"的写法,用的是 axios 提供的重载签名 `axios(url, config?)`。
2. **传入泛型实参 `<API.BaseResponseLong_>`**:告诉 axios"请把响应体 `response.data` 当成 `API.BaseResponseLong_` 类型"。这个类型是:
   ```ts
   type BaseResponseLong_ = {
     code?: number
     data?: number   // ← 业务数据,这里是"新增用户后的 id"
     message?: string
   }
   ```
3. **决定返回类型**:整个函数最终返回 `Promise<AxiosResponse<API.BaseResponseLong_>>`。

**泛型是怎么"流动"的?** axios 实例的签名简化后是这样:

```ts
interface AxiosInstance {
  <T>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>>
}
```

那个 `<T>` 是 axios 定义的**类型参数(占位符)**。你写 `request<BaseResponseLong_>(...)`,就等于把 `T = BaseResponseLong_` 代入,于是 `response.data` 的类型就是 `BaseResponseLong_`,调用方拿到响应后访问 `res.data.data` 时能享受完整的智能提示和类型检查。

> 一句话总结这一行:**用一个泛型实参,把"HTTP 调用"和"业务数据类型"绑在一起,让前后端的数据契约在前端被静态检查。**

**② 配置对象**

```ts
{
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  data: body,
  ...(options || {})
}
```

- `method: 'POST'` —— HTTP 方法。注意它是字符串字面量,axios 的类型定义把它限制成 `'GET' | 'POST' | ...` 等固定值,写错会报错。
- `headers: { 'Content-Type': 'application/json' }` —— 声明请求体是 JSON 格式。所以 `body` 会被 axios 序列化成 JSON 字符串发出去。**这是 POST 接口的标配**。
- `data: body` —— **请求体**。axios 约定:`data` 放 body(POST 用),`params` 放 URL 查询参数(GET 用,见后面的函数)。
- `...(options || {})` —— **展开运算符 + 短路兜底**,见下。

**③ `...(options || {})` —— 两个小语法点**

- `options || {}`:如果调用者没传 `options`(即为 `undefined`),就用空对象 `{}` 兜底,避免 `...undefined` 报错。这是 JS 的**短路求值**(`undefined || {}` → `{}`)。
- `...`:**对象展开运算符**,把 `options` 里的所有键值对"摊开"合并进当前配置对象。

**合并优先级**:JS 对象展开,后面的同名键会**覆盖**前面的。所以这里的写法意味着:**调用者通过 `options` 传进来的配置,可以覆盖生成器写死的默认配置**(比如覆盖 headers)。这是一个虽小但实用的设计。

### 四、其余函数:同一个模板的变体

理解了模板,剩下 9 个函数只需看差异点。

#### 1. deleteUser —— 只是换类型

- 入参 `API.DeleteRequest`(只有 `{ id?: number }`)。
- 泛型实参换成 `API.BaseResponseBoolean_`(返回 `data: boolean`,表示删除成功与否)。
- 其余完全一样。

#### 2. getUserById —— **GET 请求,关键差异在这里**

```ts
export async function getUserByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUser_>('/api/user/get', {
    method: 'GET',
    params: { ...params },   // ← 注意是 params,不是 data
    ...(options || {})
  })
}
```

四个 GET 特有点:

1. **参数名从 `body` 改成 `params`**:GET 请求按 RESTful 习惯不带 body,参数要拼到 URL 查询串上(如 `/api/user/get?id=5`)。axios 的 `params` 字段就是干这个的。
2. **注释「叠加生成的Param类型…」**:说明 Swagger 对"非 body 参数"默认不会生成一个独立的 Request 对象类型,生成器只能把这些零散的查询参数手工聚合成 `getUserByIdUsingGETParams`(就是 `{ id?: number }`)。
3. **没有 `headers: { 'Content-Type': ... }`**:GET 请求没有 body,自然不需要声明 JSON Content-Type。
4. **`params: { ...params }`**:这里特意展开拷贝了一份新对象,而不是直接 `params: params`,是为了**避免修改外部传入的原始对象**(axios 内部会往 params 上挂一些东西),是一种防御性写法。

#### 3. getLoginUser —— **无业务参数**

- 函数只接受可选的 `options`,没有业务入参——因为"获取当前登录用户"靠的是后端 session/cookie,不需要传任何 id。
- `request.ts` 里的 `withCredentials: true` 正是为这种依赖 cookie 的接口准备的。
- 泛型用 `API.BaseResponseLoginUserVO_`,返回脱敏后的登录用户信息(`LoginUserVO`)。

#### 4. getUserVOById —— 和 getUserById 几乎一样

- 唯一区别:泛型返回 `API.BaseResponseUserVO_`(返回 `UserVO`,即脱敏后的视图对象,不含 `userPassword` 等敏感字段;对比 `User` 含 `userPassword`)。

#### 5. listUserVOByPage —— 分页查询

- POST + body(查询条件 + 分页参数),泛型返回 `API.BaseResponsePageUserVO_`。`PageUserVO_` 里 `records: UserVO[]` 是当前页数据,`total`/`pages`/`current`/`size` 是分页元信息。

#### 6. userLogin

- 入参 `UserLoginRequest`(`userAccount` + `userPassword`)。
- 泛型返回 `BaseResponseLoginUserVO_`(注意和 `getLoginUser` 返回类型一样——登录成功后顺手把登录用户信息返回,前端可直接存进状态)。

#### 7. userLogout

- POST 但**没有 body**(后端只清 session)。注意这里连 `data` 都没有,直接 `method: 'POST'` + 展开 options。

#### 8. userRegister

- 入参 `UserRegisterRequest`(比 login 多一个 `checkPassword`)。
- 泛型返回 `BaseResponseLong_`(注册成功返回新用户 id)。

#### 9. updateUser

- 入参 `UserUpdateRequest`(必带 `id` + 可改的字段),返回 `BaseResponseBoolean_`。

### 五、贯穿全文件的核心 TS/JS 概念小结

| 概念 | 在本文件的体现 | 为什么这么用 |
|---|---|---|
| **泛型 `<T>`** | `request<API.BaseResponseLong_>(...)` | 让同一个 axios 实例能服务于「任意返回类型」的接口,且类型在调用处确定、全程可推导。 |
| **可选参数 `?`** | `options?: {...}`、类型字段 `id?: number` | 调用灵活,字段可缺省。 |
| **索引签名 `{[k:string]: any}`** | `options` 的类型 | 给调用者留"任意额外配置"的口子,否则固定死的配置对象无法扩展。 |
| **对象展开 `...`** | `...(options \|\| {})`、`{ ...params }` | 合并/覆盖默认配置;浅拷贝防止污染入参。 |
| **短路求值 `\|\|`** | `options \|\| {}` | 防 `undefined`,等价于 `options ?? {}`(更现代写法)。 |
| **`async`/Promise** | 每个函数都 `async` + `return` 一个 axios Promise | 让函数返回 Promise,调用方 `await`。本文件没用 `await`,async 在此主要是语义标注。 |
| **`declare namespace`** | `API.*` 类型来自 `typings.d.ts` 的全局命名空间 | 不写 import 就能用 `API.UserAddRequest`——它是全局可见的类型集合,适合自动生成的大批类型。 |
| **类型字面量 `'POST'`** | `method: 'POST'` | axios 用字符串字面量联合类型,把 method 限定为合法 HTTP 方法。 |

### 六、调用方长什么样(建立闭环)

组件里实际使用时大概是这样:

```ts
import { addUserUsingPost } from '@/api/userController'

const res = await addUserUsingPost({ userAccount: 'tom', userName: 'Tom' })
// res 的类型: AxiosResponse<API.BaseResponseLong_>
// res.data.data 就是新建用户的 id(number)
```

得益于泛型,`res.data.data` 在 IDE 里有自动补全和类型检查——**这就是这份看似重复的生成代码的真正价值**:把后端接口的入参/出参契约,以类型的形式固化到前端。

### 七、可延伸的方向

- `request` 作为 axios 实例为什么能「带泛型被调用」(axios 类型源码层面)。
- 这套 OpenAPI 生成器(`openapi.config.js`)是怎么从后端文档产出这些代码的。
- 怎么手动封装一个带 `BaseResponse<T>` 自动解包的 `request` 函数(目前调用方每次都要 `res.data.data`,可以优化)。

---

## Vue 单页应用(SPA) 启动流程:index.html → main.ts → App.vue

> 问题缘起:访问项目时,全局网页挂载流程是怎样的?又是 App,又是 main.ts,又是 index.html;为什么在 App.vue 里 `fetch` 而不是 main.ts 里获取?

一个 Vue 单页应用(SPA)启动时,实际上只有**一个真实页面** `index.html`,其余全是 JS 动态生成。下面按时间顺序把这条链子串起来。

### 一、启动链路(谁触发谁)

```
浏览器打开网址
   │
   ▼
① index.html            ← 唯一的真实 HTML 页面
   │   <div id="app"></div>           ← 空的挂载点
   │   <script src="/src/main.ts">    ← 入口脚本
   ▼
② main.ts 被执行         ← "搭舞台"
   │   createApp(App)    → 创建 Vue 应用实例
   │   app.use(Antd)     → 注册组件库
   │   app.use(createPinia()) → 注册状态管理
   │   app.use(router)   → 注册路由
   │   app.mount('#app') → 把 App.vue 渲染进那个空 div
   ▼
③ App.vue 渲染            ← 根组件开始工作
   │   <script setup> 执行 → 此时调 fetchLoginUser()
   │   模板渲染 <BasicLayout />
   ▼
④ BasicLayout.vue 渲染
   │   <GlobalHeader />    ← 头部
   │   <router-view />     ← 当前路由对应的页面
   ▼
⑤ GlobalHeader.vue 渲染
       读取 loginUserStore.loginUser → 显示用户名/登录按钮
```

**记忆要点**:`index.html`(壳)→ `main.ts`(引导)→ `App.vue`(根组件)→ 子组件层层渲染。`#app` 这个 id 在 [index.html:10](index.html#L10) 和 [main.ts:13](src/main.ts#L13) 是**同一个东西**——main.ts 把 App.vue 挂到那个 div 里。

### 二、三者的职责分工

| 文件 | 角色 | 干什么 | 不该干什么 |
|------|------|--------|-----------|
| `index.html` | 外壳 | 提供页面骨架 + 指定入口脚本 | 不写业务逻辑 |
| `main.ts` | 引导程序 | 创建 app、装插件、挂载 | 不写业务请求 |
| `App.vue` | 根组件 | 应用启动后的入口、全局初始化 | 不管具体页面布局(交给 BasicLayout) |

### 三、为什么 fetch 放 App.vue,不放 main.ts?

两个原因:

#### 1. 时序问题(Pinia 还没就绪)

在 main.ts 里如果想用 store,代码大概长这样:

```ts
const pinia = createPinia()
app.use(pinia)
const store = useLoginUserStore()  // ❌ app 还没 mount,会报错/拿不到正确实例
store.fetchLoginUser()
app.mount('#app')
```

Pinia 的 store 依赖 **active pinia 实例**。这个"激活"动作是在 `app.mount()` 之后、组件 `setup` 执行时才自动完成的。在 main.ts 顶层直接调 `useLoginUserStore()` 会失败或警告。

而 App.vue 的 `<script setup>` 是在**挂载后**执行的,此时 Pinia、Router 全部就绪,调用 store 完全安全。见 [App.vue:10-11](src/App.vue#L10-L11):

```ts
const loginUserStore = useLoginUserStore()  // ✅ 此时 Pinia 已激活
loginUserStore.fetchLoginUser()
```

#### 2. 职责清晰

- **main.ts** = "搭舞台的人",只管把 Vue + Pinia + Router + Antd 装配起来。它不该关心"登录用户是谁"这种**业务**问题。
- **App.vue** = "应用本身",应用一启动就要知道"当前登录用户是谁"——这是应用级初始化,放在根组件最合理。

而且 `<script setup>` 的代码会在组件创建时**自动执行一次**,正好符合"应用启动时拉一次"的需求;拉到数据后更新 `loginUser.value`,由于是响应式 `ref`,所有用到它的子组件(GlobalHeader)会**自动更新**,无需额外操作。

### 四、一句话总结

> `index.html` 是壳,`main.ts` 负责把舞台搭好并挂载,`App.vue` 是挂载后第一个跑起来的业务组件——所以**应用级初始化(如拉取登录态)放 App.vue**,既满足 Pinia 就绪的时序,又符合"根组件负责全局初始化"的职责划分。

---

## 异步数据加载:真值判断、await 与渲染时序

> 问题缘起:在 [userManagePage.vue](src/pages/admin/userManagePage.vue) 写 `fetchData` 拉取用户列表时,连续冒出三个疑问——
> 1. `if (res.data.data)` 到底在判断什么?`null`? `undefined`? 空列表 `[]` 算不算?
> 2. `fetchData` 明明是要"展示数据"的,为什么里面还能用 `await`?async/await 不就是异步、不阻塞吗?
> 3. 如果页面先渲染了、数据还没返回,用户看到的难道不是空列表?

### 一、`if (res.data.data)` 的判断逻辑(truthy / falsy)

`if (x)` 会把 `x` **隐式转换成布尔值**,这套规则叫真值(truthy)/假值(falsy)测试。

**Falsy 值(判定为 `false`)只有这几个:**
`false`、`0`、`-0`、`0n`、`''`/`""`/`` ``(空字符串)、`null`、`undefined`、`NaN`

**除此之外全是 truthy**,其中有两个反直觉的陷阱:

| 值 | `if (value)` | 说明 |
|----|--------------|------|
| `null` | `false` | ✅ |
| `undefined` | `false` | ✅ |
| `0` / `''` | `false` | ✅ |
| `[]`(空数组) | **`true`** ⚠️ | 空数组是对象,truthy |
| `{}`(空对象) | **`true`** ⚠️ | 空对象也是 truthy |
| `[1,2,3]` | `true` | |

**对本项目分页接口尤其重要**:看 [userController.ts:87](src/api/userController.ts#L87),`listUserVoByPageUsingPost` 返回的是 `Page<UserVO>` 对象,形如:

```js
{ records: [...], total: 12, current: 1, pageSize: 10 }
```

所以 `res.data.data` 是个**对象**。哪怕一条数据都没查到,后端也会返回 `{ records: [], total: 0 }` 这个非空对象 → `if (res.data.data)` 依然是 `true`。要真正区分"查到了"和"没查到",得写:

```js
if (res.data.data?.records?.length > 0)   // 真正有数据
```

### 二、为什么"要展示数据"还能用 await —— async/await 的真正含义

这个疑问源于一个常见误解:把"异步"理解成了"不需要等、代码立刻往下跑"。

**异步的真正含义是**:

> "我等结果的时候,不让浏览器(主线程)干等着。"
>
> 而不是说"我自己不等结果了"。

数据从服务器回来需要物理时间(网络往返),这谁都省不掉。async/await 改变的只是"**怎么等**",不是"要不要等"。

#### 两套时间线要分开看

| 时间线 | async/await 卡不卡它 |
|--------|----------------------|
| **整个浏览器**(渲染页面、响应点击、跑其他代码) | ❌ 不卡 |
| **当前这个 async 函数内部**(`fetchData` 里的代码) | ✅ 会暂停等待 |

`await` 卡的是**第二条线**(函数自己),不是第一条线(浏览器)。函数暂停期间,页面照常渲染、用户照常点按钮——**这就是"异步"**。

#### 为什么函数里"必须"await

```js
// ❌ 不用 await
const fetchData = async () => {
  const res = listUserVoByPageUsingPost(...)   // 马上返回,还是个没完成的 Promise
  if (res.data.data) { ... }                    // 💥 res.data 是 undefined,报错!
}
```

```js
// ✅ 用 await
const fetchData = async () => {
  const res = await listUserVoByPageUsingPost(...)  // 函数在这里暂停,等数据回来
  if (res.data.data) { ... }                         // 数据到了,能用 res.data.data 了
}
```

不用 await,拿到的 `res` 是个**半成品**(还没装好数据的 Promise),马上用 `res.data.data` 必然出错。`await` 的作用就是**等数据装好,再让你用**。

#### 一个类比:点餐

| 写法 | 你(函数)的行为 | 餐厅(浏览器) |
|------|------------------|----------------|
| 回调 / `.then`(老写法) | 留电话,自己先去逛街干别的,做好了被叫回来 | 一直正常营业 |
| `await`(新写法) | 就在柜台前站着等 | **照样正常营业**,服务别的客人 |

两种写法对**餐厅(浏览器)**都没卡死——这才是"异步"。区别只是**你(函数)**是"去逛街了"还是"在柜台干等"。`await` 选了后者,所以代码读起来像同步一样直观。

### 三、页面先渲染、数据后返回,用户会看到空列表吗?

**会,这是 SPA 的典型行为,不是 bug。** 时序如下:

```
1. 进入页面 → 组件渲染 → dataList = [] → 表格先渲染成空(0 行)
2. onMounted → fetchData() 发起请求(耗时 200ms~1s)
3. 请求返回 → await 恢复 → dataList.value = res.data.data.records
4. Vue 响应式触发 → 表格重新渲染 → 数据出现
```

第 1 步到第 4 步之间,用户**确实会看到一瞬间空表格**。标准解法是加**加载态(loading)**:

```vue
<a-table :loading="loading" :data-source="dataList" :columns="columns" />
```

```js
const loading = ref(false)
const dataList = ref([])

const fetchData = async () => {
  loading.value = true                        // 开始转圈
  const res = await listUserVoByPageUsingPost({ ...searchParams })
  if (res.data.data) {
    dataList.value = res.data.data.records ?? []   // 填数据
    total.value = res.data.data.total ?? 0
  }
  loading.value = false                       // 停止转圈
}

onMounted(() => fetchData())                  // 别忘了触发
```

### 四、一句话总结

> `if (x)` 走 truthy/falsy 规则,空数组 `[]`、空对象 `{}` 都是 truthy——分页接口返回的 `Page` 对象恒为 truthy,判断"有没有数据"要看 `records.length`。`async/await` 是异步(不卡浏览器),`await` 是在函数内部"优雅地等结果",两者配合:既不卡死页面、又能拿到数据;正因如此,页面可以先画空表格,等数据回来再由响应式自动刷新。

---

# 2026/07/21

## access.ts 全局权限控制:Vue Router 路由守卫 + Pinia 状态校验
```ts
// 全局权限控制文件。可以利用 Vue Router 的路由守卫实现，
// 每次切换并进入页面前，都会检查一下当前用户是否具有特定页面的权限
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import router from '@/router'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 */
router.beforeEach(async (to,from,next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  // 为了防止每次切换路由都从远程获取用户​​​信息，定义了 firstFetchLoginUser ⁠⁠⁠变量
  // 用于控制在刷新页面后只会请求后端一次
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  // 不是首次
  const toUrl = to.fullPath
  // 管理员权限
  if (toUrl.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('没有权限')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  next()
})

```

> 问题缘起:这个文件在 `main.ts` 里被 `import './access'` 引入(只为副作用,不取任何导出),它到底干了什么?为什么一个"空导入"就能实现全局权限拦截?刷新页面后 Pinia 状态丢了,权限会不会判错?

### 一、文件整体定位

`access.ts` 是项目的**全局权限控制中心**。它利用 Vue Router 的**全局前置守卫** `router.beforeEach`,在每次路由切换**之前**插入一段校验逻辑:检查当前用户是否有权限访问目标页面,没有就拦截并跳转登录。完整源码见 [access.ts](src/access.ts)。

文件很短,但浓缩了 Vue 全家桶的几个核心概念:路由守卫、Pinia 状态、async/await、模块级单例。

### 二、开头三行:模块导入

```ts
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import router from '@/router'
```

| 语句 | 导入方式 | 说明 |
|------|----------|------|
| `{ useLoginUserStore }` | **命名导入** | 从 Pinia store 文件取出工厂函数,花括号里名字必须与导出一致 |
| `{ message }` | **命名导入** | ant-design-vue 的全局消息组件,`message.error(...)` 弹红色错误提示 |
| `router` | **默认导入** | 取出 `@/router` 的 `default` 导出(路由实例),名字可自定义 |

- `@/` 是 Vite 配的路径别名,指 `src/` 目录,避免写 `../../` 相对路径。
- **Pinia store 命名约定**:工厂函数以 `use` 开头(`useLoginUserStore`),调用后返回响应式的全局状态对象。

### 三、模块级变量:解决刷新丢状态的关键

```ts
let firstFetchLoginUser = true
```

- `let`:块级作用域、可重新赋值的变量(第 20 行会改成 `false`)。
- **关键概念——模块级单例**:它写在模块顶层,整个应用生命周期内**只有一份**,跨路由跳转都共享;只有**刷新页面**才会重置为 `true`。

为什么要这个标记?因为存在一个经典痛点:

> Pinia 把状态存在 JS 内存里,**一刷新页面内存就清空**,`loginUser` 变回初始空值。如果此时直接做权限判断,会误判"用户没登录"。所以首次进入时,必须**先等后端接口返回真实登录态**,再判断;判断完就置为 `false`,后续跳转不再重复请求。

### 四、router.beforeEach —— 全局前置守卫(核心)

```ts
router.beforeEach(async (to, from, next) => {
  ...
})
```

这是 Vue Router 的**全局前置守卫**。每次路由切换前都会**异步**执行这个回调,执行完(或 `next` 被调用)才决定是否真正跳转。

#### 守卫三参数

| 参数 | 含义 | 类型 |
|------|------|------|
| `to` | 即将进入的目标路由对象 | `RouteLocationNormalized` |
| `from` | 当前正要离开的路由对象 | `RouteLocationNormalized` |
| `next` | 控制跳转走向的函数(Vue Router 4 可省略,这里保留传统写法) | `(to?) => void` |

#### 路由对象的常用属性

- `to.fullPath`:**完整路径**(含 query 参数),如 `/admin/user` 或 `/user/login?redirect=/xxx`。
- `to.path`:仅路径部分,不含 query。

#### next() 的三种用法(本文件用了两种)

```ts
next()                                  // 放行,继续进入 to
next(`/user/login?redirect=${...}`)     // 强制重定向到登录页
// next(false)                          // 中断当前导航,留在原地(本文件没用)
```

### 五、async / await —— 等后端返回再判断

```ts
router.beforeEach(async (to, from, next) => {
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()   // ← 关键
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  ...
})
```

- `async`:把守卫标为异步函数。**Vue Router 的守卫天然支持返回 Promise**,所以用 `async` 函数完美兼容——它会等函数内所有 `await` 跑完。
- `await`:**暂停当前守卫函数**,等后端接口 resolve 后再继续。

**为什么必须 await?** `fetchLoginUser()` 内部发的是异步 HTTP 请求,若不 await,下一行 `loginUser = loginUserStore.loginUser` 拿到的还是旧值(空),权限判断必然出错。这和 [userManagePage.vue](src/pages/admin/userManagePage.vue) 里"要展示数据就必须 await"是同一个道理——详见上一篇「异步数据加载」。

### 六、Pinia store 的读取

```ts
const loginUserStore = useLoginUserStore()
let loginUser = loginUserStore.loginUser
```

- `useLoginUserStore()`:在守卫里**按需调用**拿到同一个 store 实例(Pinia 是单例)。
- `loginUserStore.loginUser`:直接访问 store 里响应式状态 `loginUser`(当前登录用户对象)。
- `const` vs `let` 的细节:`const loginUserStore` 因为引用固定不变;`let loginUser` 因为第 19 行要**重新赋值**为更新后的用户对象。

### 七、权限判断逻辑与重定向

```ts
const toUrl = to.fullPath

if (toUrl.startsWith('/admin')) {
  if (!loginUser || loginUser.userRole !== 'admin') {
    message.error('没有权限')
    next(`/user/login?redirect=${to.fullPath}`)
    return
  }
}
next()
```

涉及的语法点:

1. **`String.prototype.startsWith()`**:判断字符串是否以指定前缀开头。这里用来识别"管理员专属页面"(`/admin/*`)。
2. **短路求值 `||`**:`!loginUser || loginUser.userRole !== 'admin'` —— "未登录"**或**"角色不是 admin"任一成立即无权限。
   - **顺序很重要**:先判 `!loginUser`,若未登录就直接短路,避免对 `undefined` 读 `userRole` 报错。
3. **模板字符串** `` `/user/login?redirect=${to.fullPath}` ``:把用户原本想去的路径作为 `redirect` query 参数传给登录页,**登录成功后可跳回原页面**,体验更好。
4. **`return`**:`next(...)` 后**必须 return**,否则会继续执行到末尾的 `next()`,导致"重定向"和"放行"同时触发,产生导航冲突。
5. **末尾 `next()`**:所有检查通过后的"默认放行"。

### 八、整体流程图

```
任意路由跳转触发
   │
   ▼
beforeEach 执行
   │
   ├─ 是首次加载? ──是──→ await 请求后端拿登录用户,标记为非首次
   │
   ▼
目标 URL 以 /admin 开头?
   ├─ 是 ─→ 未登录 或 非admin? ──是──→ message.error + 跳登录页(带 redirect) + return
   │                              └─否─┐
   └─ 否 ─────────────────────────────→ next() 放行
```

### 九、概念小结

| 概念 | 在本文件的作用 |
|------|----------------|
| **Vue Router 全局前置守卫** | 拦截所有路由跳转做权限校验 |
| **Pinia 状态管理** | 读取/同步全局登录用户状态 |
| **ES Module**(命名/默认导入、路径别名) | 引入依赖 |
| **async/await** | 等待异步接口返回再判断 |
| **模块级单例变量** | 标记"是否首次",处理刷新丢状态问题 |
| **路由对象**(to/from/next、fullPath) | 获取目标路径、控制导航走向 |
| **模板字符串 + query 参数 redirect** | 登录后回跳到原目标页 |
| **短路求值 `||`** | 安全地做"未登录或权限不足"判断 |
| **副作用导入**(main.ts 里 `import './access'`) | 不取导出,只为注册守卫 |

### 十、一句话总结

> `access.ts` 用 `router.beforeEach` 把"权限校验"插到每次跳转前:靠**模块级变量**识别首次加载、用 **await** 等后端返回真实登录态、用 **短路判断 + 重定向**拦下无权访问——这就是一个不写组件、只靠"副作用导入"就能守护全站路由的轻量方案。

---

## GlobalHeader.vue 菜单过滤:ref 解包、计算属性与一次 "filter is not a function" 排错

> 问题缘起:用 `filter` 按 `userRole` 动态隐藏"用户管理"菜单,结果控制台报 `Uncaught TypeError: menus?.filter is not a function`。明明写了可选链 `?.`,为什么还是崩了?

### 一、报错现场与原始代码

[GlobalHeader.vue](src/components/GlobalHeader.vue) 里用一份静态菜单 `originItems`,经 `filterMenus` 过滤后交给 `computed` 生成实际展示的 `items`:

```ts
const originItems = ref<MenuProps['items']>([ ... ])

// 权限控制 非admin不应该看见 '用户管理' 菜单
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    if (menu.key.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== "admin") {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const items = computed<MenuProps['items']>(() => filterMenus(originItems))
```

### 二、根因:ref 没有解包 `.value`

问题出在最后一行 `filterMenus(originItems)`——`originItems` 是 `ref(...)`,它是个 **RefImpl 包装对象**,真正的数组藏在 `.value` 里。把 ref 对象本身传进函数,于是:

1. 形参 `menus` 拿到的是 **RefImpl**(既不是 `null` 也不是 `undefined`)。
2. `menus?.filter(...)`:可选链 `?.` **只在左侧为 `null`/`undefined` 时短路**,RefImpl 都不是,所以**不短路**,继续访问 `.filter`。
3. RefImpl 上没有 `.filter` 属性 → `menus?.filter` 求值为 `undefined`。
4. 紧接着 `undefined(...)` 调用 → **`TypeError: menus?.filter is not a function`**。

> 默认参数 `menus = []` 和可选链 `?.` 都没救得了它——它们只防 `undefined`,不防"传错了类型的对象"。

### 三、修复

**最小改动**:在 computed 里解包 ref。

```ts
const items = computed<MenuProps['items']>(() => filterMenus(originItems.value))
```

**更地道的可选优化**:菜单数据是静态的,本就不需要 `ref`,直接用普通常量,顺带消灭 `.value` 的坑:

```ts
const originItems: MenuProps['items'] = [ ... ]
const items = computed<MenuProps['items']>(() => filterMenus(originItems))
```

响应式不会丢——真正驱动菜单变化的是 `filterMenus` 内部读到的 `loginUserStore.loginUser`,跟 `originItems` 是不是 ref 无关。

### 四、涉及的语法逐点拆

#### 1. `ref<MenuProps['items']>([...])`

- `ref<T>(value)`:创建响应式引用,泛型 `T` 指明包裹值类型。**模板里自动解包,`<script>` 里必须 `.value`**——这是 bug 的温床。
- `MenuProps['items']`:**索引访问类型**(indexed access type),取 `MenuProps` 类型里 `items` 属性的类型,复用 ant-design-vue 官方定义,免手写。

#### 2. 菜单项字段:key / icon / label / title

| 字段 | 作用 | 本文件用法 |
|------|------|-----------|
| `key` | 菜单唯一标识,点击事件 `onMenuClick({ key })` 会拿到 | 巧妙用**路由 path** 当 key,点击即跳转 |
| `icon` | 图标,**接收返回 VNode 的函数** | `() => h(HomeOutlined)` |
| `label` | 菜单文字,可以是字符串或 VNode | 第三项用 `h('a', ...)` 渲染 `<a>` 外链 |
| `title` | 鼠标悬停小气泡 | 纯展示 |

#### 3. `h()` 渲染函数

`h` 是 createVNode 的简写,签名 `h(类型, props, children)`:

```ts
h(HomeOutlined)                                          // 渲染组件
h('a', { href, target: '_blank' }, 'Author Github')      // 渲染原生 <a>
```

`icon` 写成箭头函数 `() => h(...)`,是因为 ant-design-vue 约定 `icon` 接收**函数**(延迟渲染)。

#### 4. `filterMenus` 里的语法点

- **默认参数 + 类型断言** `menus = [] as MenuProps['items']`:不传参时用 `[]` 兜底,`as` 告诉 TS 把空数组当菜单项类型(否则推断成 `never[]`)。
- **可选链 `?.filter`**:防 `undefined`。⚠️ **只防 `null`/`undefined`**,不防类型不对的对象(正是本次 bug)。
- **`Array.prototype.filter`**:回调返回 `true` 保留、`false` 剔除。
- **`menu.key.startsWith('/admin')`**:前缀匹配识别管理员菜单。
- **短路求值 `||`**:`!loginUser || loginUser.userRole !== "admin"`,先判未登录避免对 `undefined` 读属性报错(和 [access.ts](src/access.ts) 同款写法)。

#### 5. `computed` 与响应式闭环

`computed<T>(() => ...)` 声明计算属性,返回只读 ref,依赖变化才重算且带缓存。这里 `filterMenus` 内部访问了 `loginUserStore.loginUser`(Pinia state),所以 `items` 把登录态当作依赖——**登录/注销后菜单自动重新过滤**,admin 登录后"用户管理"项立刻出现,无需手动刷新。

### 五、一个潜在 TS 告警(运行时不影响)

`MenuProps['items']` 元素是联合类型(普通项 / 子菜单 / 分组…),并非每条都有 `key`。严格类型检查下 `menu.key` 可能报"属性不存在",稳妥写法是 `menu.key?.startsWith('/admin')`。

### 六、一句话总结

> 报错本质是 `ref` 对象没解包就被当数组用,`?.` 只防 `undefined` 防不了"类型不对"的对象,改成 `filterMenus(originItems.value)` 即可;整段逻辑的灵魂是——**用路由 path 当菜单 key、用 `filter` 按 `userRole` 裁剪菜单、用 `computed` 让菜单随登录态自动更新**。

---

# 2026/07/25

## 受控组件 PictureUpload:从语法到前后端联动的完整拆解

> 问题缘起:写图片上传组件 [PictureUpload.vue](src/components/PictureUpload.vue) 时,疑问像滚雪球一样冒出来——
> 1. `defineProps<Props>()` 这种泛型写法到底是什么意思?`onSuccess?: (...) => void` 这种"函数当 prop"又怎么理解?
> 2. 注释说它是"受控组件",到底受谁控制?跟普通组件有什么区别?
> 3. [PictureUpload.vue](src/components/PictureUpload.vue) 和 [AddPicturePage.vue](src/pages/AddPicturePage.vue) 两个文件,数据是怎么来回联动的?
> 4. 上传时 `res.data.code` 为什么会报「类型 `T` 上不存在属性 `code`」?
> 5. `uploadPictureUsingPost(params, {}, file)` 里那个空对象 `{}` 是干嘛的?后端明明需要 `PictureUploadRequest`,传空对象后端怎么收?
> 6. 后端的 `requestType: 'form'` 从哪来?为什么教程里的版本不用补类型?
>
> 这篇笔记把整条线索串起来:从一个组件的语法,到父子联动,再到一次完整的前后端文件上传请求。

### 附:完整源码

#### PictureUpload.vue(子组件 / 受控组件)

```vue
<template>
  <div id="pictureUpload" class="picture-upload">
    <a-upload
      list-type="picture-card"
      :show-upload-list="false"
      :customRequest="handleUpload"
      :before-upload="beforeUpload"
    >
      <img v-if="picture?.url" :src="picture?.url" alt="avatar" />
      <div v-else>
        <loading-outlined v-if="loading"></loading-outlined>
        <plus-outlined v-else></plus-outlined>
        <div class="ant-upload-text">点击或拖拽上传图片</div>
      </div>
    </a-upload>
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue';
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { uploadPictureUsingPost } from '@/api/pictureController';

/**
 * 受控组件
 * 由父组件 (图片创建页面 AddPicturePage) 来管理
 */
interface Props {
  picture?: API.PictureVO
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

/** 上传前校验 */
const beforeUpload = (file: File) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
  if (!isJpgOrPng) {
    message.error('不支持上传该格式的图片，推荐 jpg 或 png');
  }
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    message.error('不能上传超过 2M 的图片');
  }
  return isJpgOrPng && isLt2M;
};

const loading = ref<boolean>(false)

/** 上传 */
const handleUpload = async ({file}: any) => {
  loading.value = true
  try {
    // 调用后端上传图片接口时，如果已经有 pictureId
    // 表示对已上传的图片进行更新，需要将该参数也添加到请求中，否则每次都会新增图片记录
    const params = props.picture ? {id: props.picture.id} : {};
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data){
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error: any) {
    message.error('图片上传失败：' + (error?.message ?? ''))
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.picture-upload :deep(.ant-upload) {
  width: 100% !important;
  height: 100% !important;
  min-width: 152px;
  min-height: 152px;
}
.picture-upload img {
  max-width: 100%;
  max-height: 480px;
}
.ant-upload-select-picture-card i {
  font-size: 32px;
  color: #999;
}
.ant-upload-select-picture-card .ant-upload-text {
  margin-top: 8px;
  color: #666;
}
</style>
```

#### AddPicturePage.vue(父组件 / 状态持有者)

```vue
<template>
  <div id="addPicturePage">
    <PictureUpload :picture="picture" :onSuccess="onSuccess" />
  </div>
</template>

<script setup lang="ts">
import PictureUpload from '@/components/PictureUpload.vue'
import { ref } from 'vue'

const picture = ref<API.PictureVO>()
const onSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}
</script>

<style scoped></style>
```

---

### 一、第一层:`defineProps<Props>()` 语法拆解

```ts
interface Props {
  picture?: API.PictureVO
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()
```

逐点拆:

- `interface Props` —— TS 接口,描述「这个组件能接收哪些外部参数(props)」。
- `picture?` —— `?` 表示**可选属性**,父组件可传可不传。类型 `API.PictureVO` 来自 [typings.d.ts](src/api/typings.d.ts) 的全局命名空间(所以不用 import)。
- `onSuccess?: (newPicture: API.PictureVO) => void` —— 一个**函数类型的可选 prop**。`=>` 左边是参数列表,右边是返回值类型。意思是「父组件可以传一个函数进来:它接收一张新图片、没有返回值」。
- `defineProps<Props>()` —— Vue 的**编译宏**(只在编译期生效,运行时不存在,所以它不用 import)。尖括号里的 `Props` 是**基于类型的 props 声明**:Vue 编译器读这个接口,自动把每个字段变成一个 prop 并带上类型。等价于运行时写法:

```ts
const props = defineProps({
  picture: { type: Object, default: undefined },
  onSuccess: { type: Function, default: undefined }
})
```

只是 TS 写法更省事、类型也更安全。

> 💡 **关键认知:prop 不只能传数据,还能传函数。** 传数据(`picture`)是父→子单向流动;传函数(`onSuccess`)是为了让子组件能"反向通知"父组件——这正是下面父子联动的基石。

### 二、第二层:什么是"受控组件"

区别只在**状态存谁那**:

| | 状态存在哪 | 谁来改 |
|---|---|---|
| 非受控组件 | 子组件内部(`const url = ref('')`) | 子组件自己 |
| **受控组件**(本例) | **父组件** AddPicturePage 里 | **父组件**改,子组件只负责"显示 + 触发" |

PictureUpload 自己**不存**当前图片状态,而是从 `props.picture` 读(模板里 `picture?.url` 展示预览图);上传成功后也不自己改状态,而是调 `props.onSuccess(...)` 请父组件改。类比 HTML 的 `<input v-model="x">`:输入框自己不记值,值存在父组件的 `x` 里。

### 三、第三层:两个页面到底怎么联动(核心)

数据流是**单向数据流 + 回调通知**(Vue 父子通信的标准模式)。结合真实代码,完整链路如下:

```
┌──────────────────── 父:AddPicturePage.vue ────────────────────┐
│                                                                │
│  const picture = ref<API.PictureVO>()       ← 状态真正存在这里 │
│                                                                │
│  const onSuccess = (newPicture: API.PictureVO) => {           │
│      picture.value = newPicture             ← 父组件负责改状态 │
│  }                                                             │
│                                                                │
│   <PictureUpload :picture="picture" :onSuccess="onSuccess" /> │
└───────────┬──────────────────────────────────────┬────────────┘
            │  ① :picture="picture"                 │  ② :onSuccess="onSuccess"
            │     数据向下(当前图片,用于预览)        │     函数向下(让子组件能反向叫一声)
            ▼                                      ▼
┌──────────────────── 子:PictureUpload.vue ─────────────────────┐
│                                                                │
│  props.picture           ← 读父组件给的图片(模板展示其 url)    │
│                                                                │
│  handleUpload (用户选文件 → a-upload 的 customRequest 触发):   │
│    const params = props.picture ? {id: props.picture.id} : {}  │
│    const res = await uploadPictureUsingPost(params, {}, file)  │
│    if (res.data.code === 0 && res.data.data) {                 │
│        props.onSuccess?.(res.data.data)   ← ③ 反向调用回调      │
│    }                                                           │
│         │                                                      │
│         └──→ 实际执行的是父组件里的 onSuccess 函数              │
│              → picture.value = newPicture      ④ 状态更新      │
│              → 响应式触发:PictureUpload 重新渲染               │
│              → 模板里 picture?.url 变成刚上传的新图片           │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

#### 联动四步(对照代码行)

1. **父持有状态**:`const picture = ref<API.PictureVO>()`([AddPicturePage.vue:11](src/pages/AddPicturePage.vue#L11))——初始为 `undefined`。
2. **父把"数据"和"函数"都传下去**:`<PictureUpload :picture="picture" :onSuccess="onSuccess" />`([:3](src/pages/AddPicturePage.vue#L3))。两个 `:` 是 `v-bind:` 的简写。
3. **子在上传成功后反向调用回调**:`props.onSuccess?.(res.data.data)`([PictureUpload.vue](src/components/PictureUpload.vue) 的 handleUpload 里)。`?.` 是可选链——万一父组件没传 `onSuccess`,这里静默跳过不报错。
4. **回调实际是父的函数,改的是父的状态**:`picture.value = newPicture`([AddPicturePage.vue:13](src/pages/AddPicturePage.vue#L13))。改完因为是响应式 `ref`,子组件读到的 `props.picture` 自动更新,预览图刷新——**无需任何手动通知**。

> 一句话:**数据向下(`:prop`)、事件向上(`props.回调()`)**,闭环。子组件始终不拥有状态,只是"显示器 + 触发器"。

### 四、template 里的 a-upload 怎么用

```vue
<a-upload
  list-type="picture-card"
  :show-upload-list="false"
  :customRequest="handleUpload"
  :before-upload="beforeUpload"
>
```

| 属性 | 作用 |
|---|---|
| `list-type="picture-card"` | 上传区域显示成「卡片样式」(带边框的方框) |
| `:show-upload-list="false"` | 不显示下方默认的文件列表(因为本例用自定义预览) |
| `:customRequest="handleUpload"` | **覆盖默认上传行为**,把上传逻辑交给我们的 `handleUpload`。否则 a-upload 会用自己的内置请求,我们没法塞自定义参数 |
| `:before-upload="beforeUpload"` | 上传**前**的钩子,返回 `false` 会中止上传。这里用来做格式/大小校验 |

**模板的 `v-if` 渲染逻辑**(三种状态):

```vue
<img v-if="picture?.url" :src="picture?.url" />   <!-- 已有图片:显示预览 -->
<div v-else>
  <loading-outlined v-if="loading" />             <!-- 上传中:转圈 -->
  <plus-outlined v-else />                        <!-- 空闲:加号 -->
  <div class="ant-upload-text">点击或拖拽上传图片</div>
</div>
```

> 注意:`handleUpload = async ({file}: any) => {...}` 解构出 `file`。这个 `file` 是 a-upload 传给 customRequest 的参数对象里的字段(实际是 `UploadFile` 包装对象),不是浏览器原始 `File`,所以这里暂时用 `any`。而 `beforeUpload` 拿到的 `file` 是原始 `File`(见下一节的坑)。

### 五、style 语法(scoped / `:deep()` / `!important`)

```css
.picture-upload :deep(.ant-upload) {
  width: 100% !important;
  height: 100% !important;
  min-width: 152px;
  min-height: 152px;
}
```

三个关键语法点,逐个讲:

#### 1. `<style scoped>` —— 作用域隔离

`scoped` 让样式**只作用于当前组件**。原理:Vue 编译时给当前组件的每个元素加一个唯一属性(如 `data-v-a1b2c3d4`),同时把 CSS 选择器也改写带上这个属性:

```css
/* 你写的 */            .picture-upload img { ... }
/* 编译后 */            .picture-upload img[data-v-a1b2c3d4] { ... }
```

好处是不会污染全局;副作用是——**选不到子组件内部、或组件库内部渲染的元素**(它们没有这个 `data-v` 属性)。

#### 2. `:deep(.ant-upload)` —— 穿透 scoped 的深度选择器

`.ant-upload` 是 ant-design-vue 在组件**内部**渲染的元素,它没有当前组件的 `data-v` 属性。如果直接写 `.picture-upload .ant-upload`,编译后会带上 `[data-v-xxx]` 而选不中。

`:deep()` 就是解决这个的——告诉 Vue「括号里的选择器要穿透到子组件/组件库内部」:

```css
/* 你写的 */            .picture-upload :deep(.ant-upload) { ... }
/* 编译后 */            .picture-upload[data-v-a1b2c3d4] .ant-upload { ... }
/*                       ↑ data-v 只挂在 .picture-upload 上,.ant-upload 不带,所以能选中 */
```

> 凡是要改 ant-design-vue 这类组件库内部元素的样式,基本都要套一层 `:deep()`。

#### 3. `!important` —— 提权覆盖组件库默认值

ant-design-vue 给 `.ant-upload`(picture-card 模式)设了固定的宽高。我们的 `width:100%` 想铺满父容器,但**优先级不够**盖不过它,所以加 `!important` 强行提权。`!important` 是「最后手段」,能用特异性解决就别滥用。

#### 4. 其余规则

- `.picture-upload img { max-width/height }`:限制预览图大小,防止大图撑爆卡片。
- `.ant-upload-select-picture-card i / .ant-upload-text`:调加号图标和提示文字的字号、颜色——这俩选择器没套 `:deep()` 其实是**选不中的**(同 scoped 原理),属于遗留/兜底写法,不影响主功能。

### 六、上传请求的三个参数:params / body / file 分工

这是最易混淆的一处。看 [pictureController.ts](src/api/pictureController.ts) 里 `uploadPictureUsingPost(params, body, file)` 的实现,**三个参数走的是三条独立的路**:

| 参数 | 放在哪里 | controller 怎么处理 | 本例传的值 | 后端接收 |
|---|---|---|---|---|
| `params`(第1) | **URL 查询字符串** | `params: {...params}` 拼到 URL(`?id=123`) | `{id}` 或 `{}` | **`PictureUploadRequest`**(见第七节) |
| `body`(第2) | **FormData 的额外字段** | 遍历后 `formData.append(...)` | `{}` | (不对应,空着) |
| `file`(第3) | **FormData 的 file 部分** | `formData.append('file', file)` | 用户选的文件 | `@RequestPart("file") MultipartFile` |

所以「**文件根本不走 params,而是走第三个参数 file**」:

```
uploadPictureUsingPost(params, {}, file)
        │              │     │
        │              │     └─→ formData.append('file', file)  → multipart 请求体的 file 部分 ← 文件在这!
        │              └─→ FormData 额外字段(本例没有)
        └─→ URL query string(?id=xxx)
```

- **`params = {}` 时文件照样传**:只是 URL 不挂 `id`,文件照常经 `file` 参数走 FormData 上传。
- **axios 的 `params` vs `data`**:`params` 拼 URL 查询串(GET 习惯),`data` 放请求体(POST 用)。文件用 `data: formData`。

### 七、前后端对接:后端的 `PictureUploadRequest` 从哪来

> 困惑:前端 `body` 传的是空对象 `{}`,后端接口却要一个 `PictureUploadRequest`,这怎么对得上?

**对得上——`PictureUploadRequest` 不是从前端 `body` 来的,而是从前端 `params`(URL query)来的。** 看后端接口签名 [PictureController.java](../cc-picture-backend/src/main/java/com/zjcc/ccpicturebackend/controller/PictureController.java):

```java
public BaseResponse<PictureVO> uploadPicture(
        @RequestPart("file") MultipartFile multipartFile,   // ← 文件
        PictureUploadRequest pictureUploadRequest,           // ← 无任何注解!
        HttpServletRequest request)
```

`PictureUploadRequest` 这个参数**没有 `@RequestBody`、没有 `@RequestParam`、什么注解都没有**。Spring MVC 对无注解的 POJO,默认行为是:**从 URL query 参数(和 form 普通字段)按字段名绑定**到对象上。于是它会从 `?id=123&picName=xxx` 里取值填进去。

而 OpenAPI 文档(Knife4j)把这个无注解 POJO 识别成一组 query 参数,前端生成器就把它们收拢进了 `uploadPictureUsingPOSTParams`。对比两边字段——**5 个一模一样**:

| 前端 `uploadPictureUsingPOSTParams` | 后端 `PictureUploadRequest` |
|---|---|
| `fileUrl?` | `String fileUrl` |
| `id?` | `Long id` |
| `picColor?` | `String picColor` |
| `picName?` | `String picName` |
| `spaceId?` | `Long spaceId` |

完整对应关系:

| 前端参数 | HTTP 怎么传 | 后端接收 |
|---|---|---|
| `params` | URL query string | **`PictureUploadRequest`** |
| `file` | FormData 的 file 部分 | `@RequestPart("file") MultipartFile` |
| `body={}` | FormData 普通字段 | (空着,不对应任何后端参数) |

所以 `body: {}` 是 openapi 模板**预留的「额外 form 字段」槽位**,本例没东西可放就传空,它跟 `PictureUploadRequest` 毫无关系。

### 八、DTO 设计:为什么 `PictureUploadRequest` 有这么多字段

> 疑问:更新图片明明只需要一个 `id`,为什么 DTO 还要 `fileUrl/picName/spaceId/picColor`?

因为 **`PictureUploadRequest` 不是「更新专用」的,它是「上传图片」这个业务动作的通用请求对象,一个类服务多个场景**。每个字段各管一摊(均有 service 层代码佐证):

| 字段 | 服务的场景 |
|---|---|
| `id` | **更新图片**(换文件、保留老记录)——service 里据此判断新增还是更新 |
| `spaceId` | **上传到指定空间**(私人/团队空间,而非公共图库)——service 里校验空间是否存在 |
| `picName` | **自定义图片名**(默认用文件名,可手动覆盖;批量上传时自动生成「前缀+序号」) |
| `fileUrl` | **URL 上传**(另一个接口 `uploadPictureByUrl` 用,不传文件传地址) |
| `picColor` | **图片主色调**(扩展字段,留给按颜色搜索等场景) |

**核心认知:DTO 字段是「能力声明」,不是「必填清单」。** 这次更新只传 `{id}`,是因为这次只换文件;但哪天要「换文件同时改名」,因为有 `picName` 字段,前端多传一个就行,DTO 和接口都不用改。字段多 ≠ 每次都要填,字段多是给「未来的可能性」留口子。这是「胖 DTO 复用」与「拆多个小 DTO」的工程权衡,本项目选了前者。

### 九、踩过的三个坑

#### 坑 1:`res.data.code` 报「类型 `T` 上不存在属性 `code`」

**现象**:`if (res.data.code === 0 && res.data.data)` 整行报 `Property 'code' does not exist on type 'T'`,连带 `data`/`message` 全报。但 [useLoginUserStore.ts](src/stores/useLoginUserStore.ts) 里一模一样的写法却不报。

**根因**:病根不在这一行,而在生成的 [pictureController.ts](src/api/pictureController.ts) 里有 `requestType: 'form'`——它不是 `AxiosRequestConfig` 的合法字段,触发 TS2353(excess property),**连带**让 `uploadPictureUsingPost` 的返回类型泛型推断崩坏,`res.data`(本是 `AxiosResponse.data`,类型就是泛型 `T`)退化成裸 `T`。而 `getLoginUserUsingGet` 的 controller 没有 `requestType`,所以不报。

**修复**:新建 [axios-augment.d.ts](src/axios-augment.d.ts),用 TS 的**模块增强**给 axios 补上这个字段:

```ts
import 'axios'
declare module 'axios' {
  export interface AxiosRequestConfig {
    requestType?: 'form' | 'json' | 'multipart'
  }
}
```

从源头消除 TS2353,泛型推断恢复正常,`res.data.code` 自然不报。业务代码一行没动。

#### 坑 2:`beforeUpload` 报 TS2537

**现象**:`(file: UploadProps['fileList'][number])` 报「`UploadFile[] | undefined` 没有 number 索引签名」。

**根因**:`UploadProps['fileList']` 类型是 `UploadFile[] | undefined`(可能为 undefined),对它取 `[number]` 本身就是有缺陷的写法。而 ant-design-vue 的 `beforeUpload` 真实参数类型其实是 `FileType`(继承自 `File`)。

**修复**:直接用原生 `File` 类型(代码只用到 `file.type`、`file.size`,都有),顺手删掉不再使用的 `UploadProps`/`UploadChangeParam` import:

```ts
const beforeUpload = (file: File) => { ... }
```

#### 坑 3:`catch` 块警告 "Handle this exception or don't catch it at all"

**现象**:`catch(error)` 接住了异常,但 `error` 从头到尾没用,只 `message.error('图片上传失败')`。IDE 认为「要么真处理,要么别 catch」。

**修复**:把 `error` 用起来,显示真实错误原因:

```ts
} catch (error: any) {
  message.error('图片上传失败：' + (error?.message ?? ''))
}
```

- `error: any`:TS 严格模式下 catch 的 error 默认是 `unknown`,访问 `.message` 会报错,标 `any` 绕过。
- `error?.message`:可选链防空指针。
- `?? ''`:空合并,防止显示难看的 `"undefined"`。

> try 里两个分支的分工要分清:**`else` 分支**是「请求成功到达后端、但业务失败(`code != 0`)」,显示 `res.data.message`(后端中文提示);**`catch` 分支**是「请求根本没成功」(断网/超时/HTTP 500),显示 `error.message`(技术原因,如 `Network Error`)。

### 十、`requestType` 的来源 + 为什么教程不用补类型

**来源**:[openapi.config.js](openapi.config.js) 用 `@umijs/openapi` 生成 controller,它的模板 `serviceController.njk` 里有一段:

```nunjucks
{%- if api.body.mediaType === "multipart/form-data" %}
requestType: 'form',
{%- endif %}
```

即:**只要接口是 `multipart/form-data`(带文件上传),模板就输出 `requestType: 'form'`**。链路是:后端 `MultipartFile` 参数 → OpenAPI 文档标记为 multipart → 生成器命中模板分支 → 产出 `requestType: 'form'`。

**为什么教程(鱼皮)不用补类型?** 因为 `requestType` 报不报错,**取决于 axios 版本**:

| | 教程(不报) | 本项目(报) |
|---|---|---|
| axios | **^1.7.9** | **^1.18.1** |
| `AxiosRequestConfig` 是否有 `[key: string]: any` 兜底 | **有**(宽松,放行任意字段) | **没有**(严格列举,多余字段报 TS2353) |
| `request.ts` 封装 | 裸 axios 实例 | 一模一样 |
| controller 是否有 `requestType` | 有 | 有 |

axios 在 1.7.9 → 1.18.1 之间**收紧了 `AxiosRequestConfig` 类型**(移除了 index signature),旧版宽松正好绕过这个问题,新版严格就暴露了。所以补类型是针对新版 axios 的正确应对,不是写法有问题——而且 `axios-augment.d.ts` 这种**模块增强**正是 TS 官方处理「第三方库类型缺字段」的推荐做法。⚠️ 不要为了这个降级 axios,也不要直接删 controller 里的 `requestType`(会被 `npm run openapi` 重新生成覆盖)。

### 十一、概念小结

| 概念 | 在本组件的体现 |
|---|---|
| **`defineProps<Props>()`** | 基于类型的 props 声明,既传数据(`picture`)又传函数(`onSuccess`) |
| **受控组件** | 状态存父组件,子组件只读 + 触发回调 |
| **单向数据流 + 回调** | `:prop` 向下、`props.回调()` 向上 |
| **可选链 `?.`** | `props.onSuccess?.()`、`picture?.url`、`error?.message` |
| **空合并 `??`** | `error?.message ?? ''` 防 undefined |
| **`<style scoped>` + `:deep()`** | 作用域隔离 + 穿透到组件库内部元素 |
| **axios `params` vs `data`** | query 参数 vs 请求体;文件走 FormData(`data`) |
| **multipart 三参数** | params→URL query、body→额外 form 字段、file→文件 |
| **无注解 POJO 绑定** | 后端 `PictureUploadRequest` 从 URL query 按字段名绑定 |
| **胖 DTO** | 一个类服务多场景,字段是「能力声明」非「必填清单」 |
| **模块增强 `declare module`** | 给 axios 补 `requestType` 字段,治本不动生成代码 |

### 十二、一句话总结

> PictureUpload 是个**受控组件**:状态(`picture`)存在父组件 AddPicturePage,父用 `:picture` 把数据传下来给子组件预览,子组件上传成功后用 `props.onSuccess?.(...)` 把新图片传回去,父更新状态、响应式自动刷新预览——**数据向下、事件向上**。一次上传请求里,文件走 `file`(FormData)、`id` 走 `params`(URL query,对应后端无注解的 `PictureUploadRequest`)、`body` 是预留空槽;而 `requestType: 'form'` 的类型报错,根子在 axios 新版收紧了类型,用一份 `axios-augment.d.ts` 模块增强从源头治好。

---
