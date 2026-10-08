import re
from collections import Counter

text = open('../owl2gen-section.tex', encoding='utf-8').read()

depth = 0
for i, ch in enumerate(text):
    if ch == '{':
        depth += 1
    elif ch == '}':
        depth -= 1
    if depth < 0:
        print('UNBALANCED at char', i, text[max(0, i - 50):i + 10])
        break
print('final brace depth:', depth)

begins = re.findall(r'\\begin\{([a-zA-Z*]+)\}', text)
ends = re.findall(r'\\end\{([a-zA-Z*]+)\}', text)
bc, ec = Counter(begins), Counter(ends)
print('begin/end mismatch:', {k: (bc[k], ec.get(k, 0)) for k in bc if bc[k] != ec.get(k, 0)})

labels = set(re.findall(r'\\label\{([^}]+)\}', text))
refs = set(re.findall(r'\\(?:ref|Cref|cref)\{([^}]+)\}', text))
dangling = refs - labels
print('dangling refs:', dangling)

dup_labels = [k for k, v in Counter(re.findall(r'\\label\{([^}]+)\}', text)).items() if v > 1]
print('duplicate labels:', dup_labels)
