import { ossUploadViaApi } from '../api'

/** 头像等经后端写入 OSS，不触发浏览器直连 OSS，避免线上未配置 OSS CORS 时外网用户失败 */
export async function uploadAvatarViaApi(rawFile) {
  const { data } = await ossUploadViaApi(rawFile)
  return data
}

/** OSS 直传 PUT 因体积超限等返回 413 时的统一提示 */
export const MSG_OSS_FILE_TOO_LARGE = '您上传的文件过大，请换小一点的试试~'

export function ossPutFailMessage(putResponse) {
  const st = putResponse?.status
  if (st === 413) {
    return MSG_OSS_FILE_TOO_LARGE
  }
  if (st === 403) {
    return '上传被拒绝（403）：请确认 OSS 已配置跨域；若 Bucket 已禁用对象 ACL，请勿开启 object-public-read（环境变量 ALIYUN_OSS_OBJECT_PUBLIC_READ=false）'
  }
  if (st === 400) {
    return '上传被拒绝（400）：常与请求头与预签名不一致或 OSS 策略有关，请稍后重试或联系管理员'
  }
  return st ? `上传失败（HTTP ${st}）` : '上传失败'
}

/**
 * 上传流程 catch 中展示给用户的文案（含预签名接口返回 413 等情况）。
 * 注意：浏览器 fetch 失败时常为 “Failed to fetch”，与体积无关（CORS、断网、Mixed Content 等），
 * 不可一律映射为「图片过大」，否则会误伤所有失败场景。
 */
export function formatOssUploadError(err) {
  if (err?.response?.status === 413) {
    return MSG_OSS_FILE_TOO_LARGE
  }
  const raw = String(err?.message || '').trim()
  if (/failed to fetch|networkerror when attempting to fetch|load failed/i.test(raw)) {
    return '上传失败，请检查网络或稍后重试'
  }
  return raw || '上传失败'
}
