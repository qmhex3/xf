package com.frameout.core.quality

internal object TagTextMs {
    val text = TagText(
        labels = TagLabelsMs.MAP,
        fixed = mapOf(
            TagRules.FACE to "Wajah", TagRules.GROUP to "Kumpulan",
            TagRules.SMILE to "Senyuman", TagRules.EYES_CLOSED to "Mata terpejam",
            TagRules.GOOD_FACE to "Wajah berjaya", TagRules.BODY to "Seluruh badan",
            TagRules.TEXT to "Mengandungi teks", TagRules.CODE to "Sebarang kod",
            "b:contact" to "Kenalan", "b:email" to "E-mel",
            "b:isbn" to "ISBN", "b:phone" to "Telefon",
            "b:product" to "Kod produk", "b:sms" to "SMS",
            "b:plain" to "Kod teks", "b:url" to "Pautan",
            "b:wifi" to "Wi-Fi", "b:geo" to "Lokasi",
            "b:event" to "Acara", "b:license" to "Lesen memandu",
            "o:fashion_good" to "Fesyen", "o:food" to "Makanan",
            "o:home_good" to "Barangan rumah", "o:place" to "Tempat",
            "o:plant" to "Tumbuhan"
        ),
        groups = mapOf(
            "People" to "Orang", "Things" to "Benda dan pemandangan", "Categories" to "Kategori",
            "Text" to "Teks", "Codes" to "Kod"
        ),
        families = mapOf(
            "vehicles" to "Kenderaan", "animals" to "Haiwan", "people" to "Orang dan tubuh",
            "fashion" to "Fesyen dan aksesori", "food" to "Makanan dan minuman", "nature" to "Alam semula jadi dan pemandangan",
            "plants" to "Tumbuhan dan taman", "sky" to "Langit dan cuaca", "places" to "Bangunan dan tempat",
            "home" to "Rumah dan hiasan dalaman", "devices" to "Peranti dan skrin", "documents" to "Dokumen dan papan tulis",
            "music" to "Muzik dan tarian", "sports" to "Sukan dan aktiviti luar", "events" to "Majlis dan acara",
            "toys" to "Mainan dan patung", "crafts" to "Seni dan kraf tangan", "objects" to "Barangan dan alatan"
        ),
        contentTags = mapOf(
            "building" to "Bangunan", "skyscraper" to "Pencakar langit", "bridge" to "Jambatan",
            "tower" to "Menara", "mosque" to "Masjid", "church" to "Gereja",
            "castle" to "Istana dan kubu", "landmark" to "Mercu tanda", "road" to "Jalan raya",
            "meal" to "Hidangan", "fastfood" to "Makanan segera", "sweets" to "Manisan",
            "bread" to "Roti", "fruit" to "Buah-buahan", "vegetable" to "Sayur-sayuran",
            "coffee" to "Kopi", "drink" to "Minuman", "soccer" to "Bola sepak",
            "swimming" to "Sukan air", "running" to "Larian dan pertandingan", "dance" to "Tarian",
            "concert" to "Konsert", "party" to "Parti", "wedding" to "Perkahwinan",
            "fireworks" to "Bunga api", "fire" to "Api", "car" to "Kereta",
            "heavy" to "Van dan lori", "bus" to "Bas", "motorcycle" to "Motosikal",
            "bicycle" to "Basikal", "train" to "Kereta api", "plane" to "Kapal terbang",
            "helicopter" to "Helikopter", "boat" to "Bot dan kapal", "dog" to "Anjing",
            "cat" to "Kucing", "bird" to "Burung", "horse" to "Kuda",
            "cattle" to "Lembu", "bear" to "Beruang", "reptile" to "Reptilia",
            "insect" to "Serangga", "mountain" to "Gunung", "beach" to "Pantai",
            "desert" to "Gurun", "forest" to "Hutan", "water" to "Tasik dan sungai",
            "waterfall" to "Air terjun", "sunset" to "Matahari terbenam", "flower" to "Bunga",
            "ice" to "Ais dan glasier", "storm" to "Ribut", "phone" to "Telefon",
            "computer" to "Komputer", "tv" to "TV", "instrument" to "Alat muzik",
            "clock" to "Jam", "bag" to "Beg", "glasses" to "Cermin mata",
            "toy" to "Mainan", "shoes" to "Kasut", "hat" to "Topi",
            "dress" to "Pakaian wanita", "clothes" to "Pakaian", "jewellery" to "Perhiasan",
            "face" to "Wajah", "smile" to "Senyuman", "group" to "Kumpulan orang",
            "baby" to "Bayi", "crowd" to "Orang ramai dan pasukan", "text" to "Teks",
            "code" to "Kod QR dan bar"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Kenderaan", "Animals" to "Haiwan", "Nature" to "Alam semula jadi",
            "Places" to "Tempat dan bangunan", "Food" to "Makanan dan minuman", "Activities" to "Sukan dan acara",
            "Devices" to "Peranti dan barangan", "Fashion" to "Fesyen", "People" to "Orang dan teks"
        ),
        engines = mapOf(
            "google_lens" to "Mengecam objek, tempat dan teks serta menjejak asal usul imej",
            "yandex_images" to "Terbaik untuk mencari salinan imej dan babak yang serupa",
            "bing_visual" to "Imej yang kelihatan serupa dan produk yang boleh dibeli",
            "tineye" to "Menjejak tempat imej mula-mula muncul dan versi yang telah disunting",
            "google_images" to "Carian imej songsang klasik: ketik ikon kamera untuk memuat naik imej",
            "baidu_images" to "Sangat berkesan untuk tapak web, produk dan rangkaian sosial Asia",
            "saucenao" to "Mencari sumber anime, manga atau ilustrasi",
            "other_apps" to "Hantar imej ke mana-mana aplikasi carian imej yang dipasang"
        ),
        compare = mapOf(
            "stack" to "Bertindan", "side" to "Bersebelahan",
            "slider" to "Peluncur", "swap" to "Tukar",
            "fit" to "Muatkan", "zoomIn" to "Zum masuk",
            "zoomOut" to "Zum keluar", "resetZoom" to "Set semula zum",
            "hint" to "Cubit untuk zum · ketik dua kali", "loadFailed" to "Tidak dapat memuatkan imej",
            "divider" to "Pembahagi perbandingan", "diff" to "Perbezaan",
            "diffBusy" to "Mengira perbezaan…", "link" to "Pautkan zum",
            "unlink" to "Zum bebas", "actualSize" to "Piksel sebenar",
            "diffPercentPrefix" to "Berubah: "
        )
    )
}
