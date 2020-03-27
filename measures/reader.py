#!/usr/bin/env python3

import re

def get_output(text):
    last_end = 0
    composed = ''
    for match in _find_lists(text):
        composed += text[last_end:match.start()]
        last_end = match.end()
    composed += text[last_end:]
    return composed

def get_lists(text):
    result = {}
    for match in _find_lists(text):
        result[match.group(1)] = list(_to_lines(match.group(2)))
    return result

def sort_items(items):
    return sorted(items, reverse=True, key=lambda x: x[1])

def make_dict(lines):
    return dict(map(_parse_dataline, lines))

def quota(number, total):
    return '%.2f%%' % (number * 100 / total)

def sublists(lines):
    current = []
    for line in lines:
        if 'list delimeter' == line:
            yield current
            current = []
            continue
        current.append(line)
    yield current

def _find_lists(text):
    regex = r'start list (\w+)\n(.*?)\nend list \1\n'
    return re.finditer(regex, text, re.DOTALL)

def _to_lines(text):
    for line in text.split('\n'):
        line = line.strip()
        if not len(line):
            continue
        yield line

def _parse_dataline(line):
    match = re.search(r'(.*): ([0-9]+)', line)
    return (match.group(1), int(match.group(2)))
