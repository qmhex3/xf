package com.frameout.core.quality

internal object TagTextPt {
    val text = TagText(
        labels = TagLabelsPt.MAP,
        fixed = mapOf(
            TagRules.FACE to "Rostos", TagRules.GROUP to "Grupo",
            TagRules.SMILE to "Sorriso", TagRules.EYES_CLOSED to "Olhos fechados",
            TagRules.GOOD_FACE to "Rosto aceitável", TagRules.BODY to "Corpo inteiro",
            TagRules.TEXT to "Contém texto", TagRules.CODE to "Qualquer código",
            "b:contact" to "Contato", "b:email" to "E-mail",
            "b:isbn" to "ISBN", "b:phone" to "Telefone",
            "b:product" to "Código de produto", "b:sms" to "SMS",
            "b:plain" to "Código de texto", "b:url" to "Link",
            "b:wifi" to "Wi-Fi", "b:geo" to "Localização",
            "b:event" to "Evento", "b:license" to "Licença",
            "o:fashion_good" to "Moda", "o:food" to "Produtos alimentícios",
            "o:home_good" to "Artigos para casa", "o:place" to "Lugares",
            "o:plant" to "Plantas"
        ),
        groups = mapOf(
            "People" to "Pessoas", "Things" to "Objetos e cenas", "Categories" to "Categorias",
            "Text" to "Texto", "Codes" to "Códigos"
        ),
        families = mapOf(
            "vehicles" to "Veículos", "animals" to "Animais", "people" to "Pessoas e corpo",
            "fashion" to "Moda e acessórios", "food" to "Comidas e bebidas", "nature" to "Natureza e paisagens",
            "plants" to "Plantas e jardins", "sky" to "Céu e clima", "places" to "Edifícios e lugares",
            "home" to "Casa e interiores", "devices" to "Aparelhos e telas", "documents" to "Documentos e quadros",
            "music" to "Música e dança", "sports" to "Esportes e ar livre", "events" to "Festas e eventos",
            "toys" to "Brinquedos e personagens", "crafts" to "Artes e artesanato", "objects" to "Objetos e ferramentas"
        ),
        contentTags = mapOf(
            "building" to "Edifícios", "skyscraper" to "Arranha-céus", "bridge" to "Pontes",
            "tower" to "Torres", "mosque" to "Mesquitas", "church" to "Igrejas",
            "castle" to "Castelos e palácios", "landmark" to "Monumentos", "road" to "Estradas",
            "meal" to "Pratos", "fastfood" to "Fast food", "sweets" to "Doces",
            "bread" to "Pão", "fruit" to "Frutas", "vegetable" to "Legumes",
            "coffee" to "Café", "drink" to "Bebidas", "soccer" to "Futebol",
            "swimming" to "Esportes aquáticos", "running" to "Corridas e competições", "dance" to "Dança",
            "concert" to "Shows", "party" to "Festas", "wedding" to "Casamentos",
            "fireworks" to "Fogos de artifício", "fire" to "Fogo", "car" to "Carros",
            "heavy" to "Vans e caminhões", "bus" to "Ônibus", "motorcycle" to "Motos",
            "bicycle" to "Bicicletas", "train" to "Trens", "plane" to "Aviões",
            "helicopter" to "Helicópteros", "boat" to "Barcos e navios", "dog" to "Cachorros",
            "cat" to "Gatos", "bird" to "Pássaros", "horse" to "Cavalos",
            "cattle" to "Gado bovino", "bear" to "Ursos", "reptile" to "Répteis",
            "insect" to "Insetos", "mountain" to "Montanhas", "beach" to "Praias",
            "desert" to "Deserto", "forest" to "Florestas", "water" to "Lagos e rios",
            "waterfall" to "Cachoeiras", "sunset" to "Pôr do sol", "flower" to "Flores",
            "ice" to "Gelo e geleiras", "storm" to "Tempestades", "phone" to "Telefones",
            "computer" to "Computadores", "tv" to "Televisores", "instrument" to "Instrumentos musicais",
            "clock" to "Relógios", "bag" to "Bolsas", "glasses" to "Óculos",
            "toy" to "Brinquedos", "shoes" to "Calçados", "hat" to "Chapéus",
            "dress" to "Vestidos", "clothes" to "Roupas", "jewellery" to "Joias",
            "face" to "Rostos", "smile" to "Sorrisos", "group" to "Grupos de pessoas",
            "baby" to "Bebês", "crowd" to "Multidões e equipes", "text" to "Texto",
            "code" to "QR codes e códigos de barras"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Veículos", "Animals" to "Animais", "Nature" to "Natureza",
            "Places" to "Lugares e edifícios", "Food" to "Comidas e bebidas", "Activities" to "Esportes e eventos",
            "Devices" to "Aparelhos e objetos", "Fashion" to "Moda", "People" to "Pessoas e texto"
        ),
        engines = mapOf(
            "google_lens" to "Reconhece objetos, lugares e texto, e encontra a origem da imagem",
            "yandex_images" to "Ótimo para encontrar cópias da imagem e cenas semelhantes",
            "bing_visual" to "Imagens visualmente semelhantes e produtos para comprar",
            "tineye" to "Descobre onde a imagem apareceu pela primeira vez, além de suas versões editadas",
            "google_images" to "Busca reversa de imagem clássica: toque no ícone da câmera para enviar a imagem",
            "baidu_images" to "Eficaz para sites asiáticos, produtos e redes sociais",
            "saucenao" to "Encontra a origem de um anime, mangá ou ilustração",
            "other_apps" to "Envie a imagem para qualquer app de busca de imagens instalado"
        ),
        compare = mapOf(
            "stack" to "Empilhado", "side" to "Lado a lado",
            "slider" to "Controle deslizante", "swap" to "Trocar",
            "fit" to "Ajustar", "zoomIn" to "Aumentar zoom",
            "zoomOut" to "Diminuir zoom", "resetZoom" to "Redefinir zoom",
            "hint" to "Pince para dar zoom · toque duas vezes", "loadFailed" to "Não foi possível carregar a imagem",
            "divider" to "Divisor de comparação", "diff" to "Diferença",
            "diffBusy" to "Calculando diferenças…", "link" to "Vincular zoom",
            "unlink" to "Zoom independente", "actualSize" to "Pixels reais",
            "diffPercentPrefix" to "Alterado: "
        )
    )
}
