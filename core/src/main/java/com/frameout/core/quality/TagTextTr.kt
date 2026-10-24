package com.frameout.core.quality

internal object TagTextTr {
    val text = TagText(
        labels = TagLabelsTr.MAP,
        fixed = mapOf(
            TagRules.FACE to "Yüzler", TagRules.GROUP to "Grup",
            TagRules.SMILE to "Gülümsüyor", TagRules.EYES_CLOSED to "Gözler kapalı",
            TagRules.GOOD_FACE to "İyi yüz", TagRules.BODY to "Vücut",
            TagRules.TEXT to "Metin var", TagRules.CODE to "Herhangi bir kod",
            "b:contact" to "Kişi", "b:email" to "E-posta",
            "b:isbn" to "ISBN", "b:phone" to "Telefon",
            "b:product" to "Ürün kodu", "b:sms" to "SMS",
            "b:plain" to "Düz metin kodu", "b:url" to "Bağlantı",
            "b:wifi" to "Wi-Fi", "b:geo" to "Konum",
            "b:event" to "Etkinlik", "b:license" to "Lisans",
            "o:fashion_good" to "Moda", "o:food" to "Yiyecekler",
            "o:home_good" to "Ev eşyaları", "o:place" to "Yerler",
            "o:plant" to "Bitkiler"
        ),
        groups = mapOf(
            "People" to "İnsanlar", "Things" to "Nesneler ve sahneler", "Categories" to "Kategoriler",
            "Text" to "Metin", "Codes" to "Kodlar"
        ),
        families = mapOf(
            "vehicles" to "Taşıtlar", "animals" to "Hayvanlar", "people" to "İnsan ve vücut",
            "fashion" to "Moda ve aksesuarlar", "food" to "Yiyecek ve içecek", "nature" to "Doğa ve manzara",
            "plants" to "Bitkiler ve bahçeler", "sky" to "Gökyüzü ve hava durumu", "places" to "Binalar ve yerler",
            "home" to "Ev ve iç mekân", "devices" to "Cihazlar ve ekranlar", "documents" to "Belgeler ve panolar",
            "music" to "Müzik ve dans", "sports" to "Spor ve açık hava", "events" to "Kutlamalar ve etkinlikler",
            "toys" to "Oyuncaklar ve karakterler", "crafts" to "Sanat ve el işi", "objects" to "Eşyalar ve aletler"
        ),
        contentTags = mapOf(
            "building" to "Binalar", "skyscraper" to "Gökdelenler", "bridge" to "Köprüler",
            "tower" to "Kuleler", "mosque" to "Camiler", "church" to "Kiliseler",
            "castle" to "Kaleler ve saraylar", "landmark" to "Simge yapılar", "road" to "Yollar",
            "meal" to "Yemekler", "fastfood" to "Fast food", "sweets" to "Tatlılar",
            "bread" to "Ekmek", "fruit" to "Meyveler", "vegetable" to "Sebzeler",
            "coffee" to "Kahve", "drink" to "İçecekler", "soccer" to "Futbol",
            "swimming" to "Su sporları", "running" to "Koşu ve yarışlar", "dance" to "Dans",
            "concert" to "Konserler", "party" to "Partiler", "wedding" to "Düğünler",
            "fireworks" to "Havai fişekler", "fire" to "Ateş", "car" to "Arabalar",
            "heavy" to "Minibüsler ve kamyonlar", "bus" to "Otobüsler", "motorcycle" to "Motosikletler",
            "bicycle" to "Bisikletler", "train" to "Trenler", "plane" to "Uçaklar",
            "helicopter" to "Helikopterler", "boat" to "Tekneler ve gemiler", "dog" to "Köpekler",
            "cat" to "Kediler", "bird" to "Kuşlar", "horse" to "Atlar",
            "cattle" to "Sığırlar", "bear" to "Ayılar", "reptile" to "Sürüngenler",
            "insect" to "Böcekler", "mountain" to "Dağlar", "beach" to "Plajlar",
            "desert" to "Çöl", "forest" to "Ormanlar", "water" to "Göller ve nehirler",
            "waterfall" to "Şelaleler", "sunset" to "Gün batımları", "flower" to "Çiçekler",
            "ice" to "Buz ve buzullar", "storm" to "Fırtınalar", "phone" to "Telefonlar",
            "computer" to "Bilgisayarlar", "tv" to "Televizyonlar", "instrument" to "Müzik aletleri",
            "clock" to "Saatler", "bag" to "Çantalar", "glasses" to "Gözlükler",
            "toy" to "Oyuncaklar", "shoes" to "Ayakkabılar", "hat" to "Şapkalar",
            "dress" to "Elbiseler", "clothes" to "Giysiler", "jewellery" to "Mücevherler",
            "face" to "Yüzler", "smile" to "Gülümsemeler", "group" to "İnsan grupları",
            "baby" to "Bebekler", "crowd" to "Kalabalıklar ve takımlar", "text" to "Metin",
            "code" to "QR ve barkodlar"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Taşıtlar", "Animals" to "Hayvanlar", "Nature" to "Doğa",
            "Places" to "Yerler", "Food" to "Yiyecek ve içecek", "Activities" to "Spor ve etkinlikler",
            "Devices" to "Cihazlar ve eşyalar", "Fashion" to "Moda", "People" to "İnsanlar ve metin"
        ),
        engines = mapOf(
            "google_lens" to "Nesneleri, yerleri ve metni tanır; görselin kaynağını bulur",
            "yandex_images" to "Bir görselin kopyalarını ve benzer sahneleri bulmada en iyisi",
            "bing_visual" to "Görsel olarak benzer resimler ve alışveriş eşleşmeleri",
            "tineye" to "Bir görselin ilk ortaya çıktığı yeri ve düzenlenmiş hâllerini bulur",
            "google_images" to "Klasik ters görsel arama: yüklemek için kamera simgesine dokunun",
            "baidu_images" to "Asya web siteleri, ürünleri ve sosyal medya için güçlü",
            "saucenao" to "Anime, manga ve illüstrasyonların kaynağını bulur",
            "other_apps" to "Kareyi görsel arayabilen yüklü herhangi bir uygulamaya gönder"
        ),
        compare = mapOf(
            "stack" to "Üst üste", "side" to "Yan yana",
            "slider" to "Kaydırıcı", "swap" to "Değiştir",
            "fit" to "Sığdır", "zoomIn" to "Yakınlaştır",
            "zoomOut" to "Uzaklaştır", "resetZoom" to "Yakınlaştırmayı sıfırla",
            "hint" to "Yakınlaştırmak için kıstırın · çift dokunun", "loadFailed" to "Görsel yüklenemedi",
            "divider" to "Karşılaştırma ayırıcısı", "diff" to "Fark",
            "diffBusy" to "Farklar hesaplanıyor…", "link" to "Yakınlaştırmayı bağla",
            "unlink" to "Bağımsız yakınlaştırma", "actualSize" to "Gerçek piksel",
            "diffPercentPrefix" to "Fark: "
        )
    )
}
