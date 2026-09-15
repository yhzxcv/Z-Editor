# -*- coding: utf-8 -*-
"""
代码图鉴 TXT -> 静态网站数据文件（一次性迁移工具）

用法:
    python migrate.py [章节id ...]      不给参数则处理全部已配置章节
    python migrate.py --preview <id>    只看将要生成什么，不落盘
    python migrate.py --force  <id>     允许覆盖已存在的数据文件

⚠ 数据文件（data/ch-*.js）早已是手工维护的唯一真源，已存在的文件默认不覆盖。
  植物和僵尸两章手工改过很多，绝不能再跑一次迁移。

产出:
    codex-site/data/ch-<id>.js     每个章节一个数据文件（此后为唯一真源，手工维护）
    codex-site/tools/qc-report.txt 质检报告：被丢弃/存疑的行，逐条列出

设计约定见 ../data/README.md。规则按章节配置在 CHAPTERS 里。
"""
import os
import re
import sys
import io
import subprocess

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

HERE = os.path.dirname(os.path.abspath(__file__))
SITE = os.path.dirname(HERE)
SRC = os.path.join(os.path.dirname(SITE), '代码图鉴.txt')
DATA = os.path.join(SITE, 'data')
QC = os.path.join(HERE, 'qc-report.txt')

# ── 条目行解析 ────────────────────────────────────────────────────────────
# 形如：  [来源] 名称 code（备注）   /   名称 code   /   [来源]名称 code
ITEM_RE = re.compile(
    r'^\s*'
    r'(?:\[(?P<src>[^\]]*)\]\s*)?'          # 可选 [来源]
    r'(?P<name>.*?)'                         # 中文名
    r'\s*'
    r'(?P<code>[A-Za-z][A-Za-z0-9_]*)'       # 代码
    r'\s*'
    r'(?:[（(](?P<note>[^）)]*)[）)])?'      # 可选尾随备注
    r'\s*$'
)


# 整行被括注包裹（如 `（星寻tips：…）`）→ 说明文字，不是条目
WRAPPED_RE = re.compile(r'^[（(].*[）)]$')
# 整行只有一个代码（障碍物章节有 57 条把名字和代码拆成两行写）
BARE_CODE_RE = re.compile(r'^[A-Za-z][A-Za-z0-9_]*$')
# 任意一层标题行的首字符
MARK_RE = re.compile(r'^(?:--|[◆◇○\-=#（(])')
# 分组标题尾部的括注（如 `远征之门（星寻tips：…）`）→ 拆成分组说明
TITLE_TAIL_RE = re.compile(r'^(?P<t>.*?)\s*[（(](?P<n>[^（()）]+)[）)]\s*$')


def split_title_note(title):
    m = TITLE_TAIL_RE.match(title)
    if m and m.group('t').strip():
        return m.group('t').strip(), m.group('n').strip()
    return title, ''


def is_group_label(line):
    """`(箭箭爆头)` 这类小游戏分组标签：短、无冒号。"""
    if not WRAPPED_RE.match(line):
        return False
    inner = line[1:-1]
    return len(inner) <= 12 and '：' not in inner and ':' not in inner


def is_parent_label(line, lines, idx):
    """无代码的短行，且其下紧跟着有条目的行 → 父级标签。

    形如远征之门的： 精英猎人僵尸 / 普通难度关卡1-6 soudache_d1_n6_hunter_elite
    带 `?` 的（如 `忧郁菇 ???`）视为条目本身而非标签。
    """
    if '?' in line or '？' in line or len(line) > 14:
        return False
    for j in range(idx + 1, len(lines)):
        s = lines[j].strip()
        if not s:
            continue
        if s.startswith(('-', '--', '○', '●', '■', '□', '#', '(', '（')):
            return False
        return parse_item(s) is not None
    return False


