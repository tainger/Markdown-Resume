# 字典树（Trie / 前缀树）

字典树是一棵**多叉树**，专门存储**字符串集合**并支持高效的**前缀查询**。它把公共前缀合并成同一条路径——「查一个词是否存在」「有多少词以某前缀开头」都只需 \(O(L)\)（`L` 为串长），与集合大小无关。是「前缀匹配、自动补全、单词搜索、异或最大值」类题的标准结构。

---

## 一、核心思想

**每个节点代表一个字符，从根到某节点的路径拼成一个前缀**；用一个布尔位标记「某节点是否为一个完整单词的结尾」。

存入 `["cat", "car", "card", "dog"]`：

```
        (root)
        /     \
       c       d
       |       |
       a       o
      / \      |
     t*  r     g*
         |
         d*             * = isEnd（一个完整单词到此结束）

"cat"、"car"、"card"、"dog" 共享前缀 c-a / c-a-r，节省空间也加速查询
```

| 概念 | 说明 |
|:---|:---|
| **节点** | 存指向子节点的引用（按字符），及 `isEnd` 标记 |
| **边** | 代表一个字符 |
| **isEnd** | 标记「从根到此」构成一个完整单词 |
| **公共前缀共享** | 相同前缀只存一条路径，这是 Trie 的精髓 |

---

## 二、节点定义与三大操作

小写字母场景用定长数组 `children[26]`；字符集大时改用 `HashMap`。

```java
class Trie {
    private final Trie[] children = new Trie[26];   // 26 个小写字母
    private boolean isEnd = false;

    // ---------- 插入 ----------
    public void insert(String word) {
        Trie node = this;
        for (char c : word.toCharArray()) {
            int i = c - 'a';
            if (node.children[i] == null) node.children[i] = new Trie();
            node = node.children[i];                 // 逐字符下沉，缺则新建
        }
        node.isEnd = true;                           // 末尾打上单词结束标记
    }

    // ---------- 查完整单词 ----------
    public boolean search(String word) {
        Trie node = find(word);
        return node != null && node.isEnd;           // 要走到且是单词结尾
    }

    // ---------- 查是否有该前缀 ----------
    public boolean startsWith(String prefix) {
        return find(prefix) != null;                 // 能走到即可，不看 isEnd
    }

    // 沿字符路径走，走不通返回 null
    private Trie find(String s) {
        Trie node = this;
        for (char c : s.toCharArray()) {
            int i = c - 'a';
            if (node.children[i] == null) return null;
            node = node.children[i];
        }
        return node;
    }
}
```

> `search` 与 `startsWith` 唯一区别：**前者要求终点 `isEnd == true`，后者只要能走到**。

---

## 三、经典应用套路

| 场景 | 说明 |
|:---|:---|
| **前缀匹配 / 自动补全** | 输入前缀，遍历子树收集所有以它开头的词 |
| **单词搜索 II（212）** | 网格 DFS + Trie 剪枝，一次性匹配多个单词 |
| **拼写检查 / 敏感词过滤** | 大量词典下 \(O(L)\) 判定，比逐个比对快 |
| **添加与搜索单词（211）** | 支持 `.` 通配，遇 `.` 对所有子节点递归 |
| **01-Trie 求最大异或对（421）** | 把数字按二进制位建 Trie，贪心走「相反位」求最大异或 |

---

## 四、复杂度分析

| 操作 | 时间 | 说明 |
|:---|:---:|:---|
| 插入 / 查询单词 / 查前缀 | \(O(L)\) | `L` = 字符串长度，**与词表大小无关** |
| 空间 | \(O(N \times L \times \Sigma)\) 最坏 | `N` 词数、`Σ` 字符集大小；共享前缀会明显省空间 |

> 优势：查询只与**串长**有关；用哈希集合虽也能 \(O(L)\) 查「完整词」，但**「前缀查询」是 Trie 独有的强项**（哈希做不到高效前缀统计）。

---

## 五、易错点与技巧

### 1. `search` 必须校验 `isEnd`

存了 `"card"` 后 `search("car")` 应为 `true`（因为 "car" 也被单独插入过才算），别把「路径存在」当成「单词存在」。

### 2. 字符集选对存储

只有小写字母用 `Trie[26]`；含大小写/数字/任意字符改 `HashMap<Character, Trie>`，避免数组过大浪费。

### 3. 通配符匹配用递归

遇 `.` 要对**所有非空子节点**分别递归，任一分支成功即成功。

### 4. 大词表注意内存

节点数可能爆炸，`Trie[26]` 每节点固定占 26 引用；内存敏感时用 `HashMap` 惰性存储。

### 5. 删除需谨慎

删词要沿路清 `isEnd`，并仅在「无其他词共用」时才回收节点，一般刷题很少真删。

---

## 六、一句话总结

字典树把字符串按字符逐层展开、公共前缀共享一条路径，插入/查询/前缀判断都只要 **\(O(L)\)** 且与词表规模无关——**`search` 看 `isEnd`、`startsWith` 只看能否走到**是它的核心区别；前缀匹配、自动补全、多词网格搜索、01-Trie 异或都是它的主场，小写字母用 `Trie[26]`、字符集大用 `HashMap`。

---

## 七、相关题目

| 题目 | 考点 |
|:---|:---|
| 208. 实现 Trie（前缀树） | 模板题：insert/search/startsWith |
| 211. 添加与搜索单词 | `.` 通配符递归 |
| 212. 单词搜索 II | 网格 DFS + Trie 剪枝 |
| 421. 数组中两个数的最大异或值 | 01-Trie 贪心 |
| 648. 单词替换 | 前缀替换 |

> 相关：Trie 是「字符维度的多叉树」，遍历思路同 [二叉树.md](二叉树.md)/[图.md](图.md)；只查完整词而无需前缀统计时，[哈希表.md](哈希表.md) 更省事。
