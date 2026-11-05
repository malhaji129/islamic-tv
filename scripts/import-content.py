#!/usr/bin/env python3
"""Reproduce the ancient Arabic corpus from locally downloaded primary texts.

Downloads are explicit: Quran JSON comes from the URL below; Ghurar HTML comes
from https://ablibrary.net/book_content/b/8813/{page}, pages 1..840.
No modern introduction, variant apparatus, or page-spanning saying is imported.
"""
import argparse
import collections
import hashlib
import html
import json
from pathlib import Path
import re
import unicodedata

ROOT = Path(__file__).resolve().parents[1]
QURAN_SOURCE = 'https://api.alquran.cloud/v1/quran/quran-uthmani'
GHURAR_SOURCE = 'https://ablibrary.net/book_content/b/8813/'
QURAN_NOTICE = """Tanzil Quran Text
Copyright (C) 2007-2021 Tanzil Project
License: Creative Commons Attribution 3.0

This copy of the Quran text is carefully produced, highly
verified and continuously monitored by a group of specialists
in Tanzil Project.

TERMS OF USE:

- Permission is granted to copy and distribute verbatim copies
  of this text, but CHANGING IT IS NOT ALLOWED.

- This Quran text can be used in any website or application,
  provided that its source (Tanzil Project) is clearly indicated,
  and a link is made to tanzil.net to enable users to keep
  track of changes.

- This copyright notice shall be included in all verbatim copies
  of the text, and shall be reproduced appropriately in all files
  derived from or containing substantial portion of this text.

Please check updates at: http://tanzil.net/updates/
Source: https://tanzil.net/
License terms: https://tanzil.net/docs/text_license
CC BY 3.0: https://creativecommons.org/licenses/by/3.0/
"""
PART = re.compile(r'<(h[1-6]|span)\b[^>]*(?:id="content-|class="select-text)[^>]*>(.*?)</\1>', re.S)
# Editorial variants, brackets and parenthetical glosses are omitted wholesale.
APPARATUS = re.compile(r'[0-9٠-٩\[\]()*]|نسخة|نسخ|كذا في|وفي الأصل|وفي الاصل')

def normalized(text):
    text = ''.join(c for c in unicodedata.normalize('NFD', text) if unicodedata.category(c) != 'Mn')
    return re.sub(r'[^\w]', '', text).replace('ٱ', 'ا')

def encode(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':')).encode('utf-8')

def digest(value):
    return hashlib.sha256(encode(value)).hexdigest()

def extract(quran_path, ghurar_dir, existing):
    raw = json.loads(quran_path.read_text())
    surahs = raw['data']['surahs']
    assert len(surahs) == 114
    quran = []
    for surah in surahs:
        for ayah in surah['ayahs']:
            # U+FEFF is a byte-order mark, not a Quran character.
            text = ayah['text'].removeprefix('\ufeff')
            number = ayah['numberInSurah']
            quran.append({'kind': 'quran', 'text': text,
                'reference': f"{surah['name']} · الآية {number}",
                'source': f"https://api.alquran.cloud/v1/ayah/{surah['number']}:{number}/quran-uthmani"})
    assert len(quran) == 6236
    sayings, seen = [], set()
    stats = collections.Counter()
    for page in range(1, 841):
        source = (ghurar_dir / f'{page}.html').read_text()
        values = [(tag, html.unescape(re.sub('<[^>]+>', '', text))) for tag, text in PART.findall(source)]
        for i, (tag, number) in enumerate(values):
            if tag == 'span' or not number.isdecimal():
                continue
            # Both numbered boundaries must occur on this page. This excludes
            # incomplete edge fragments without guessing or reconstructing text.
            if not (i+2 < len(values) and values[i+1][0] == 'span'
                    and values[i+2][0] != 'span' and values[i+2][1].isdecimal()):
                stats['boundary_or_non_single_span'] += 1
                continue
            text = values[i+1][1]
            if APPARATUS.search(text):
                stats['editorial_apparatus_omitted'] += 1
                continue
            key = normalized(text)
            if key in seen:
                stats['duplicate_sayings_omitted'] += 1
                continue
            seen.add(key)
            sayings.append({'kind': 'hadith', 'text': text,
                'reference': f'الإمام علي (عليه السلام) · غرر الحكم · ص {page}، فقرة {number}',
                'source': GHURAR_SOURCE + str(page)})
    # Preserve exact existing public-domain devotional excerpts, once each.
    duas = []
    for item in existing['items']:
        if item['kind'] == 'dua' and item['text'] not in {d['text'] for d in duas}:
            duas.append(item)
    return quran + sayings + duas, dict(stats)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--quran', type=Path, required=True)
    parser.add_argument('--ghurar-dir', type=Path, required=True)
    parser.add_argument('--check', action='store_true', help='compare source extraction without writing')
    args = parser.parse_args()
    path = ROOT / 'app/src/main/assets/content.json'
    existing = json.loads(path.read_text())
    items, stats = extract(args.quran, args.ghurar_dir, existing)
    counts = dict(collections.Counter(i['kind'] for i in items))
    if args.check:
        assert existing['items'] == items, 'Bundled corpus differs from source extraction'
        print('Exact source extraction verified:', counts)
        return
    path.write_bytes(encode({'version': digest(items), 'notices': {'quran': QURAN_NOTICE}, 'items': items}) + b'\n')
    manifest = {'format': 1, 'counts': counts,
        'item_sha256': {kind: digest([i for i in items if i['kind'] == kind]) for kind in counts},
        'asset_sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
        'quran_input_sha256': hashlib.sha256(args.quran.read_bytes()).hexdigest(),
        'ghurar_pages_sha256': hashlib.sha256(b''.join(hashlib.sha256((args.ghurar_dir / f'{p}.html').read_bytes()).digest() for p in range(1,841))).hexdigest(),
        'exclusions': stats}
    (ROOT / 'tests/content/manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    (ROOT / 'app/src/main/assets/content-manifest.json').write_bytes(encode(manifest) + b'\n')
    print(counts, stats, 'asset bytes', path.stat().st_size)

if __name__ == '__main__':
    main()
