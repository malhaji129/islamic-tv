package org.local.karbalascreen;

import android.content.Context;
import android.graphics.Typeface;

/** Optional local fonts; clean public builds use the bundled open-license Amiri font. */
final class InfoFonts {
    private static Typeface display, sans;
    static synchronized Typeface display(Context context) {
        if (display == null) display = load(context, "thmanyah_display");
        return display;
    }
    static synchronized Typeface sans(Context context) {
        if (sans == null) sans = load(context, "thmanyah_sans");
        return sans;
    }
    private static Typeface load(Context context, String name) {
        int id = context.getResources().getIdentifier(name, "font", context.getPackageName());
        return context.getResources().getFont(id == 0 ? R.font.amiri_regular : id);
    }
}
