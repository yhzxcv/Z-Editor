/* 代码图鉴 · 界面
 *
 * 三层结构：
 *   章节 chips（罕见操作，一行放得下）
 *   分组 chips（高频跳转，横向滚动 + 滚动高亮）
 *   条目列表（两行：名上码下，整行点击即复制）
 * 宽屏下侧栏接管章节与目录，分组 chips 隐藏。
 *
 * 所有滚动都发生在 .content 内部（它是唯一的滚动容器），
 * 因此吸顶标题的 top 恒为 0，不需要按断点算偏移。
 */
(function () {
  'use strict';

  var content = document.getElementById('content');
  var chapterbar = document.getElementById('chapterbar');
  var groupbar = document.getElementById('groupbar');
  var sidebar = document.getElementById('sidebar');
  var scrim = document.getElementById('scrim');
  var input = document.getElementById('q');
  var clearBtn = document.getElementById('q-clear');
  var toastEl = document.getElementById('toast');
  var settingsEl = document.getElementById('settings');

  var LS_KEY = 'codex.settings.v1';
  var settings = { format: 'plain', theme: 'auto' };

  var state = {
    chapter: null,
    query: '',
    searching: false,
    rows: [],          // 当前列表项：浏览态是本章全部，搜索态是命中的 [{ch, it}]
    activeGroup: 0
  };

  var ICON_COPY = '<svg viewBox="0 0 16 16" width="15" height="15" aria-hidden="true">' +
    '<rect x="5.6" y="5.6" width="8.4" height="8.4" rx="1.7" fill="none" stroke="currentColor" stroke-width="1.4"/>' +
    '<path d="M10.4 3.6v-.8c0-.7-.6-1.3-1.3-1.3H3.7c-.7 0-1.3.6-1.3 1.3v5.4c0 .7.6 1.3 1.3 1.3h.8" ' +
    'fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>';

  var ICON_CHECK = '<svg viewBox="0 0 16 16" width="15" height="15" aria-hidden="true">' +
    '<path d="M3 8.4l3.2 3.2L13 4.8" fill="none" stroke="currentColor" stroke-width="1.9" ' +
    'stroke-linecap="round" stroke-linejoin="round"/></svg>';

  // ── 工具 ──────────────────────────────────────────────────────────────
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function copyText(text) {
    if (navigator.clipboard && window.isSecureContext) {
      return navigator.clipboard.writeText(text);
    }
    // file:// 或旧浏览器兜底
    return new Promise(function (resolve, reject) {
      var ta = document.createElement('textarea');
      ta.value = text;
      ta.setAttribute('readonly', '');
      ta.style.cssText = 'position:fixed;top:-1000px;opacity:0';
      document.body.appendChild(ta);
      ta.select();
      ta.setSelectionRange(0, text.length);
      var ok = false;
      try { ok = document.execCommand('copy'); } catch (e) { ok = false; }
      document.body.removeChild(ta);
      ok ? resolve() : reject(new Error('execCommand copy 失败'));
    });
  }

  var toastTimer = null;
  function toast(msg, isErr) {
    toastEl.textContent = msg;
    toastEl.classList.toggle('err', !!isErr);
    toastEl.hidden = false;
    requestAnimationFrame(function () { toastEl.classList.add('show'); });
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      toastEl.classList.remove('show');
      setTimeout(function () { toastEl.hidden = true; }, 200);
    }, isErr ? 2600 : 1500);
  }

  // ── 设置 ──────────────────────────────────────────────────────────────
  function loadSettings() {
    try {
      var raw = localStorage.getItem(LS_KEY);
      if (raw) {
        var o = JSON.parse(raw);
        if (o.format === 'plain' || o.format === 'quoted') settings.format = o.format;
        if (o.theme === 'auto' || o.theme === 'light' || o.theme === 'dark') settings.theme = o.theme;
      }
    } catch (e) { /* 隐私模式 / 禁用存储：用默认值即可 */ }
  }

  function saveSettings() {
    try { localStorage.setItem(LS_KEY, JSON.stringify(settings)); } catch (e) { /* 同上 */ }
  }

  function applyTheme() {
    var el = document.documentElement;
    if (settings.theme === 'auto') el.removeAttribute('data-theme');
    else el.setAttribute('data-theme', settings.theme);
  }

  // ── 渲染：条目 ────────────────────────────────────────────────────────
  function groupBadgeOf(rec) {
    return rec.it.unit ? rec.it.unit.title : '';
  }

  function itemHTML(it, rowIdx, badge) {
    return '<button class="item" type="button" data-r="' + rowIdx + '">' +
      '<span class="it-head">' +
        '<span class="it-name">' + esc(it.name) + '</span>' +
        (it.src ? '<span class="it-src">' + esc(it.src) + '</span>' : '') +
        (it.note ? '<span class="it-note">' + esc(it.note) + '</span>' : '') +
        (badge ? '<span class="it-group">' + esc(badge) + '</span>' : '') +
      '</span>' +
      '<span class="it-code">' + esc(it.code) + '</span>' +
      '<span class="it-copy">' + ICON_COPY + '</span>' +
      '</button>';
  }

  function groupHeadHTML(u) {
    return '<h2 class="group-head">' +
      '<span class="gh-title">' + esc(u.title) + '</span>' +
      (u.note ? '<span class="gh-note">' + esc(u.note) + '</span>' : '') +
      '<span class="gh-count">' + u.items.length + '</span>' +
      '</h2>';
  }

  function renderBrowse() {
    var ch = state.chapter;
    state.rows = [];
    var html = '';
    var gi = -1;

    ch.units.forEach(function (u) {
      if (u.kind === 'section') {
        html += '<div class="sect-head">' +
          '<span class="sect-title">' + esc(u.title) + '</span>' +
          (u.note ? '<span class="sect-note">' + esc(u.note) + '</span>' : '') +
          '</div>';
        return;
      }
      gi++;
      var start = state.rows.length;
      u.items.forEach(function (it) { state.rows.push({ ch: ch, it: it }); });

      html += '<section class="group" data-g="' + gi + '">' + groupHeadHTML(u) + '<div class="items">';
      for (var i = start; i < state.rows.length; i++) html += itemHTML(state.rows[i].it, i, '');
      html += '</div></section>';
    });

    content.innerHTML = html;
    cacheGroupEls();
    renderGroupbar();
  }

  function renderSearch() {
    var res = window.CodexSearch.search(window.Codex.chapters, state.query);
    state.rows = res.hits;

    if (!res.hits.length) {
      content.innerHTML = '<div class="empty">' +
        '<b>没有匹配「' + esc(state.query) + '」的条目</b>' +
        '<p>可以试试中文名、代码片段，或拼音首字母（<code>jrsz</code> → 巨人僵尸）。</p>' +
        '</div>';
      return;
    }

    var html = '<div class="result-bar">' +
      '<b>' + res.total + '</b> 条结果' +
      (res.truncated ? '<span class="rb-warn">只显示前 ' + res.hits.length + ' 条，请补充关键词</span>' : '') +
      '</div><section class="group"><div class="items">';
    for (var i = 0; i < res.hits.length; i++) {
      html += itemHTML(res.hits[i].it, i, groupBadgeOf(res.hits[i]));
    }
    html += '</div></section>';
    content.innerHTML = html;
  }

  function renderContent() {
    if (state.searching) renderSearch(); else renderBrowse();
    content.scrollTop = 0;
  }

  function cacheGroupEls() {
    state.chapter.groupUnits.forEach(function (u) { u.el = null; });
    var els = content.querySelectorAll('.group');
    for (var i = 0; i < els.length; i++) {
      var g = +els[i].getAttribute('data-g');
      if (state.chapter.groupUnits[g]) state.chapter.groupUnits[g].el = els[i];
    }
  }

  // ── 渲染：导航 ────────────────────────────────────────────────────────
  function renderChapterbar() {
    var html = '';
    window.Codex.chapters.forEach(function (ch) {
      html += '<button class="chip ch' + (ch === state.chapter ? ' on' : '') +
        '" data-ch="' + esc(ch.id) + '">' + esc(ch.title) +
        '<span class="chip-n">' + ch.count + '</span></button>';
    });
    chapterbar.innerHTML = html;
  }

  function renderGroupbar() {
    var html = '';
    var lastSec = null;
    state.chapter.groupUnits.forEach(function (u, i) {
      if (u.section && u.section !== lastSec) {
        lastSec = u.section;
        html += '<span class="chip-sep">' + esc(u.section) + '</span>';
      }
      html += '<button class="chip" data-g="' + i + '">' + esc(u.title) + '</button>';
    });
    groupbar.innerHTML = html;
    markActive();
    groupbar.scrollLeft = 0;
  }

  function renderSidebar() {
    var html = '<div class="side-head">代码图鉴</div><div class="side-body">';
    html += '<div class="side-label">章节</div>';
    window.Codex.chapters.forEach(function (ch) {
      html += '<button class="side-ch' + (ch === state.chapter ? ' on' : '') + '" data-ch="' + esc(ch.id) + '">' +
        '<span class="side-t">' + esc(ch.title) + '</span>' +
        '<span class="side-n">' + ch.count + '</span></button>';
    });
    html += '<div class="side-label">' + esc(state.chapter.title) + ' · 目录</div>';
    var lastSec = null;
    state.chapter.groupUnits.forEach(function (u, i) {
      if (u.section && u.section !== lastSec) {
        lastSec = u.section;
        html += '<div class="side-sec">' + esc(u.section) + '</div>';
      }
      html += '<button class="side-g" data-g="' + i + '">' +
        '<span class="side-t">' + esc(u.title) + '</span>' +
        '<span class="side-n">' + u.items.length + '</span></button>';
    });
    html += '</div>';
    sidebar.innerHTML = html;
  }

  function markActive() {
    var chips = groupbar.querySelectorAll('.chip[data-g]');
    for (var i = 0; i < chips.length; i++) {
      chips[i].classList.toggle('on', +chips[i].getAttribute('data-g') === state.activeGroup);
    }
    var on = groupbar.querySelector('.chip.on');
    if (on && (on.offsetLeft < groupbar.scrollLeft ||
               on.offsetLeft + on.offsetWidth > groupbar.scrollLeft + groupbar.clientWidth)) {
      groupbar.scrollTo({ left: on.offsetLeft - groupbar.clientWidth / 2 + on.offsetWidth / 2, behavior: 'smooth' });
    }
    var sides = sidebar.querySelectorAll('.side-g');
    for (var j = 0; j < sides.length; j++) {
      sides[j].classList.toggle('on', +sides[j].getAttribute('data-g') === state.activeGroup);
    }
  }

  // ── 滚动高亮 ──────────────────────────────────────────────────────────
  var ticking = false;
  function onScroll() {
    if (ticking || state.searching) return;
    ticking = true;
    requestAnimationFrame(function () { ticking = false; spy(); });
  }

  function spy() {
    var gus = state.chapter.groupUnits;
    var top = content.scrollTop + 4;
    var cur = 0;
    for (var i = 0; i < gus.length; i++) {
      var el = gus[i].el;
      if (el && el.offsetTop <= top) cur = i; else break;
    }
    if (cur !== state.activeGroup) { state.activeGroup = cur; markActive(); }
  }

  function gotoGroup(i, closeDrawer) {
    var u = state.chapter.groupUnits[i];
    if (!u || !u.el) return;
    content.scrollTo({ top: u.el.offsetTop, behavior: 'auto' });
    state.activeGroup = i;
    markActive();
    if (closeDrawer) closeSide();
  }

  // ── 章节切换 / 搜索 ───────────────────────────────────────────────────
  function pickChapter(id) {
    state.chapter = window.Codex.get(id) || window.Codex.first();
    state.activeGroup = 0;
    renderChapterbar();
    renderSidebar();
    renderContent();
    if (!state.searching) renderGroupbar();
  }

  function setQuery(q) {
    state.query = q;
    var was = state.searching;
    state.searching = !!q.trim();
    document.body.classList.toggle('is-searching', state.searching);
    if (state.searching || was) renderContent();
    if (!state.searching) { renderGroupbar(); spy(); }
  }

  // ── 复制 ──────────────────────────────────────────────────────────────
  function doCopy(it, rowEl) {
    var text = settings.format === 'quoted' ? '"' + it.code + '"' : it.code;
    copyText(text).then(function () {
      toast('已复制 ' + text);
      var icon = rowEl.querySelector('.it-copy');
      rowEl.classList.add('copied');
      if (icon) icon.innerHTML = ICON_CHECK;
      setTimeout(function () {
        rowEl.classList.remove('copied');
        if (icon) icon.innerHTML = ICON_COPY;
      }, 700);
    }).catch(function () {
      toast('复制失败，请长按代码手动选择', true);
    });
  }

  // ── 侧栏 / 设置面板开合 ───────────────────────────────────────────────
  function openSide() { sidebar.classList.add('open'); scrim.classList.add('open'); }
  function closeSide() { sidebar.classList.remove('open'); scrim.classList.remove('open'); }

  function toggleSettings(force) {
    var show = force === undefined ? settingsEl.hidden : force;
    settingsEl.hidden = !show;
    document.getElementById('btn-settings').classList.toggle('on', show);
  }

  function syncSettingsUI() {
    var segs = settingsEl.querySelectorAll('.seg');
    for (var i = 0; i < segs.length; i++) {
      var key = segs[i].getAttribute('data-key');
      var btns = segs[i].querySelectorAll('button');
      for (var j = 0; j < btns.length; j++) {
        btns[j].classList.toggle('on', btns[j].getAttribute('data-val') === settings[key]);
      }
    }
  }

  // ── 事件绑定 ──────────────────────────────────────────────────────────
  function bind() {
    content.addEventListener('scroll', onScroll, { passive: true });

    content.addEventListener('click', function (ev) {
      var row = ev.target.closest ? ev.target.closest('.item') : null;
      if (!row) return;
      var rec = state.rows[+row.getAttribute('data-r')];
      if (rec) doCopy(rec.it, row);
    });

    chapterbar.addEventListener('click', function (ev) {
      var b = ev.target.closest ? ev.target.closest('[data-ch]') : null;
      if (b) pickChapter(b.getAttribute('data-ch'));
    });

    groupbar.addEventListener('click', function (ev) {
      var b = ev.target.closest ? ev.target.closest('[data-g]') : null;
      if (b) gotoGroup(+b.getAttribute('data-g'), false);
    });

    sidebar.addEventListener('click', function (ev) {
      var t = ev.target.closest ? ev.target.closest('[data-ch],[data-g]') : null;
      if (!t) return;
      if (t.hasAttribute('data-ch')) { pickChapter(t.getAttribute('data-ch')); closeSide(); }
      else gotoGroup(+t.getAttribute('data-g'), true);
    });

    input.addEventListener('input', function () {
      clearBtn.hidden = !input.value;
      setQuery(input.value);
    });
    input.addEventListener('keydown', function (ev) {
      if (ev.key === 'Escape') { input.value = ''; clearBtn.hidden = true; setQuery(''); input.blur(); }
    });
    clearBtn.addEventListener('click', function () {
      input.value = ''; clearBtn.hidden = true; setQuery(''); input.focus();
    });

    document.getElementById('btn-menu').addEventListener('click', openSide);
    scrim.addEventListener('click', closeSide);

    document.getElementById('btn-settings').addEventListener('click', function (ev) {
      ev.stopPropagation();
      toggleSettings();
    });
    settingsEl.addEventListener('click', function (ev) {
      ev.stopPropagation();
      var b = ev.target.closest ? ev.target.closest('button[data-val]') : null;
      if (!b) return;
      var key = b.parentNode.getAttribute('data-key');
      settings[key] = b.getAttribute('data-val');
      saveSettings();
      if (key === 'theme') applyTheme();
      syncSettingsUI();
    });
    document.addEventListener('click', function () { toggleSettings(false); });

    document.addEventListener('keydown', function (ev) {
      if (ev.key === 'Escape') { closeSide(); toggleSettings(false); }
      // 「/」或 Ctrl+K 直接聚焦搜索
      var typing = /^(INPUT|TEXTAREA)$/.test(document.activeElement.tagName);
      if (!typing && (ev.key === '/' || (ev.key === 'k' && (ev.ctrlKey || ev.metaKey)))) {
        ev.preventDefault();
        input.focus();
        input.select();
      }
    });
  }

  // ── 启动 ──────────────────────────────────────────────────────────────
  function loadChapters(list) {
    return Promise.all(list.map(function (src) {
      return new Promise(function (resolve) {
        var s = document.createElement('script');
        s.src = src;
        s.async = false;                 // 动态插入时保持执行顺序
        s.onload = function () { resolve({ src: src, ok: true }); };
        s.onerror = function () { resolve({ src: src, ok: false }); };
        document.head.appendChild(s);
      });
    }));
  }

  function boot() {
    loadSettings();
    applyTheme();
    syncSettingsUI();
    bind();

    var manifest = window.CODEX_MANIFEST || [];
    loadChapters(manifest).then(function (results) {
      var failed = results.filter(function (r) { return !r.ok; });
      if (failed.length) console.warn('数据文件加载失败：', failed.map(function (f) { return f.src; }));

      if (!window.Codex.chapters.length) {
        content.innerHTML = '<div class="empty"><b>没有加载到任何章节数据</b>' +
          '<p>请检查 <code>js/chapters.js</code> 里的路径，以及 <code>data/</code> 下是否存在对应文件。</p>' +
          (failed.length ? '<p class="empty-list">' + failed.map(function (f) { return esc(f.src); }).join('<br>') + '</p>' : '') +
          '</div>';
        return;
      }
      pickChapter(window.Codex.chapters[0].id);
    });
  }

  boot();
})();
