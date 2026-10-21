> Historical audit retained for traceability; it describes the pre-repair state and is not the current implementation status.

# FrameX — تقرير النواقص المكتشفة

تاريخ الفحص: 2026-10-03

## النتيجة التنفيذية

الأرشيف `FrameX-completed.zip` ليس مكتملاً رغم وصفه في README بأنه يحتوي على المراحل 0–15. توجد أربع فئات من النقص:

1. ملفات CI/CD كانت منفصلة فعلًا عن الأرشيف الرئيسي، وتم العثور عليها داخل `.github.7z` واستخراجها.
2. حزمة FFmpeg ليست تنفيذًا فعليًا؛ طبقة JNI الحالية Stub ولا تقوم بفك أي إطار.
3. الاستئناف والفهرسة لا يحققان مواصفات الخطة بالكامل؛ حقول `frameCount` و`durationMs` لا تُملأ، والفهرس غير محفوظ في Room، والاستئناف لا يحافظ على وضع الاستخراج وAuto-save.
4. أجزاء من الواجهة والمحركات ما زالت Prototype: الخط المحلي غير موجود، بعض الوظائف غير مطبقة أو غير متصلة، وتوجد عناصر واجهة خارج نظام الترجمة/التصميم.

## 1. الملفات التي كانت مفقودة واسترجعتها

من `.github.7z` وُجدت هذه الملفات:

- `.github/workflows/build.yml`
- `.github/workflows/ffmpeg-build.yml`
- `.github/workflows/keystore.yml`
- `.github/workflows/release.yml`
- `.github/workflows/verify-versions.yml`

تم وضعها في نسخة مشروع جديدة دون تغيير بقية الملفات.

## 2. أهم النواقص الوظيفية

### A. FFmpeg — ناقص بشكل حاسم

`codecpack/src/main/cpp/framex_ffmpeg.cpp` لا يحتوي على FFmpeg فعليًا:

- `nativeProbe()` يعيد `stub`.
- `nativeFrameCount()` يعيد `0`.
- `nativeDecodeFrame()` يعيد `-1`.
- `nativeWidth()` و`nativeHeight()` يعيدان `0`.
- `CMakeLists.txt` يبني `framex_ffmpeg.cpp` وحده ولا يربطه بمكتبات FFmpeg.

النتيجة: مسار MKV/AVI/WMV/FLV/TS/… لا يمكن اعتباره عاملًا، حتى لو بُنيت مكتبات FFmpeg خارجيًا.

### B. Animated WebP / APNG / AVIF

`FormatProbe` يصنف WebP وAPNG وPNG كـ `AnimatedImage`، لكن `DecodeRouter` يرسل `AnimatedImage` إلى `FrameDecoder` القائم على `MediaExtractor/MediaCodec`.

لا توجد طبقة `ImageDecoder` للإطارات المتحركة، ولا يوجد مسار فعلي لـ Animated WebP أو APNG. كما لا توجد معالجة AVIF متحرك واضحة.

### C. Live Photo / Motion Photo

يوجد `MotionPhotoExtractor`، لكنه يقرأ الملف كاملًا إلى الذاكرة عبر `readBytes()` ثم يستخرج الجزء المضمن. هذا لا يطابق هدف الاستخدام الاقتصادي للذاكرة لملفات كبيرة.

كما أن `findLivePair()` يعتمد على مطابقة أسماء تقريبية ضمن MediaStore، وليس على نموذج موثوق لمعرّف المحتوى/الزوج.

### D. الفهرسة والاستئناف

الخطة تصف تخزين عدد الإطارات وأزمنتها وأقرب Keyframe في Room. التنفيذ الحالي يبني `VideoIndex` كاملًا داخل الذاكرة أثناء عملية الاستخراج ولا يحفظه.

الأوضح أن `ProjectEntity` يملك:
- `durationMs`
- `frameCount`
- `lastPtsUs`

لكن البحث في الكود لا يظهر أي مسار يحدّث `frameCount` أو `durationMs` بعد الفهرسة. لذلك بطاقة المشروع ونسبة التقدم والـBatch لا تملك إجماليًا دقيقًا دائمًا.

الاستئناف في `LibraryViewModel.resume()` يعيد الطلب بوضع `EveryFrame` و`autoSave=false` دائمًا، بغض النظر عن الطلب الأصلي.

إضافة إلى ذلك، `lastPtsUs` لا يُستخدم داخل `ExtractEngine` لتجاوز الإطارات التي تم حفظها سابقًا؛ الاستخراج يعيد الحساب من قائمة العينات الجديدة، ما يعرّض الاستئناف لإعادة إدخال الإطارات بدل متابعة نقطة ثابتة.

### E. إعادة التجميع

`VideoReassembler` يمرر العينات المطابقة لنطاقات محفوظة إلى `MediaMuxer`. هذا لا يضمن إعادة تجميع GOP سليمة لجميع الترميزات وحالات الحذف، ولا توجد طبقة لإعادة ترميز الجزء المتأثر فقط كما تصف الخطة.

هذا يعني أن الجزء الخاص بـ R&D ما زال Prototype وليس تنفيذًا مطابقًا لمعيار القبول النهائي.

### F. تعديلات العارض لا تصل إلى الحفظ

`ViewerViewModel` يحفظ crop في `FrameEditEntity`، وكذلك التدوير والقلب موجودان في نموذج التعديل.

لكن `FrameExporter.saveToGallery()` يقرأ `fullPath` أو `thumbnailPath` مباشرة، ولا يطبق `FrameEdit` قبل التصدير.

