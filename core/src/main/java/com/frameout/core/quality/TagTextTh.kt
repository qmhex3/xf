package com.frameout.core.quality

internal object TagTextTh {
    val text = TagText(
        labels = TagLabelsTh.MAP,
        fixed = mapOf(
            TagRules.FACE to "ใบหน้า", TagRules.GROUP to "กลุ่มคน",
            TagRules.SMILE to "รอยยิ้ม", TagRules.EYES_CLOSED to "หลับตา",
            TagRules.GOOD_FACE to "ใบหน้าที่ดี", TagRules.BODY to "เต็มตัว",
            TagRules.TEXT to "มีข้อความ", TagRules.CODE to "โค้ดทุกชนิด",
            "b:contact" to "ผู้ติดต่อ", "b:email" to "อีเมล",
            "b:isbn" to "ISBN", "b:phone" to "โทรศัพท์",
            "b:product" to "รหัสสินค้า", "b:sms" to "SMS",
            "b:plain" to "โค้ดข้อความ", "b:url" to "ลิงก์",
            "b:wifi" to "Wi-Fi", "b:geo" to "ตำแหน่ง",
            "b:event" to "กิจกรรม", "b:license" to "ใบอนุญาต",
            "o:fashion_good" to "แฟชั่น", "o:food" to "อาหาร",
            "o:home_good" to "ของใช้ในบ้าน", "o:place" to "สถานที่",
            "o:plant" to "พืช"
        ),
        groups = mapOf(
            "People" to "ผู้คน", "Things" to "สิ่งของและทิวทัศน์", "Categories" to "หมวดหมู่",
            "Text" to "ข้อความ", "Codes" to "โค้ด"
        ),
        families = mapOf(
            "vehicles" to "ยานพาหนะ", "animals" to "สัตว์", "people" to "ผู้คนและร่างกาย",
            "fashion" to "แฟชั่นและเครื่องประดับ", "food" to "อาหารและเครื่องดื่ม", "nature" to "ธรรมชาติและทิวทัศน์",
            "plants" to "พืชและสวน", "sky" to "ท้องฟ้าและสภาพอากาศ", "places" to "อาคารและสถานที่",
            "home" to "บ้านและการตกแต่งภายใน", "devices" to "อุปกรณ์และหน้าจอ", "documents" to "เอกสารและกระดาน",
            "music" to "ดนตรีและการเต้นรำ", "sports" to "กีฬาและกิจกรรมกลางแจ้ง", "events" to "เทศกาลและงานเลี้ยง",
            "toys" to "ของเล่นและตัวละคร", "crafts" to "งานฝีมือและศิลปะ", "objects" to "ข้าวของและเครื่องมือ"
        ),
        contentTags = mapOf(
            "building" to "อาคาร", "skyscraper" to "ตึกระฟ้า", "bridge" to "สะพาน",
            "tower" to "หอคอย", "mosque" to "มัสยิด", "church" to "โบสถ์",
            "castle" to "ปราสาทและพระราชวัง", "landmark" to "สถานที่สำคัญและแลนด์มาร์ก", "road" to "ถนน",
            "meal" to "มื้ออาหาร", "fastfood" to "ฟาสต์ฟู้ด", "sweets" to "ขนมหวาน",
            "bread" to "ขนมปัง", "fruit" to "ผลไม้", "vegetable" to "ผัก",
            "coffee" to "กาแฟ", "drink" to "เครื่องดื่ม", "soccer" to "ฟุตบอล",
            "swimming" to "กีฬาทางน้ำ", "running" to "วิ่งและแข่งขัน", "dance" to "เต้นรำ",
            "concert" to "คอนเสิร์ต", "party" to "ปาร์ตี้", "wedding" to "งานแต่งงาน",
            "fireworks" to "พลุ", "fire" to "ไฟ", "car" to "รถยนต์",
            "heavy" to "รถตู้และรถบรรทุก", "bus" to "รถบัส", "motorcycle" to "มอเตอร์ไซค์",
            "bicycle" to "จักรยาน", "train" to "รถไฟ", "plane" to "เครื่องบิน",
            "helicopter" to "เฮลิคอปเตอร์", "boat" to "เรือ", "dog" to "สุนัข",
            "cat" to "แมว", "bird" to "นก", "horse" to "ม้า",
            "cattle" to "ปศุสัตว์", "bear" to "หมี", "reptile" to "สัตว์เลื้อยคลาน",
            "insect" to "แมลง", "mountain" to "ภูเขา", "beach" to "ชายหาด",
            "desert" to "ทะเลทราย", "forest" to "ป่า", "water" to "ทะเลสาบและแม่น้ำ",
            "waterfall" to "น้ำตก", "sunset" to "พระอาทิตย์ตก", "flower" to "ดอกไม้",
            "ice" to "น้ำแข็งและธารน้ำแข็ง", "storm" to "พายุ", "phone" to "โทรศัพท์",
            "computer" to "คอมพิวเตอร์", "tv" to "ทีวี", "instrument" to "เครื่องดนตรี",
            "clock" to "นาฬิกา", "bag" to "กระเป๋า", "glasses" to "แว่นตา",
            "toy" to "ของเล่น", "shoes" to "รองเท้า", "hat" to "หมวก",
            "dress" to "ชุดเดรส", "clothes" to "เสื้อผ้า", "jewellery" to "เครื่องประดับ",
            "face" to "ใบหน้า", "smile" to "รอยยิ้ม", "group" to "กลุ่มคน",
            "baby" to "ทารก", "crowd" to "ฝูงชนและกลุ่มคน", "text" to "ข้อความ",
            "code" to "QR โค้ดและบาร์โค้ด"
        ),
        contentGroups = mapOf(
            "Vehicles" to "ยานพาหนะ", "Animals" to "สัตว์", "Nature" to "ธรรมชาติ",
            "Places" to "สถานที่และอาคาร", "Food" to "อาหารและเครื่องดื่ม", "Activities" to "กีฬาและงานกิจกรรม",
            "Devices" to "อุปกรณ์และข้าวของ", "Fashion" to "แฟชั่น", "People" to "ผู้คนและข้อความ"
        ),
        engines = mapOf(
            "google_lens" to "ระบุวัตถุ สถานที่ และข้อความ พร้อมค้นหาต้นทางของภาพ",
            "yandex_images" to "เหมาะกับการหาภาพที่ถูกคัดลอกและฉากที่คล้ายกัน",
            "bing_visual" to "ภาพที่หน้าตาคล้ายกันและสินค้าที่ซื้อได้",
            "tineye" to "ค้นหาว่าภาพนี้ปรากฏครั้งแรกที่ไหน รวมถึงเวอร์ชันที่ถูกแก้ไข",
            "google_images" to "การค้นหาด้วยภาพแบบดั้งเดิม: แตะไอคอนกล้องเพื่ออัปโหลดภาพ",
            "baidu_images" to "เหมาะกับเว็บไซต์เอเชีย สินค้า และโซเชียลมีเดีย",
            "saucenao" to "ค้นหาต้นทางของอนิเมะ มังงะ หรือภาพประกอบ",
            "other_apps" to "ส่งภาพไปยังแอปค้นหาภาพที่ติดตั้งไว้ได้ทุกแอป"
        ),
        compare = mapOf(
            "stack" to "ซ้อนบนล่าง", "side" to "เคียงข้างกัน",
            "slider" to "แถบเลื่อน", "swap" to "สลับ",
            "fit" to "พอดี", "zoomIn" to "ซูมเข้า",
            "zoomOut" to "ซูมออก", "resetZoom" to "รีเซ็ตการซูม",
            "hint" to "ใช้สองนิ้วถ่างเพื่อซูม · แตะสองครั้ง", "loadFailed" to "โหลดรูปภาพไม่สำเร็จ",
            "divider" to "ตัวแบ่งการเปรียบเทียบ", "diff" to "ส่วนต่าง",
            "diffBusy" to "กำลังคำนวณส่วนต่าง…", "link" to "ซูมพร้อมกัน",
            "unlink" to "ซูมแยกกัน", "actualSize" to "พิกเซลจริง",
            "diffPercentPrefix" to "เปลี่ยนแปลง: "
        )
    )
}