def parse_item(line):
    """返回 (name, src, code, note)；无法解析出 code 时返回 None。"""
    m = ITEM_RE.match(line)
    if not m or not m.group('code'):
        return None
    name = m.group('name').strip()
    src = (m.group('src') or '').strip().rstrip('｜|')      # [爆炸坚果｜] → 爆炸坚果
    # 名称为空且没有来源 → 不可用（纯代码行由调用方先处理）
    return name, src, m.group('code'), (m.group('note') or '').strip()


# ── JS 字面量 ────────────────────────────────────────────────────────────
def js_str(s):
    return "'" + s.replace('\\', '\\\\').replace("'", "\\'") + "'"


def join_notes(notes):
    """多条说明合成一个字符串。

    注意：不能写成 'a' 'b' 这样的相邻字面量 —— Python 允许，JS 是语法错误。
    """
    return ' '.join(n.strip() for n in notes if n.strip())


def js_item(it):
    parts = ['name: ' + js_str(it['name']), 'code: ' + js_str(it['code'])]
    if it.get('src'):
        parts.append('src: ' + js_str(it['src']))
    if it.get('note'):
        parts.append('note: ' + js_str(it['note']))
    return '{ ' + ', '.join(parts) + ' },'


def emit_group(out, title, items, pad, note=''):
    """输出一个分组（只有一个 items 层）。"""
    out.append('%s{' % pad)
    out.append('%s  title: %s,' % (pad, js_str(title)))
    if note:
        out.append('%s  note: %s,' % (pad, js_str(note)))
    out.append('%s  items: [' % pad)
    for it in items:
        out.append('%s    %s' % (pad, js_item(it)))
    out.append('%s  ],' % pad)
    out.append('%s},' % pad)


# ── 章节配置 ─────────────────────────────────────────────────────────────
# range   : TXT 中的行范围（1-based，含首尾，首行为《章节名》）
# section : 顶层大类标记（1 个字符），无则 None
# group   : 分组标记字符列表
# drop    : 直接丢弃的行前缀（视觉分隔线等）
CHAPTERS = {
    'plant': dict(
        title='植物代码',
        range=(44, 551),
        section='■',
        group=['-', '□'],
        skip=[],
        container=True,        # --世界 / --类别划分 是容器，子项提升为平级世界
        subgroup_labels=False,
        drop='=',
    ),
    'zombie': dict(
        title='僵尸代码',
        range=(553, 1945),
        section=None,          # 不建 ● 层，只按主题归属分一层
        # ● 也作为分组：●主线/●时空秘境及节日/●玩法专属 无直属条目，会被空分组规则去掉；
        # ●特殊僵尸 下直接挂着 5 条（雪人、花瓶巨人…），必须保留
        group=['●', '○', '--'],
        skip=['-'],            # -精英僵尸 等子标签不建层级
        container=False,
        subgroup_labels=False,
        drop='=',
        # 无尽模式专属僵尸：代码含 danger 的是该世界的无尽版本，与主线关**同名**
        # （如 黑暗巨人僵尸 dark_gargantuar / dark_gargantuar_danger 同属黑暗时代），
        # 名称前缀「无尽」以示区分。
        # 后缀含 danger 共 21 条，与源文件中 -无尽模式 标签下的条目完全重合：
        # 14 条 *_gargantuar_danger + 7 条 *_dangerroom（复兴时代 剧团操纵者及四名演员）。
        name_prefix=[('danger', '无尽')],
    ),
    # 结构最随意的一章，用 levels 引擎（见 migrate_levels 上方注释）。
    # 层级：◆/◇ 都是大类；○ 是常规障碍物那半边的分组；
    #       -- 只在裂缝合集里出现，- 在有些大类是分组、有些是子标签。
    'obstacle': dict(
        title='障碍物代码',
        range=(2344, 3087),
        section=['◆', '◇'],
        levels=['○', '--', '-'],       # 由浅到深
        drop_titles=['◆可自定义障碍物'],   # 每条下面挂着「属性」行，整段不要
        container_titles=['◆常规障碍物'],  # 纯容器，其下的 ◇ 提升为大类
        drop='=',
    ),
}


