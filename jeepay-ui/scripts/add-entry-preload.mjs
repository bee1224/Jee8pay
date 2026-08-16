#!/usr/bin/env node
/**
 * build 後將 entry JS/CSS 的 <link rel="preload"> 注入 dist/index.html。
 *
 * 原因：vite 2.9 無法在 build 期間解析尚未產生的 hashed asset 路徑
 * （source index.html 若寫死 preload 路徑會 build 失敗），因此在 build 完成後
 * 由本腳本讀取 dist/index.html 中 vite 產出的 entry script/stylesheet，
 * 自動加入同名的 preload link，加快首載。
 *
 * 用法：node scripts/add-entry-preload.mjs <dist 目錄>（預設 ./dist）
 * 失敗時 exit 1，讓 build 在 CI 中 loud-fail。
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { join, resolve } from 'node:path'

const distDir = resolve(process.argv[2] || 'dist')
const htmlPath = join(distDir, 'index.html')

let html
try {
  html = readFileSync(htmlPath, 'utf-8')
} catch {
  console.error(`ADD_PRELOAD=FAIL_INDEX_MISSING ${htmlPath}`)
  process.exit(1)
}

if (/rel="preload"/.test(html)) {
  console.log('ADD_PRELOAD=SKIP_ALREADY_PRESENT')
  process.exit(0)
}

const scriptMatch = html.match(
  /<script type="module"[^>]*crossorigin[^>]*src="([^"]+\.js)"/
)
const styleMatch = html.match(/<link rel="stylesheet"[^>]*href="([^"]+\.css)"/)

if (!scriptMatch) {
  console.error('ADD_PRELOAD=FAIL_ENTRY_SCRIPT_NOT_FOUND')
  process.exit(1)
}

const links = [
  `<link rel="preload" as="script" href="${scriptMatch[1]}" crossorigin />`,
]
if (styleMatch) {
  links.push(`<link rel="preload" as="style" href="${styleMatch[1]}" />`)
}

const anchor = '</title>'
if (!html.includes(anchor)) {
  console.error('ADD_PRELOAD=FAIL_TITLE_ANCHOR_MISSING')
  process.exit(1)
}

html = html.replace(anchor, `${anchor}\n    ${links.join('\n    ')}`, 1)
writeFileSync(htmlPath, html, 'utf-8')
console.log(`ADD_PRELOAD=OK ${links.join(' ')}`)
