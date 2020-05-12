import reader

def do(lists):
    lines = next(lists)
    assert not len(list(lists))

    total, tasks = _load_data(lines)
    print(_show('RUNTIME', total))
    for task, time in tasks.items():
        perc = reader.quota(time, total)
        print(_show(task, time) + ' ' + perc)

def _load_data(lines):
    data = reader.make_dict(lines)
    total = data['outer']
    del data['outer']
    return total, data

def _show(task, time):
    task = (task + ':').ljust(13)
    time = '%.3fs' % (time / 1000)
    return task + time