# ── 引擎 ────────────────────────────────────────────────────────────────
def is_container(lines, idx, cfg):
    """判断某个 '--' 标题是容器（子项提升）还是分组自身。

    容器 = 其后的下一个标题之前存在更深一级的 '-' 标题。
    """
    for j in range(idx + 1, len(lines)):
        s = lines[j].strip()
        if not s:
            continue
        if s.startswith('--') or (cfg['section'] and s.startswith(cfg['section'])):
            return False
        if s.startswith('-'):
            return True
    return False


def migrate(cid, cfg, report):
    lo, hi = cfg['range']
    all_lines = open(SRC, encoding='utf-8').read().split('\n')
    lines = all_lines[lo - 1:hi]
    sec_mark = cfg['section']

    intro = []
    sections = []          # [{title, children:[{title, items:[]}]}]
    cur_sec = None
    cur_grp = None
    dropped = []
    labels = []            # 被降级为来源小字的父级标签
    prefixed = []          # 被加了「无尽」等前缀的条目
    pending_src = ''       # 当前生效的父级标签

    def group(title):
        nonlocal cur_grp, cur_sec, pending_src
        pending_src = ''
        t, note = split_title_note(title)
        cur_grp = {'title': t, 'items': []}
        if note:
            cur_grp['note'] = [note]
        if cur_sec is None:
            cur_sec = {'title': '', 'children': []}
            sections.append(cur_sec)
        cur_sec['children'].append(cur_grp)

    def add_note(text):
        """说明文字挂到当前分组；无分组则挂到大类 / 章节前言。"""
        if cur_grp is not None:
            cur_grp.setdefault('note', []).append(text)
        elif cur_sec is not None:
            cur_sec.setdefault('note', []).append(text)
        else:
            intro.append(text)

    for i, raw in enumerate(lines):
        s = raw.strip()
        if not s:
            continue
        if s.startswith('《') or s.startswith(cfg['drop']):
            continue

        if sec_mark and s.startswith(sec_mark):
            cur_sec = {'title': s[1:].strip(), 'children': []}
            sections.append(cur_sec)
            cur_grp = None
            pending_src = ''
            continue

        # 说明行：# 开头，或整行被括注包裹且不是短标签
        if s.startswith('#'):
            add_note(s.lstrip('#').strip())
            continue
        if is_group_label(s):
            if cfg.get('subgroup_labels'):
                group(s[1:-1].strip())
            else:
                # 小游戏名等标签：同样降级为后续条目的来源小字
                pending_src = s[1:-1].strip()
                labels.append((lo + i, s))
            continue
        if WRAPPED_RE.match(s):
            add_note(s)
            continue

        if s.startswith('--'):
            if cfg.get('container') and is_container(lines, i, cfg):
                cur_grp = None                # 容器本身不建节点，子项提升
                continue
            group(s[2:].strip())
            continue

        if any(s.startswith(g) for g in cfg['group']):
            group(s[1:].strip())
            continue

        if any(s.startswith(g) for g in cfg.get('skip', [])):
            continue                          # 该层标记不建节点，条目并入当前分组

        # 普通行
        if cur_grp is None:
            add_note(s)
            continue
        it = parse_item(s)
        if it is None:
            # 无代码的短行且其下紧跟着条目 → 视为父级标签，降级为后续条目的来源小字
            if is_parent_label(s, lines, i):
                pending_src = s
                labels.append((lo + i, s))
            else:
                dropped.append((lo + i, s))
            continue
        name, src, code, note = it
        if not name and not src:
            dropped.append((lo + i, s))
            continue
        for key, pfx in cfg.get('name_prefix', []):
            if key in code:
                prefixed.append((lo + i, name, code, pfx))
                name = pfx + name
                break
        # pending_src 持续生效到下一个标签/分组为止（一个标签下可能有多条）
        cur_grp['items'].append({'name': name, 'src': src or pending_src,
                                 'code': code, 'note': note})

    # 丢掉没有任何直属条目的分组（如 ●主线 这种纯容器），并留痕
    empty = []
    for sec in sections:
        keep = []
        for g in sec['children']:
            if g['items']:
                keep.append(g)
            else:
                empty.append(g['title'])
        sec['children'] = keep
    sections = [s for s in sections if s['children']]

    # ── 写数据文件 ──
    out = []
    out.append('// 代码图鉴 · %s' % cfg['title'])
    out.append('// 本文件是网站的唯一数据来源，可直接手工增删改。')
    out.append('// 条目字段：name 中文名（必填）｜code 代码（必填）｜src 来源小字（可选）｜note 备注小字（可选）')
    out.append('// 分组字段：title 标题｜items 条目列表｜children 子分组（与 items 二选一）')
    out.append('')
    out.append('Codex.add({')
    out.append("  id: %s," % js_str(cid))
    out.append("  title: %s," % js_str(cfg['title']))
    if intro:
        out.append('  intro: [')
        for n in intro:
            out.append('    %s,' % js_str(n))
        out.append('  ],')
    def emit_items(g, pad):
        emit_group(out, g['title'], g['items'], pad,
                   join_notes(g['note']) if g.get('note') else '')

    out.append('  groups: [')
    if len(sections) == 1 and not sections[0]['title']:
        # 单层结构：直接铺开分组，不套无意义的外壳
        for g in sections[0]['children']:
            emit_items(g, '    ')
    else:
        for sec in sections:
            out.append('    {')
            if sec['title']:
                out.append('      title: %s,' % js_str(sec['title']))
            if sec.get('note'):
                out.append('      note: %s,' % js_str(join_notes(sec['note'])))
            out.append('      children: [')
            for g in sec['children']:
                emit_items(g, '        ')
            out.append('      ],')
            out.append('    },')
    out.append('  ],')
    out.append('});')
    out.append('')

    written = True
    if not PREVIEW:
        written = write_data(cid, out)

    # ── 统计 ──
    ngroups = sum(len(s['children']) for s in sections)
    nitems = sum(len(g['items']) for s in sections for g in s['children'])
    report.append('【%s】%s' % (cid, cfg['title']))
    report.append('  分组 %d 个，条目 %d 条，前言 %d 行，丢弃 %d 行'
                  % (ngroups, nitems, len(intro), len(dropped)))
    if not written:
        report.append('  （未覆盖 data/ch-%s.js：下面是按 TXT 解析的结果，'
                      '与线上文件不一定一致）' % cid)
    for sec in sections:
        if sec['title']:
            report.append('  ┌ %s' % sec['title'])
        for grp in sec['children']:
            report.append('    %-32s %3d' % (grp['title'], len(grp['items'])))
        if not sec['title']:
            report.append('    %-32s %3d' % ('（以上合计）', sum(len(g['items']) for g in sec['children'])))
    if prefixed:
        report.append('  -- 名称被加前缀的条目（无尽模式专属，与主线关同名需区分）--')
        for ln, old, code, pfx in prefixed:
            report.append('    L%-6d %s%s   (%s)' % (ln, pfx, old, code))
    if empty:
        report.append('  -- 因无直属条目而略去的容器标题（其子分组已提升）--')
        report.append('    ' + '、'.join(empty))
    if labels:
        report.append('  -- 降级为「来源小字」的父级标签（其下条目已被收纳）--')
        for ln, txt in labels:
            report.append('    L%-6d %s' % (ln, txt[:80]))
    if dropped:
        report.append('  -- 被丢弃的行（无代码或无法解析）--')
        for ln, txt in dropped:
            report.append('    L%-6d %s' % (ln, txt[:80]))
    report.append('')
    return nitems


