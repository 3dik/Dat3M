import reader

def do(lists, rlimit, alimit):
    basic = reader.make_dict(next(lists))
    for i, (rels, axis, summary) in enumerate(_extract_steps(lists), 1):
        total = sum(basic.values()) + sum(summary.values())
        qrels = reader.quota(summary['rels'], total)
        qaxis = reader.quota(summary['axis'], total)
        fmt = '%d: total: %d relations: %s axioms: %s'
        print(fmt % (i, total, qrels, qaxis))

        show_step(total, axis, rels, rlimit, alimit)
        print('', end='', flush=True)

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

def _extract_steps(lists):
    for ntuple in _yield_ntuples(lists, 3):
        yield tuple(map(reader.make_dict, ntuple))

def _yield_ntuples(iterator, n):
    assert n > 0
    for first in iterator:
        content = [first]
        for i in range(n - 1):
            content.append(next(iterator))
        yield tuple(content)
