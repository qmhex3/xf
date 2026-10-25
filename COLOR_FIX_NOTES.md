# COLOR_FIX_NOTES

ملاحظة: الملف لم يكن موجوداً داخل app_fx_fixed_v11.zip، فأُنشئ هنا من وصف v11 في طلب العمل ومن قراءة الكود.

## v11 (كما وُصف، لم يُعدَّل)
قيم colr تتقدم على افتراضات الكودك، nclc لا يفرض limited، VuiColor لـH.264/HEVC، HdrDetector يقرأ VUI.

## v12: ما تغيّر
1. AV1: `Av1Color.kt` يقرأ color_config من sequence header (csd-0 بصيغة av1C أو OBUs مجردة). يعالج color_description_present_flag=0 وحالة sRGB (1/13/0) وmono_chrome. AV1 غير الموسوم وlimited لا يُعلن مدى (تحتفظ الحاوية/RangeProbe بالقرار).
2. VP9 في MP4: `Vp9Color.parseVpcc` يقرأ vpcC من stsd مباشرة (دون الاعتماد على الـextractor) ويُدمج مع colr (colr أولاً، ومدى vpcC يكمل nclc). vpcC بقيمة 2/2/2 وfull=0 لا يُعدّ إعلاناً للمدى.
3. VP9/AV1 في MKV/WebM: `MkvColour.kt` يقرأ عنصر Colour (Range/Matrix/Transfer/Primaries) من أول 1MiB.
4. الأولوية: bitstream (VUI/AV1) > الحاوية > افتراضات الكودك. الربط في DecoderOrientation.readOrientation. HdrDetector يستخدم Av1Color أيضاً.
5. VuiColor: Bits/Truncated/copy صارت internal وأُضيفت uvlc.
6. HDR→SDR: إزالة تشبع تدريجية في HdrToneMapper.kt وFfmpegHdr.cpp (المساران). الوزن = DESAT_STRENGTH * t^DESAT_EXPONENT حيث t موقع أعلى قناة بين الركبة والذروة. صفر حتى الركبة (152 نت) فالألوان المتوسطة لا تتغير. الثوابت: `HdrTables.DESAT_STRENGTH=0.6`، `DESAT_EXPONENT=1.5` وما يقابلها kDesatStrength/kDesatExponent في C++ (يجب تعديلها معاً). المُنشئ يقبل `desaturation` (0 يعطل المرحلة).
   - GainmapBuilder لم يُعدَّل: الـgain يبقى نسبة إضاءة، فقاعدة SDR تفقد جزءاً من تشبع الإضاءات العالية عند إعادة بناء HDR.

## ما تحقق منه وكيف
- محللات AV1/vpcC/Matroska: نسخة Python (scripts/color_verify/colorref.py) على ~20 عينة حقيقية (libaom، SVT-AV1، libvpx-vp9؛ limited/full؛ 601/709/2020؛ HDR10/HLG؛ mono؛ 4:4:4؛ 12bit؛ sRGB؛ timing-info؛ OBU مجرد؛ mp4/webm) طابقت ffprobe في كل الحالات.
- HDR: تطابق C++ المجمّع (g++) مع نسخة Python من Kotlin: فرق أقصى 1/255 على PQ(709/2020/P3/DCI) وHLG، قبل التعديل وبعده.
- أثر إزالة التشبع (60 ألف بكسل اصطناعي): يتغير 2-16% من بكسلات 100-500 نت، ومتوسط التشبع يتغير بحدود 0.001. السبب أن fitGamut يزيل معظم تشبع الإضاءات العالية أصلاً. فالتأثير خفيف ومضبوط لا جذري. رفع DESAT_STRENGTH ممكن لكن يحتاج حكماً بصرياً على فيديو حقيقي.
- قاعدة غير الموسوم: متطابقة (601 إذا h<=576 && w<1280، وإلا 709) في ColorParams.isSd وswsColorspace.
- RangeProbe: يُصفَّر في FrameDecoder.configureConverter لكلا المحولين، ويعمل فقط عند rangeDeclared=false.