# ── 层级引擎（障碍物这类「标记含义随大类而变」的章节）────────────────────
# 障碍物章节里 `-` 有两种身份：
#   在 ◇初始布置/事件生成 下，它是 ○世界 的子标签（-主线关/-小游戏）→ 该降级成来源小字
#   在 ◆牌面纷争/◆回忆之旅障碍物 下，它本身就是分组
# 所以不能像植物/僵尸那样全局写死一层，改成 levels 由浅到深排，
# 每个大类内部「出现过的最浅标记」当分组层，更深的降级为来源小字。
def migrate_levels(cid, cfg, report):
    lo, hi = cfg['range']
    all_lines = open(SRC, encoding='utf-8').read().split('\n')
    raw = all_lines[lo - 1:hi]

    sec_marks = cfg['section']
    levels = cfg['levels']
    drop_titles = set(cfg.get('drop_titles', []))
    container_titles = set(cfg.get('container_titles', []))

    # ① 合并「名字行 + 紧跟的裸代码行」
    merged = []
    i = 0
    while i < len(raw):
        s = raw[i].strip()
        nxt = raw[i + 1].strip() if i + 1 < len(raw) else ''
        if (s and BARE_CODE_RE.match(nxt) and not BARE_CODE_RE.match(s)
                and not MARK_RE.match(s)
                and not re.search(r'[A-Za-z][A-Za-z0-9_]*$', s)):
            merged.append((lo + i, s + ' ' + nxt))
            i += 2
            continue
        merged.append((lo + i, s))
        i += 1

    # ② 切段：大类标记开头，容器不建节点，忽略的整段跳过
    sections = []          # [{'title','lines':[(ln,text)]}]
    intro, notes_at = [], []
    cur = None
    skipping = False
    for ln, s in merged:
        if not s:
            continue
        if s[0] in sec_marks:
            if s in drop_titles:
                skipping, cur = True, None
                continue
            skipping = False
            if s in container_titles:      # ◆常规障碍物：本身不建节点
                cur = None
                continue
            cur = {'title': s[1:].strip(), 'lines': []}
            sections.append(cur)
            continue
        if skipping:
            continue
        if cur is None:
            if s.startswith('#'):
                intro.append(s.lstrip('#').strip())
            elif WRAPPED_RE.match(s):
                intro.append(s)
            continue
        if is_group_label(s):
            cur['lines'].append((ln, s))        # 小游戏标签，留给展开阶段当来源小字
            continue
        if s.startswith('#') or WRAPPED_RE.match(s):
            # 说明文字一律挂到大类上（障碍物的说明都是整段的吐槽/提示）
            cur.setdefault('notes', []).append(s.lstrip('#').strip())
            continue
        cur['lines'].append((ln, s))

    # ③ 每个大类挑分组层
    def group_level(sec):
        for lv in levels:
            if any(t.startswith(lv) for _, t in sec['lines']):
                return lv
        return None

    # ④ 展开成 大类/分组/条目
    #    entries 保持文件顺序：有子标题的当大类，没子标题的降级成普通分组，两类交错排列
    entries = []
    dropped, labels = [], []

    for sec in sections:
        # 大标题不做「括号拆note」：源文件里 ◇初始布置/事件生成(主线和秘境) 和 (其他)
        # 正是靠括号区分的，拆掉就同名了 —— 而 u.section 就是标题，同名会让
        # 分组 chips 和侧栏目录漏掉第二个大类的分隔符。
        title = sec['title']
        note = ' '.join(sec.get('notes', [])).strip()
        lv = group_level(sec)

        if lv is None:
            # 无名分层可用 → 整个大类降级为一个普通分组
            g = {'title': title, 'note': note, 'items': []}
            pending_src = ''
            for ln, t in sec['lines']:
                it = parse_item(t)
                if it is None:
                    dropped.append((ln, t))
                    continue
                name, src, code, inote = it
                g['items'].append({'name': name, 'src': src or pending_src,
                                   'code': code, 'note': inote})
            flat_groups_entry = {'kind': 'flat', 'title': title,
                                 'note': note, 'items': g['items']}
            entries.append(flat_groups_entry)
            continue

        deeper = levels[levels.index(lv) + 1:]
        groups = []
        cur_g = None
        pending_src = ''
        for ln, t in sec['lines']:
            if t.startswith(lv):
                cur_g = {'title': t[len(lv):].strip(), 'items': []}
                gt, gn = split_title_note(cur_g['title'])
                cur_g['title'], cur_g['note'] = gt, gn
                groups.append(cur_g)
                pending_src = ''
                continue
            if any(t.startswith(d) for d in deeper):
                pending_src = t[1:].strip()     # -主线关 / -无尽模式 → 来源小字
                labels.append((ln, t))
                continue
            # `(铜人阵)` 这类小游戏标签：短、无冒号，降级为来源小字而非分组说明
            if is_group_label(t):
                pending_src = t[1:-1].strip()
                labels.append((ln, t))
                continue
            if cur_g is None:
                dropped.append((ln, t))
                continue
            it = parse_item(t)
            if it is None:
                dropped.append((ln, t))
                continue
            name, src, code, inote = it
            cur_g['items'].append({'name': name, 'src': src or pending_src,
                                   'code': code, 'note': inote})

        groups = [g for g in groups if g['items']]
        if groups:
            entries.append({'kind': 'sec', 'title': title, 'note': note,
                            'groups': groups})
        else:
            entries.append({'kind': 'flat', 'title': title, 'note': note,
                            'items': []})

    return _emit_levels(cid, cfg, entries, intro, report, dropped, labels)


