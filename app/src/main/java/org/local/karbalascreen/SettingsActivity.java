package org.local.karbalascreen;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private Options options;
    private LinearLayout list;
    private TextView locationSummary;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); options = new Options(this); AdhanScheduler.refresh(this);
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(Color.rgb(8, 15, 18)); scroll.setFillViewport(true);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); list.setPadding(dp(56), dp(28), dp(56), dp(32));
        scroll.addView(list); setContentView(scroll);
        text("سكينة كربلاء", 30, 0xFFDDE2DB);
        LocationProfile profile=LocationStore.read(options.prefs);
        locationSummary=text(profile==null?"اختر المدينة أولاً لتفعيل المواقيت والطقس":profile.city+" · "+profile.timezone+" · "+profile.source(), 17, 0xFF91B7AA);
        Button cityButton=button("المدينة والتوقيت وطريقة الصلاة وتصحيح الدقائق",v->startActivity(new Intent(this,LocationActivity.class)));
        Button preview = button("معاينة الشاشة", v -> startActivity(new Intent(this, PreviewActivity.class)));
        button("اختيار شاشة السكون في إعدادات التلفاز", v -> {
            try { startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS)); }
            catch (android.content.ActivityNotFoundException ignored) { android.widget.Toast.makeText(this, "افتح إعدادات الجهاز ثم شاشة السكون", android.widget.Toast.LENGTH_LONG).show(); }
        });
        Button interval = button("", null); updateInterval(interval);
        interval.setOnClickListener(v -> {
            int[] intervals = {30, 45, 60, 90}; int at = 0;
            for (int i = 0; i < intervals.length; i++) if (intervals[i] == options.interval()) at = i;
            options.prefs.edit().putInt("interval", intervals[(at + 1) % intervals.length]).apply(); updateInterval(interval);
        });
        Button offset = button("", null); updateOffset(offset);
        offset.setOnClickListener(v -> { int next = options.offset() == 2 ? -2 : options.offset() + 1; options.prefs.edit().putInt("offset", next).apply(); updateOffset(offset); });
        toggle("weather", "طقس المدينة المختارة"); toggle("prayers", "مواقيت الصلاة"); toggle("duas", "الأدعية والحِكم"); toggle("photos", "صور الأماكن المقدسة");
        toggle("effects", "المؤثرات الجوية");
        button("معاينة الأجواء والأوقات", v -> startActivity(new Intent(this, AtmospherePreviewActivity.class)));
        Button adhan = button("", null); updateAdhan(adhan);
        adhan.setOnClickListener(v -> {
            if(!AdhanScheduler.available(this)){android.widget.Toast.makeText(this,"الصوت غير متاح في هذه النسخة؛ تتطلب المعاينة والتنبيهات ملفاً محلياً مرخصاً",android.widget.Toast.LENGTH_LONG).show();return;}
            options.prefs.edit().putBoolean("adhan", !options.enabled("adhan")).apply();
            AdhanScheduler.refresh(this); updateAdhan(adhan);
        });
        button("معاينة صوت الأذان", v -> {
            if (AdhanScheduler.available(this)) startForegroundService(new Intent(this, AdhanPlaybackService.class).setAction(AdhanPlaybackService.PREVIEW));
            else android.widget.Toast.makeText(this, "ملف الأذان المحلي غير متاح", android.widget.Toast.LENGTH_LONG).show();
        });
        button("إيقاف الأذان الآن", v -> stopService(new Intent(this, AdhanPlaybackService.class)));
        text("الأذان: مصطفى الصراف · يؤذّن في العتبتين الحسينية والعباسية. تسجيل محلي من أرشيف صوت الشيعة\nhttps://shiavoice.com/play-ym6tb\nيعمل للفجر والظهر والعشاء عند المواقيت المشتركة المنشورة في المصدر. لا يغيّر مستوى صوت التلفاز ولا يوقظ الشاشة النائمة. تظل تلاوة الآيات وعرض النصوص صامتة. قد يتأخر التنبيه إذا قيّد النظام المنبّهات؛ لا يُعاد الأذان بعد تأخر أكثر من دقيقة.", 15, 0xFFDDE2DB);
        text("الريموت: يسار / يمين لتصفح النص في المعاينة، زر القائمة للإعدادات، رجوع للخروج. في شاشة السكون، أي زر ينهي العرض.", 16, 0xFF91B7AA);
        text("المصادر والخصوصية", 23, 0xFFB8A77E);
        text("مواقيت كربلاء الرسمية عند الاختيار الصريح: العتبة العباسية المقدسة · كربلاء؛ المدن الأخرى مواقيت حسابية عبر AlAdhan (جعفري افتراضياً) مع اختيار الطريقة وتصحيح الدقائق. تاريخها الهجري حسابي قابل للتصحيح. https://aladhan.com/prayer-times-api\nhttps://www.main.home.alkafeel.net/?lang=ar\nيعرض الظهر وقت الظهرين المنشور، والعشاء وقت المغرب والعشاء المنشور في المصدر. يظهر الإمساك في رمضان فقط إذا توفر وقت موثّق من المصدر؛ لا يُستنتج من الفجر. لا تظهر مواقيت غير مؤرخة بتاريخ اليوم. عند غياب التاريخ المنشور، يعرض التطبيق تاريخاً هجرياً قمرياً حسابياً قابلاً للتصحيح؛ قد يختلف عن الرؤية الشرعية. عند اختيار تصحيح، يعرض التاريخ الحسابي مع مقدار التصحيح.", 15, 0xFFDDE2DB);
        text("القرآن الكريم: يظهر اسم السورة ورقم الآية أسفل كل نص. النص العثماني الكامل: Tanzil Project (CC BY 3.0) عبر AlQuran Cloud\nhttps://tanzil.net\nhttps://alquran.cloud\nالأدعية: مقتطفات من دعاء كميل المنشور في حقيبة المؤمن\nhttps://hmomen.com/content/dua/8\nهذا تطبيق شخصي مستقل، ولا يمثل الجهات المذكورة أو تطبيقاتها الرسمية.", 15, 0xFFDDE2DB);
        text(ContentStore.summary(this) + " الحِكم من غرر الحكم ودرر الكلم للآمدي. يظهر رقم الصفحة والفقرة المحلية أسفل كل حكمة؛ يختلف الترقيم بين الأقسام والطبعات. النصوص من طبعة دار الكتاب الإسلامي، تحقيق مهدي الرجائي، كما نشرتها مكتبة أهل البيت. نسبة النص للمصدر لا تعني تصحيح سند كل رواية.\nhttps://ablibrary.net/book_content/b/8813/1\nhttps://api.alquran.cloud/v1/quran/quran-uthmani", 15, 0xFFDDE2DB);
        text("يحفظ التطبيق آخر مقطع معروض تلقائياً، ويستأنف منه بعد إغلاق شاشة السكون أو إعادة تشغيل التلفاز. المعاينة التوضيحية مستقلة عن موضع القراءة المحفوظ.", 15, 0xFF91B7AA);
        java.util.List<PhotoStore.Photo> photos = PhotoStore.load(this);
        if (!photos.isEmpty()) {
            text("مصادر صور الخلفية · صورة ثابتة تتغير كل عشر دقائق على الأقل", 20, 0xFFB8A77E);
            for (PhotoStore.Photo photo : photos) text(photo.caption + "\n" + photo.credit + "\n" + photo.source + "\n" + photo.license + "\n" + photo.changes, 14, 0xFFDDE2DB);
        }
        text("الطقس: Open-Meteo · CC BY 4.0\nhttps://open-meteo.com\nالموقع هو المدينة المؤكدة من الإعدادات، ولا يتم تغييره تلقائياً. البحث اليدوي عبر Open-Meteo / GeoNames. الاقتراح عبر ipwho.is عند طلبك فقط وبعد تأكيد المدينة؛ لا يحفظ التطبيق عنوان الإنترنت. يُجلب الطقس أثناء ظهور الشاشة فقط، كل 30 دقيقة كحد أقصى. يظهر عمر الرصد، وتوسم البيانات الأقدم من 30 دقيقة بأنها قديمة. لا يطلب التطبيق موقع الجهاز أو الميكروفون، الصوت مخصص للأذان فقط عند تفعيله أو معاينته.", 15, 0xFFDDE2DB);
        text("المؤثرات الجوية زخارف مرئية اختيارية وليست تحذيرات. الرياح من سرعة الرياح المسجلة، والغبار من نمذجة CAMS عبر Open-Meteo فقط، بعتبة مرئية 50 ميكروغرام/م³؛ ليست عتبة صحية ولا يُستنتج الغبار من الرياح. تتوقف مؤثرات الطقس بعد 30 دقيقة من عمر بياناته، والغبار وفق عمر بياناته المستقل.\nhttps://open-meteo.com/en/docs\nhttps://open-meteo.com/en/docs/air-quality-api\nمصدر الغبار: CAMS ENSEMBLE / CAMS global forecasts · Copernicus Atmosphere Monitoring Service. طلب إضافي واحد للغبار كل 30 دقيقة كحد أقصى أثناء الظهور.\nالشروق والغروب من بيانات اليوم أو فترات محلية تقريبية عند غيابها؛ المراحل المرئية ليست مواقيت شرعية. شاشة المعاينة توضيحية ولا تغيّر البيانات أو جداول الأذان. لا صوت للمؤثرات الجوية.", 14, 0xFF91B7AA);
        text("الساعة والتاريخ والطقس والتنبيهات بحسب منطقة IANA للمدينة المختارة، بما فيها التوقيت الصيفي. تتغير مواضع العرض قليلاً كل دقيقة لتقليل ثبات البكسلات. تعمل زخارف المطر والثلج والرياح بحد أقصى 8 إطارات/ثانية عند تفعيلها فقط؛ وتتوقف عندما تختفي الشاشة. المعاينة لا تمنع السكون أو تطيل مهلة التلفاز.", 14, 0xFF91B7AA);
        (profile==null?cityButton:preview).requestFocus();
    }
    @Override protected void onResume(){super.onResume();if(locationSummary!=null){LocationProfile p=LocationStore.read(options.prefs);locationSummary.setText(p==null?"اختر المدينة أولاً لتفعيل المواقيت والطقس":p.city+" · "+p.timezone+" · "+p.source());}}
    private void updateAdhan(Button button) {
        button.setText(!AdhanScheduler.available(this)?"أذان الصلوات الثلاث: غير متاح · لا يوجد ملف صوت مرخّص محلي":"أذان الصلوات الثلاث: "+(options.enabled("adhan")?"مفعّل":"متوقف"));
    }
    private void updateInterval(Button button) { button.setText("تغيير النص: كل " + options.interval() + " ثانية  ·  اضغط للتغيير"); }
    private void updateOffset(Button button) { int value = options.offset(); button.setText("تصحيح التاريخ الهجري الحسابي: " + (value > 0 ? "+" : "") + value + " يوم  ·  اضغط للتغيير"); }
    private void toggle(String key, String title) {
        Button button = button("", null); button.setText(title + ": " + (options.enabled(key) ? "مفعّل" : "متوقف"));
        button.setOnClickListener(v -> { options.prefs.edit().putBoolean(key, !options.enabled(key)).apply(); button.setText(title + ": " + (options.enabled(key) ? "مفعّل" : "متوقف")); });
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String text, int size, int color) {
        TextView view = new TextView(this); view.setAutoLinkMask(android.text.util.Linkify.WEB_URLS); view.setText(text); view.setTextSize(size); view.setTextColor(color); view.setTypeface(size >= 23 ? InfoFonts.display(this) : InfoFonts.sans(this)); view.setLineSpacing(dp(3), 1.05f); view.setPadding(0, dp(8), 0, dp(10)); view.setGravity(Gravity.START); list.addView(view, new LinearLayout.LayoutParams(-1, -2)); return view;
    }
    private Button button(String text, View.OnClickListener click) {
        Button button = new Button(this); button.setText(text); button.setAllCaps(false); button.setTextSize(18); button.setTextColor(0xFFE1E7E0); button.setTypeface(InfoFonts.sans(this)); button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START); button.setPadding(dp(20), dp(10), dp(20), dp(10)); button.setMinHeight(dp(54));
        StateListDrawable states = new StateListDrawable(); states.addState(new int[]{android.R.attr.state_focused}, shape(0xFF29443F, 0xFFB8A77E)); states.addState(new int[]{android.R.attr.state_pressed}, shape(0xFF29443F, 0xFFB8A77E)); states.addState(new int[]{}, shape(0xFF142329, 0xFF29403F)); button.setBackground(states);
        button.setOnClickListener(click); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(0, dp(5), 0, dp(5)); list.addView(button, params); return button;
    }
    private GradientDrawable shape(int color, int stroke) { GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(dp(7)); shape.setStroke(dp(2), stroke); return shape; }
}
