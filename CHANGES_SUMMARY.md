# ملخص التعديلات الأمنية - نظام Pro

## 1. core/src/main/java/com/frameout/core/pro/Entitlements.kt
- التخزين المحلي لحالة Pro صار عبر EncryptedSharedPreferences (مفتاح AES256-GCM من Android Keystore)
  بدل SharedPreferences العادي. أي تعديل مباشر على ملف XML بالروت يصبح غير مجدٍ لأن القيم
  مشفّرة ولا يوجد مفتاح فك تشفير خارج الـ Keystore نفسه.
- ProAccess.permits() و isProActive() صارت fail-closed (ترجع false بدل true) لو Entitlements
  لسا ما انربط.

## 2. app/src/main/java/com/frameout/app/billing/ProCheck.kt
- locked() و pass() صارت fail-closed لنفس السبب.

## 3. app/src/main/java/com/frameout/app/FrameOutApp.kt
- استبدال فحص "هل ملف frameout_prefs.xml موجود؟" (قابل للتزوير بإنشاء ملف فاضي بالروت)
  بفحص PackageManager.firstInstallTime (بيانات نظام حقيقية يصعب تزويرها بمجرد لمس ملف).

## 4. app/build.gradle.kts
- إضافة PAYWALL_CUTOFF_MS كـ BuildConfig field، قابل للتحديد عبر -PpaywallCutoffMs=...
  (لازم تحدده أنت = تاريخ إطلاق الـ paywall فعليًا بالمللي ثانية).

## 5. core/src/main/java/com/frameout/core/util/RuntimeGate.kt
- probe() لما يفشل، الآن يرمي RuntimeIntegrityException فعليًا بدل ما يسجل باللوق بس.
  هذا يوقف عملية الاستخراج فعليًا تحت جهاز تم اختراقه/حقن كود فيه (Frida/Xposed) أو تم العبث بتوقيعه.

## 6. app/src/main/java/com/frameout/app/security/Sentinel.kt
- إصلاح ثغرة: probe كان يسمح لحالة "Hooked" (Xposed موجود) ويمنع بس "Tampered".
  الآن يمنع أي حالة غير "Clean".

## 7. codecpack/.../MediaFailure.kt + MediaFailureClassifier.kt + RuntimeIntegrityException.kt
- إضافة نوع فشل جديد "Integrity" مصنّف fatal (غير قابل لإعادة المحاولة) ومرتبط برسالة
  عربي/إنجليزي واضحة للمستخدم بدل ما يطلع كرسالة فشل عامة غامضة.

## 8. gradle/libs.versions.toml + core/build.gradle.kts
- إضافة مكتبة androidx.security:security-crypto:1.1.0 لدعم EncryptedSharedPreferences.

---

## ملاحظات مهمة قبل البناء (build)
1. لازم تحدد -PpaywallCutoffMs بتاريخ حقيقي (مثلاً تاريخ أول نشر فيه الـ paywall)، وإلا
   القيمة الافتراضية = وقت البناء نفسه، يعني ولا تثبيت قديم يتغرندفاذر تلقائيًا (آمن لكن
   قد يضر مستخدمين قدامى حقيقيين لو ما حددتها).
2. المستخدمون الحاليون اللي عندهم بيانات Pro بالملف القديم (frameout_pro) ما راح تنتقل تلقائيًا
   للملف الجديد المشفّر frameout_pro_enc. لكن BillingManager.refresh() يعيد التحقق من
   Google Play عند كل إقلاع تطبيق ويملأ القيم من جديد تلقائيًا، فالمشتركين الحقيقيين يرجعون
   Pro تلقائيًا بأول فتح للتطبيق (بشرط الاتصال بالإنترنت ومزامنة Google Play Billing).
3. لازم تسوي Gradle sync / build كامل للتأكد من عدم وجود كسر تجميعي (ما قدرت أشغل gradle هنا
   لعدم توفر اتصال شبكة بالحاوية).

---

## 9. دمج RevenueCat (طبقة تحقق سيرفري حقيقية)

### الملفات الجديدة/المعدّلة
- `app/src/main/java/com/frameout/app/billing/RevenueCatSync.kt` (جديد): يشغّل RevenueCat بوضع
  "purchasesAreCompletedBy = MY_APP" (الاسم الحالي لما كان يسمى Observer Mode) — يعني
  BillingManager تبعك يضل هو اللي يفتح شاشة الشراء ويديرها زي ما هو، لكن كل عملية شراء يشوفها
  تنعاد التحقق منها سيرفريًا عند RevenueCat قبل لا تنعتمد.
