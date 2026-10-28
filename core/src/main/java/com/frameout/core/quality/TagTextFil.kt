package com.frameout.core.quality

internal object TagTextFil {
    val text = TagText(
        labels = TagLabelsFil.MAP,
        fixed = mapOf(
            TagRules.FACE to "Mga mukha", TagRules.GROUP to "Grupo",
            TagRules.SMILE to "Ngiti", TagRules.EYES_CLOSED to "Nakapikit na mata",
            TagRules.GOOD_FACE to "Magandang mukha", TagRules.BODY to "Buong katawan",
            TagRules.TEXT to "May teksto", TagRules.CODE to "Anumang code",
            "b:contact" to "Contact", "b:email" to "Email",
            "b:isbn" to "ISBN", "b:phone" to "Telepono",
            "b:product" to "Code ng produkto", "b:sms" to "SMS",
            "b:plain" to "Code na teksto", "b:url" to "Link",
            "b:wifi" to "Wi-Fi", "b:geo" to "Lokasyon",
            "b:event" to "Kaganapan", "b:license" to "Lisensya sa pagmamaneho",
            "o:fashion_good" to "Fashion", "o:food" to "Pagkain",
            "o:home_good" to "Gamit sa bahay", "o:place" to "Mga lugar",
            "o:plant" to "Mga halaman"
        ),
        groups = mapOf(
            "People" to "Mga tao", "Things" to "Mga bagay at kapaligiran", "Categories" to "Mga kategorya",
            "Text" to "Teksto", "Codes" to "Mga code"
        ),
        families = mapOf(
            "vehicles" to "Mga sasakyan", "animals" to "Mga hayop", "people" to "Mga tao at katawan",
            "fashion" to "Fashion at accessories", "food" to "Pagkain at inumin", "nature" to "Kalikasan at tanawin",
            "plants" to "Mga halaman at hardin", "sky" to "Langit at panahon", "places" to "Mga gusali at lugar",
            "home" to "Bahay at interior", "devices" to "Mga device at screen", "documents" to "Mga dokumento at board",
            "music" to "Musika at sayaw", "sports" to "Palakasan at outdoor", "events" to "Mga salu-salo at kaganapan",
            "toys" to "Mga laruan at karakter", "crafts" to "Sining at crafts", "objects" to "Mga bagay at kagamitan"
        ),
        contentTags = mapOf(
            "building" to "Mga gusali", "skyscraper" to "Mga skyscraper", "bridge" to "Mga tulay",
            "tower" to "Mga tore", "mosque" to "Mga moske", "church" to "Mga simbahan",
            "castle" to "Mga kastilyo at palasyo", "landmark" to "Mga landmark", "road" to "Mga kalsada",
            "meal" to "Mga putahe", "fastfood" to "Fast food", "sweets" to "Matatamis at kendi",
            "bread" to "Tinapay", "fruit" to "Prutas", "vegetable" to "Gulay",
            "coffee" to "Kape", "drink" to "Mga inumin", "soccer" to "Football",
            "swimming" to "Palakasang pantubig", "running" to "Pagtakbo at karera", "dance" to "Sayaw",
            "concert" to "Mga konsiyerto", "party" to "Mga salu-salo", "wedding" to "Mga kasalan",
            "fireworks" to "Mga paputok", "fire" to "Apoy", "car" to "Mga kotse",
            "heavy" to "Mga van at trak", "bus" to "Mga bus", "motorcycle" to "Mga motorsiklo",
            "bicycle" to "Mga bisikleta", "train" to "Mga tren", "plane" to "Mga eroplano",
            "helicopter" to "Mga helikopter", "boat" to "Mga bangka at barko", "dog" to "Mga aso",
            "cat" to "Mga pusa", "bird" to "Mga ibon", "horse" to "Mga kabayo",
            "cattle" to "Mga baka at kalabaw", "bear" to "Mga oso", "reptile" to "Mga reptilya",
            "insect" to "Mga insekto", "mountain" to "Mga bundok", "beach" to "Mga dalampasigan",
            "desert" to "Mga disyerto", "forest" to "Mga kagubatan", "water" to "Mga lawa at ilog",
            "waterfall" to "Mga talon", "sunset" to "Mga paglubog ng araw", "flower" to "Mga bulaklak",
            "ice" to "Yelo at glacier", "storm" to "Mga bagyo", "phone" to "Mga telepono",
            "computer" to "Mga computer", "tv" to "Mga TV", "instrument" to "Mga instrumentong pangmusika",
            "clock" to "Mga orasan", "bag" to "Mga bag", "glasses" to "Mga salamin sa mata",
            "toy" to "Mga laruan", "shoes" to "Mga sapatos", "hat" to "Mga sombrero",
            "dress" to "Mga bestida", "clothes" to "Mga damit", "jewellery" to "Mga alahas",
            "face" to "Mga mukha", "smile" to "Mga ngiti", "group" to "Mga grupo ng tao",
            "baby" to "Mga sanggol", "crowd" to "Mga madla at koponan", "text" to "Teksto",
            "code" to "Mga QR at barcode"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Mga sasakyan", "Animals" to "Mga hayop", "Nature" to "Kalikasan",
            "Places" to "Mga lugar at gusali", "Food" to "Pagkain at inumin", "Activities" to "Palakasan at kaganapan",
            "Devices" to "Mga device at bagay", "Fashion" to "Fashion", "People" to "Mga tao at teksto"
        ),
        engines = mapOf(
            "google_lens" to "Kumikilala ng mga bagay, lugar at teksto, at hinahanap ang pinagmulan ng larawan",
            "yandex_images" to "Pinakamahusay sa paghahanap ng mga kopya ng larawan at magkakahawig na eksena",
            "bing_visual" to "Mga larawang magkahawig sa itsura at mga produktong mabibili",
            "tineye" to "Hinahanap kung saan unang lumabas ang larawan, pati ang mga na-edit na bersyon",
            "google_images" to "Klasikong reverse image search: i-tap ang icon ng camera para mag-upload ng larawan",
            "baidu_images" to "Napakahusay para sa mga website, produkto at social network sa Asya",
            "saucenao" to "Hinahanap ang pinagmulan ng anime, manga o ilustrasyon",
            "other_apps" to "Ipadala ang larawan sa kahit anong naka-install na app sa paghahanap ng larawan"
        ),
        compare = mapOf(
            "stack" to "Patong-patong", "side" to "Magkatabi",
            "slider" to "Slider", "swap" to "Magpalit",
            "fit" to "Isukat", "zoomIn" to "Mag-zoom in",
            "zoomOut" to "Mag-zoom out", "resetZoom" to "I-reset ang zoom",
            "hint" to "Mag-pinch para mag-zoom · mag-double tap", "loadFailed" to "Hindi na-load ang larawan",
            "divider" to "Divider ng paghahambing", "diff" to "Pagkakaiba",
            "diffBusy" to "Kinukuwenta ang mga pagkakaiba…", "link" to "Ikonekta ang zoom",
            "unlink" to "Hiwalay na zoom", "actualSize" to "Totoong pixel",
            "diffPercentPrefix" to "Nagbago: "
        )
    )
}
