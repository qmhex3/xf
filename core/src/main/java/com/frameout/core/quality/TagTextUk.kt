package com.frameout.core.quality

internal object TagTextUk {
    val text = TagText(
        labels = TagLabelsUk.MAP,
        fixed = mapOf(
            TagRules.FACE to "Обличчя", TagRules.GROUP to "Група",
            TagRules.SMILE to "Усмішка", TagRules.EYES_CLOSED to "Заплющені очі",
            TagRules.GOOD_FACE to "Гарне обличчя", TagRules.BODY to "Людина в повний зріст",
            TagRules.TEXT to "Містить текст", TagRules.CODE to "Будь-який код",
            "b:contact" to "Контакт", "b:email" to "Електронна пошта",
            "b:isbn" to "ISBN", "b:phone" to "Телефон",
            "b:product" to "Код товару", "b:sms" to "SMS",
            "b:plain" to "Текстовий код", "b:url" to "Посилання",
            "b:wifi" to "Wi-Fi", "b:geo" to "Місцезнаходження",
            "b:event" to "Подія", "b:license" to "Водійське посвідчення",
            "o:fashion_good" to "Мода", "o:food" to "Їжа",
            "o:home_good" to "Товари для дому", "o:place" to "Місця",
            "o:plant" to "Рослини"
        ),
        groups = mapOf(
            "People" to "Люди", "Things" to "Предмети та сцени", "Categories" to "Категорії",
            "Text" to "Текст", "Codes" to "Коди"
        ),
        families = mapOf(
            "vehicles" to "Транспорт", "animals" to "Тварини", "people" to "Люди й тіло",
            "fashion" to "Мода й аксесуари", "food" to "Їжа й напої", "nature" to "Природа й краєвиди",
            "plants" to "Рослини й сади", "sky" to "Небо й погода", "places" to "Будівлі та місця",
            "home" to "Дім і декор", "devices" to "Пристрої й екрани", "documents" to "Документи й дошки",
            "music" to "Музика й танці", "sports" to "Спорт і відпочинок на природі", "events" to "Свята й події",
            "toys" to "Іграшки й фігурки", "crafts" to "Мистецтво й рукоділля", "objects" to "Предмети й інструменти"
        ),
        contentTags = mapOf(
            "building" to "Будівлі", "skyscraper" to "Хмарочоси", "bridge" to "Мости",
            "tower" to "Вежі", "mosque" to "Мечеті", "church" to "Церкви",
            "castle" to "Замки й палаци", "landmark" to "Визначні місця", "road" to "Дороги",
            "meal" to "Страви", "fastfood" to "Фастфуд", "sweets" to "Солодощі",
            "bread" to "Хліб", "fruit" to "Фрукти", "vegetable" to "Овочі",
            "coffee" to "Кава", "drink" to "Напої", "soccer" to "Футбол",
            "swimming" to "Водні види спорту", "running" to "Біг і змагання", "dance" to "Танці",
            "concert" to "Концерти", "party" to "Вечірки", "wedding" to "Весілля",
            "fireworks" to "Феєрверки", "fire" to "Вогонь", "car" to "Автомобілі",
            "heavy" to "Фургони й вантажівки", "bus" to "Автобуси", "motorcycle" to "Мотоцикли",
            "bicycle" to "Велосипеди", "train" to "Потяги", "plane" to "Літаки",
            "helicopter" to "Гелікоптери", "boat" to "Човни й кораблі", "dog" to "Собаки",
            "cat" to "Коти", "bird" to "Птахи", "horse" to "Коні",
            "cattle" to "Велика рогата худоба", "bear" to "Ведмеді", "reptile" to "Рептилії",
            "insect" to "Комахи", "mountain" to "Гори", "beach" to "Пляжі",
            "desert" to "Пустеля", "forest" to "Ліси", "water" to "Озера й річки",
            "waterfall" to "Водоспади", "sunset" to "Заходи сонця", "flower" to "Квіти",
            "ice" to "Лід і льодовики", "storm" to "Бурі", "phone" to "Телефони",
            "computer" to "Комп’ютери", "tv" to "Телевізори", "instrument" to "Музичні інструменти",
            "clock" to "Годинники", "bag" to "Сумки", "glasses" to "Окуляри",
            "toy" to "Іграшки", "shoes" to "Взуття", "hat" to "Капелюхи",
            "dress" to "Сукні", "clothes" to "Одяг", "jewellery" to "Прикраси",
            "face" to "Обличчя", "smile" to "Усмішки", "group" to "Групи людей",
            "baby" to "Немовлята", "crowd" to "Натовпи й команди", "text" to "Текст",
            "code" to "QR-коди та штрихкоди"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Транспорт", "Animals" to "Тварини", "Nature" to "Природа",
            "Places" to "Місця й будівлі", "Food" to "Їжа й напої", "Activities" to "Спорт і події",
            "Devices" to "Пристрої й предмети", "Fashion" to "Мода", "People" to "Люди й текст"
        ),
        engines = mapOf(
            "google_lens" to "Розпізнає об’єкти, місця й текст і знаходить джерело зображення",
            "yandex_images" to "Чудово підходить для пошуку копій зображення та схожих сцен",
            "bing_visual" to "Візуально схожі зображення та товари для купівлі",
            "tineye" to "Знаходить, де зображення з’явилося вперше, і його відредаговані версії",
            "google_images" to "Класичний зворотний пошук зображень: торкніться значка камери, щоб завантажити зображення",
            "baidu_images" to "Дуже добре працює з вебсайтами, товарами й соціальними мережами Азії",
            "saucenao" to "Знаходить джерело аніме, манги чи ілюстрації",
            "other_apps" to "Надсилає зображення в будь-який установлений застосунок для пошуку зображень"
        ),
        compare = mapOf(
            "stack" to "Накладання", "side" to "Поруч",
            "slider" to "Повзунок", "swap" to "Поміняти місцями",
            "fit" to "Вмістити", "zoomIn" to "Збільшити",
            "zoomOut" to "Зменшити", "resetZoom" to "Скинути масштаб",
            "hint" to "Розведіть пальці для масштабування · торкніться двічі", "loadFailed" to "Не вдалося завантажити зображення",
            "divider" to "Роздільник порівняння", "diff" to "Різниця",
            "diffBusy" to "Обчислення відмінностей…", "link" to "Зв’язати масштаб",
            "unlink" to "Незалежний масштаб", "actualSize" to "Справжні пікселі",
            "diffPercentPrefix" to "Змінено: "
        )
    )
}