- `BillingManager.handle()`: بطّلنا الاعتماد المباشر على نتيجة BillingClient المحلية. إذا
  RevenueCat مفعّل (مفتاح API موجود)، نستخدم RevenueCatSync.sync() فقط. إذا ما هو مفعّل
  (تطوير بدون مفتاح)، يرجع للسلوك القديم المحلي كـ fallback (بدون حماية حقيقية).
- `FrameOutApp.kt`: استدعاء RevenueCatSync.configure() + refreshVerified() عند إقلاع التطبيق.
- `Entitlements.kt`: إضافة `ProAccess.applyVerifiedEntitlement(active, plan)` كمسار الدخول
  الوحيد المفروض تستخدمه أي طبقة تحقق خارجية (RevenueCat الآن، أي شيء ثاني بالمستقبل).
- `gradle/libs.versions.toml` + `app/build.gradle.kts`: إضافة مكتبة
  `com.revenuecat.purchases:purchases` (نسخة 8.15.0 مبدئيًا)، ومفتاحين BuildConfig:
  REVENUECAT_API_KEY و REVENUECAT_ENTITLEMENT_ID.

### لازم تسويه أنت قبل البناء
1. **تأكد من رقم نسخة SDK الصحيح** من صفحة RevenueCat "Install the SDK" نفسها (السطر كان
   مقطوع بالصورة اللي أرسلتها) — غيّر `revenueCat = "8.15.0"` بـ `libs.versions.toml` لو مختلف.
2. **أنشئ Entitlement اسمه "pro" بالظبط** بلوحة تحكم RevenueCat (تبويب Entitlements)، واربطه
   بالمنتجين `frameout_pro` (الاشتراك) و `frameout_pro_lifetime` (الدفعة الواحدة). لو سميته
   شي ثاني غير "pro"، غيّر قيمة REVENUECAT_ENTITLEMENT_ID بـ app/build.gradle.kts.
3. **مرر مفتاح الـ API وقت البناء**، ما تحطه مباشرة بالكود:
   `./gradlew assembleRelease -PrevenueCatApiKey=your_real_key_here`
   أو عن طريق متغير البيئة REVENUECAT_API_KEY. المفتاح اللي شفته بالصورة (test_...) مفتاح
   تجريبي (Sandbox) فقط — قبل النشر الفعلي لازم تستبدله بمفتاح الإنتاج (Production) من
   إعدادات المشروع بلوحة RevenueCat.
4. **حدد أسعار حقيقية بـ Google Play Console** لمنتجات frameout_pro (base plans: monthly,
   yearly) و frameout_pro_lifetime — الحقول اللي حطيتها بلوحة RevenueCat (مثل "3 months"
   أو "weekly") هي بس وصف تلقائي اقترحته RevenueCat، مو سعر فعلي؛ السعر الحقيقي ينضبط من
   Google Play Console نفسه، وبعدين RevenueCat يسحبه تلقائيًا.
5. تأكد إن أسماء الـ base plan IDs بـ Google Play Console مطابقة تمامًا لـ "monthly" و"yearly"
   المستخدمة بـ `BillingCatalog.kt`، وإلا الـ paywall ما يقدر يلقى العروض.


## 10. الانتقال إلى RevenueCat الكامل (يحل محل القسم 9)
- SDK صار 10.28.1، ومعه `purchases-ui` (Paywall و Customer Center).
- RevenueCat هو من ينفّذ الشراء (`PurchasesAreCompletedBy.REVENUECAT`). حُذف اعتماد التطبيق على Play Billing مباشرة (`BillingManager` الآن فوق RevenueCat).
- Entitlement الصلاحية: `frame_out_pro`.
- المفتاح حسب نوع البناء: debug = Test Store، release = `goog_` ويفشل البناء بدونه.
- `RevenueCatPaywall.kt`: Paywall من اللوحة داخل `PaywallHost` (يُعطَّل بـ `-PrevenueCatPaywall=false`).
- الإعدادات: صف "إدارة الاشتراك" يفتح Customer Center.
- التفاصيل وخطوات اللوحة: `docs/REVENUECAT_SETUP.md`.

## 11. شارة PRO الذهبية (ProStatusBadge)
- `billing/ProStatusBadge.kt`: شارة ذهبية بجانب اسم التطبيق في ترويسة المكتبة.
  - غير مشترك: ذهبية بلمعة وسهم، والضغط يفتح شاشة الدفع.
  - مشترك: تتحول بتلاشي إلى شارة هادئة بعلامة ✓، والضغط يفتح Customer Center (لو فيه اشتراك RevenueCat فعلي).
  - لا تظهر إذا Pro غير مفعّل في البناء (لا يوجد شراء).
- شاشات البداية: حُذفت الشارة القديمة `onboarding/ProBadge.kt`، وتظهر الجديدة (للعرض فقط) في شاشة «اختر الإطار الحاسم» وحدها (`ProPages` في `OnboardingScreen.kt`).
