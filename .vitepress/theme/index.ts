import DefaultTheme from 'vitepress/theme'
import { h } from 'vue'
import type { Theme } from 'vitepress'
import GiscusComment from './GiscusComment.vue'
import CodeFolder from './CodeFolder.vue'
import CardScanner from './srs/CardScanner.vue'

export default {
  extends: DefaultTheme,
  Layout() {
    return h(DefaultTheme.Layout, null, {
      // 笔记页：SRS 卡片扫描（先于代码折叠与评论）
      'doc-after': () => [h(CardScanner), h(CodeFolder), h(GiscusComment)],
    })
  },
} satisfies Theme
