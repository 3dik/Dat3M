#!/usr/bin/env python3

import reader

def do(lines, rlimit, alimit):
    basic, steps = _load_data(lines)
    for i, (rels, axis, summary) in enumerate(steps, 1):
        total = sum(basic.values()) + sum(summary.values())
        qrels = reader.quota(summary['rels'], total)
        qaxis = reader.quota(summary['axis'], total)
        fmt = '%d: total: %d relations: %s axioms: %s'
        print(fmt % (i, total, qrels, qaxis))

        show_step(total, axis, rels, rlimit, alimit)

def show_step(total, axis, rels, rlimit, alimit):
    for rel, number in reader.sort_items(rels.items())[:rlimit]:
        rel = rel.ljust(25)
        num = str(number).rjust(9)
        perc = reader.quota(number, total).rjust(10)
        print('%s %s %s' % (rel, num, perc))

    for axi, number in reader.sort_items(axis.items())[:alimit]:
        num = str(number).rjust(9)
        perc = reader.quota(number, total).rjust(10)
        name = axi.rjust(35)
        print('%s %s %s' % (num, perc, name))

def _load_data(lines):
    sublists = list(reader.sublists(lines))
    basic = reader.make_dict(sublists.pop(0));

    assert not (len(sublists) % 3)
    steps = []
    while len(sublists):
        rels = reader.make_dict(sublists.pop(0))
        axis = reader.make_dict(sublists.pop(0))
        summary = reader.make_dict(sublists.pop(0))
        steps.append((rels, axis, summary))
    return basic, steps
