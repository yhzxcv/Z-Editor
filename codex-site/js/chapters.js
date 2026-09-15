/* 章节清单
 *
 * 新增一个章节的完整流程：
 *   1. 在 tools/migrate.py 的 CHAPTERS 里加一节配置，跑 `python migrate.py <id>`
 *   2. 生成的文件名填到下面这个数组里
 * 顺序即界面上章节 chips 的显示顺序。
 *
 * 也可以不跑迁移，直接手写 data/ch-xxx.js —— 只要最后调用 Codex.add({...}) 即可。
 */
window.CODEX_MANIFEST = [
  'data/ch-plant.js',
  'data/ch-zombie.js',
  'data/ch-obstacle.js'
];
