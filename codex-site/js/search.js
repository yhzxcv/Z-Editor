/* 代码图鉴 · 检索
 *
 * 全局跨章节搜索：用户在「僵尸」页搜「豌豆射手」也应该有结果，
 * 所以索引按章惰性构建、缓存在 chapter 上，查询时把所有已加载章节一起打分。
 * 几千次字符串比较，无需防抖也不会卡。
 *
 * 支持四种输入：
 *   代码片段      gargantuar
 *   中文名片段    巨人
 *   拼音首字母    jrsz -> 巨人僵尸
 *   去下划线代码  darkgargantuar -> dark_gargantuar
 * 空格分隔多关键词时取交集（每条都得命中），分数累加。
 */
window.CodexSearch = (function () {
  'use strict';

  var MAX_RESULTS = 400;      // 再多也没人翻，截断并提示细化关键词

  // 命中位置权重：完全相等 > 前缀 > 包含
  var W = {
    codeEq: 1000, codePre: 860, codeInc: 640,
    nameEq: 950, namePre: 820, nameInc: 600,
    pinyinPre: 470, pinyinInc: 360,
    codeFlatInc: 430,          // 去掉下划线后的代码
    srcInc: 260,
    noteInc: 180
  };

  function flat(s) { return s.replace(/[^a-z0-9]/g, ''); }

  function buildIndex(ch) {
    if (ch._index) return ch._index;
    var py = window.Pinyin;
    ch._index = ch.items.map(function (it) {
      return {
        it: it,
        name: it.name.toLowerCase(),
        code: it.code.toLowerCase(),
        codeFlat: flat(it.code.toLowerCase()),
        src: it.src.toLowerCase(),
        note: it.note.toLowerCase(),
        py: py ? py.initials(it.name) : ''
      };
    });
    return ch._index;
  }

  function scoreOne(e, q) {
    var s = 0;

    if (e.code === q) s = W.codeEq;
    else if (e.code.indexOf(q) === 0) s = W.codePre;
    else if (e.code.indexOf(q) > 0) s = W.codeInc;

    if (e.name === q) s = Math.max(s, W.nameEq);
    else if (e.name.indexOf(q) === 0) s = Math.max(s, W.namePre);
    else if (e.name.indexOf(q) > 0) s = Math.max(s, W.nameInc);

    if (e.py) {
      if (e.py.indexOf(q) === 0) s = Math.max(s, W.pinyinPre);
      else if (e.py.indexOf(q) > 0) s = Math.max(s, W.pinyinInc);
    }

    // 去下划线命中只作兜底，不该盖过名称/代码的包含命中
    if (s < W.codeInc && e.codeFlat.indexOf(q) >= 0) s = Math.max(s, W.codeFlatInc);

    // 来源/备注同理，只在前面的都没命中时才认
    if (s < W.pinyinInc) {
      if (e.src && e.src.indexOf(q) >= 0) s = Math.max(s, W.srcInc);
      else if (e.note && e.note.indexOf(q) >= 0) s = Math.max(s, W.noteInc);
    }
    return s;
  }

  /**
   * @param {Array} chapters 已加载的章节
   * @returns {{hits: Array<{ch, it}>, total: number, truncated: boolean}}
   */
  function search(chapters, rawQuery) {
    var q = (rawQuery || '').trim().toLowerCase();
    if (!chapters || !chapters.length || !q) {
      return { hits: [], total: 0, truncated: false };
    }

    var terms = q.split(/\s+/).filter(Boolean);
    var found = [];

    chapters.forEach(function (ch) {
      var index = buildIndex(ch);
      for (var i = 0; i < index.length; i++) {
        var e = index[i], sum = 0, ok = true;
        for (var t = 0; t < terms.length; t++) {
          var sc = scoreOne(e, terms[t]);
          if (!sc) { ok = false; break; }
          sum += sc;
        }
        if (ok) found.push({ ch: ch, it: e.it, s: sum });
      }
    });

    // 同分时短名靠前（更可能是用户要的），再同分按图鉴原序，保证结果稳定
    found.sort(function (a, b) {
      if (b.s !== a.s) return b.s - a.s;
      var la = a.it.name.length, lb = b.it.name.length;
      if (la !== lb) return la - lb;
      if (a.ch.id !== b.ch.id) return a.ch.id < b.ch.id ? -1 : 1;
      return a.it.seq - b.it.seq;
    });

    var total = found.length;
    var hits = found.slice(0, MAX_RESULTS).map(function (h) { return { ch: h.ch, it: h.it }; });
    return { hits: hits, total: total, truncated: total > hits.length };
  }

  return { search: search, MAX_RESULTS: MAX_RESULTS };
})();
