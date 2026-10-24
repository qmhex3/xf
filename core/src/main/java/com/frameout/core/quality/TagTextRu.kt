package com.frameout.core.quality

internal object TagTextRu {
    val text = TagText(
        labels = TagLabelsRu.MAP,
        fixed = mapOf(
            TagRules.FACE to "Лица", TagRules.GROUP to "Группа",
            TagRules.SMILE to "Улыбка", TagRules.EYES_CLOSED to "Глаза закрыты",
            TagRules.GOOD_FACE to "Хорошее лицо", TagRules.BODY to "Тело",
            TagRules.TEXT to "Есть текст", TagRules.CODE to "Любой код",
            "b:contact" to "Контакт", "b:email" to "Эл. почта",
            "b:isbn" to "ISBN", "b:phone" to "Телефон",
            "b:product" to "Код товара", "b:sms" to "SMS",
            "b:plain" to "Текстовый код", "b:url" to "Ссылка",
            "b:wifi" to "Wi-Fi", "b:geo" to "Местоположение",
            "b:event" to "Событие", "b:license" to "Лицензия",
            "o:fashion_good" to "Мода", "o:food" to "Продукты",
            "o:home_good" to "Товары для дома", "o:place" to "Места",
            "o:plant" to "Растения"
        ),
        groups = mapOf(
            "People" to "Люди", "Things" to "Предметы и сцены", "Categories" to "Категории",
            "Text" to "Текст", "Codes" to "Коды"
        ),
        families = mapOf(
            "vehicles" to "Транспорт", "animals" to "Животные", "people" to "Люди и тело",
            "fashion" to "Мода и аксессуары", "food" to "Еда и напитки", "nature" to "Природа и пейзажи",
            "plants" to "Растения и сады", "sky" to "Небо и погода", "places" to "Здания и места",
            "home" to "Дом и интерьер", "devices" to "Устройства и экраны", "documents" to "Документы и доски",
            "music" to "Музыка и танцы", "sports" to "Спорт и активный отдых", "events" to "Праздники и события",
            "toys" to "Игрушки и персонажи", "crafts" to "Искусство и рукоделие", "objects" to "Вещи и инструменты"
        ),
        contentTags = mapOf(
            "building" to "Здания", "skyscraper" to "Небоскрёбы", "bridge" to "Мосты",
            "tower" to "Башни", "mosque" to "Мечети", "church" to "Церкви",
            "castle" to "Замки и дворцы", "landmark" to "Достопримечательности", "road" to "Дороги",
            "meal" to "Блюда", "fastfood" to "Фастфуд", "sweets" to "Сладости",
            "bread" to "Хлеб", "fruit" to "Фрукты", "vegetable" to "Овощи",
            "coffee" to "Кофе", "drink" to "Напитки", "soccer" to "Футбол",
            "swimming" to "Водный спорт", "running" to "Бег и гонки", "dance" to "Танцы",
            "concert" to "Концерты", "party" to "Вечеринки", "wedding" to "Свадьбы",
            "fireworks" to "Фейерверки", "fire" to "Огонь", "car" to "Автомобили",
            "heavy" to "Фургоны и грузовики", "bus" to "Автобусы", "motorcycle" to "Мотоциклы",
            "bicycle" to "Велосипеды", "train" to "Поезда", "plane" to "Самолёты",
            "helicopter" to "Вертолёты", "boat" to "Лодки и корабли", "dog" to "Собаки",
            "cat" to "Кошки", "bird" to "Птицы", "horse" to "Лошади",
            "cattle" to "Крупный рогатый скот", "bear" to "Медведи", "reptile" to "Рептилии",
            "insect" to "Насекомые", "mountain" to "Горы", "beach" to "Пляжи",
            "desert" to "Пустыня", "forest" to "Леса", "water" to "Озёра и реки",
            "waterfall" to "Водопады", "sunset" to "Закаты", "flower" to "Цветы",
            "ice" to "Лёд и ледники", "storm" to "Бури", "phone" to "Телефоны",
            "computer" to "Компьютеры", "tv" to "Телевизоры", "instrument" to "Музыкальные инструменты",
            "clock" to "Часы", "bag" to "Сумки", "glasses" to "Очки",
            "toy" to "Игрушки", "shoes" to "Обувь", "hat" to "Шляпы",
            "dress" to "Платья", "clothes" to "Одежда", "jewellery" to "Украшения",
            "face" to "Лица", "smile" to "Улыбки", "group" to "Группы людей",
            "baby" to "Младенцы", "crowd" to "Толпы и команды", "text" to "Текст",
            "code" to "QR и штрихкоды"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Транспорт", "Animals" to "Животные", "Nature" to "Природа",
            "Places" to "Места и здания", "Food" to "Еда и напитки", "Activities" to "Спорт и события",
            "Devices" to "Устройства и вещи", "Fashion" to "Мода", "People" to "Люди и текст"
        ),
        engines = mapOf(
            "google_lens" to "Распознаёт объекты, места и текст и находит источник изображения",
            "yandex_images" to "Лучше всего находит копии изображения и похожие сцены",
            "bing_visual" to "Визуально похожие изображения и товары для покупки",
            "tineye" to "Находит, где изображение появилось впервые, и его изменённые версии",
            "google_images" to "Классический обратный поиск по картинке: нажмите значок камеры, чтобы загрузить",
            "baidu_images" to "Хорош для азиатских сайтов, товаров и соцсетей",
            "saucenao" to "Находит источник аниме, манги и артов",
            "other_apps" to "Отправьте кадр в любое установленное приложение для поиска по изображениям"
        ),
        compare = mapOf(
            "stack" to "Столбец", "side" to "Рядом",
            "slider" to "Ползунок", "swap" to "Поменять",
            "fit" to "По размеру", "zoomIn" to "Приблизить",
            "zoomOut" to "Отдалить", "resetZoom" to "Сбросить масштаб",
            "hint" to "Сведите пальцы для масштаба · дважды коснитесь", "loadFailed" to "Не удалось загрузить изображение",
            "divider" to "Разделитель сравнения", "diff" to "Разница",
            "diffBusy" to "Вычисление различий…", "link" to "Связать масштаб",
            "unlink" to "Независимый масштаб", "actualSize" to "Реальные пиксели",
            "diffPercentPrefix" to "Изменено: "
        )
    )
}
