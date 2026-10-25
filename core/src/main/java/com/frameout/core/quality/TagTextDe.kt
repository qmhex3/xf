package com.frameout.core.quality

internal object TagTextDe {
    val text = TagText(
        labels = TagLabelsDe.MAP,
        fixed = mapOf(
            TagRules.FACE to "Gesichter", TagRules.GROUP to "Gruppe",
            TagRules.SMILE to "Lächeln", TagRules.EYES_CLOSED to "Geschlossene Augen",
            TagRules.GOOD_FACE to "Gutes Gesicht", TagRules.BODY to "Ganzer Körper",
            TagRules.TEXT to "Enthält Text", TagRules.CODE to "Beliebiger Code",
            "b:contact" to "Kontakt", "b:email" to "E-Mail",
            "b:isbn" to "ISBN", "b:phone" to "Telefon",
            "b:product" to "Produktcode", "b:sms" to "SMS",
            "b:plain" to "Textcode", "b:url" to "Link",
            "b:wifi" to "WLAN", "b:geo" to "Standort",
            "b:event" to "Termin", "b:license" to "Führerschein",
            "o:fashion_good" to "Mode", "o:food" to "Lebensmittel",
            "o:home_good" to "Haushaltswaren", "o:place" to "Orte",
            "o:plant" to "Pflanzen"
        ),
        groups = mapOf(
            "People" to "Personen", "Things" to "Objekte und Szenen", "Categories" to "Kategorien",
            "Text" to "Text", "Codes" to "Codes"
        ),
        families = mapOf(
            "vehicles" to "Fahrzeuge", "animals" to "Tiere", "people" to "Personen und Körper",
            "fashion" to "Mode und Accessoires", "food" to "Essen und Trinken", "nature" to "Natur und Landschaften",
            "plants" to "Pflanzen und Gärten", "sky" to "Himmel und Wetter", "places" to "Gebäude und Orte",
            "home" to "Zuhause und Einrichtung", "devices" to "Geräte und Bildschirme", "documents" to "Dokumente und Tafeln",
            "music" to "Musik und Tanz", "sports" to "Sport und Outdoor", "events" to "Feiern und Veranstaltungen",
            "toys" to "Spielzeug und Figuren", "crafts" to "Kunst und Basteln", "objects" to "Gegenstände und Werkzeuge"
        ),
        contentTags = mapOf(
            "building" to "Gebäude", "skyscraper" to "Wolkenkratzer", "bridge" to "Brücken",
            "tower" to "Türme", "mosque" to "Moscheen", "church" to "Kirchen",
            "castle" to "Burgen und Schlösser", "landmark" to "Sehenswürdigkeiten", "road" to "Straßen",
            "meal" to "Gerichte", "fastfood" to "Fast Food", "sweets" to "Süßigkeiten",
            "bread" to "Brot", "fruit" to "Obst", "vegetable" to "Gemüse",
            "coffee" to "Kaffee", "drink" to "Getränke", "soccer" to "Fußball",
            "swimming" to "Wassersport", "running" to "Laufen und Wettkämpfe", "dance" to "Tanz",
            "concert" to "Konzerte", "party" to "Partys", "wedding" to "Hochzeiten",
            "fireworks" to "Feuerwerk", "fire" to "Feuer", "car" to "Autos",
            "heavy" to "Transporter und Lkw", "bus" to "Busse", "motorcycle" to "Motorräder",
            "bicycle" to "Fahrräder", "train" to "Züge", "plane" to "Flugzeuge",
            "helicopter" to "Hubschrauber", "boat" to "Boote und Schiffe", "dog" to "Hunde",
            "cat" to "Katzen", "bird" to "Vögel", "horse" to "Pferde",
            "cattle" to "Rinder", "bear" to "Bären", "reptile" to "Reptilien",
            "insect" to "Insekten", "mountain" to "Berge", "beach" to "Strände",
            "desert" to "Wüste", "forest" to "Wälder", "water" to "Seen und Flüsse",
            "waterfall" to "Wasserfälle", "sunset" to "Sonnenuntergänge", "flower" to "Blumen",
            "ice" to "Eis und Gletscher", "storm" to "Stürme", "phone" to "Telefone",
            "computer" to "Computer", "tv" to "Fernseher", "instrument" to "Musikinstrumente",
            "clock" to "Uhren", "bag" to "Taschen", "glasses" to "Brillen",
            "toy" to "Spielzeug", "shoes" to "Schuhe", "hat" to "Hüte",
            "dress" to "Kleider", "clothes" to "Kleidung", "jewellery" to "Schmuck",
            "face" to "Gesichter", "smile" to "Lächeln", "group" to "Personengruppen",
            "baby" to "Babys", "crowd" to "Menschenmengen und Teams", "text" to "Text",
            "code" to "QR-Codes und Barcodes"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Fahrzeuge", "Animals" to "Tiere", "Nature" to "Natur",
            "Places" to "Orte und Gebäude", "Food" to "Essen und Trinken", "Activities" to "Sport und Veranstaltungen",
            "Devices" to "Geräte und Gegenstände", "Fashion" to "Mode", "People" to "Personen und Text"
        ),
        engines = mapOf(
            "google_lens" to "Erkennt Objekte, Orte und Text und findet die Quelle des Bildes",
            "yandex_images" to "Ideal, um Kopien des Bildes und ähnliche Szenen zu finden",
            "bing_visual" to "Visuell ähnliche Bilder und Produkte zum Kaufen",
            "tineye" to "Findet, wo das Bild zuerst erschienen ist, sowie bearbeitete Versionen",
            "google_images" to "Klassische umgekehrte Bildersuche: Tippe auf das Kamerasymbol, um das Bild hochzuladen",
            "baidu_images" to "Stark bei asiatischen Websites, Produkten und sozialen Netzwerken",
            "saucenao" to "Findet die Quelle eines Animes, Mangas oder einer Illustration",
            "other_apps" to "Sende das Bild an eine beliebige installierte App für die Bildersuche"
        ),
        compare = mapOf(
            "stack" to "Gestapelt", "side" to "Nebeneinander",
            "slider" to "Schieberegler", "swap" to "Tauschen",
            "fit" to "Einpassen", "zoomIn" to "Vergrößern",
            "zoomOut" to "Verkleinern", "resetZoom" to "Zoom zurücksetzen",
            "hint" to "Zum Zoomen zusammenziehen · doppeltippen", "loadFailed" to "Bild konnte nicht geladen werden",
            "divider" to "Vergleichstrenner", "diff" to "Differenz",
            "diffBusy" to "Unterschiede werden berechnet…", "link" to "Zoom koppeln",
            "unlink" to "Zoom unabhängig", "actualSize" to "Echte Pixel",
            "diffPercentPrefix" to "Geändert: "
        )
    )
}
