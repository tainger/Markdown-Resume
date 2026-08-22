import DefaultTheme from 'vitepress/theme'
import { h } from 'vue'
import type { Theme } from 'vitepress'
import GiscusComment from './GiscusComment.vue'
import CodeFolder from './CodeFolder.vue'

export default {
  extends: DefaultTheme,
  Layout() {
    return h(DefaultTheme.Layout, null, {
      // 把代码折叠器（题解目录生效）和评论组件插入到文档内容底部
      'doc-after': () => [h(CodeFolder), h(GiscusComment)],
    })
  },
} satisfies Theme
