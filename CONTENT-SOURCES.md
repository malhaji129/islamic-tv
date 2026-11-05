# Content provenance

The offline asset contains **16,342 source entries**: **6,236 Quran ayahs**, **10,096 distinct sayings attributed to Imam Ali**, and **10 devotional excerpts**. Counts describe complete source records, not display pages. The JSON is approximately 4.92 MB. No Arabic scripture or saying was generated, paraphrased, or reconstructed.

## Quran

All 114 surahs and all 6,236 ayahs of the `quran-uthmani` edition were imported from [AlQuran Cloud's full Quran endpoint](https://api.alquran.cloud/v1/quran/quran-uthmani). Each entry retains its surah name, ayah number, and direct HTTPS ayah endpoint. Diacritics, orthography, Quranic signs, and the source's opening basmalas are preserved exactly. Only a leading U+FEFF byte-order mark is removed; no Quran letter or mark is changed.

The digital Arabic text is credited to the [Tanzil Project](https://tanzil.net/), served by AlQuran Cloud / Islamic Network. Tanzil Quran Text, Copyright (C) 2007–2021 Tanzil Project, Creative Commons Attribution 3.0; verbatim redistribution with attribution and no text changes. The full required copyright notice is retained in the asset's top-level `notices.quran` field and in the import script. See [Tanzil's text license](https://tanzil.net/docs/text_license), [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/), [updates](https://tanzil.net/updates/), and [AlQuran Cloud's terms](https://alquran.cloud/terms-and-conditions). No translations or recitations are included in this corpus.

## Imam Ali sayings

The ancient Arabic aphorisms come from **غرر الحكم ودرر الكلم**, compiled by **عبد الواحد الآمدي التميمي**, as transcribed by [Ahlulbayt Library, book 8813](https://ablibrary.net/book_content/b/8813/1). The site's bibliographic record identifies editor **السيد مهدي الرجائي** and publisher **دار الكتاب الإسلامي، قم**, 840 digital pages. The [edition notice on digital page 4](https://ablibrary.net/book_content/b/8813/4) identifies the second edition, 1410 AH / 1990 CE.

Every retained quotation is copied exactly from a single, complete numbered paragraph in that transcription. Each entry links to the precise digital page and gives **page plus local paragraph number**. Paragraph numbering restarts by section; these numbers must not be presented as global hadith numbers or as numbering shared by every edition. Example: [digital page 68](https://ablibrary.net/book_content/b/8813/68), paragraph 1311.

The import inspected all 840 pages. It conservatively omitted 788 numbered blocks at page boundaries or without a single unambiguous text span, 13 entries containing editorial brackets/variants/parenthetical apparatus, and 55 duplicate aphorisms. The remaining **10,096 unique sayings** preserve the published transcription, including its spelling. Normalization is used only to detect duplicates; it never changes displayed text. The earlier 19 repeatedly reused quotations have been replaced by this much larger corpus.

Only the underlying ancient aphorisms are reproduced as public-domain historical text. The 1990 edition's modern introduction, editorial commentary, variant readings, page design, library interface, and other modern material are **not** included or relicensed. The site and edition retain their respective rights. This is verification against a named published transcription, **not** an assertion that every narration has an authenticated chain, that all textual variants have been critically collated, or that any shrine endorses this app.

## Display and rotation

The asset always stores complete source entries. At runtime, long entries are divided at existing whitespace into conservative pages of up to 180 UTF-16 characters, without changing or dropping source characters. The reference explicitly identifies a **مقطع من الآية / الحكمة / الدعاء** and **الجزء x/y**. All pages of one entry remain consecutive. Joining the pages reproduces the original entry exactly. The reference and fragment label are separate UI fields, never inserted into Quran text.

Rotation gives Quran, sayings, and (when enabled) devotional excerpts one source-record turn each. Shorter categories cycle while the longest category completes, so a large Quran or sayings library does not prevent another category from appearing. Runtime scheduling repetitions are not counted as additional unique source entries.

## Reading progress

The LIVE display commits the currently shown fragment synchronously to private preferences whenever it appears. A new screensaver instance or TV process restores that same fragment, then the next scheduled turn advances normally. State includes a stable source/text fingerprint, fragment identity, sequence position, and corpus version. After category changes or corpus updates it first finds the same page, then the same source record if repagination removed the fragment; a removed record starts at the beginning. The atmosphere demonstration neither reads nor writes LIVE progress. This local state survives ordinary process/power restarts; uninstalling the app or clearing app data removes it.

The corpus is parsed once per process and cached for subsequent screensaver entries and category toggles. Settings uses the generated small `content-manifest.json` for counts and concise fixed source links; it never parses the whole corpus or lists hundreds of book-page URLs.

## Verification and reproduction

Run `sh scripts/test-content.sh` with JDK 17 available through `JAVA_HOME`. It tests source-preserving word-boundary pagination and balanced category rotation, then checks the asset's hashes, all 114 surah/ayah ranges, citations, Arabic text, complete entry counts, absence of editorial markers, and uniqueness of the sayings. `tests/content/manifest.json` records hashes and exclusion counts. The corpus checks are offline. The full production-loader regression test uses the real org.json 20240303 library (SHA-256 pinned by the script); set `JSON_JAR` to an existing copy for a fully offline run, otherwise the script downloads it from Maven Central. Android asset/preferences fixtures replace only the Android host APIs; the production loader, parser, pagination, cursor adapter, and real 4.9 MB asset are exercised. It also verifies that the HTTP response cap remains 512 KB while the bundled corpus has its own 10 MB cap.

For independent source comparison, obtain the full JSON from the Quran endpoint above and pages 1 through 840 from `https://ablibrary.net/book_content/b/8813/{page}` as UTF-8 HTML files named `1.html` through `840.html`. Then run:

```sh
python3 scripts/import-content.py --quran /path/to/quran-uthmani.json --ghurar-dir /path/to/pages --check
```

The source comparison was run successfully for this bundled corpus. Removing `--check` deliberately rebuilds the asset and manifest from those inputs. The script selects only complete source paragraphs, never joins damaged fragments or invents missing words. Raw downloaded modern book pages are not bundled in the repository.

## Other sources

Devotional excerpts: public-domain ancient supplication of Kumayl as published at [Haqibat Elmomen](https://hmomen.com/content/dua/8). Excerpts only; this independent app is not the official Haqibat Elmomen app. No app logos, recordings, or account integration.

Karbala prayer timings and observed Hijri date: [Alkafeel prayer source](https://www.main.home.alkafeel.net/?lang=ar), validated against the Gregorian publication date in Asia/Baghdad. Other selected cities use the separately attributed calculated-prayer provider; see the project README and in-app source labels.

Weather: [Open-Meteo](https://open-meteo.com/en/docs), using the selected city rather than GPS.

Photos: real Karbala shrine photographs from Wikimedia Commons; original credit/license/source are recorded in `app/src/main/assets/wallpapers.json`. Runtime center-crop and dark overlay only; no generated shrine images.
