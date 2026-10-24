// 模板裡的 $t 由 appMain.ts 掛在 globalProperties
export {}
declare module 'vue' {
  interface ComponentCustomProperties {
    $t: typeof import('./index').t
  }
}
