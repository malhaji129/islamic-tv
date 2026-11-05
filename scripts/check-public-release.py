#!/usr/bin/env python3
"""Verify public APK resource exclusions and launcher size without reading private files."""
from pathlib import Path
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
apks = list((root / 'app/build/outputs/apk/release').glob('*.apk'))
if not apks:
    apks = list((root / 'app/build/outputs/apk/debug').glob('*.apk'))
assert apks, 'Build a public screensaver APK first'
for apk in apks:
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        assert not any('thmanyah' in name.lower() or 'adhan_karbala' in name.lower() for name in names), str(apk)
        assert 'assets/content.json' in names
        assert not any(name.endswith(('.keystore', '.jks', '.key', '.pem')) for name in names)
    print('Public APK verified:', apk.relative_to(root), apk.stat().st_size, 'bytes')
launcher = root / 'launcher/app/build/outputs/apk/release/app-release.apk'
if launcher.exists():
    assert launcher.stat().st_size < 589824, 'Launcher exceeds upstream size budget'
    print('Launcher size verified:', launcher.stat().st_size, 'bytes')
