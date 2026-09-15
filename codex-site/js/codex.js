/* 代码图鉴 · 数据注册表
 *
 * 数据文件（data/ch-*.js）通过 Codex.add({...}) 注册自己，本文件只做三件事：
 *   1. 规整结构、补全缺省字段
 *   2. 把「两级结构」压平成顺序渲染单元 units，UI 只认 units，不关心原始层级
 *   3. 给每个条目回填归属（unit）和全局序号（seq），渲染与检索共用
 *
 * 所以新增章节只要写一个 data/ch-xxx.js 并在 js/chapters.js 里登记即可，UI 不用改。
 */
window.Codex = (function () {
  'use strict';

  var chapters = [];
  var byId = {};

  function normItem(it) {
    return {
      name: (it.name || '').trim(),
      code: (it.code || '').trim(),
      src: (it.src || '').trim(),
      note: (it.note || '').trim()
    };
  }

  function normGroup(g) {
    var out = { title: (g.title || '').trim(), note: (g.note || '').trim() };
    if (g.children && g.children.length) {
      out.children = g.children.map(normGroup);
      // 数据约定 children 与 items 二选一，但真同时出现也不该丢数据
      if (g.items && g.items.length) out.items = g.items.map(normItem);
    } else {
      out.items = (g.items || []).map(normItem);
    }
    return out;
  }

  /* 两级结构 -> 顺序单元流
   *   有 children 的大类 -> 一个 section 单元 + N 个 group 单元
   *   无 children 的普通组 -> 一个 group 单元
   * section 单元只当分隔横幅渲染，不参与分组跳转与滚动高亮。
   */
  function buildUnits(groups) {
    var units = [];
    groups.forEach(function (g) {
      if (g.children && g.children.length) {
        units.push({ kind: 'section', title: g.title, note: g.note });
        if (g.items && g.items.length) {
          units.push({ kind: 'group', title: g.title, note: '', section: g.title, items: g.items });
        }
        g.children.forEach(function (sub) {
          units.push({
            kind: 'group', title: sub.title, note: sub.note,
            section: g.title, items: sub.items || []
          });
        });
      } else {
        units.push({ kind: 'group', title: g.title, note: g.note, section: '', items: g.items || [] });
      }
    });
    return units;
  }

  function add(ch) {
    if (!ch || !ch.id) throw new Error('Codex.add: 缺少 id');

    var c = {
      id: ch.id,
      title: (ch.title || ch.id).trim(),
      intro: (ch.intro || []).slice(),
      groups: (ch.groups || []).map(normGroup)
    };
    c.units = buildUnits(c.groups);
    c.groupUnits = c.units.filter(function (u) { return u.kind === 'group'; });
    c.count = c.groupUnits.reduce(function (n, u) { return n + u.items.length; }, 0);

    // 条目扁平表：搜索和渲染都按这里的下标定位（data-i="<seq>"）
    c.items = [];
    c.itemsByCode = {};
    c.groupUnits.forEach(function (u) {
      u.items.forEach(function (it) {
        it.unit = u;
        it.seq = c.items.length;
        c.items.push(it);
        // 代码基本唯一；真撞车时保留第一条，避免搜索串到别的条目
        if (it.code && !(it.code in c.itemsByCode)) c.itemsByCode[it.code] = it;
      });
    });

    if (byId[c.id]) chapters[chapters.indexOf(byId[c.id])] = c;   // 重复注册以最后一次为准
    else chapters.push(c);
    byId[c.id] = c;
    return c;
  }

  return {
    add: add,
    chapters: chapters,
    get: function (id) { return byId[id] || null; },
    first: function () { return chapters[0] || null; }
  };
})();