## v12 (مراجعة ثانية): ما أُصلح وما شُغّل فعلاً
- شُغّلت اختبارات Kotlin فعلياً (kotlinc 2.0.21 مع بدائل Android الصغيرة في scripts/color_verify/android_stubs، وJUnit 4.13.2): Av1ColorTest وContainerColourTest وHdrDesaturationTest وVuiColorTest وHdrToneMapperTest وHdrPeakTest = 48 ناجحاً من 48. الأمر: scripts/color_verify/run_kotlin_tests.sh. أُزيلت التعليقات من ملفات v12 لأن scripts/check-comments.sh كان يفشل عليها (ومنها تعليق قديم في ColorParams.kt).
- أول تشغيل كشف عطلين لم يكونا ظاهرين بالقراءة:
  1. VuiColor.unescape ينهار (NegativeArraySizeException) عند NAL من بايت واحد بعد start code (SPS مبتور). أُصلح بحارس `to <= from`.
  2. اختبار brightYellowLosesSaturation كان يفترض أثراً (+4) لا يحدث فعلاً عند القوة 0.6 (القياس الحقيقي في Kotlin وPython: فرق درجة واحدة). صُحح الاختبار ليقيس بقوة 1.0 على عينة قياسها الفعلي: (250,244,41) إلى (250,244,52)، وأُضيف اختبار أن القوة الافتراضية بين الصفر والكاملة.
- محللات Av1Color وVp9Color.parseVpcc وMkvColour (Kotlin المترجم نفسه، لا نسخة Python) على 17 عينة ffmpeg حقيقية (libaom: 601/709/2020، PQ وHLG، full/limited، gray، sRGB/RGB، بلا وسوم؛ libvpx-vp9 في mp4 وwebm؛ AV1 في webm) طابقت ffprobe في المدى والأولية والنقل والمصفوفة في كل الحالات.
- تطابق C++ (FfmpegHdr.cpp مجمّعاً بـg++) مع نسخة Python من Kotlin مجدداً: فرق أقصى 1/255 في 6 تركيبات (PQ 709/2020/P3/DCI، HLG، ذروة 1000 و4000).
- قياس أثر إزالة التشبع: بإدخال ساطع مشبع (إشارة 0.6-0.8)، 99% من البكسلات يقصّها fitGamut أصلاً وتشبعها المتوسط نحو 0.20 مع إزالة التشبع أو بدونها. حتى وزن أقوى مبني على نسبة الضغط لم يغيّر المتوسط إلا بمقدار 0.001-0.015. فمسألة التشبع في الإضاءات العالية يحسمها fitGamut لا هذه المرحلة، ولم أرفع القوة بلا حكم بصري. إن بقي تشبع زائد في فيديو حقيقي فالأرجح مصدره الألوان المتوسطة (مصفوفة/مدى/أولية خاطئة)، لا تحويل HDR.

- مقارنة تحويل YUV→RGB رقمياً بمرجع ffmpeg (swscale بـaccurate_rnd وfull_chroma_int، مدخل yuv444p بتدرج يغطي فضاء YUV، ومعاملات ColorParams.rv/gu/gv/bu وجداول luma/chroma من Kotlin المترجم): للمصفوفات 601 و709 و2020 بمدى full متوسط الفرق المطلق ≤0.003 لكل قناة وأقصى فرق 1. بمدى limited نفس النتيجة عدا 195 بكسلاً من 49152 يتجاوز فيها الأزرق الخام 511/1023 (قيم YUV مستحيلة في فيديو حقيقي) فيلتف swscale بينما يقص Kotlin، والقص هو السلوك الصحيح. لم تُقارَن إعادة التشكيل (resampling) للشروما.

