#!/usr/bin/env python3

import reader

def do(lines, limit):
    basic, steps = _load_data(lines)
    for i, (rels, summary) in enumerate(steps, 1):
        total = sum(basic.values()) + sum(summary.values())
        qrels = reader.quota(summary['rels'], total)
        qaxis = reader.quota(summary['axis'], total)
        fmt = '%d: total: %d relations: %s axioms: %s'
        print(fmt % (i, total, qrels, qaxis))

        show_step(total, rels, limit)

def show_step(total, rels, limit):
    items = reader.sort_items(rels.items())
    for rel, number in items[:limit]:
        rel = rel.ljust(25)
        num = str(number).rjust(9)
        perc = reader.quota(number, total).rjust(10)
        print('%s %s %s' % (rel, num, perc))

def _load_data(lines):
    sublists = list(reader.sublists(lines))
    basic = reader.make_dict(sublists.pop(0));

    assert not (len(sublists) % 2)
    steps = []
    while len(sublists):
        rels = reader.make_dict(sublists.pop(0))
        summary = reader.make_dict(sublists.pop(0))
        steps.append((rels, summary))
    return basic, steps
