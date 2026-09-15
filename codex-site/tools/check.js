/* 数据层自检：不依赖浏览器，直接在 node 里加载数据文件与检索模块。
 * 用法: node tools/check.js
 * 只覆盖「数据 -> 索引 -> 检索」这条链路；界面部分靠浏览器里看。
 */
'use strict';
const path = require('path');
const fs = require('fs');

global.window = global;

const ROOT = path.join(__dirname, '..');
require(path.join(ROOT, 'js/pinyin-table.js'));
require(path.join(ROOT, 'js/codex.js'));
require(path.join(ROOT, 'js/search.js'));

const manifest = require(path.join(ROOT, 'js/chapters.js')) || null;
const src = fs.readFileSync(path.join(ROOT, 'js/chapters.js'), 'utf8');
const files = [...src.matchAll(/'([^']+\.js)'/g)].map(m => m[1]);
if (!files.length) { console.error('chapters.js 里没解析出任何数据文件'); process.exit(1); }
files.forEach(f => require(path.join(ROOT, f)));

let fail = 0;
function ok(cond, label, extra) {
  if (!cond) fail++;
  console.log(`  ${cond ? 'OK  ' : '!!  '}${label}${extra ? '   ' + extra : ''}`);
}

console.log(`数据文件 ${files.length} 个\n`);

// ── 结构完整性 ────────────────────────────────────────────────────────
console.log('结构');
let totalItems = 0, totalGroups = 0;
for (const ch of Codex.chapters) {
  totalItems += ch.count;
  totalGroups += ch.groupUnits.length;

  const bad = [];
  ch.items.forEach(it => {
    if (!it.name) bad.push('无名 ' + it.code);
    if (!/^[A-Za-z][A-Za-z0-9_]*$/.test(it.code)) bad.push('代码可疑 ' + it.name + ' / ' + it.code);
  });
  const empties = ch.groupUnits.filter(u => !u.items.length).map(u => u.title);

  ok(bad.length === 0, `${ch.id} 字段完整`, bad.length ? bad.slice(0, 5).join('; ') : `${ch.count} 条`);
  ok(empties.length === 0, `${ch.id} 无空分组`, empties.join('、'));
  ok(ch.items.length === ch.count, `${ch.id} 条目数与统计一致`);
  // data-i 用的是扁平下标，重复会导致点错行
  const seqs = new Set(ch.items.map(i => i.seq));
  ok(seqs.size === ch.items.length, `${ch.id} 序号唯一`);
}
console.log(`\n合计 ${Codex.chapters.length} 章 / ${totalGroups} 组 / ${totalItems} 条\n`);

// ── 检索 ──────────────────────────────────────────────────────────────
console.log('检索');
const cases = [
  ['gargantuar', '代码片段', h => h.length > 0],
  ['dark_gargantuar', '精确代码', h => h[0] && h[0].it.code === 'dark_gargantuar'],
  ['darkgargantuar', '去掉下划线的代码', h => h.some(x => x.it.code === 'dark_gargantuar')],
  ['豌豆射手', '中文名', h => h[0] && h[0].it.name === '豌豆射手'],
  ['巨人', '中文片段', h => h.length > 5],
  ['jrjs', '拼音首字母', h => h.some(x => x.it.name === '巨人僵尸')],
  ['wjajjrjs', '拼音首字母（带无尽前缀）', h => h.some(x => x.it.code === 'egypt_gargantuar_danger')],
  ['无尽黑暗巨人僵尸', '带前缀的全名', h => h[0] && h[0].it.code === 'dark_gargantuar_danger'],
  ['巨人 黑暗', '空格多关键词取交集', h => h.length > 0 && h.every(x => /巨人/.test(x.it.name) && /黑暗/.test(x.it.name))],
  ['peashooter sunflower', '两个互斥关键词 -> 空', h => h.length === 0],
  ['zzzznotexist', '无结果', h => h.length === 0],
];
for (const [q, label, check] of cases) {
  const r = CodexSearch.search(Codex.chapters, q);
  ok(check(r.hits), `${label}  "${q}"`, `${r.total} 条` + (r.hits[0] ? `，首位 ${r.hits[0].it.name} / ${r.hits[0].it.code}` : ''));
}

// 跨章节：在僵尸数据里搜植物，必须也能命中
const cross = CodexSearch.search(Codex.chapters, '豌豆射手');
ok(cross.hits.some(x => x.ch.id === 'plant'), '跨章节检索（植物名在全局结果里）');

// 拼音首字母不能把无关条目排到前面
const py = CodexSearch.search(Codex.chapters, 'hajrjs');
ok(py.hits[0] && py.hits[0].it.name === '黑暗巨人僵尸', '拼音首字母排序', py.hits[0] ? py.hits[0].it.name : '空');

// ── 无尽前缀 ──────────────────────────────────────────────────────────
console.log('\n无尽模式前缀');
const z = Codex.get('zombie');
const danger = z.items.filter(i => /danger/.test(i.code));
ok(danger.length === 21, `带 danger 的条目 21 条`, String(danger.length));
ok(danger.every(i => i.name.startsWith('无尽')), '全部带「无尽」前缀',
   danger.filter(i => !i.name.startsWith('无尽')).map(i => i.code).join(','));
ok(z.items.filter(i => i.code === 'dark_gargantuar')[0].name === '黑暗巨人僵尸',
   '主线关同名条目未被误加前缀');
ok(!z.items.some(i => /danger/.test(i.code) && i.name.replace('无尽', '') !== i.name.replace('无尽', '')),
   '前缀只加一次');

console.log(fail ? `\n${fail} 项未通过` : '\n全部通过');
process.exit(fail ? 1 : 0);
