# Frame out Codec Pack Signing

أنشئ مفتاح RSA-3072 جديدًا مرة واحدة قبل أول بناء عبر `scripts/generate-codecpack-signing-key.sh`. السكربت يكتب المفتاحين تحت `build/codecpack-signing`، ويُبقي المفتاح الخاص خارج المستودع تلقائيًا.

```text
bash scripts/generate-codecpack-signing-key.sh
```

استبدل محتوى المفتاح العام الموجود في `codecpack/src/main/java/com/frameout/codecpack/CodecPackVerifier.kt` بمحتوى `codecpack-signing-public-key.pem`، ثم حوّل محتوى المفتاح الخاص إلى GitHub Actions Secret باسم `CODECPACK_SIGNING_KEY_PEM`.

لا تضع المفتاح الخاص في المستودع أو داخل ZIP. سكربت `package-codecpack.sh` يرفض أي مفتاح غير RSA-3072.

أي ملف codec pack بلا `SIGNING_PAYLOAD.txt` وتوقيع `SIGNATURE.bin` مطابق للمفتاح العام يُرفض قبل نسخ أي مكتبات native.

حدود الاستيراد الحالية: 48 MiB للحزمة، 64 MiB غير مضغوط، و10 MiB لكل entry، مع رفض المسارات الإضافية والتكرارات. التثبيت يتم عبر staging ثم تبديل مجلد الإصدار بعد اكتمال التحقق.

حزمة FFmpeg نفسها تُتحقق من توقيع الإصدار قبل البناء، مع تثبيت بصمة مفتاح FFmpeg الرسمي `FCF986EA15E6E293A5644F10B4322F04D67658D8`.
