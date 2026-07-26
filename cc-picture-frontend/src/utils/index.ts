// 工具类
import { saveAs } from "file-saver"

/**
 * 格式化文件大小
 * @param size
 */
export const formatSize = (size?: number) => {
  if (!size) return '未知'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

/**
 * 从网址里提取扩展名（含点），如 ".webp"；提取不到返回空串
 */
function getExtFromUrl(url: string): string {
  // 先去掉 ? 后面的查询参数和 # 锚点，只看路径本身
  const cleanUrl = url.split('?')[0].split('#')[0]
  const lastSegment = cleanUrl.split('/').pop() ?? ''
  const dotIdx = lastSegment.lastIndexOf('.')
  return dotIdx >= 0 ? lastSegment.slice(dotIdx).toLowerCase() : ''
}

/**
 * 下载图片
 * @param url 图片下载地址
 * @param fileName 要保存为的文件名（不带后缀也行，会按网址自动补）
 */
export async function downloadImage(url?: string, fileName?: string) {
  if (!url) {
    return
  }
  // picture.name 一般不带后缀，从网址里取出 .webp / .jpg 等拼上，避免下载下来没后缀
  const ext = getExtFromUrl(url)
  const fullName = fileName && ext ? fileName + ext : fileName

  // 如果直接 saveAs(url, 名字)：跨域资源下浏览器会忽略指定的文件名，
  // 改用网址里的原始文件名（比如 OSS 生成的 2026-07-26_xxx.webp）。
  // 所以先把图片抓成 Blob 再保存，文件名才能由我们决定。
  // 用 fetch 拉成一个 Blob（内存里的二进制），再 saveAs(blob, 名字)。
  // 这时候数据已经在手里，跟域无关
  try {
    const res = await fetch(url)
    if (!res.ok) throw new Error('网络响应异常')
    const blob = await res.blob()
    saveAs(blob, fullName)
  } catch {
    // 兜底：抓取失败（通常是对象存储没开跨域 CORS），只能退回直接打开链接下载。
    // 这种情况下文件名仍可能由服务器决定，名字不一定生效。
    const a = document.createElement('a')
    a.href = url
    a.download = fullName ?? ''
    a.target = '_blank'
    a.click()
  }
}
