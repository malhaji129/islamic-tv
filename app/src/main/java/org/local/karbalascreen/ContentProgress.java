package org.local.karbalascreen;
import android.content.SharedPreferences;
import java.util.List;
/** Small atomic, synchronous preference snapshot survives process/power restarts. */
final class ContentProgress {
    private ContentProgress() {}
    static ContentCursor cursor(SharedPreferences prefs,boolean live) {
        return new ContentCursor(new ContentCursor.Store() {
            public ContentCursor.State read() {
                String page=prefs.getString("reading_page","");
                if(page.isEmpty())return null;
                return new ContentCursor.State(prefs.getString("reading_version",""),page,
                    prefs.getString("reading_record",""),prefs.getInt("reading_index",0));
            }
            public boolean write(ContentCursor.State state) {
                return prefs.edit().putString("reading_version",state.version)
                    .putString("reading_page",state.page).putString("reading_record",state.record)
                    .putInt("reading_index",state.index).commit();
            }
        },live);
    }
    static int restore(SharedPreferences prefs,List<ContentStore.Item> items,boolean live) {
        return cursor(prefs,live).restore(items,ContentStore.version());
    }
    static boolean save(SharedPreferences prefs,List<ContentStore.Item> items,int index,boolean live) {
        return cursor(prefs,live).save(items,index,ContentStore.version());
    }
}