## مقارنة HDR (PQ/BT.2020) بمرجع ffmpeg
المرجع: zscale (npl=203 فيكون 203 نت = 1.0، ثم p=bt709، ثم t=iec61966-2-1 بلا dither) مع tonemap=none/hable/mobius (desat=0, peak=4.926). المدخل 131072 بكسلاً داخل نطاق BT.709 (نصفها تحت ركبة المنحنى 152 نت، ونصفها حتى 1000 نت). الإعادة: scripts/color_verify/hdr_ffmpeg_compare.py مع HdrDump.kt.
- تحت الركبة (لا ضغط): Frame out يطابق سلسلة ffmpeg الخطية (PQ EOTF ثم 2020→709 ثم sRGB) في 100% من البكسلات بفرق ≤1 وأقصى فرق 2 (متوسط 0.063). الفرق 2 من تكميم جداول 4096 خطوة قرب السواد. فالألوانية (EOTF والأولية وOETF) مطابقة لـffmpeg.
- فوق الركبة: الفرق ناتج عن منحنى الضغط نفسه. مقابل القص tonemap=none متوسط 5.5 (Frame out يضغط ffmpeg يقص). مقابل mobius 29.8 وhable 45.2. وhable عند ffmpeg يغيّر حتى ما تحت الركبة (متوسط 50 درجة، يُغمّق المتوسطات) فلا يطابق الأصل أصلاً، بينما منحنى Frame out يترك ما تحت 152 نت دون تغيير.
- ألوان خارج نطاق BT.709 (مثال: ألوان 2020 مشبعة): ffmpeg يقص كل قناة على حدة فيتغير الـhue، وFrame out يضغطها نحو لمعان الإشارة بـfitGamut فيحفظ الـhue والإضاءة. لذلك تكبر الفروق عليها (على عينة عشوائية 82% منها مقصوصة عند ffmpeg) وهي فروق مقصودة.
- قرار صاحب الطلب: إبقاء منحنى Frame out ومقارنته فقط. لم أستبدله بمنحنى ffmpeg. إن أردت مطابقة tonemap محدد فأخبرني أي منحنى وأي npl وأي معالجة لخارج النطاق.
- لم تُجرَّب HLG ولا Dolby Vision مقابل ffmpeg.

## ما لم يُختبر
- Gradle وAndroid SDK غير متاحين هنا، فلم يُبنَ المشروع ولم تُشغَّل اختبارات Gradle نفسها.
- ما يصل فعلاً من MediaExtractor إلى MediaFormat لم يُفحص على جهاز، وكذلك الربط في DecoderOrientation وFrameDecoder (يعتمد على Android).
- Gradle: compileDebugKotlin والاختبارات داخل مشروع Android الكامل لم تُشغَّل هنا. الاختبارات الستة أعلاه شُغّلت خارجه مع بدائل Android، فقد تختلف التفاصيل عن android.jar الحقيقي.
- قراءة color_space من uncompressed header لـVP9 (اختيارية): لم تُنفَّذ عمداً، لأن تنفيذها يتطلب قراءة أول عينة من MediaExtractor داخل readOrientation وهو تدخل في مسار التشغيل بفائدة ضئيلة بعد قراءة vpcC وعنصر Colour.
- لم تُقارَن الألوان بمرجع ffmpeg zscale/tonemap (فشل تشغيل السلسلة في بيئتي).

## FallbackFrames/FallbackColor (توثيق فقط)
المسار الاحتياطي (MediaMetadataRetriever) لا يمر بـColorHints/VuiColor/RangeProbe، ولا يحوّل HDR ولا gainmap، ويترك مدى/مصفوفة التحويل للإطار (يختلف حسب إصدار Android). FallbackColor يوحّد فضاء اللون إلى sRGB فقط. النتيجة قد تختلف عن المسار الرئيسي ولم يُقِس ذلك على جهاز.

## v13: خريطة gain لكل قناة وبدقة كاملة (HDR المحفوظ كـ Ultra HDR)
- المشكلة: الـgain كان قيمة إضاءة واحدة بنصف الدقة، فلا يُعاد بناء ألوان HDR الأصلية (في العينة الاصطناعية 71% من البكسلات الساطعة بخطأ >10% في قناة).
- الحل (Kotlin: HdrToneMapper وGainCodec وGainmapBuilder، وC++: FfmpegHdr وFfmpegGainPack وFfmpegColor): gain مستقل لكل قناة R/G/B بدقة الصورة الكاملة (بلا halve). يُحسب من قيمة HDR الخطية في فضاء العرض ومن كود SDR النهائي المكتوب فعلاً (بعد الـdither): gain = (hdr + 1/64) / (sdr + 1/64). فأي تكميم أو dither في القاعدة يُعوَّض داخل الـgain.
- الاحتفاظ بـ16 بت لكل قناة حتى لحظة الكتابة، ثم تكميم إلى 8 بت بمدى ratioMin/ratioMax مضبوط لكل قناة وللإطار نفسه، مع وقوع gain=1 تماماً على عقدة من شبكة التكميم.
- بكسل خارج فضاء العرض (قناة HDR سالبة) يأخذ gain=1 لأنه لا يُمثَّل بنسبة موجبة، ولا يوسّع مدى الخريطة.
- نطاق ln(gain) الداخلي صار [-4.2, 4.0] (يغطي حتى ~10000 نت).
- JpgHdr يُحفظ دائماً بجودة 100 (تُضغط خريطة الـgain JPEG أيضاً).
- JNI: meta صار 8 قيم (w,h,ratioMin[3],ratioMax[3]) والمخزن w*h*3 بايت. اختبار HdrToneMapperTest الجديد يتحقق من إعادة البناء ومن ثبات الواحد.

