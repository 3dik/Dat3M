import re

class Reader():
    def __init__(self, stream):
        self.stream = stream
        self.output = ''

    # The iterator on the right side of each yielded tuple must be exhausted
    # before the next element of *this* method is yielded.
    def extract_lists(self):
        for line in self.stream:
            match = re.match(r'start list (\w+)$', line)
            if match is None:
                self.output += line
                continue

            name = match.group(1)
            yield name, self._extract_sublists(name)

    def get_output(self):
        return self.output

    def _extract_sublists(self, name):
        entries = []
        for line in self.stream:
            deli_match = re.match(r'list delimeter$', line)
            end_match = re.match(r'end list %s$' % re.escape(name), line)

            if not any([deli_match, end_match]):
                entries.append(line.strip())
                continue

            yield entries
            if end_match:
                break
            entries = []

def sort_items(items):
    return sorted(items, reverse=True, key=lambda x: x[1])

def make_dict(lines):
    return dict(map(_parse_dataline, lines))

def quota(number, total):
    return '%.2f%%' % (number * 100 / total)

def _parse_dataline(line):
    match = re.search(r'(.*): ([0-9]+)', line)
    return (match.group(1), int(match.group(2)))
