#!/usr/bin/env python3
"""Offline checks for corpus coverage, integrity, citations and unique sayings."""
import collections
import hashlib
import importlib.util
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('content_import', ROOT / 'scripts/import-content.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
asset = ROOT / 'app/src/main/assets/content.json'
manifest = json.loads((ROOT / 'tests/content/manifest.json').read_text())
assert json.loads((ROOT / 'app/src/main/assets/content-manifest.json').read_text()) == manifest
corpus = json.loads(asset.read_text())
assert corpus['notices']['quran'] == module.QURAN_NOTICE
items = corpus['items']
assert corpus['version'] == module.digest(items), 'Corpus version must follow its source records'
assert asset.stat().st_size < 10_000_000, 'Corpus exceeds the asset budget'
assert hashlib.sha256(asset.read_bytes()).hexdigest() == manifest['asset_sha256'], 'Content changed without source verification'
counts = dict(collections.Counter(i['kind'] for i in items))
assert counts == manifest['counts'] == {'quran': 6236, 'hadith': 10096, 'dua': 10}
for kind in counts:
    group = [i for i in items if i['kind'] == kind]
    assert module.digest(group) == manifest['item_sha256'][kind]
    for item in group:
        assert set(item) == {'kind', 'text', 'reference', 'source'}
        assert item['text'].strip() and re.search('[\u0621-\u064a]', item['text'])
        assert item['reference'].strip() and item['source'].startswith('https://')
        assert '\ufffd' not in item['text'] and '\ufeff' not in item['text']
        assert max(map(len, item['text'].split())) <= 180, 'An indivisible source word exceeds the TV budget'
quran = [i for i in items if i['kind'] == 'quran']
# Standard Hafs ayah counts: sequential coverage tests missing/extra verse IDs.
ayah_counts = [7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6]
expected = [f'https://api.alquran.cloud/v1/ayah/{surah}:{ayah}/quran-uthmani' for surah, count in enumerate(ayah_counts,1) for ayah in range(1,count+1)]
assert [i['source'] for i in quran] == expected, 'Quran verse coverage/order differs'
sayings = [i for i in items if i['kind'] == 'hadith']
assert len({module.normalized(i['text']) for i in sayings}) == len(sayings), 'Duplicate sayings inflate the count'
for item in sayings:
    m = re.fullmatch(r'https://ablibrary.net/book_content/b/8813/(\d+)',item['source'])
    assert m and f"ص {m.group(1)}، فقرة " in item['reference']
    assert not module.APPARATUS.search(item['text']), 'Editorial text in sayings'
print(f'Content verified: {counts}; {len(items)} source entries; {asset.stat().st_size:,} bytes; 114 surahs; no duplicate sayings')