### ما قيس
- Kotlin: 49 من 49 ناجحاً (scripts/color_verify/run_kotlin_tests.sh).
- C++ (g++ على FfmpegHdr.cpp وFfmpegGainPack.h فعلياً) على 262144 بكسلاً اصطناعياً بمحاكاة معادلة Android: خطأ نسبي متوسط ~0.4%، أقصى ~1.3%، ومتوسط فرق ~0.2-0.3 درجة 8-بت. العينة قاسية عمداً (ألوان مشبعة ساطعة تفتح المدى).
- حدّ لا يتجاوزه 8 بت: خريطة gain بـ8 بت لا تعيد HDR بصفر فرق عندما يتسع مدى الإطار؛ التطابق التام يحتاج حفظاً بعمق ≥10 بت (مثلاً PNG 16 بت مع cICP أو AVIF 10 بت) وهذا لم يُنفَّذ.
- ألوان BT.2020 خارج sRGB/P3 لا تُعاد بنسبة موجبة (تبقى مضغوطة داخل الفضاء).

### لم يُختبر
- لم يُبنَ المشروع بـGradle/NDK هنا ولا جُرّب على جهاز Android 14+، وFfmpegColor.cpp لم يُترجَم (رؤوس FFmpeg غير متاحة).

## v13 (تتمة): صيغة PNG HDR 16-bit للحفظ الدقيق
- الفكرة: Ultra HDR لا يعيد HDR بصفر فرق لأن الـgain بـ8 بت. الصيغة الجديدة `png_hdr16` تحفظ إشارة الفيديو نفسها (بعد YUV→RGB بالمصفوفة المصرّح بها، قبل أي tone-mapping أو gamut) بدقة 16 بت لكل قناة، مع وسم cICP (أولية/نقل PQ أو HLG/مدى كامل) ومعه eXIf عند تفعيل البيانات الوصفية.
- الالتقاط: Yuv10Job يخزن r,g,b قبل finish (حدّ الكمّ 16 بت)، ويطبّق Orient فيخرج مدوَّراً. يُفعَّل فقط عبر HdrFrameFetcher.fetchExact (التصدير) وبشروط: HDR، مسار Image 10 بت، step==1 (بلا تصغير للذاكرة)، aspect==1.
- الربط: ExportSourceLoader يجلب الإطار عبر fetchExact قبل المسار القديم. HdrSignalRegistry (WeakHashMap) يربط الإشارة بالـBitmap وImageFormat.compress يكتبها عبر Png16Writer ثم يحذفها.
- التراجع التلقائي (ImageFormat.resolve): بلا إشارة (تعديل، قص، تدوير/ألوان، تصغير الحجم، فشل الالتقاط، مسار FFmpeg أو MediaMetadataRetriever) → JpgHdr إن وُجد gainmap وإلا Png.
- Png16Writer: فلتر Sub، Deflater، IDAT بكتل 64KB، IHDR 16 بت RGB.
- ما اختُبر: Png16WriterTest (4/4) يفك الملف بـjavax.imageio المستقل ويقارن كل عينة 16 بت بالأصل: تطابق تام؛ cICP قبل IDAT؛ ImageFormatTest محدَّث (لم يُشغَّل لاعتماده على android.jar). فحوص الأسلوب سليمة.
- لم يُختبر: بناء Gradle، التشغيل على جهاز، وصول الإشارة فعلاً من MediaCodec (P010) على أجهزة حقيقية. قراءة الملفات: دعم cICP يختلف بين العارضات (Chrome/Firefox/Photoshop وبعض المعارض الحديثة تقرؤه؛ غيرها قد تعرضه SDR).

