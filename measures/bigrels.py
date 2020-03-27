#!/usr/bin/env python3

import reader

def do(lines, limit):
    rels, summary = _load_data(lines)

    total = sum(summary.values())
    qrels = reader.quota(summary['rels'], total)
    qaxis = reader.quota(summary['axis'], total)
    print('total: %d relations: %s axioms: %s' % (total, qrels, qaxis))

    items = reader.sort_items(rels.items())
    for rel, number in items[:limit]:
        rel = rel.ljust(25)
        num = str(number).rjust(9)
        perc = reader.quota(number, total).rjust(10)
        print('%s %s %s' % (rel, num, perc))

def _load_data(lines):
    rels, summary = reader.sublists(lines)
    rels = reader.make_dict(rels)
    summary = reader.make_dict(summary)
    return rels, summary
