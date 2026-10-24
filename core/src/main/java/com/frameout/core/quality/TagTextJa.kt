package com.frameout.core.quality

internal object TagTextJa {
    val text = TagText(
        labels = TagLabelsJa.MAP,
        fixed = mapOf(
            TagRules.FACE to "顔", TagRules.GROUP to "集合",
            TagRules.SMILE to "笑顔", TagRules.EYES_CLOSED to "目を閉じている",
            TagRules.GOOD_FACE to "良い顔", TagRules.BODY to "全身",
            TagRules.TEXT to "テキストあり", TagRules.CODE to "すべてのコード",
            "b:contact" to "連絡先", "b:email" to "メール",
            "b:isbn" to "ISBN", "b:phone" to "電話番号",
            "b:product" to "商品コード", "b:sms" to "SMS",
            "b:plain" to "テキストコード", "b:url" to "リンク",
            "b:wifi" to "Wi-Fi", "b:geo" to "位置情報",
            "b:event" to "予定", "b:license" to "ライセンス",
            "o:fashion_good" to "ファッション", "o:food" to "食べ物",
            "o:home_good" to "生活用品", "o:place" to "場所",
            "o:plant" to "植物"
        ),
        groups = mapOf(
            "People" to "人物", "Things" to "モノと風景", "Categories" to "カテゴリ",
            "Text" to "テキスト", "Codes" to "コード"
        ),
        families = mapOf(
            "vehicles" to "乗り物", "animals" to "動物", "people" to "人物と体",
            "fashion" to "ファッションと小物", "food" to "食べ物と飲み物", "nature" to "自然と風景",
            "plants" to "植物と庭", "sky" to "空と天気", "places" to "建物と場所",
            "home" to "家とインテリア", "devices" to "デバイスと画面", "documents" to "書類とボード",
            "music" to "音楽とダンス", "sports" to "スポーツとアウトドア", "events" to "行事とイベント",
            "toys" to "おもちゃとキャラクター", "crafts" to "アートと工芸", "objects" to "物と道具"
        ),
        contentTags = mapOf(
            "building" to "建物", "skyscraper" to "高層ビル", "bridge" to "橋",
            "tower" to "塔", "mosque" to "モスク", "church" to "教会",
            "castle" to "城", "landmark" to "ランドマーク", "road" to "道路",
            "meal" to "食事", "fastfood" to "ファストフード", "sweets" to "スイーツ",
            "bread" to "パン", "fruit" to "果物", "vegetable" to "野菜",
            "coffee" to "コーヒー", "drink" to "飲み物", "soccer" to "サッカー",
            "swimming" to "水上スポーツ", "running" to "ランニングと競技", "dance" to "ダンス",
            "concert" to "コンサート", "party" to "パーティー", "wedding" to "結婚式",
            "fireworks" to "花火", "fire" to "火", "car" to "自動車",
            "heavy" to "バンとトラック", "bus" to "バス", "motorcycle" to "バイク",
            "bicycle" to "自転車", "train" to "電車", "plane" to "飛行機",
            "helicopter" to "ヘリコプター", "boat" to "ボートと船", "dog" to "犬",
            "cat" to "猫", "bird" to "鳥", "horse" to "馬",
            "cattle" to "牛", "bear" to "クマ", "reptile" to "爬虫類",
            "insect" to "昆虫", "mountain" to "山", "beach" to "ビーチ",
            "desert" to "砂漠", "forest" to "森", "water" to "湖と川",
            "waterfall" to "滝", "sunset" to "夕日", "flower" to "花",
            "ice" to "氷と氷河", "storm" to "嵐", "phone" to "スマホ",
            "computer" to "コンピューター", "tv" to "TV", "instrument" to "楽器",
            "clock" to "時計", "bag" to "バッグ", "glasses" to "メガネ",
            "toy" to "おもちゃ", "shoes" to "靴", "hat" to "帽子",
            "dress" to "ドレス", "clothes" to "衣類", "jewellery" to "ジュエリー",
            "face" to "顔", "smile" to "笑顔", "group" to "集合写真",
            "baby" to "赤ちゃん", "crowd" to "群衆とチーム", "text" to "テキスト",
            "code" to "QRコードとバーコード"
        ),
        contentGroups = mapOf(
            "Vehicles" to "乗り物", "Animals" to "動物", "Nature" to "自然",
            "Places" to "場所と建物", "Food" to "食べ物と飲み物", "Activities" to "スポーツとイベント",
            "Devices" to "デバイスと物", "Fashion" to "ファッション", "People" to "人物とテキスト"
        ),
        engines = mapOf(
            "google_lens" to "物、場所、テキストを認識し、画像の出典を探します",
            "yandex_images" to "画像のコピーや似たシーンを探すのに適しています",
            "bing_visual" to "似た画像や購入できる商品を探します",
            "tineye" to "画像が最初に登場した場所や加工されたバージョンを探します",
            "google_images" to "従来の逆画像検索です。カメラアイコンをタップして画像をアップロードします",
            "baidu_images" to "アジアのサイト、商品、SNSの検索に便利です",
            "saucenao" to "アニメ、マンガ、イラストの出典を探します",
            "other_apps" to "インストール済みの他の画像検索アプリに画像を送ります"
        ),
        compare = mapOf(
            "stack" to "上下", "side" to "左右",
            "slider" to "スライダー", "swap" to "入れ替え",
            "fit" to "全体表示", "zoomIn" to "拡大",
            "zoomOut" to "縮小", "resetZoom" to "拡大をリセット",
            "hint" to "2本指で拡大 · ダブルタップ", "loadFailed" to "画像を読み込めませんでした",
            "divider" to "比較の境界線", "diff" to "差分",
            "diffBusy" to "差分を計算中…", "link" to "拡大を連動",
            "unlink" to "拡大の連動を解除", "actualSize" to "実寸ピクセル",
            "diffPercentPrefix" to "変化: "
        )
    )
}