def _emit_levels(cid, cfg, entries, intro, report, dropped, labels):
    out = []
    out.append('// 代码图鉴 · %s' % cfg['title'])
    out.append('// 本文件是网站的唯一数据来源，可直接手工增删改。')
    out.append('// 条目字段：name 中文名（必填）｜code 代码（必填）｜src 来源小字（可选）｜note 备注小字（可选）')
    out.append('// 分组字段：title 标题｜items 条目列表｜children 子分组（与 items 二选一）')
    out.append('')
    out.append('Codex.add({')
    out.append('  id: %s,' % js_str(cid))
    out.append('  title: %s,' % js_str(cfg['title']))
    if intro:
        out.append('  intro: [')
        for n in intro:
            out.append('    %s,' % js_str(n))
        out.append('  ],')
    out.append('  groups: [')
    for e in entries:
        if e['kind'] == 'flat':
            emit_group(out, e['title'], e['items'], '    ', e['note'])
            continue
        out.append('    {')
        out.append('      title: %s,' % js_str(e['title']))
        if e['note']:
            out.append('      note: %s,' % js_str(e['note']))
        out.append('      children: [')
        for g in e['groups']:
            emit_group(out, g['title'], g['items'], '        ', g['note'])
        out.append('      ],')
        out.append('    },')
    out.append('  ],')
    out.append('});')
    out.append('')

    written = True
    if not PREVIEW:
        written = write_data(cid, out)

    # 别写成一行条件表达式：真值分支里的 g 会漏到外层作用域，抓到上一个循环的残留值
    nitems = 0
    for e in entries:
        if e['kind'] == 'flat':
            nitems += len(e['items'])
        else:
            nitems += sum(len(grp['items']) for grp in e['groups'])
    nsecs = sum(1 for e in entries if e['kind'] == 'sec')
    ngroups = (sum(1 for e in entries if e['kind'] == 'flat')
               + sum(len(e['groups']) for e in entries if e['kind'] == 'sec'))
    report.append('【%s】%s' % (cid, cfg['title']))
    report.append('  大类 %d 个，分组 %d 个，条目 %d 条，前言 %d 行，丢弃 %d 行'
                  % (nsecs, ngroups, nitems, len(intro), len(dropped)))
    if not written:
        report.append('  （未覆盖 data/ch-%s.js：下面是按 TXT 解析的结果，'
                      '与线上文件不一定一致）' % cid)
    for e in entries:
        if e['kind'] == 'flat':
            report.append('  （无子标题，降级为普通分组）%-28s %3d'
                          % (e['title'], len(e['items'])))
            continue
        report.append('  ┌ %s' % e['title'])
        for g in e['groups']:
            report.append('    %-32s %3d' % (g['title'], len(g['items'])))
    if labels:
        report.append('  -- 降级为「来源小字」的子标题 --')
        report.append('    ' + '、'.join(sorted({t for _, t in labels})))
    if dropped:
        report.append('  -- 被丢弃的行（无代码或无法解析）--')
        for ln, txt in dropped:
            report.append('    L%-6d %s' % (ln, txt[:80]))
    report.append('')
    return nitems


