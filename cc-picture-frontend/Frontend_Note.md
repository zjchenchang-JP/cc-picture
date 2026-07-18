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