## v15 (غير مكتمل): ما أُنجز وما لم يُختبر

### أُضيف
- C++: التقاط إشارة RGB 16 بت (قبل toneMapHdr16، مع التدوير) عبر FfmpegSignal.h وFfmpegSignalCapture.h وFfmpegSignalStore.cpp وFfmpegRotate.h، وJNI: nativeSetSignalEnabled وnativeLastSignal. لا التقاط عند Dolby Vision أو matrixDecode أو التصغير أو مدى/مصفوفة/نقل غير معلن أو أولية غير 709/2020/P3.
- Kotlin: FfmpegSignal وFfmpegSignalLink وFrameBudget.fitExact وFrameSourceDecoder.decodeExact وHdrFrameSource.isHdrSource؛ ExportSourceLoader: MediaCodec ثم FFmpeg (HDR فقط) ثم Ultra HDR.

### اختُبر فعلاً
- g++ (مع ASAN/UBSAN/TSAN): scripts/color_verify/signal_test.cpp (التدوير والتجميع والشروط والمخزن) ناجح.
- check-comments وcheck-lines وcheck-function-lines ناجحة.
- قياس مرشحات PNG (png_filter_bench*.py) على عينات اصطناعية من صور مُرمَّزة PQ/HLG بـHEVC: Sub الأفضل (65.5% PQ، 62.8% HLG من الخام)؛ Paeth 71.9/68.6؛ تكيّفي ~71.7/67.7؛ المستوى 9 يوفّر ~0.2%. لم يُعدَّل Png16Writer.
- AVIF: RGB 10 بت دون فقد يخسر حتى 63 درجة (16 بت)؛ YCbCr الأصلي دون فقد مطابق بالبت لكنه يحتاج مُرمِّز AV1 غير موجود في المشروع، فلم يُضف.

### لم يُختبر
- Kotlin لم يُترجَم ولم تُشغَّل اختباراته (لا kotlinc/شبكة)؛ لا اختبارات وحدة Kotlin جديدة.
- FfmpegColor.cpp وملفات الجلسة وJNI لم تُترجَم (لا رؤوس FFmpeg/JNI). لا اختبار على جهاز.
- لم يُتحقق من التدوير مقابل transpose في ffmpeg، ولا من PNG بـffprobe/قارئ مستقل.
- لم تُضف iCCP وmDCV وcLLI. لم تُنفَّذ المعالجة على شرائح ولم يُعدَّل DecodeBudget. أرقام الذاكرة الأصلية تقديرية.

### مشكلة مفتوحة (مهمة)
- yuv10_signal_mirror.py (نسخة Yuv10Job) مقابل swscale rgb48 على 4:4:4: متوسط فرق 88 وأقصى 195 من 65535؛ المرجع ≈ 0.99615 × الإشارة (قريب من 65280/65535، تخمين غير مؤكد). مسار FFmpeg يلتقط خرج swscale فقد يرث هذا الفرق (~0.4%). لم يُحسم أي المسارين الصحيح، ولم يُفصل أثر الشروما على 4:2:0. لا يُدَّعى التطابق التام قبل حسمه.

## v13: معايرة اللون على مرجع النظام (لإصلاح تشبّع الأحمر/التباين العالي في عارض الإطارات)

### الأعراض والتشخيص
لقطة المحرر تمرّ عبر `MediaMetadataRetriever` (تحويل النظام) فتخرج صحيحة، أما الإطارات المستخرَجة فتمرّ عبر محوّل YUV→RGB الداخلي الذي يثق بما يصله من بيانات (قيم الـdecoder/الحاوية/VUI).
قياس رقمي على لقطتي الشاشة (بعد محاذاتهما): التباين يتضاعف تقريباً في الدرجات المتوسطة، التشبع يرتفع من ~95 إلى ~135 (×2.5 في Cb)، البرتقالي ينزاح ~14° نحو الأحمر، والفرق يتغير من بكسل لآخر. نموذج عام (γ + مصفوفة 3×3) يعطي خطأ 20 RMS، بينما تطبيق منطق HDR الحالي (PQ→SDR + BT.2020→709) على ألوان اللقطة الجيدة يعطي 11.7 (مقابل 36.7 بلا تحويل). أي أن مسار الاستخراج يقرأ الفيديو كـHDR/BT.2020 (أو يطبّق تحويل HDR مختلفاً عمّا يعرضه النظام) بينما العرض الصحيح هو ما يعرضه المشغّل.
أسباب معروفة لهذا في أندرويد: بعض الـdecoders تُعلن BT.2020/PQ كقيمة افتراضية لملفات 10-bit بلا VUI، أو تُعلن primaries خاطئة، أو مدى (limited/full) خاطئاً.

