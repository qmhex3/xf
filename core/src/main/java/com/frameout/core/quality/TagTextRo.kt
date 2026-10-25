package com.frameout.core.quality

internal object TagTextRo {
    val text = TagText(
        labels = TagLabelsRo.MAP,
        fixed = mapOf(
            TagRules.FACE to "Fețe", TagRules.GROUP to "Grup",
            TagRules.SMILE to "Zâmbet", TagRules.EYES_CLOSED to "Ochi închiși",
            TagRules.GOOD_FACE to "Față bună", TagRules.BODY to "Corp întreg",
            TagRules.TEXT to "Conține text", TagRules.CODE to "Orice cod",
            "b:contact" to "Contact", "b:email" to "E-mail",
            "b:isbn" to "ISBN", "b:phone" to "Telefon",
            "b:product" to "Cod de produs", "b:sms" to "SMS",
            "b:plain" to "Cod text", "b:url" to "Link",
            "b:wifi" to "Wi-Fi", "b:geo" to "Locație",
            "b:event" to "Eveniment", "b:license" to "Permis de conducere",
            "o:fashion_good" to "Modă", "o:food" to "Alimente",
            "o:home_good" to "Articole de uz casnic", "o:place" to "Locuri",
            "o:plant" to "Plante"
        ),
        groups = mapOf(
            "People" to "Persoane", "Things" to "Obiecte și scene", "Categories" to "Categorii",
            "Text" to "Text", "Codes" to "Coduri"
        ),
        families = mapOf(
            "vehicles" to "Vehicule", "animals" to "Animale", "people" to "Persoane și corp",
            "fashion" to "Modă și accesorii", "food" to "Mâncare și băuturi", "nature" to "Natură și peisaje",
            "plants" to "Plante și grădini", "sky" to "Cer și vreme", "places" to "Clădiri și locuri",
            "home" to "Casă și amenajări", "devices" to "Dispozitive și ecrane", "documents" to "Documente și table",
            "music" to "Muzică și dans", "sports" to "Sport și aer liber", "events" to "Sărbători și evenimente",
            "toys" to "Jucării și figurine", "crafts" to "Artă și lucru manual", "objects" to "Obiecte și unelte"
        ),
        contentTags = mapOf(
            "building" to "Clădiri", "skyscraper" to "Zgârie-nori", "bridge" to "Poduri",
            "tower" to "Turnuri", "mosque" to "Moschei", "church" to "Biserici",
            "castle" to "Castele și palate", "landmark" to "Obiective turistice", "road" to "Drumuri",
            "meal" to "Preparate", "fastfood" to "Fast food", "sweets" to "Dulciuri",
            "bread" to "Pâine", "fruit" to "Fructe", "vegetable" to "Legume",
            "coffee" to "Cafea", "drink" to "Băuturi", "soccer" to "Fotbal",
            "swimming" to "Sporturi nautice", "running" to "Alergare și competiții", "dance" to "Dans",
            "concert" to "Concerte", "party" to "Petreceri", "wedding" to "Nunți",
            "fireworks" to "Artificii", "fire" to "Foc", "car" to "Mașini",
            "heavy" to "Camionete și camioane", "bus" to "Autobuze", "motorcycle" to "Motociclete",
            "bicycle" to "Biciclete", "train" to "Trenuri", "plane" to "Avioane",
            "helicopter" to "Elicoptere", "boat" to "Bărci și nave", "dog" to "Câini",
            "cat" to "Pisici", "bird" to "Păsări", "horse" to "Cai",
            "cattle" to "Bovine", "bear" to "Urși", "reptile" to "Reptile",
            "insect" to "Insecte", "mountain" to "Munți", "beach" to "Plaje",
            "desert" to "Deșert", "forest" to "Păduri", "water" to "Lacuri și râuri",
            "waterfall" to "Cascade", "sunset" to "Apusuri", "flower" to "Flori",
            "ice" to "Gheață și ghețari", "storm" to "Furtuni", "phone" to "Telefoane",
            "computer" to "Calculatoare", "tv" to "Televizoare", "instrument" to "Instrumente muzicale",
            "clock" to "Ceasuri", "bag" to "Genți", "glasses" to "Ochelari",
            "toy" to "Jucării", "shoes" to "Încălțăminte", "hat" to "Pălării",
            "dress" to "Rochii", "clothes" to "Îmbrăcăminte", "jewellery" to "Bijuterii",
            "face" to "Fețe", "smile" to "Zâmbete", "group" to "Grupuri de persoane",
            "baby" to "Bebeluși", "crowd" to "Mulțimi și echipe", "text" to "Text",
            "code" to "Coduri QR și coduri de bare"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Vehicule", "Animals" to "Animale", "Nature" to "Natură",
            "Places" to "Locuri și clădiri", "Food" to "Mâncare și băuturi", "Activities" to "Sport și evenimente",
            "Devices" to "Dispozitive și obiecte", "Fashion" to "Modă", "People" to "Persoane și text"
        ),
        engines = mapOf(
            "google_lens" to "Recunoaște obiecte, locuri și text și găsește sursa imaginii",
            "yandex_images" to "Ideal pentru a găsi copii ale imaginii și scene similare",
            "bing_visual" to "Imagini similare vizual și produse de cumpărat",
            "tineye" to "Găsește unde a apărut prima dată imaginea, precum și versiunile editate",
            "google_images" to "Căutare inversă clasică de imagini: atinge pictograma camerei pentru a încărca imaginea",
            "baidu_images" to "Puternic pentru site-uri asiatice, produse și rețele sociale",
            "saucenao" to "Găsește sursa unui anime, manga sau a unei ilustrații",
            "other_apps" to "Trimite imaginea către orice aplicație instalată pentru căutare de imagini"
        ),
        compare = mapOf(
            "stack" to "Suprapus", "side" to "Una lângă alta",
            "slider" to "Glisor", "swap" to "Schimbă",
            "fit" to "Potrivește", "zoomIn" to "Mărește",
            "zoomOut" to "Micșorează", "resetZoom" to "Resetează zoomul",
            "hint" to "Ciupește pentru zoom · atinge de două ori", "loadFailed" to "Imaginea nu a putut fi încărcată",
            "divider" to "Separator de comparare", "diff" to "Diferență",
            "diffBusy" to "Se calculează diferențele…", "link" to "Cuplează zoomul",
            "unlink" to "Zoom independent", "actualSize" to "Pixeli reali",
            "diffPercentPrefix" to "Modificat: "
        )
    )
}
