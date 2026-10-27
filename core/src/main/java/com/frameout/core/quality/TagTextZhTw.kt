package com.frameout.core.quality

internal object TagTextZhTw {
    val text = TagText(
        labels = TagLabelsZhTw.MAP,
        fixed = mapOf(
            TagRules.FACE to "人臉", TagRules.GROUP to "合影",
            TagRules.SMILE to "微笑", TagRules.EYES_CLOSED to "閉眼",
            TagRules.GOOD_FACE to "良好的臉", TagRules.BODY to "全身",
            TagRules.TEXT to "含文字", TagRules.CODE to "所有 QR Code／條碼",
            "b:contact" to "聯絡人", "b:email" to "電子郵件",
            "b:isbn" to "ISBN", "b:phone" to "電話號碼",
            "b:product" to "商品碼", "b:sms" to "簡訊",
            "b:plain" to "文字碼", "b:url" to "連結",
            "b:wifi" to "Wi-Fi", "b:geo" to "位置",
            "b:event" to "行程", "b:license" to "證件",
            "o:fashion_good" to "時尚", "o:food" to "食品",
            "o:home_good" to "家居用品", "o:place" to "場所",
            "o:plant" to "植物"
        ),
        groups = mapOf(
            "People" to "人物", "Things" to "物品和場景", "Categories" to "類別",
            "Text" to "文字", "Codes" to "QR Code 和條碼"
        ),
        families = mapOf(
            "vehicles" to "交通工具", "animals" to "動物", "people" to "人物和身體",
            "fashion" to "時尚和配飾", "food" to "食物和飲品", "nature" to "自然和風景",
            "plants" to "植物和花園", "sky" to "天空和天氣", "places" to "建築和場所",
            "home" to "家居和室內", "devices" to "裝置和螢幕", "documents" to "文件和白板",
            "music" to "音樂和舞蹈", "sports" to "運動和戶外", "events" to "節日和活動",
            "toys" to "玩具和角色", "crafts" to "藝術和手工", "objects" to "物品和工具"
        ),
        contentTags = mapOf(
            "building" to "建築", "skyscraper" to "摩天大樓", "bridge" to "橋",
            "tower" to "塔", "mosque" to "清真寺", "church" to "教堂",
            "castle" to "城堡", "landmark" to "地標", "road" to "道路",
            "meal" to "餐食", "fastfood" to "速食", "sweets" to "甜點",
            "bread" to "麵包", "fruit" to "水果", "vegetable" to "蔬菜",
            "coffee" to "咖啡", "drink" to "飲料", "soccer" to "足球",
            "swimming" to "水上運動", "running" to "跑步和田徑", "dance" to "舞蹈",
            "concert" to "音樂會", "party" to "派對", "wedding" to "婚禮",
            "fireworks" to "煙花", "fire" to "火", "car" to "汽車",
            "heavy" to "貨車和卡車", "bus" to "公車", "motorcycle" to "機車",
            "bicycle" to "腳踏車", "train" to "火車", "plane" to "飛機",
            "helicopter" to "直升機", "boat" to "船只", "dog" to "狗",
            "cat" to "貓", "bird" to "鳥", "horse" to "馬",
            "cattle" to "牛", "bear" to "熊", "reptile" to "爬行動物",
            "insect" to "昆蟲", "mountain" to "山", "beach" to "海灘",
            "desert" to "沙漠", "forest" to "森林", "water" to "湖泊和河流",
            "waterfall" to "瀑布", "sunset" to "日落", "flower" to "花",
            "ice" to "冰和冰川", "storm" to "風暴", "phone" to "手機",
            "computer" to "電腦", "tv" to "電視", "instrument" to "樂器",
            "clock" to "鐘表", "bag" to "包", "glasses" to "眼鏡",
            "toy" to "玩具", "shoes" to "鞋", "hat" to "帽子",
            "dress" to "連衣裙", "clothes" to "服裝", "jewellery" to "珠寶",
            "face" to "人臉", "smile" to "微笑", "group" to "合影",
            "baby" to "嬰兒", "crowd" to "人群和團隊", "text" to "文字",
            "code" to "QR Code 和條碼"
        ),
        contentGroups = mapOf(
            "Vehicles" to "交通工具", "Animals" to "動物", "Nature" to "自然",
            "Places" to "場所和建築", "Food" to "食物和飲品", "Activities" to "運動和活動",
            "Devices" to "裝置和物品", "Fashion" to "時尚", "People" to "人物和文字"
        ),
        engines = mapOf(
            "google_lens" to "辨識物體、地點和文字，並查找圖片的來源",
            "yandex_images" to "適合查找圖片的副本和相似場景",
            "bing_visual" to "查找相似圖片和可購買的商品",
            "tineye" to "查找圖片最早出現的位置以及經過編輯的版本",
            "google_images" to "傳統的以圖搜圖。點按相機圖標上傳圖片",
            "baidu_images" to "適合搜尋中文網站、商品和社交媒體",
            "saucenao" to "查找動漫、漫畫和插畫的出處",
            "other_apps" to "把圖片傳送到你已安裝的其他以圖搜圖應用程式"
        ),
        compare = mapOf(
            "stack" to "上下", "side" to "左右",
            "slider" to "滑桿", "swap" to "互換",
            "fit" to "適應螢幕", "zoomIn" to "放大",
            "zoomOut" to "縮小", "resetZoom" to "重置縮放",
            "hint" to "雙指縮放 · 雙擊", "loadFailed" to "無法載入圖片",
            "divider" to "對比分隔線", "diff" to "差異",
            "diffBusy" to "正在計算差異…", "link" to "同步縮放",
            "unlink" to "取消同步縮放", "actualSize" to "實際像素",
            "diffPercentPrefix" to "變化："
        )
    )
}
