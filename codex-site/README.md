# 代码图鉴（静态站）

给 PvZ2 关卡编辑器用户查游戏代码用的网站。编辑器里会给一个链接跳到这里。

纯静态：**没有构建步骤，没有框架，没有依赖**。原生 JS + CSS，双击 `index.html` 就能在
`file://` 下跑起来，也能原样丢到 GitHub Pages。

## 文件结构

```
index.html          页面骨架
css/style.css       全部样式（含深浅色主题、响应式）
js/chapters.js      章节清单 —— 加章节改这里
js/pinyin-table.js  拼音首字母表（生成物，勿手改）
js/codex.js         数据注册表：规整结构 -> 渲染单元 -> 扁平条目表
js/search.js        检索与打分
js/app.js           界面：渲染、滚动高亮、复制、设置
data/ch-*.js        数据，唯一真源，手工维护。格式见 data/README.md
tools/migrate.py    迁移工具（TXT -> data/ch-*.js）。**已存在的文件默认不覆盖**，
                    想覆盖要显式加 `--force` —— 植物/僵尸两章手工改过很多，误跑一次就全没了
tools/gen_pinyin.py 重新生成拼音表
tools/check.js      数据层自检，改完数据跑一下
tools/qc-report.txt 迁移质检报告
```

## 本地预览

直接双击 `index.html` 即可。

要模拟线上环境（比如之后加 Service Worker），起个本地服务器：

```bash
python -m http.server 8000 --directory codex-site    # 然后访问 http://localhost:8000
```

## 部署（Cloudflare Pages）

没有构建步骤，所以配置就是「把 `codex-site/` 当静态目录发出去」：

| 配置项 | 值 |
|---|---|
| 根目录 | `/`（仓库根） |
| 构建命令 | 留空 |
| 输出目录 | `codex-site` |

两种发布方式：

- **Git 集成**：控制台连上仓库，以后 push 到 `master` 自动部署。生产分支设 `master`。
- **直接上传**（适合只想要个临时预览）：`npx wrangler pages deploy codex-site --project-name z-editor-codex`

`_headers` 是唯一的 Cloudflare 专属文件，管缓存与几个安全头，放在输出目录根部。
站点没有构建步骤、文件名也不带哈希，缓存靠「每次部署自动清缓存」来失效。

**换平台几乎零成本**：全部是相对路径，`codex-site/` 整个目录丢给任何静态托管都能跑，
挂在子路径下也不用改代码。

## 设计要点

**为什么数据是 `.js` 而不是 `.json`**：`file://` 下 `fetch` 会被 CORS 拦掉，而 `<script>`
不会。用 `.js` + `Codex.add({...})` 还顺带允许写注释和尾逗号，手工维护友好得多。

**为什么滚动容器是 `.content` 而不是 window**：这样吸顶的分组标题 `top` 恒为 0，
不用按断点重算偏移量；顶栏和分组 chips 也永远不动。代价是不能靠地址栏自动收起，
对工具类页面来说划算。

**分组 chips 的滚动高亮**用 `.group` 的 `offsetTop` 判定（不是 `.group-head`——
sticky 元素的 offsetTop 会被吸顶位移污染），配合 `position: relative` 的 `.content` 作为
`offsetParent`。所以改布局时别给 `.group` 加 `position`。

**检索是全局跨章节的**：在「僵尸」页搜「豌豆射手」也要有结果，所以索引按章惰性构建、
缓存在 `chapter._index` 上，查询时把所有已加载章节一起打分。1500 条规模下线性扫描
比倒排索引更快也更好维护，暂时不需要防抖。

**拼音表**用 GB2312 一级汉字区（按拼音排序）的 23 个边界字离线切出来，只依赖标准库，
不用装 `pypinyin`。覆盖 3755 个常用字；二级汉字和生僻字没有拼音首字母，按原字匹配即可。

## 断点

只有一个：**940px**。

- 以下：单列，章节 chips + 分组 chips + 吸顶分组标题；侧栏变成 ☰ 抽屉（同一份 DOM）。
- 以上：侧栏常驻（章节 + 目录），两个 chips 行让位，条目区变 2–3 列网格，复制图标改成 hover 显示。

## 待办

- Service Worker 离线缓存（需要 https 或 localhost，托管方案定了再做）
- 其余 7 章迁移：动物事件、工具代码、场景/地图代码、音乐模块、状态代码、子弹代码、主题代码
- 模块代码章节（217 个 JSON 模板，复制时要剥掉 `#` 注释）—— 二期
- 编辑器侧加链接
