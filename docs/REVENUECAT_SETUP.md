# إعداد RevenueCat لتطبيق Frame Out

التطبيق يستخدم RevenueCat SDK 10.28.1 بوضع الشراء الكامل (RevenueCat ينفّذ الشراء ويتحقق منه على سيرفراته).
الصلاحية الوحيدة اللي تفتح Pro هي Entitlement اسمه بالضبط: `frame_out_pro`.

## 1. لوحة RevenueCat

1. **Products**: أنشئ المنتجات الثلاثة. في Test Store (للتجربة): `monthly` و`yearly` و`lifetime`.
   في Google Play لاحقاً: اشتراك `frameout_pro` بخطتين أساسيتين `monthly` و`yearly`، ومنتج لمرة واحدة `frameout_pro_lifetime`.
2. **Entitlements**: أنشئ `frame_out_pro` واربط به المنتجات الثلاثة (كل منتج جديد لازم يُربط به).
3. **Offerings**: أنشئ offering وعيّنه **Current**، وفيه ثلاث packages بالأنواع القياسية:
   `$rc_monthly` و`$rc_annual` و`$rc_lifetime`. التطبيق يحدد الخطة من نوع الـ package.
4. **Paywalls**: أنشئ Paywall وانشره واربطه بالـ offering. بدون Paywall منشور تظهر شاشة افتراضية بسيطة.
5. **Customer Center**: فعّله من إعداداته في اللوحة (أزرار الإدارة والإلغاء والدعم تُضبط من هناك).

## 2. المفاتيح

| نوع البناء | المفتاح | كيف يُمرَّر |
|---|---|---|
| debug | Test Store `test_...` | افتراضي في `app/build.gradle.kts`، أو `-PrevenueCatTestKey=...` |
| release | Google Play `goog_...` | `-PrevenueCatApiKey=...` أو متغير `REVENUECAT_API_KEY` |

مفتاح `test_` داخل نسخة release يتسبب بإغلاق التطبيق عمداً من RevenueCat. لذلك:
- بناء release لمتجر Play (`-PstoreBuild=true`) **يفشل** إذا المفتاح مو `goog_`.
- التطبيق نفسه يرفض تهيئة RevenueCat بمفتاح `test_` في release بدل ما ينهار.

## 3. التجربة

- شغّل نسخة debug مع `-PstoreBuild=true` (بدونها الدفع غير مفعّل أصلاً في التطبيق).
- شراء Test Store يظهر نافذة محلية بدل Google Play.
- واجهة الدفع الداخلية (تصميم التطبيق) هي الافتراضية في كل النسخ، والشراء يتم عبر RevenueCat.
- شاشة Paywall من لوحة RevenueCat اختيارية فقط: `-PrevenueCatPaywall=true` (تظهر فارغة إن لم تضبط Paywall في اللوحة).

## 4. قبل النشر

- [ ] مفتاح `goog_` في بناء release.
- [ ] في RevenueCat: ربط حساب Google Play (Service Account) ثم Real-time developer notifications، وهذا شرط التحقق من المشتريات الحقيقية.
- [ ] المنتجات الحقيقية من Play مربوطة بـ `frame_out_pro` وبالـ offering الحالي.
- [ ] تجربة شراء حقيقي بحساب اختبار Play (License testers) على مفتاح `goog_`، مش Test Store.
- [ ] تجربة الاستعادة من Customer Center ومن الـ Paywall.

## 5. كيف يتحقق التطبيق

- Pro تُفتح وتُغلق من `RevenueCatSync.apply` فقط، ومن `CustomerInfo` راجع من RevenueCat.
- الردود موقّعة (Trusted Entitlements، وضع INFORMATIONAL). رد فشل توقيعه لا يفتح Pro في release.
- رد قديم (أكثر من يومين) لا يجدد ختم التحقق الذي تعتمد عليه فترة السماح بدون إنترنت.

- Customer Center مبني على Material 3، فيُلَفّ بثيم Material مشتق من ألوان التطبيق (`frameMaterialScheme`). راجعه بصرياً في الوضعين الفاتح والداكن.