النتيجة: واجهة Crop/Rotate/Flip تسجل التعديل، لكن الحفظ/المشاركة لا يقدمان بالضرورة الصورة المعدلة.

### G. الكولاج أقل من الخطة

التنفيذ الحالي يوفر قوالب Grid فقط:

- 2x2
- 3x3
- 2x3
- 1x2
- 2x1
- 4x4

ولا توجد قوالب مستقلة لـ:
- Film strip
- Storyboard
- Mosaic
- Contact sheet

كما أن شاشة الكولاج تحفظ بصيغة JPG فقط، رغم أن الخطة تطلب PNG أو JPG، ولا يوجد ختم زمني أو تحكم بانحناء الزوايا في التنفيذ الحالي.

### H. إعدادات الاستخراج غير مكتملة

الشاشة تعرض:
- Every frame
- Every 5
- Keyframes
- Fixed 30
- Scene

لكن `ExtractScheduler.buildRequest()` لا يرمّز `SceneChange` ضمن الفرع الخاص به، لذلك يمكن أن تتحول حالة `scene` إلى `EveryFrame` عند الإرسال.

كذلك لا يظهر خيار `Every N milliseconds` من واجهة الإعدادات رغم وجوده في المحرك.

### I. المعرض الداخلي

`MediaScanner` يجلب الفيديو والصور من MediaStore، ثم يستبعد أي عنصر مصنف `Image`. لهذا يتم اعتماد GIF/Motion/Video فقط، لكن لا توجد معالجة فعلية شاملة لأنواع الصور المتحركة.

كما أن الوصول الجزئي على Android 14+ موجود في الصلاحيات، لكن مسار واجهة "إدارة الوصول" المطلوبة في الخطة غير ظاهر في `PickerScreen`.

### J. الخط

مجلد `designsystem/src/main/res/font/` فارغ، بينما `Type.kt` يستخدم `FontFamily.SansSerif`.

أي أن `IBM Plex Sans Arabic` المطلوبة في الخطة ليست مضمّنة حاليًا.

## 3. نظام التصميم — تطبيق جزئي

`FrameDock` و`GlassSurface` موجودان، لكن معظم الشاشات لا تستخدم Dock الموحد.

على سبيل المثال:
- `LibraryScreen` يستخدم `FrameDock`.
- `ViewerScreen` يستخدم Dock.
- `FramesScreen` يستخدم `FramesToolbar` مخصصًا وعريضًا.
- `ExtractProgressScreen` لا يستخدم Morph Dock.
- `TrimScreen` يعتمد صفًا علويًا وأزرارًا مخصصة.
- `PickerScreen` يعتمد شريطًا عاديًا وصينية سفلية.

كما أن `FramesToolbar` يحتوي عددًا كبيرًا من الأدوات في كبسولة واحدة، مخالفًا لقاعدة "4 عناصر كحد أقصى" في الخطة.

## 4. تعارضات الإصدارات داخل المشروع

الخطة الحالية تقول:

- AGP 9.4.0
- Gradle 9.7.1
- Kotlin 2.4.20

بينما الأرشيف يحتوي:

- AGP 9.4.1
- Kotlin 2.4.20
- README يذكر Gradle 9.8.0
- WorkManager 2.12.0
- Compose BOM 2026.09.00

وWorkflows المسترجعة تستخدم Gradle 9.8.0.

هذه ليست بالضرورة أخطاء توافق، لكنها عدم اتساق بين الخطة وGradle scripts وREADME وCI. ويلزم تحقق رسمي قبل اعتمادها.

## 5. نواقص CI/CD التي وجدتها داخل `.github.7z`

الـWorkflows المسترجعة نفسها تحتوي على نقاط تحتاج مراجعة قبل الاعتماد:

- `build.yml` يستخدم `gradle/actions/setup-gradle@v6` مع Gradle 9.8.0.
- `release.yml` يستخدم `softprops/action-gh-release@v2`.
- `ffmpeg-build.yml` يستخدم `nttld/setup-ndk@v1` وNDK `r27c`.
- `ffmpeg-build.yml` يطبع LOAD segments فقط ولا يفرض فشلًا إذا كان محاذاة 16KB غير صحيحة.
- `verify-versions.yml` يشغل `scripts/verify-versions.sh` فقط، والسكريبت الحالي يستخدم `<latest>` من metadata ولا يطبق منطق استبعاد alpha/beta/rc كما تنص الخطة بدقة.

## 6. ما أعتبره "مفقودًا" وليس مجرد تحسين

الترتيب هنا حسب الاعتماد الوظيفي، وليس تصنيفًا سياسيًا أو تفضيلًا:

- FFmpeg JNI الحقيقي وربطه بالمكتبات.
- استخراج Animated WebP/APNG/AVIF.
- فهرسة محفوظة واستئناف حقيقي يحافظ على mode/autoSave.
- تحديث `durationMs` و`frameCount`.
- تطبيق `FrameEdit` عند التصدير/المشاركة/الكولاج.
- استكمال قوالب وخيارات الكولاج.
- إدخال خط IBM Plex Sans Arabic.
- توحيد Dock/Morph عبر الشاشات.
- تدقيق CI بعد توحيد Gradle/AGP والإصدارات.

## 7. تحقق البناء

لم أتمكن من إجراء Build فعلي محليًا لأن بيئة الفحص لا تحتوي على Gradle executable، كما أن البحث الحي عبر الويب غير متاح في هذه الجلسة. لذلك لا أعتبر أي إصدار حديث أو توافق خارجي متحققًا.

