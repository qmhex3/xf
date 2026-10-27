package com.frameout.core.quality

internal object TagTextId {
    val text = TagText(
        labels = TagLabelsId.MAP,
        fixed = mapOf(
            TagRules.FACE to "Wajah", TagRules.GROUP to "Kelompok",
            TagRules.SMILE to "Senyuman", TagRules.EYES_CLOSED to "Mata terpejam",
            TagRules.GOOD_FACE to "Wajah bagus", TagRules.BODY to "Seluruh tubuh",
            TagRules.TEXT to "Berisi teks", TagRules.CODE to "Kode apa pun",
            "b:contact" to "Kontak", "b:email" to "Email",
            "b:isbn" to "ISBN", "b:phone" to "Telepon",
            "b:product" to "Kode produk", "b:sms" to "SMS",
            "b:plain" to "Kode teks", "b:url" to "Tautan",
            "b:wifi" to "Wi-Fi", "b:geo" to "Lokasi",
            "b:event" to "Acara", "b:license" to "SIM",
            "o:fashion_good" to "Mode", "o:food" to "Makanan",
            "o:home_good" to "Perabot rumah tangga", "o:place" to "Tempat",
            "o:plant" to "Tumbuhan"
        ),
        groups = mapOf(
            "People" to "Orang", "Things" to "Benda dan pemandangan", "Categories" to "Kategori",
            "Text" to "Teks", "Codes" to "Kode"
        ),
        families = mapOf(
            "vehicles" to "Kendaraan", "animals" to "Hewan", "people" to "Orang dan tubuh",
            "fashion" to "Mode dan aksesori", "food" to "Makanan dan minuman", "nature" to "Alam dan pemandangan",
            "plants" to "Tumbuhan dan taman", "sky" to "Langit dan cuaca", "places" to "Bangunan dan tempat",
            "home" to "Rumah dan interior", "devices" to "Perangkat dan layar", "documents" to "Dokumen dan papan tulis",
            "music" to "Musik dan tari", "sports" to "Olahraga dan aktivitas luar ruang", "events" to "Pesta dan acara",
            "toys" to "Mainan dan boneka", "crafts" to "Seni dan kerajinan tangan", "objects" to "Barang dan peralatan"
        ),
        contentTags = mapOf(
            "building" to "Bangunan", "skyscraper" to "Pencakar langit", "bridge" to "Jembatan",
            "tower" to "Menara", "mosque" to "Masjid", "church" to "Gereja",
            "castle" to "Kastel dan benteng", "landmark" to "Landmark", "road" to "Jalan raya",
            "meal" to "Hidangan", "fastfood" to "Makanan cepat saji", "sweets" to "Camilan manis",
            "bread" to "Roti", "fruit" to "Buah-buahan", "vegetable" to "Sayuran",
            "coffee" to "Kopi", "drink" to "Minuman", "soccer" to "Sepak bola",
            "swimming" to "Olahraga air", "running" to "Lari dan perlombaan", "dance" to "Tari",
            "concert" to "Konser", "party" to "Pesta", "wedding" to "Pernikahan",
            "fireworks" to "Kembang api", "fire" to "Api", "car" to "Mobil",
            "heavy" to "Van dan truk", "bus" to "Bus", "motorcycle" to "Sepeda motor",
            "bicycle" to "Sepeda", "train" to "Kereta api", "plane" to "Pesawat",
            "helicopter" to "Helikopter", "boat" to "Perahu dan kapal", "dog" to "Anjing",
            "cat" to "Kucing", "bird" to "Burung", "horse" to "Kuda",
            "cattle" to "Sapi", "bear" to "Beruang", "reptile" to "Reptil",
            "insect" to "Serangga", "mountain" to "Gunung", "beach" to "Pantai",
            "desert" to "Gurun", "forest" to "Hutan", "water" to "Danau dan sungai",
            "waterfall" to "Air terjun", "sunset" to "Matahari terbenam", "flower" to "Bunga",
            "ice" to "Es dan gletser", "storm" to "Badai", "phone" to "Telepon",
            "computer" to "Komputer", "tv" to "TV", "instrument" to "Alat musik",
            "clock" to "Jam", "bag" to "Tas", "glasses" to "Kacamata",
            "toy" to "Mainan", "shoes" to "Sepatu", "hat" to "Topi",
            "dress" to "Pakaian wanita", "clothes" to "Pakaian", "jewellery" to "Perhiasan",
            "face" to "Wajah", "smile" to "Senyuman", "group" to "Kelompok orang",
            "baby" to "Bayi", "crowd" to "Kerumunan dan tim", "text" to "Teks",
            "code" to "Kode QR dan batang"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Kendaraan", "Animals" to "Hewan", "Nature" to "Alam",
            "Places" to "Tempat dan bangunan", "Food" to "Makanan dan minuman", "Activities" to "Olahraga dan acara",
            "Devices" to "Perangkat dan barang", "Fashion" to "Mode", "People" to "Orang dan teks"
        ),
        engines = mapOf(
            "google_lens" to "Mengenali objek, tempat, dan teks serta melacak asal gambar",
            "yandex_images" to "Terbaik untuk menemukan salinan gambar dan adegan serupa",
            "bing_visual" to "Gambar yang tampak mirip dan produk yang bisa dibeli",
            "tineye" to "Melacak kapan gambar pertama kali muncul dan versi yang telah diedit",
            "google_images" to "Pencarian gambar terbalik klasik: ketuk ikon kamera untuk mengunggah gambar",
            "baidu_images" to "Sangat efektif untuk situs web, produk, dan jejaring sosial Asia",
            "saucenao" to "Mencari sumber anime, manga, atau ilustrasi",
            "other_apps" to "Kirim gambar ke aplikasi pencarian gambar mana pun yang terpasang"
        ),
        compare = mapOf(
            "stack" to "Bertumpuk", "side" to "Berdampingan",
            "slider" to "Penggeser", "swap" to "Tukar",
            "fit" to "Pas layar", "zoomIn" to "Perbesar",
            "zoomOut" to "Perkecil", "resetZoom" to "Atur ulang zoom",
            "hint" to "Cubit untuk zoom · ketuk dua kali", "loadFailed" to "Tidak dapat memuat gambar",
            "divider" to "Pembagi perbandingan", "diff" to "Perbedaan",
            "diffBusy" to "Menghitung perbedaan…", "link" to "Tautkan zoom",
            "unlink" to "Zoom independen", "actualSize" to "Piksel asli",
            "diffPercentPrefix" to "Berubah: "
        )
    )
}
