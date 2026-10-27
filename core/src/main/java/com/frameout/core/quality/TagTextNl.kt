package com.frameout.core.quality

internal object TagTextNl {
    val text = TagText(
        labels = TagLabelsNl.MAP,
        fixed = mapOf(
            TagRules.FACE to "Gezichten", TagRules.GROUP to "Groepen mensen",
            TagRules.SMILE to "Glimlach", TagRules.EYES_CLOSED to "Ogen dicht",
            TagRules.GOOD_FACE to "Goed gezicht", TagRules.BODY to "Heel lichaam",
            TagRules.TEXT to "Bevat tekst", TagRules.CODE to "Elke code",
            "b:contact" to "Contact", "b:email" to "E-mail",
            "b:isbn" to "ISBN", "b:phone" to "Telefoon",
            "b:product" to "Productcode", "b:sms" to "SMS",
            "b:plain" to "Tekstcode", "b:url" to "Link",
            "b:wifi" to "Wi-Fi", "b:geo" to "Locatie",
            "b:event" to "Evenement", "b:license" to "Rijbewijs",
            "o:fashion_good" to "Mode", "o:food" to "Eten",
            "o:home_good" to "Huishoudelijke artikelen", "o:place" to "Plaats",
            "o:plant" to "Planten"
        ),
        groups = mapOf(
            "People" to "Mensen", "Things" to "Dingen en landschappen", "Categories" to "Categorieën",
            "Text" to "Tekst", "Codes" to "Codes"
        ),
        families = mapOf(
            "vehicles" to "Voertuigen", "animals" to "Dieren", "people" to "Mensen en lichaam",
            "fashion" to "Mode en accessoires", "food" to "Eten en drinken", "nature" to "Natuur en landschappen",
            "plants" to "Planten en tuin", "sky" to "Lucht en weer", "places" to "Gebouwen en plaatsen",
            "home" to "Huis en meubels", "devices" to "Apparaten en schermen", "documents" to "Documenten en borden",
            "music" to "Muziek en dans", "sports" to "Sport en buitenactiviteiten", "events" to "Feesten en evenementen",
            "toys" to "Speelgoed en personages", "crafts" to "Handwerk en kunst", "objects" to "Voorwerpen en gereedschap"
        ),
        contentTags = mapOf(
            "building" to "Gebouwen", "skyscraper" to "Wolkenkrabbers", "bridge" to "Bruggen",
            "tower" to "Torens", "mosque" to "Moskeeën", "church" to "Kerken",
            "castle" to "Kastelen en paleizen", "landmark" to "Bezienswaardigheden en monumenten", "road" to "Wegen",
            "meal" to "Maaltijden", "fastfood" to "Fastfood", "sweets" to "Snoep en desserts",
            "bread" to "Brood en gebak", "fruit" to "Fruit", "vegetable" to "Groenten",
            "coffee" to "Koffie", "drink" to "Dranken", "soccer" to "Voetbal",
            "swimming" to "Watersport", "running" to "Hardlopen en wedstrijden", "dance" to "Dans",
            "concert" to "Concerten", "party" to "Feesten", "wedding" to "Bruiloften",
            "fireworks" to "Vuurwerk", "fire" to "Vuur", "car" to "Auto's",
            "heavy" to "Bestelwagens en vrachtwagens", "bus" to "Bussen", "motorcycle" to "Motoren",
            "bicycle" to "Fietsen", "train" to "Treinen", "plane" to "Vliegtuigen",
            "helicopter" to "Helikopters", "boat" to "Boten", "dog" to "Honden",
            "cat" to "Katten", "bird" to "Vogels", "horse" to "Paarden",
            "cattle" to "Vee", "bear" to "Beren", "reptile" to "Reptielen",
            "insect" to "Insecten", "mountain" to "Bergen", "beach" to "Stranden",
            "desert" to "Woestijnen", "forest" to "Bossen", "water" to "Meren en rivieren",
            "waterfall" to "Watervallen", "sunset" to "Zonsondergangen", "flower" to "Bloemen",
            "ice" to "IJs en gletsjers", "storm" to "Stormen", "phone" to "Telefoons",
            "computer" to "Computers", "tv" to "Televisies", "instrument" to "Muziekinstrumenten",
            "clock" to "Klokken", "bag" to "Tassen", "glasses" to "Brillen",
            "toy" to "Speelgoed", "shoes" to "Schoenen", "hat" to "Hoeden",
            "dress" to "Jurken", "clothes" to "Kleding", "jewellery" to "Sieraden",
            "face" to "Gezichten", "smile" to "Glimlachen", "group" to "Groepen mensen",
            "baby" to "Baby's", "crowd" to "Menigten en teams", "text" to "Tekst",
            "code" to "QR- en streepjescodes"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Voertuigen", "Animals" to "Dieren", "Nature" to "Natuur",
            "Places" to "Plaatsen en gebouwen", "Food" to "Eten en drinken", "Activities" to "Sport en evenementen",
            "Devices" to "Apparaten en voorwerpen", "Fashion" to "Mode", "People" to "Mensen en tekst"
        ),
        engines = mapOf(
            "google_lens" to "Herkent objecten, plaatsen en tekst en vindt de oorsprong van de afbeelding",
            "yandex_images" to "Het best om kopieën van de afbeelding en vergelijkbare scènes te vinden",
            "bing_visual" to "Visueel vergelijkbare afbeeldingen en producten die je kunt kopen",
            "tineye" to "Vindt waar de afbeelding voor het eerst verscheen, ook bewerkte versies",
            "google_images" to "Klassiek zoeken op afbeelding: tik op het camerapictogram om de afbeelding te uploaden",
            "baidu_images" to "Sterk voor Aziatische sites, producten en sociale netwerken",
            "saucenao" to "Vindt de bron van een anime, manga of illustratie",
            "other_apps" to "Stuur de afbeelding naar een geïnstalleerde app voor zoeken op afbeelding"
        ),
        compare = mapOf(
            "stack" to "Gestapeld", "side" to "Naast elkaar",
            "slider" to "Schuifregelaar", "swap" to "Wisselen",
            "fit" to "Passend", "zoomIn" to "Inzoomen",
            "zoomOut" to "Uitzoomen", "resetZoom" to "Zoom herstellen",
            "hint" to "Knijp om te zoomen · dubbeltik", "loadFailed" to "Afbeelding kon niet worden geladen",
            "divider" to "Scheidingslijn voor vergelijking", "diff" to "Verschil",
            "diffBusy" to "Verschillen berekenen…", "link" to "Zoom koppelen",
            "unlink" to "Zoom ontkoppelen", "actualSize" to "Werkelijke pixels",
            "diffPercentPrefix" to "Gewijzigd: "
        )
    )
}