### الإصلاح
- `ColorVariants.kt`: قراءات بديلة لإشارة اللون (SDR بمصفوفة الميتاداتا/709/601/2020 دون تحويل gamut، وبمدى معكوس، وقراءة الميتاداتا بمدى معكوس).
- `ColorCalibrator.kt` + `CalibrationRule.kt`: عند أول إطار من كل فيديو (مرة واحدة ثم تُحفظ النتيجة في ذاكرة العملية حسب الـURI) يقارن إطاراً مفكوكاً (فسيفساء 24×24) بإطار النظام عند نفس الـPTS. إن كان الفرق ≤ 4.5 تبقى القراءة التلقائية؛ وإلا يجرّب البدائل ويختار الأقرب بشرط أن يزيل ≥25% من الخطأ و≥3 درجات. إن كان أفضل خطأ > 60 (إطاران غير قابلين للمقارنة) لا يُتخذ قرار وتُعاد المحاولة (حتى 3 مرات).
- `FrameDecoder` يستدعي المعايرة قبل أول تحويل ويطبّق النتيجة على `YuvBitmapConverter` و`BufferYuvConverter` (`variantId`). تُتخطى المعايرة عندما يكون المصدر HDR فعلاً وإعداد الحفظ HDR (gain map / PNG 16-bit) مفعّلاً حتى لا يضيع الـHDR.
- السجل: سطر `ColorCalibrator variant=… autoError=… candidates=…` في DiagLog يبين القرار.
- اختبارات: `ColorCalibrationTest` (مضافة إلى `run_kotlin_tests.sh`).

### حدود
- مسار FFmpeg (حاويات/كودكات لا يدعمها النظام) لا يوجد له مرجع نظام للمقارنة، فلم يتغير.
- لم يُشغَّل التطبيق على جهاز في هذه البيئة؛ المنطق البحت كُمبل واختُبر مع stubs، وكود أندرويد (`FrameDecoder`/`ColorCalibrator`) يحتاج بناءً وتجربة على الجهاز.

## v16: منحنى HDR أوسع للإضاءة العالية + معاينة HDR متوقفة
- TONE_KNEE=0.55 (كان 0.75) في HdrTables.kt وkToneKnee في FfmpegHdr.cpp. ركبة إزالة التشبع منفصلة DESAT_KNEE=0.75 (سلوكها لم يتغير). مقارنة على تدرج 0-1000 nit: خيار ref 203->0.8 يغمّق الظلال حتى 18 درجة عند 100 nit فاستُبعد؛ BT.2390 (Lt=203/250) يشبّع الأبيض عند ~750 nit؛ ركبة 0.55 تترك كل ما تحت ~112 nit مطابقاً للقديم. البدائل: 0.60 (203 nit=231) و0.50 (=225، حدّ اختبار diffuseWhiteStaysBright).
- حُقّق بمحاكاة numpy (تطابق أرقامك ±1) وبترجمة FfmpegHdr.cpp بـg++ (فرق أقصى 1/255 مقابل kt_mirror.py). قبل التعديل كان C++ يختلف عن Kotlin حتى 115 درجة على بكسلات خارج الغامت لغياب fitHigh.
- لم يُترجم Kotlin ولم تُشغَّل اختبارات Gradle هنا (لا kotlinc). التأكيدات الجديدة والقديمة نُفّذت بمحاكاة Python فقط.
- المعاينة: Media3 setVideoEffects لم يُعتمد لأن مسار المؤثرات يُبقي خرج HDR ويحتاج SurfaceView، ولم أتحقق من API يفرض خرج SDR في 1.11.1. الإطار المتوقف فقط يُعرض مطابقاً للمحفوظ؛ التشغيل ما زال داكناً.
