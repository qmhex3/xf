package com.frameout.core.quality

internal object TagTextZh {
    val text = TagText(
        labels = TagLabelsZh.MAP,
        fixed = mapOf(
            TagRules.FACE to "人脸", TagRules.GROUP to "合影",
            TagRules.SMILE to "微笑", TagRules.EYES_CLOSED to "闭眼",
            TagRules.GOOD_FACE to "良好的脸", TagRules.BODY to "全身",
            TagRules.TEXT to "含文字", TagRules.CODE to "所有二维码/条码",
            "b:contact" to "联系人", "b:email" to "电子邮件",
            "b:isbn" to "ISBN", "b:phone" to "电话号码",
            "b:product" to "商品码", "b:sms" to "短信",
            "b:plain" to "文本码", "b:url" to "链接",
            "b:wifi" to "Wi-Fi", "b:geo" to "位置",
            "b:event" to "日程", "b:license" to "证件",
            "o:fashion_good" to "时尚", "o:food" to "食品",
            "o:home_good" to "家居用品", "o:place" to "场所",
            "o:plant" to "植物"
        ),
        groups = mapOf(
            "People" to "人物", "Things" to "物品和场景", "Categories" to "类别",
            "Text" to "文字", "Codes" to "二维码和条码"
        ),
        families = mapOf(
            "vehicles" to "交通工具", "animals" to "动物", "people" to "人物和身体",
            "fashion" to "时尚和配饰", "food" to "食物和饮品", "nature" to "自然和风景",
            "plants" to "植物和花园", "sky" to "天空和天气", "places" to "建筑和场所",
            "home" to "家居和室内", "devices" to "设备和屏幕", "documents" to "文档和白板",
            "music" to "音乐和舞蹈", "sports" to "运动和户外", "events" to "节日和活动",
            "toys" to "玩具和角色", "crafts" to "艺术和手工", "objects" to "物品和工具"
        ),
        contentTags = mapOf(
            "building" to "建筑", "skyscraper" to "摩天大楼", "bridge" to "桥",
            "tower" to "塔", "mosque" to "清真寺", "church" to "教堂",
            "castle" to "城堡", "landmark" to "地标", "road" to "道路",
            "meal" to "餐食", "fastfood" to "快餐", "sweets" to "甜点",
            "bread" to "面包", "fruit" to "水果", "vegetable" to "蔬菜",
            "coffee" to "咖啡", "drink" to "饮料", "soccer" to "足球",
            "swimming" to "水上运动", "running" to "跑步和田径", "dance" to "舞蹈",
            "concert" to "音乐会", "party" to "派对", "wedding" to "婚礼",
            "fireworks" to "烟花", "fire" to "火", "car" to "汽车",
            "heavy" to "货车和卡车", "bus" to "公交车", "motorcycle" to "摩托车",
            "bicycle" to "自行车", "train" to "火车", "plane" to "飞机",
            "helicopter" to "直升机", "boat" to "船只", "dog" to "狗",
            "cat" to "猫", "bird" to "鸟", "horse" to "马",
            "cattle" to "牛", "bear" to "熊", "reptile" to "爬行动物",
            "insect" to "昆虫", "mountain" to "山", "beach" to "海滩",
            "desert" to "沙漠", "forest" to "森林", "water" to "湖泊和河流",
            "waterfall" to "瀑布", "sunset" to "日落", "flower" to "花",
            "ice" to "冰和冰川", "storm" to "风暴", "phone" to "手机",
            "computer" to "电脑", "tv" to "电视", "instrument" to "乐器",
            "clock" to "钟表", "bag" to "包", "glasses" to "眼镜",
            "toy" to "玩具", "shoes" to "鞋", "hat" to "帽子",
            "dress" to "连衣裙", "clothes" to "服装", "jewellery" to "珠宝",
            "face" to "人脸", "smile" to "微笑", "group" to "合影",
            "baby" to "婴儿", "crowd" to "人群和团队", "text" to "文字",
            "code" to "二维码和条码"
        ),
        contentGroups = mapOf(
            "Vehicles" to "交通工具", "Animals" to "动物", "Nature" to "自然",
            "Places" to "场所和建筑", "Food" to "食物和饮品", "Activities" to "运动和活动",
            "Devices" to "设备和物品", "Fashion" to "时尚", "People" to "人物和文字"
        ),
        engines = mapOf(
            "google_lens" to "识别物体、地点和文字，并查找图片的来源",
            "yandex_images" to "适合查找图片的副本和相似场景",
            "bing_visual" to "查找相似图片和可购买的商品",
            "tineye" to "查找图片最早出现的位置以及经过编辑的版本",
            "google_images" to "传统的以图搜图。点按相机图标上传图片",
            "baidu_images" to "适合搜索中文网站、商品和社交媒体",
            "saucenao" to "查找动漫、漫画和插画的出处",
            "other_apps" to "把图片发送到你已安装的其他以图搜图应用"
        ),
        compare = mapOf(
            "stack" to "上下", "side" to "左右",
            "slider" to "滑块", "swap" to "互换",
            "fit" to "适应屏幕", "zoomIn" to "放大",
            "zoomOut" to "缩小", "resetZoom" to "重置缩放",
            "hint" to "双指缩放 · 双击", "loadFailed" to "无法加载图片",
            "divider" to "对比分隔线", "diff" to "差异",
            "diffBusy" to "正在计算差异…", "link" to "同步缩放",
            "unlink" to "取消同步缩放", "actualSize" to "实际像素",
            "diffPercentPrefix" to "变化："
        )
    )
}
