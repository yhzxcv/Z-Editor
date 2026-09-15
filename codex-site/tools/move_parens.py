# -*- coding: utf-8 -*-
"""把条目标题里的括号内容搬到 note 字段（一次性清理，可重复运行）

    魔术帽(生产职业)  ->  { name: '魔术帽', code: '...', note: '生产职业' },

括号成对识别，全角（）半角()混用也能处理（`(内容）` 这种不配对的也行）。
名字会被去掉括号部分并压缩空白；括号内容成为 note。

只改匹配到的那些行，其余行逐字节保留，所以对手工维护的数据文件是安全的，
重复运行也不会叠加（第二次已无括号可搬）。

用法:
    python move_parens.py --dry-run plant     先看要改哪些，不落盘
    python move_parens.py plant               改植物
    python move_parens.py                     改 data/ 下全部章节
"""
import io
import os
import re
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(os.path.dirname(HERE), 'data')

# 全角半角都认，且允许左全角右半角这种混搭
PAREN_RE = re.compile(r'[（(]\s*([^）)]*?)\s*[）)]')
# 条目行里的 key: 'value'（value 里可能有 \' 转义）
FIELD_RE = re.compile(r"(\w+): '((?:[^'\\]|\\.)*)'")
ITEM_RE = re.compile(r'^(\s*)\{(.*)\},\s*$')


def extract_fields(body):
    """返回 [(key, value), ...]；不是合法的纯字段行则返回 None。"""
    pairs = FIELD_RE.findall(body)
    if not pairs:
        return None
    # 把匹配到的片段抠掉，剩下的应该只有标点和空白，否则说明这行有别的结构
    left = FIELD_RE.sub('', body)
    if left.replace(',', '').strip():
        return None
    keys = [k for k, _ in pairs]
    if 'name' not in keys or 'code' not in keys:
        return None
    if any(k not in ('name', 'code', 'src', 'note') for k in keys):
        return None
    return pairs


def process_line(line):
    """返回 (新行, 说明) 或 (None, 原因)。"""
    m = ITEM_RE.match(line)
    if not m:
        return None, None
    pairs = extract_fields(m.group(2))
    if pairs is None:
        return None, None

    fields = dict(pairs)
    name = fields['name']
    if '（' not in name and '(' not in name:
        return None, None

    notes = [n for n in PAREN_RE.findall(name) if n.strip()]
    if not notes:
        return None, None

    newname = PAREN_RE.sub('', name)
    newname = re.sub(r'\s+', ' ', newname).strip()
    if not newname:
        return None, '整个名字都是括号内容，跳过'

    if fields.get('note'):
        # 两种情况含义不同，交给人工判断，不自动合并
        return None, '已有 note「%s」，需人工决定怎么并' % fields['note']

    fields['name'] = newname
    fields['note'] = ' '.join(notes)

    order = [k for k in ('name', 'code', 'src', 'note') if k in fields]
    body = ', '.join("%s: '%s'" % (k, fields[k]) for k in order)
    return '%s{%s},' % (m.group(1), body), '%s -> note「%s」' % (name, fields['note'])


def process_file(path, dry_run):
    with open(path, encoding='utf-8', newline='') as f:
        lines = f.read().split('\n')

    changed, skipped = [], []
    for i, line in enumerate(lines):
        new, info = process_line(line)
        if info and not new:
            skipped.append((i + 1, info))
        elif new:
            changed.append((i + 1, line.strip(), new.strip()))
            lines[i] = new

    tag = os.path.basename(path)
    if not changed and not skipped:
        print('  %s：没有需要搬的括号' % tag)
        return 0

    for ln, old, new in changed:
        print('  L%-5d %s' % (ln, old))
        print('      ->  %s' % new)
    for ln, why in skipped:
        print('  L%-5d 跳过：%s' % (ln, why))

    if changed and not dry_run:
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write('\n'.join(lines))
        print('  已写入 %s（%d 行）' % (tag, len(changed)))
    elif changed:
        print('  [dry-run] %s 将改动 %d 行' % (tag, len(changed)))
    return len(changed)


def main():
    argv = sys.argv[1:]
    dry = '--dry-run' in argv
    want = [a for a in argv if not a.startswith('--')]

    if want:
        files = [os.path.join(DATA, 'ch-%s.js' % c) for c in want]
    else:
        files = sorted(os.path.join(DATA, f) for f in os.listdir(DATA)
                       if f.startswith('ch-') and f.endswith('.js'))

    total = 0
    for path in files:
        if not os.path.exists(path):
            print('找不到 %s' % path)
            continue
        print('【%s】' % os.path.basename(path))
        total += process_file(path, dry)
        print('')
    print(('[dry-run] 共 %d 行待改，未落盘' if dry else '共改动 %d 行') % total)


if __name__ == '__main__':
    main()
