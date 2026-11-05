package org.local.karbalascreen;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class ContentStore {
    private static List<Item> cachedAll, cachedQuran;
    private static String corpusVersion = "";
    static String version() { return corpusVersion; }
    private static String fingerprint(String text) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for(byte value:hash)hex.append("0123456789abcdef".charAt((value & 255) >>> 4)).append("0123456789abcdef".charAt(value & 15));
        return hex.toString();
    }
    static final class Item implements ContentCursor.Page {
        final String kind, text, reference, source;
        private final String recordId, pageId;
        Item(String kind, String text, String reference, String source, String recordId, int page) {
            this.kind = kind; this.text = text; this.reference = reference; this.source = source;
            this.recordId = recordId; this.pageId = recordId + "|page=" + page;
        }
        public String pageId() { return pageId; }
        public String recordId() { return recordId; }
    }
    static String summary(Context context) {
        try (InputStream input = context.getAssets().open("content-manifest.json")) {
            JSONObject counts = new JSONObject(StreamText.read(input)).getJSONObject("counts");
            return "المكتبة: " + counts.getInt("quran") + " آية كاملة، و" + counts.getInt("hadith")
                    + " حكمة متميزة، و" + counts.getInt("dua") + " مقتطفات دعائية.";
        } catch (Exception ignored) { return "مكتبة الآيات والحِكم والمقتطفات الدعائية."; }
    }
    static synchronized List<Item> load(Context context, boolean duas) {
        if (cachedAll == null) {
            try (InputStream input = context.getAssets().open("content.json")) {
                cachedAll = Collections.unmodifiableList(parse(input));
                LinkedHashMap<String, Item> quran = new LinkedHashMap<>();
                for (Item item : cachedAll) if (item.kind.equals("quran")) quran.put(item.pageId(), item);
                cachedQuran = Collections.unmodifiableList(new ArrayList<>(quran.values()));
            } catch (Exception ignored) {
                // Do not cache failures: a later entry can retry the asset.
                return Collections.emptyList();
            }
        }
        return duas ? cachedAll : cachedQuran;
    }
    private static List<Item> parse(InputStream input) throws Exception {
        List<Item> items = new ArrayList<>();
        List<List<Item>> quran = new ArrayList<>(), sayings = new ArrayList<>(), devotionals = new ArrayList<>();
        JSONObject root = new JSONObject(ContentAssetText.read(input));
        corpusVersion = root.getString("version");
        JSONArray array = root.getJSONArray("items");
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String kind = item.optString("kind"), text = item.optString("text"), reference = item.optString("reference"), source = item.optString("source");
                if (!ScreenPolicy.contentEnabled(kind, true)) continue;
                if (text.trim().isEmpty() || reference.trim().isEmpty() || !source.startsWith("https://")) continue;
                String recordId = kind + "|" + source + "|" + reference + "|" + fingerprint(text);
                List<String> pages = ContentPagination.split(text, ContentPagination.TV_CHARACTERS);
                List<Item> block = new ArrayList<>();
                for (int page = 0; page < pages.size(); page++) {
                    String label = pages.size() > 1
                            ? " · " + (kind.equals("quran") ? "مقطع من الآية" : kind.equals("hadith") ? "مقطع من الحكمة" : "مقطع من الدعاء")
                              + " · الجزء " + (page + 1) + "/" + pages.size() : "";
                    block.add(new Item(kind, pages.get(page), reference + label, source, recordId, page));
                }
                (kind.equals("quran") ? quran : kind.equals("hadith") ? sayings : devotionals).add(block);
            }
        // Balance source records, then flatten each record's pages together so a
        // long ayah is read in order before the next category begins.
        for (List<Item> block : ContentSequence.balance(Arrays.asList(quran, sayings, devotionals))) items.addAll(block);
        return items;
    }
}
