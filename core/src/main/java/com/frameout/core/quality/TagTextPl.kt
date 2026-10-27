package com.frameout.core.quality

internal object TagTextPl {
    val text = TagText(
        labels = TagLabelsPl.MAP,
        fixed = mapOf(
            TagRules.FACE to "Twarze", TagRules.GROUP to "Grupy osób",
            TagRules.SMILE to "Uśmiech", TagRules.EYES_CLOSED to "Zamknięte oczy",
            TagRules.GOOD_FACE to "Dobra twarz", TagRules.BODY to "Cała sylwetka",
            TagRules.TEXT to "Zawiera tekst", TagRules.CODE to "Dowolny kod",
            "b:contact" to "Kontakt", "b:email" to "E-mail",
            "b:isbn" to "ISBN", "b:phone" to "Telefon",
            "b:product" to "Kod produktu", "b:sms" to "SMS",
            "b:plain" to "Kod tekstowy", "b:url" to "Link",
            "b:wifi" to "Wi-Fi", "b:geo" to "Lokalizacja",
            "b:event" to "Wydarzenie", "b:license" to "Prawo jazdy",
            "o:fashion_good" to "Moda", "o:food" to "Jedzenie",
            "o:home_good" to "Artykuły domowe", "o:place" to "Miejsce",
            "o:plant" to "Rośliny"
        ),
        groups = mapOf(
            "People" to "Ludzie", "Things" to "Rzeczy i krajobrazy", "Categories" to "Kategorie",
            "Text" to "Tekst", "Codes" to "Kody"
        ),
        families = mapOf(
            "vehicles" to "Pojazdy", "animals" to "Zwierzęta", "people" to "Ludzie i ciało",
            "fashion" to "Moda i dodatki", "food" to "Jedzenie i napoje", "nature" to "Przyroda i krajobrazy",
            "plants" to "Rośliny i ogród", "sky" to "Niebo i pogoda", "places" to "Budynki i miejsca",
            "home" to "Dom i meble", "devices" to "Urządzenia i ekrany", "documents" to "Dokumenty i znaki",
            "music" to "Muzyka i taniec", "sports" to "Sport i aktywność na świeżym powietrzu", "events" to "Imprezy i wydarzenia",
            "toys" to "Zabawki i postacie", "crafts" to "Rękodzieło i sztuka", "objects" to "Przedmioty i narzędzia"
        ),
        contentTags = mapOf(
            "building" to "Budynki", "skyscraper" to "Wieżowce", "bridge" to "Mosty",
            "tower" to "Wieże", "mosque" to "Meczety", "church" to "Kościoły",
            "castle" to "Zamki i pałace", "landmark" to "Zabytki i atrakcje", "road" to "Drogi",
            "meal" to "Posiłki", "fastfood" to "Fast food", "sweets" to "Słodycze i desery",
            "bread" to "Pieczywo i wypieki", "fruit" to "Owoce", "vegetable" to "Warzywa",
            "coffee" to "Kawa", "drink" to "Napoje", "soccer" to "Piłka nożna",
            "swimming" to "Sporty wodne", "running" to "Bieganie i zawody", "dance" to "Taniec",
            "concert" to "Koncerty", "party" to "Imprezy", "wedding" to "Wesela",
            "fireworks" to "Fajerwerki", "fire" to "Ogień", "car" to "Samochody",
            "heavy" to "Furgonetki i ciężarówki", "bus" to "Autobusy", "motorcycle" to "Motocykle",
            "bicycle" to "Rowery", "train" to "Pociągi", "plane" to "Samoloty",
            "helicopter" to "Helikoptery", "boat" to "Łodzie", "dog" to "Psy",
            "cat" to "Koty", "bird" to "Ptaki", "horse" to "Konie",
            "cattle" to "Bydło", "bear" to "Niedźwiedzie", "reptile" to "Gady",
            "insect" to "Owady", "mountain" to "Góry", "beach" to "Plaże",
            "desert" to "Pustynie", "forest" to "Lasy", "water" to "Jeziora i rzeki",
            "waterfall" to "Wodospady", "sunset" to "Zachody słońca", "flower" to "Kwiaty",
            "ice" to "Lód i lodowce", "storm" to "Burze", "phone" to "Telefony",
            "computer" to "Komputery", "tv" to "Telewizory", "instrument" to "Instrumenty muzyczne",
            "clock" to "Zegary", "bag" to "Torby", "glasses" to "Okulary",
            "toy" to "Zabawki", "shoes" to "Buty", "hat" to "Kapelusze",
            "dress" to "Sukienki", "clothes" to "Odzież", "jewellery" to "Biżuteria",
            "face" to "Twarze", "smile" to "Uśmiechy", "group" to "Grupy osób",
            "baby" to "Niemowlęta", "crowd" to "Tłumy i drużyny", "text" to "Tekst",
            "code" to "Kody QR i kreskowe"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Pojazdy", "Animals" to "Zwierzęta", "Nature" to "Przyroda",
            "Places" to "Miejsca i budynki", "Food" to "Jedzenie i napoje", "Activities" to "Sport i wydarzenia",
            "Devices" to "Urządzenia i przedmioty", "Fashion" to "Moda", "People" to "Ludzie i tekst"
        ),
        engines = mapOf(
            "google_lens" to "Rozpoznaje obiekty, miejsca i tekst oraz znajduje źródło obrazu",
            "yandex_images" to "Najlepszy do znajdowania kopii obrazu i podobnych scen",
            "bing_visual" to "Wizualnie podobne obrazy i produkty, które można kupić",
            "tineye" to "Znajduje, gdzie obraz pojawił się po raz pierwszy, także jego edytowane wersje",
            "google_images" to "Klasyczne wyszukiwanie obrazem: dotknij ikony aparatu, aby przesłać obraz",
            "baidu_images" to "Mocny w serwisach azjatyckich, produktach i sieciach społecznościowych",
            "saucenao" to "Znajduje źródło anime, mangi lub ilustracji",
            "other_apps" to "Wyślij obraz do zainstalowanej aplikacji do wyszukiwania obrazem"
        ),
        compare = mapOf(
            "stack" to "Nałożone", "side" to "Obok siebie",
            "slider" to "Suwak", "swap" to "Zamień",
            "fit" to "Dopasuj", "zoomIn" to "Powiększ",
            "zoomOut" to "Pomniejsz", "resetZoom" to "Resetuj powiększenie",
            "hint" to "Uszczypnij, aby powiększyć · dotknij dwukrotnie", "loadFailed" to "Nie udało się wczytać obrazu",
            "divider" to "Linia podziału porównania", "diff" to "Różnica",
            "diffBusy" to "Obliczanie różnic…", "link" to "Połącz powiększenie",
            "unlink" to "Rozłącz powiększenie", "actualSize" to "Rzeczywiste piksele",
            "diffPercentPrefix" to "Zmieniono: "
        )
    )
}
