package com.frameout.core.quality

internal object TagTextVi {
    val text = TagText(
        labels = TagLabelsVi.MAP,
        fixed = mapOf(
            TagRules.FACE to "Khuôn mặt", TagRules.GROUP to "Nhóm người",
            TagRules.SMILE to "Nụ cười", TagRules.EYES_CLOSED to "Nhắm mắt",
            TagRules.GOOD_FACE to "Khuôn mặt đẹp", TagRules.BODY to "Toàn thân",
            TagRules.TEXT to "Có văn bản", TagRules.CODE to "Mọi loại mã",
            "b:contact" to "Liên hệ", "b:email" to "Email",
            "b:isbn" to "ISBN", "b:phone" to "Điện thoại",
            "b:product" to "Mã sản phẩm", "b:sms" to "SMS",
            "b:plain" to "Mã văn bản", "b:url" to "Liên kết",
            "b:wifi" to "Wi-Fi", "b:geo" to "Vị trí",
            "b:event" to "Sự kiện", "b:license" to "Giấy phép",
            "o:fashion_good" to "Thời trang", "o:food" to "Thức ăn",
            "o:home_good" to "Đồ gia dụng", "o:place" to "Địa điểm",
            "o:plant" to "Cây cối"
        ),
        groups = mapOf(
            "People" to "Con người", "Things" to "Đồ vật và cảnh quan", "Categories" to "Danh mục",
            "Text" to "Văn bản", "Codes" to "Mã"
        ),
        families = mapOf(
            "vehicles" to "Phương tiện", "animals" to "Động vật", "people" to "Con người và cơ thể",
            "fashion" to "Thời trang và phụ kiện", "food" to "Đồ ăn và thức uống", "nature" to "Thiên nhiên và phong cảnh",
            "plants" to "Cây cối và vườn", "sky" to "Bầu trời và thời tiết", "places" to "Tòa nhà và địa điểm",
            "home" to "Nhà cửa và nội thất", "devices" to "Thiết bị và màn hình", "documents" to "Tài liệu và bảng",
            "music" to "Âm nhạc và khiêu vũ", "sports" to "Thể thao và hoạt động ngoài trời", "events" to "Tiệc tùng và sự kiện",
            "toys" to "Đồ chơi và nhân vật", "crafts" to "Thủ công và nghệ thuật", "objects" to "Đồ vật và dụng cụ"
        ),
        contentTags = mapOf(
            "building" to "Tòa nhà", "skyscraper" to "Nhà chọc trời", "bridge" to "Cầu",
            "tower" to "Tháp", "mosque" to "Nhà thờ Hồi giáo", "church" to "Nhà thờ",
            "castle" to "Lâu đài và cung điện", "landmark" to "Địa danh và công trình nổi tiếng", "road" to "Đường",
            "meal" to "Bữa ăn", "fastfood" to "Đồ ăn nhanh", "sweets" to "Đồ ngọt",
            "bread" to "Bánh mì", "fruit" to "Trái cây", "vegetable" to "Rau củ",
            "coffee" to "Cà phê", "drink" to "Đồ uống", "soccer" to "Bóng đá",
            "swimming" to "Thể thao dưới nước", "running" to "Chạy bộ và thi đấu", "dance" to "Khiêu vũ",
            "concert" to "Buổi hòa nhạc", "party" to "Tiệc", "wedding" to "Đám cưới",
            "fireworks" to "Pháo hoa", "fire" to "Lửa", "car" to "Ô tô",
            "heavy" to "Xe van và xe tải", "bus" to "Xe buýt", "motorcycle" to "Xe máy",
            "bicycle" to "Xe đạp", "train" to "Tàu hỏa", "plane" to "Máy bay",
            "helicopter" to "Trực thăng", "boat" to "Thuyền", "dog" to "Chó",
            "cat" to "Mèo", "bird" to "Chim", "horse" to "Ngựa",
            "cattle" to "Gia súc", "bear" to "Gấu", "reptile" to "Bò sát",
            "insect" to "Côn trùng", "mountain" to "Núi", "beach" to "Bãi biển",
            "desert" to "Sa mạc", "forest" to "Rừng", "water" to "Hồ và sông",
            "waterfall" to "Thác nước", "sunset" to "Hoàng hôn", "flower" to "Hoa",
            "ice" to "Băng và sông băng", "storm" to "Bão", "phone" to "Điện thoại",
            "computer" to "Máy tính", "tv" to "Tivi", "instrument" to "Nhạc cụ",
            "clock" to "Đồng hồ", "bag" to "Túi", "glasses" to "Kính mắt",
            "toy" to "Đồ chơi", "shoes" to "Giày", "hat" to "Mũ",
            "dress" to "Váy", "clothes" to "Quần áo", "jewellery" to "Trang sức",
            "face" to "Khuôn mặt", "smile" to "Nụ cười", "group" to "Nhóm người",
            "baby" to "Em bé", "crowd" to "Đám đông và nhóm người", "text" to "Văn bản",
            "code" to "Mã QR và mã vạch"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Phương tiện", "Animals" to "Động vật", "Nature" to "Thiên nhiên",
            "Places" to "Địa điểm và tòa nhà", "Food" to "Đồ ăn và thức uống", "Activities" to "Thể thao và sự kiện",
            "Devices" to "Thiết bị và đồ vật", "Fashion" to "Thời trang", "People" to "Con người và văn bản"
        ),
        engines = mapOf(
            "google_lens" to "Nhận diện đồ vật, địa điểm và văn bản, đồng thời tìm nguồn gốc của ảnh",
            "yandex_images" to "Phù hợp để tìm ảnh bị sao chép và các cảnh tương tự",
            "bing_visual" to "Ảnh trông giống nhau và sản phẩm có thể mua",
            "tineye" to "Tìm xem ảnh này xuất hiện lần đầu ở đâu, kể cả các phiên bản đã chỉnh sửa",
            "google_images" to "Tìm kiếm bằng hình ảnh kiểu truyền thống: nhấn biểu tượng máy ảnh để tải ảnh lên",
            "baidu_images" to "Phù hợp với các trang web châu Á, sản phẩm và mạng xã hội",
            "saucenao" to "Tìm nguồn của anime, manga hoặc tranh minh họa",
            "other_apps" to "Gửi ảnh đến bất kỳ ứng dụng tìm kiếm ảnh nào bạn đã cài"
        ),
        compare = mapOf(
            "stack" to "Xếp dọc", "side" to "Cạnh nhau",
            "slider" to "Thanh trượt", "swap" to "Hoán đổi",
            "fit" to "Vừa khung", "zoomIn" to "Phóng to",
            "zoomOut" to "Thu nhỏ", "resetZoom" to "Đặt lại thu phóng",
            "hint" to "Chụm hai ngón để thu phóng · nhấn đúp", "loadFailed" to "Không thể tải ảnh",
            "divider" to "Thanh chia so sánh", "diff" to "Khác biệt",
            "diffBusy" to "Đang tính toán khác biệt…", "link" to "Thu phóng đồng bộ",
            "unlink" to "Thu phóng riêng", "actualSize" to "Điểm ảnh thực tế",
            "diffPercentPrefix" to "Thay đổi: "
        )
    )
}
