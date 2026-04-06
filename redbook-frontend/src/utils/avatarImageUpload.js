/** 与 accept="image/*" 一致：常见后缀；非 image/* 时用文件头防改后缀绕过 */

const MSG = '请选择 JPG、PNG、GIF、WebP、SVG 等常见图片格式'

/** 仅当文件实际超过该大小时提示，避免把网络类错误误判为体积问题 */
export const MSG_AVATAR_FILE_TOO_LARGE = '上传的图片过大，换小一点的试试~'

/** 头像单文件上限（与常见 OSS/浏览器直传体验对齐，过大易超时或失败） */
const AVATAR_MAX_BYTES = 10 * 1024 * 1024

const ALLOWED_EXT = new Set([
  'jpg',
  'jpeg',
  'jfif',
  'pjpeg',
  'pjp',
  'png',
  'apng',
  'gif',
  'webp',
  'bmp',
  'dib',
  'svg',
  'ico',
  'heic',
  'heif',
  'avif',
])

function fileExt(name) {
  const parts = String(name || '').split('.')
  if (parts.length < 2) return ''
  return parts.pop().toLowerCase()
}

function isBinaryImageMagic(buf) {
  const n = buf.byteLength
  if (n < 12) return false
  const u = new Uint8Array(buf)
  if (u[0] === 0xff && u[1] === 0xd8 && u[2] === 0xff) return true
  if (u[0] === 0x89 && u[1] === 0x50 && u[2] === 0x4e && u[3] === 0x47) return true
  if (
    u[0] === 0x47 &&
    u[1] === 0x49 &&
    u[2] === 0x46 &&
    u[3] === 0x38 &&
    (u[4] === 0x37 || u[4] === 0x39)
  ) {
    return true
  }
  if (u[0] === 0x52 && u[1] === 0x49 && u[2] === 0x46 && u[3] === 0x46) {
    return n >= 12 && String.fromCharCode(u[8], u[9], u[10], u[11]) === 'WEBP'
  }
  if (u[0] === 0x42 && u[1] === 0x4d) return true
  if (u[0] === 0x00 && u[1] === 0x00 && u[2] === 0x01 && u[3] === 0x00) return true
  if (u[4] === 0x66 && u[5] === 0x74 && u[6] === 0x79 && u[7] === 0x70) {
    const brand = String.fromCharCode(u[8], u[9], u[10], u[11]).toLowerCase()
    return ['heic', 'heix', 'hevc', 'hevx', 'mif1', 'msf1', 'avif', 'avis'].includes(brand)
  }
  return false
}

async function sniffIsImage(rawFile, ext) {
  if (ext === 'svg') {
    const t = await rawFile.slice(0, 512).text()
    const s = t.trimStart().toLowerCase()
    return s.startsWith('<svg') || s.startsWith('<?xml')
  }
  const buf = await rawFile.slice(0, 32).arrayBuffer()
  return isBinaryImageMagic(buf)
}

/**
 * @param {File} rawFile
 * @param {{ warning: (msg: string) => void }} ElMessage
 * @returns {Promise<boolean>}
 */
export async function beforeUploadAvatarImage(rawFile, ElMessage) {
  if (!rawFile) {
    ElMessage.warning(MSG)
    return false
  }
  if (rawFile.size > AVATAR_MAX_BYTES) {
    ElMessage.warning(MSG_AVATAR_FILE_TOO_LARGE)
    return false
  }
  const ext = fileExt(rawFile.name)
  const mime = String(rawFile.type || '').toLowerCase().trim()

  if (mime.startsWith('image/')) {
    return true
  }

  if (!ALLOWED_EXT.has(ext)) {
    ElMessage.warning(MSG)
    return false
  }

  if (mime && mime !== 'application/octet-stream') {
    ElMessage.warning(MSG)
    return false
  }

  try {
    const ok = await sniffIsImage(rawFile, ext)
    if (!ok) ElMessage.warning(MSG)
    return ok
  } catch {
    ElMessage.warning(MSG)
    return false
  }
}
