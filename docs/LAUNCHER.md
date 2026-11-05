# المشغّل الاختياري / Optional launcher

المشغّل مستقل واختياري؛ تطبيق شاشة التوقف يعمل بدونه. هذا المصدر مخصّص لأجهزة Android TV وGoogle TV التي تعمل بنظام Android 8.0 أو أحدث (API 26). لا ينطبق على Tizen أوwebOS أوRoku أوVIDAA. قد تمنع بعض الشركات اختيار مشغّل آخر؛ لا يتجاوز هذا المشروع قيود الجهاز ولا يضمن التوافق مع كل طراز.

## البناء من المصدر

المتطلبات: JDK 17، وAndroid SDK Platform 34، وBuild Tools 34.0.0. اضبط `ANDROID_HOME` على SDK المحلي لديك. يتضمن المجلد Gradle Wrapper 8.9 مع بصمة SHA-256 مثبتة؛ يستخدم Android Gradle Plugin 8.5.2. يتطلب تنزيل أدوات البناء والاعتماديات اتصالاً بالإنترنت، لكن تطبيق المشغّل لا يطلب صلاحية `INTERNET`.

من جذر المستودع:

```sh
cd launcher
./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

الناتج المحلي: `launcher/app/build/outputs/apk/release/app-release.apk`. لا نضع APK جاهزاً أو مفاتيح توقيع في المستودع. إعداد البناء الموروث يوقّع النسخة محلياً بمفتاح Android التجريبي الخاص ببيئة البناء؛ ابنِ تحديثاتك في البيئة نفسها أو احتفظ بمفتاح توقيعك بشكل خاص. المصدر يستخدم خط Outfit المرفق بترخيص OFL، ولا يحتاج إلى خطوط شخصية.

## التثبيت واختيار الشاشة الرئيسية

1. ابنِ التطبيق ثم ثبّت APK بنفسك بالطريقة الموثوقة المتاحة لجهازك، مع السماح لمصدر التثبيت المحدد عند الحاجة.
2. افتح إعدادات التلفاز الأصلية، ثم التطبيقات ← التطبيقات الافتراضية ← تطبيق الشاشة الرئيسية (قد تختلف الأسماء)، واختر CleanYourTV إذا كان الجهاز يتيح ذلك.
3. احتفظ بالمشغّل الأصلي مفعّلاً. للعودة، اختره من إعدادات الشاشة الرئيسية نفسها.

لا توجد هنا أدوات ADB أو إزالة تطبيقات أو تعطيل للمشغّل الأصلي. لا تفعّل خدمات إمكانية الوصول أو منح صلاحيات خاصة لتجربة هذا الإعداد العام. لا يفعّل البناء تلك الخدمات تلقائياً.

الحزمة الموروثة هي `com.gathin.tvhome`؛ قد يُعد تثبيتها تحديثاً لنسخة CleanYourTV الموجودة إذا وافق توقيعها. اختر بيئة اختبار مناسبة عند وجود نسخة سابقة.

## الإعداد العام

الحركة متوقفة، وتقليل الحركة مفعّل. مكتبة التطبيقات قابلة لاختيار التطبيقات التي تظهر على الرئيسية: افتح مكتبة التطبيقات، واضغط OK لإضافة التطبيق أو إزالته. لا توجد قائمة تطبيقات شخصية مسبقة. بقيت تطبيقات الشريط السفلي الافتراضية من المصدر الأصلي؛ التطبيقات غير المثبتة تُتجاهل وتُملأ الفراغات من التطبيقات المثبتة وفق سلوك المصدر الأصلي.

بعد تثبيت تطبيق شاشة التوقف `org.local.karbalascreen`، يظهر زر «شاشة التوقف» في الشريط العلوي ويفتح المعاينة عبر `org.local.karbalascreen.SHOW_PREVIEW`. يُخفى الزر إذا لم يكن التطبيق مثبتاً. يبقى `stock_home_redirect` فارغاً و`restore_key_services` متوقفاً. إعدادات المستخدم أو السمات التي يضيفها لاحقاً قد تتجاوز قيم البناء.

## Source and attribution

This optional, independent launcher is derived from [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), by Gathin, pinned to commit [`572ce2cbe98b53654cea3d262ee8558a5872dfd1`](https://github.com/Gathin23/cleanyourtv/tree/572ce2cbe98b53654cea3d262ee8558a5872dfd1/launcher). It is not an upstream release or a claim of universal TV compatibility.

The source snapshot contains 245 upstream tracked launcher files, excluding `build-and-install.sh` and `build-and-install.ps1`. The upstream parent MIT license is retained at [launcher/LICENSE](../launcher/LICENSE); Outfit and Fluent Emoji notices remain at [FONT-LICENSE-OFL.txt](../launcher/FONT-LICENSE-OFL.txt) and [ICONS-LICENSE-FLUENT-EMOJI.txt](../launcher/ICONS-LICENSE-FLUENT-EMOJI.txt).

Local changes are limited to generic configuration, optional text labels on top-bar app buttons (`MainActivity.java` and `top_app_button_label.xml`), and `FLAG_ACTIVITY_CLEAR_TOP` for the general Android Settings entry (`SystemSettings.java`). No new dependencies or permissions are added. Device backups, private settings, fonts, keys, build caches and prebuilt APKs are excluded.

English quick start: install JDK 17 and Android SDK 34 / Build Tools 34.0.0, set `ANDROID_HOME`, then run the command above from `launcher/`. Install your locally built APK manually on Android TV 8.0+ and select CleanYourTV through the TV’s native default-Home settings if supported. Keep the original launcher enabled and select it again there to restore it. No stock-launcher disabling, automated accessibility setup or debloat procedure is supplied. The screensaver app works without this launcher. Hardware compatibility and remote navigation still need checking on each TV.
