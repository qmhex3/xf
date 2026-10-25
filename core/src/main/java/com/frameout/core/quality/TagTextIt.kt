package com.frameout.core.quality

internal object TagTextIt {
    val text = TagText(
        labels = TagLabelsIt.MAP,
        fixed = mapOf(
            TagRules.FACE to "Volti", TagRules.GROUP to "Gruppo",
            TagRules.SMILE to "Sorriso", TagRules.EYES_CLOSED to "Occhi chiusi",
            TagRules.GOOD_FACE to "Volto ben riuscito", TagRules.BODY to "Figura intera",
            TagRules.TEXT to "Contiene testo", TagRules.CODE to "Qualsiasi codice",
            "b:contact" to "Contatto", "b:email" to "Email",
            "b:isbn" to "ISBN", "b:phone" to "Telefono",
            "b:product" to "Codice prodotto", "b:sms" to "SMS",
            "b:plain" to "Codice di testo", "b:url" to "Link",
            "b:wifi" to "Wi-Fi", "b:geo" to "Posizione",
            "b:event" to "Evento", "b:license" to "Patente di guida",
            "o:fashion_good" to "Moda", "o:food" to "Cibo",
            "o:home_good" to "Articoli per la casa", "o:place" to "Luoghi",
            "o:plant" to "Piante"
        ),
        groups = mapOf(
            "People" to "Persone", "Things" to "Oggetti e scene", "Categories" to "Categorie",
            "Text" to "Testo", "Codes" to "Codici"
        ),
        families = mapOf(
            "vehicles" to "Veicoli", "animals" to "Animali", "people" to "Persone e corpo",
            "fashion" to "Moda e accessori", "food" to "Cibo e bevande", "nature" to "Natura e paesaggi",
            "plants" to "Piante e giardini", "sky" to "Cielo e meteo", "places" to "Edifici e luoghi",
            "home" to "Casa e arredamento", "devices" to "Dispositivi e schermi", "documents" to "Documenti e lavagne",
            "music" to "Musica e danza", "sports" to "Sport e aria aperta", "events" to "Feste ed eventi",
            "toys" to "Giocattoli e statuine", "crafts" to "Arte e artigianato", "objects" to "Oggetti e attrezzi"
        ),
        contentTags = mapOf(
            "building" to "Edifici", "skyscraper" to "Grattacieli", "bridge" to "Ponti",
            "tower" to "Torri", "mosque" to "Moschee", "church" to "Chiese",
            "castle" to "Castelli e palazzi", "landmark" to "Monumenti", "road" to "Strade",
            "meal" to "Piatti", "fastfood" to "Fast food", "sweets" to "Dolci",
            "bread" to "Pane", "fruit" to "Frutta", "vegetable" to "Verdure",
            "coffee" to "Caffè", "drink" to "Bevande", "soccer" to "Calcio",
            "swimming" to "Sport acquatici", "running" to "Corse e gare", "dance" to "Danza",
            "concert" to "Concerti", "party" to "Feste", "wedding" to "Matrimoni",
            "fireworks" to "Fuochi d’artificio", "fire" to "Fuoco", "car" to "Auto",
            "heavy" to "Furgoni e camion", "bus" to "Autobus", "motorcycle" to "Moto",
            "bicycle" to "Biciclette", "train" to "Treni", "plane" to "Aerei",
            "helicopter" to "Elicotteri", "boat" to "Barche e navi", "dog" to "Cani",
            "cat" to "Gatti", "bird" to "Uccelli", "horse" to "Cavalli",
            "cattle" to "Bovini", "bear" to "Orsi", "reptile" to "Rettili",
            "insect" to "Insetti", "mountain" to "Montagne", "beach" to "Spiagge",
            "desert" to "Deserto", "forest" to "Foreste", "water" to "Laghi e fiumi",
            "waterfall" to "Cascate", "sunset" to "Tramonti", "flower" to "Fiori",
            "ice" to "Ghiaccio e ghiacciai", "storm" to "Temporali", "phone" to "Telefoni",
            "computer" to "Computer", "tv" to "Televisori", "instrument" to "Strumenti musicali",
            "clock" to "Orologi", "bag" to "Borse", "glasses" to "Occhiali",
            "toy" to "Giocattoli", "shoes" to "Scarpe", "hat" to "Cappelli",
            "dress" to "Abiti", "clothes" to "Abbigliamento", "jewellery" to "Gioielli",
            "face" to "Volti", "smile" to "Sorrisi", "group" to "Gruppi di persone",
            "baby" to "Neonati", "crowd" to "Folle e squadre", "text" to "Testo",
            "code" to "Codici QR e a barre"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Veicoli", "Animals" to "Animali", "Nature" to "Natura",
            "Places" to "Luoghi ed edifici", "Food" to "Cibo e bevande", "Activities" to "Sport ed eventi",
            "Devices" to "Dispositivi e oggetti", "Fashion" to "Moda", "People" to "Persone e testo"
        ),
        engines = mapOf(
            "google_lens" to "Riconosce oggetti, luoghi e testo e trova l’origine dell’immagine",
            "yandex_images" to "Ideale per trovare copie dell’immagine e scene simili",
            "bing_visual" to "Immagini visivamente simili e prodotti da acquistare",
            "tineye" to "Trova dove l’immagine è apparsa per la prima volta, e le versioni modificate",
            "google_images" to "Classica ricerca inversa di immagini: tocca l’icona della fotocamera per caricare l’immagine",
            "baidu_images" to "Molto efficace con siti web, prodotti e social network asiatici",
            "saucenao" to "Trova l’origine di un anime, un manga o un’illustrazione",
            "other_apps" to "Invia l’immagine a qualsiasi app installata per la ricerca di immagini"
        ),
        compare = mapOf(
            "stack" to "Sovrapposto", "side" to "Affiancato",
            "slider" to "Cursore", "swap" to "Scambia",
            "fit" to "Adatta", "zoomIn" to "Ingrandisci",
            "zoomOut" to "Riduci", "resetZoom" to "Reimposta zoom",
            "hint" to "Pizzica per ingrandire · tocca due volte", "loadFailed" to "Impossibile caricare l’immagine",
            "divider" to "Divisore di confronto", "diff" to "Differenza",
            "diffBusy" to "Calcolo delle differenze…", "link" to "Collega zoom",
            "unlink" to "Zoom indipendente", "actualSize" to "Pixel reali",
            "diffPercentPrefix" to "Modificato: "
        )
    )
}