PREVIEW = False
FORCE = False


def write_data(cid, out):
    """落盘。已经存在的文件默认不覆盖 —— 数据文件早已转为手工维护的真源，
    误跑一次 `python migrate.py`（不给参数＝全部章节）就会把手工改动全冲掉。"""
    path = os.path.join(DATA, 'ch-%s.js' % cid)
    if os.path.exists(path) and not FORCE:
        print('跳过 data/ch-%s.js：文件已存在（手工维护的真源），未覆盖。要覆盖加 --force' % cid)
        return False
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(out))
    print('已生成 data/ch-%s.js' % cid)
    return True


def main():
    global PREVIEW, FORCE
    argv = sys.argv[1:]
    PREVIEW = '--preview' in argv
    FORCE = '--force' in argv
    want = [a for a in argv if not a.startswith('--')] or list(CHAPTERS)
    import hashlib
    blob = open(SRC, 'rb').read()
    report = ['代码图鉴迁移质检报告', '=' * 60, '',
              '源文件: 代码图鉴.txt',
              '  大小 %d 字节 / %d 行' % (len(blob), blob.count(b'\n') + 1),
              '  SHA1 %s' % hashlib.sha1(blob).hexdigest(),
              '  （数据以此版本为准；TXT 若再改动需重新迁移）', '']
    total = 0
    for cid in want:
        if cid not in CHAPTERS:
            print('未知章节: %s' % cid)
            continue
        cfg = CHAPTERS[cid]
        fn = migrate_levels if 'levels' in cfg else migrate
        total += fn(cid, cfg, report)
        if PREVIEW:
            print('预览 data/ch-%s.js' % cid)
    # ── 生成物语法自检 ──
    # 这里是手工拼 JS 字面量，最容易出「结构看着对、其实是非法语法」的错
    # （曾经把分组 note 写进 items 数组里，浏览器只会整章空白）。用 node 兜一道。
    if not PREVIEW:
        bad = []
        for cid in want:
            if cid not in CHAPTERS:
                continue
            path = os.path.join(DATA, 'ch-%s.js' % cid)
            try:
                r = subprocess.run(['node', '--check', path],
                                   capture_output=True, text=True)
            except OSError:
                bad = []
                report.append('（本机没有 node，跳过生成物语法自检）')
                break
            if r.returncode != 0:
                bad.append((cid, r.stderr.strip().split('\n')[0:4]))
        if bad:
            for cid, err in bad:
                print('!! data/ch-%s.js 语法错误：' % cid)
                print('   ' + '\n   '.join(err))
            with open(QC, 'w', encoding='utf-8', newline='\n') as f:
                f.write('\n'.join(report))
            sys.exit(1)
        report.append('生成物 JS 语法自检：全部通过（node --check）')
        report.append('')

    with open(QC, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(report))
    print('合计条目 %d，质检报告 -> tools/qc-report.txt' % total)


if __name__ == '__main__':
    main()
