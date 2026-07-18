// 如果采用传⁡⁡⁡统开发方式，针⁠对⁠每⁠个请求都要单独编写代码，很​麻烦
// 使用 OpenAPI 工具 （https://www.npmjs.com/package/@umijs/openapi），直接自动生成即可
import { generateService } from '@umijs/openapi'

generateService({
  requestLibPath: "import request from '@/request'",
  schemaPath: 'http://localhost:8123/api/v2/api-docs',
  serversPath: './src'
})
