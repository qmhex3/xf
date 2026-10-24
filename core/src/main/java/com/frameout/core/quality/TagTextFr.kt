package com.frameout.core.quality

internal object TagTextFr {
    val text = TagText(
        labels = TagLabelsFr.MAP,
        fixed = mapOf(
            TagRules.FACE to "Visages", TagRules.GROUP to "Groupe",
            TagRules.SMILE to "Sourire", TagRules.EYES_CLOSED to "Yeux fermés",
            TagRules.GOOD_FACE to "Visage correct", TagRules.BODY to "Corps entier",
            TagRules.TEXT to "Contient du texte", TagRules.CODE to "N’importe quel code",
            "b:contact" to "Contact", "b:email" to "E-mail",
            "b:isbn" to "ISBN", "b:phone" to "Téléphone",
            "b:product" to "Code produit", "b:sms" to "SMS",
            "b:plain" to "Code texte", "b:url" to "Lien",
            "b:wifi" to "Wi-Fi", "b:geo" to "Position",
            "b:event" to "Événement", "b:license" to "Permis",
            "o:fashion_good" to "Mode", "o:food" to "Produits alimentaires",
            "o:home_good" to "Articles pour la maison", "o:place" to "Lieux",
            "o:plant" to "Plantes"
        ),
        groups = mapOf(
            "People" to "Personnes", "Things" to "Objets et scènes", "Categories" to "Catégories",
            "Text" to "Texte", "Codes" to "Codes"
        ),
        families = mapOf(
            "vehicles" to "Véhicules", "animals" to "Animaux", "people" to "Personnes et corps",
            "fashion" to "Mode et accessoires", "food" to "Nourriture et boissons", "nature" to "Nature et paysages",
            "plants" to "Plantes et jardins", "sky" to "Ciel et météo", "places" to "Bâtiments et lieux",
            "home" to "Maison et intérieur", "devices" to "Appareils et écrans", "documents" to "Documents et tableaux",
            "music" to "Musique et danse", "sports" to "Sport et plein air", "events" to "Fêtes et événements",
            "toys" to "Jouets et personnages", "crafts" to "Arts et loisirs créatifs", "objects" to "Objets et outils"
        ),
        contentTags = mapOf(
            "building" to "Bâtiments", "skyscraper" to "Gratte-ciel", "bridge" to "Ponts",
            "tower" to "Tours", "mosque" to "Mosquées", "church" to "Églises",
            "castle" to "Châteaux et palais", "landmark" to "Monuments", "road" to "Routes",
            "meal" to "Plats", "fastfood" to "Restauration rapide", "sweets" to "Sucreries",
            "bread" to "Pain", "fruit" to "Fruits", "vegetable" to "Légumes",
            "coffee" to "Café", "drink" to "Boissons", "soccer" to "Football",
            "swimming" to "Sports nautiques", "running" to "Course et compétitions", "dance" to "Danse",
            "concert" to "Concerts", "party" to "Fêtes", "wedding" to "Mariages",
            "fireworks" to "Feux d’artifice", "fire" to "Feu", "car" to "Voitures",
            "heavy" to "Fourgons et camions", "bus" to "Bus", "motorcycle" to "Motos",
            "bicycle" to "Vélos", "train" to "Trains", "plane" to "Avions",
            "helicopter" to "Hélicoptères", "boat" to "Bateaux et navires", "dog" to "Chiens",
            "cat" to "Chats", "bird" to "Oiseaux", "horse" to "Chevaux",
            "cattle" to "Bovins", "bear" to "Ours", "reptile" to "Reptiles",
            "insect" to "Insectes", "mountain" to "Montagnes", "beach" to "Plages",
            "desert" to "Désert", "forest" to "Forêts", "water" to "Lacs et rivières",
            "waterfall" to "Cascades", "sunset" to "Couchers de soleil", "flower" to "Fleurs",
            "ice" to "Glace et glaciers", "storm" to "Tempêtes", "phone" to "Téléphones",
            "computer" to "Ordinateurs", "tv" to "Téléviseurs", "instrument" to "Instruments de musique",
            "clock" to "Horloges et montres", "bag" to "Sacs", "glasses" to "Lunettes",
            "toy" to "Jouets", "shoes" to "Chaussures", "hat" to "Chapeaux",
            "dress" to "Robes", "clothes" to "Vêtements", "jewellery" to "Bijoux",
            "face" to "Visages", "smile" to "Sourires", "group" to "Groupes de personnes",
            "baby" to "Bébés", "crowd" to "Foules et équipes", "text" to "Texte",
            "code" to "QR codes et codes-barres"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Véhicules", "Animals" to "Animaux", "Nature" to "Nature",
            "Places" to "Lieux et bâtiments", "Food" to "Nourriture et boissons", "Activities" to "Sport et événements",
            "Devices" to "Appareils et objets", "Fashion" to "Mode", "People" to "Personnes et texte"
        ),
        engines = mapOf(
            "google_lens" to "Reconnaît les objets, les lieux et le texte, et retrouve la source de l’image",
            "yandex_images" to "Idéal pour retrouver des copies de l’image et des scènes similaires",
            "bing_visual" to "Images visuellement similaires et produits à acheter",
            "tineye" to "Trouve où l’image est apparue pour la première fois, ainsi que ses versions modifiées",
            "google_images" to "Recherche d’image inversée classique : touchez l’icône d’appareil photo pour envoyer l’image",
            "baidu_images" to "Efficace pour les sites asiatiques, les produits et les réseaux sociaux",
            "saucenao" to "Trouve la source d’un animé, d’un manga ou d’une illustration",
            "other_apps" to "Envoyez l’image à n’importe quelle application de recherche d’images installée"
        ),
        compare = mapOf(
            "stack" to "Empilé", "side" to "Côte à côte",
            "slider" to "Curseur", "swap" to "Échanger",
            "fit" to "Ajuster", "zoomIn" to "Zoom avant",
            "zoomOut" to "Zoom arrière", "resetZoom" to "Réinitialiser le zoom",
            "hint" to "Pincez pour zoomer · touchez deux fois", "loadFailed" to "Impossible de charger l’image",
            "divider" to "Séparateur de comparaison", "diff" to "Différence",
            "diffBusy" to "Calcul des différences…", "link" to "Lier le zoom",
            "unlink" to "Zoom indépendant", "actualSize" to "Pixels réels",
            "diffPercentPrefix" to "Modifié\u00a0: "
        )
    )
}
