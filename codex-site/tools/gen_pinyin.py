# -*- coding: utf-8 -*-
"""生成拼音首字母表 codex-site/js/pinyin-table.js（一次性，改动极少时重跑）

原理：GB2312 一级汉字区（区 16-55，码位 0xB0A1-0xD7F9）是按拼音排序的，
用 23 个「边界字」切分即可得到每个字的拼音首字母，无需任何第三方库。
二级汉字区（区 56-87）按部首笔画排序，此法不适用，故不给首字母。

只依赖标准库 codecs，产出两张等长的字符串：
    PY_CHARS    按 UTF-16 码位升序排列的汉字（便于 JS 二分查找）
    PY_INITIALS 与之逐字对应的首字母（非一级汉字为空格，表示无拼音）
"""
import io
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

HERE = __import__('os').path.dirname(__import__('os').path.abspath(__file__))
OUT = __import__('os').path.join(__import__('os').path.dirname(HERE), 'js', 'pinyin-table.js')

# 每个字母在这一段汉字的起始边界字
BOUNDARY = '啊芭擦搭蛾发噶哈击喀垃妈拿哦啪期然撒塌挖昔压匝'
LETTERS = 'ABCDEFGHJKLMNOPQRSTWXYZ'

GB_LO, GB_HI = 0xB0A1, 0xD7F9          # GB2312 一级汉字区


def gb_code(ch):
    try:
        b = ch.encode('gb2312')
    except UnicodeEncodeError:
        return None
    return b[0] * 256 + b[1] if len(b) == 2 else None


def initial_of(ch):
    code = gb_code(ch)
    if code is None or not (GB_LO <= code <= GB_HI):
        return ' '                      # 非一级汉字：无拼音首字母
    letter = LETTERS[0]
    for i, b in enumerate(BOUNDARY):
        if code >= gb_code(b):
            letter = LETTERS[i]
        else:
            break
    return letter


def main():
    pairs = []
    for cp in range(0x4E00, 0xA000):
        ch = chr(cp)
        ini = initial_of(ch)
        if ini != ' ':
            pairs.append((ch, ini))
    pairs.sort(key=lambda p: ord(p[0]))

    chars = ''.join(c for c, _ in pairs)
    inits = ''.join(i for _, i in pairs)

    out = [
        '// 代码图鉴 · 拼音首字母表（由 tools/gen_pinyin.py 生成，勿手改）',
        '// 覆盖 GB2312 一级汉字 %d 个；二级汉字与生僻字无拼音，按原字匹配即可。' % len(chars),
        '// PY_CHARS 按码位升序，PY_INITIALS 逐字对应；查表见 js/search.js 的 pinyinInitials()。',
        '(function (root) {',
        "  var CHARS = '%s';" % chars,
        "  var INITS = '%s';" % inits,
        '  function initial(ch) {',
        '    var lo = 0, hi = CHARS.length - 1, c = ch.charCodeAt(0);',
        '    while (lo <= hi) {',
        '      var mid = (lo + hi) >> 1, m = CHARS.charCodeAt(mid);',
        '      if (m === c) return INITS.charAt(mid);',
        '      if (m < c) lo = mid + 1; else hi = mid - 1;',
        '    }',
        "    return '';",
        '  }',
        "  // 黑暗巨人僵尸 -> 'hajrsz'",
        '  function initials(text) {',
        '    var out = \'\';',
        '    for (var i = 0; i < text.length; i++) {',
        '      var ch = text.charAt(i), code = text.charCodeAt(i);',
        "      if (code >= 0x4e00 && code < 0xa000) out += initial(ch).toLowerCase();",
        "      else if (/[a-z0-9]/i.test(ch)) out += ch.toLowerCase();",
        '    }',
        '    return out;',
        '  }',
        '  root.Pinyin = { initials: initials, initial: initial, size: CHARS.length };',
        '})(window);',
        '',
    ]
    with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(out))
    print('已生成 %s' % OUT)
    print('  收录一级汉字 %d 个，文件 %d 字节' % (len(chars), len('\n'.join(out).encode('utf-8'))))

    # 自检：逐条核对拼音首字母
    cases = [
        ('黑暗巨人僵尸', 'hajrjs'), ('豌豆射手', 'wdss'), ('向日葵', 'xrk'),
        ('平安时代', 'pasd'), ('重金属巨人僵尸', 'zjsjrjs'),
        ('无尽埃及巨人僵尸', 'wjajjrjs'), ('火炬树桩', 'hjsz'),
        ('僵尸', 'js'), ('植物', 'zw'), ('阳光', 'yg'), ('寒冰射手', 'hbss'),
    ]
    bad = 0
    for w, want in cases:
        got = ''.join(initial_of(c) for c in w).lower()
        ok = got == want
        bad += 0 if ok else 1
        print('  %-12s %-12s %s' % (w, got, 'OK' if ok else '!! 期望 ' + want))
    print('自检 %d/%d 通过' % (len(cases) - bad, len(cases)))


if __name__ == '__main__':
    main()
