package com.frameout.core.quality

internal object TagTextKo {
    val text = TagText(
        labels = TagLabelsKo.MAP,
        fixed = mapOf(
            TagRules.FACE to "얼굴", TagRules.GROUP to "단체",
            TagRules.SMILE to "웃는 얼굴", TagRules.EYES_CLOSED to "눈 감음",
            TagRules.GOOD_FACE to "좋은 얼굴", TagRules.BODY to "전신",
            TagRules.TEXT to "텍스트 포함", TagRules.CODE to "모든 코드",
            "b:contact" to "연락처", "b:email" to "이메일",
            "b:isbn" to "ISBN", "b:phone" to "전화번호",
            "b:product" to "상품 코드", "b:sms" to "SMS",
            "b:plain" to "텍스트 코드", "b:url" to "링크",
            "b:wifi" to "Wi-Fi", "b:geo" to "위치",
            "b:event" to "일정", "b:license" to "라이선스",
            "o:fashion_good" to "패션", "o:food" to "음식",
            "o:home_good" to "생활용품", "o:place" to "장소",
            "o:plant" to "식물"
        ),
        groups = mapOf(
            "People" to "사람", "Things" to "사물 및 장면", "Categories" to "카테고리",
            "Text" to "텍스트", "Codes" to "코드"
        ),
        families = mapOf(
            "vehicles" to "탈것", "animals" to "동물", "people" to "사람과 신체",
            "fashion" to "패션 및 액세서리", "food" to "음식과 음료", "nature" to "자연과 풍경",
            "plants" to "식물과 정원", "sky" to "하늘과 날씨", "places" to "건물과 장소",
            "home" to "집과 인테리어", "devices" to "기기와 화면", "documents" to "문서와 보드",
            "music" to "음악과 춤", "sports" to "스포츠와 야외 활동", "events" to "행사와 이벤트",
            "toys" to "장난감과 캐릭터", "crafts" to "미술과 공예", "objects" to "물건과 도구"
        ),
        contentTags = mapOf(
            "building" to "건물", "skyscraper" to "고층 건물", "bridge" to "다리",
            "tower" to "탑", "mosque" to "모스크", "church" to "교회",
            "castle" to "성", "landmark" to "랜드마크", "road" to "도로",
            "meal" to "식사", "fastfood" to "패스트푸드", "sweets" to "디저트",
            "bread" to "빵", "fruit" to "과일", "vegetable" to "채소",
            "coffee" to "커피", "drink" to "음료", "soccer" to "축구",
            "swimming" to "수상 스포츠", "running" to "달리기와 경기", "dance" to "춤",
            "concert" to "콘서트", "party" to "파티", "wedding" to "결혼식",
            "fireworks" to "불꽃놀이", "fire" to "불", "car" to "자동차",
            "heavy" to "밴과 트럭", "bus" to "버스", "motorcycle" to "오토바이",
            "bicycle" to "자전거", "train" to "기차", "plane" to "비행기",
            "helicopter" to "헬리콥터", "boat" to "보트와 선박", "dog" to "강아지",
            "cat" to "고양이", "bird" to "새", "horse" to "말",
            "cattle" to "소", "bear" to "곰", "reptile" to "파충류",
            "insect" to "곤충", "mountain" to "산", "beach" to "해변",
            "desert" to "사막", "forest" to "숲", "water" to "호수와 강",
            "waterfall" to "폭포", "sunset" to "일몰", "flower" to "꽃",
            "ice" to "얼음과 빙하", "storm" to "폭풍", "phone" to "휴대폰",
            "computer" to "컴퓨터", "tv" to "TV", "instrument" to "악기",
            "clock" to "시계", "bag" to "가방", "glasses" to "안경",
            "toy" to "장난감", "shoes" to "신발", "hat" to "모자",
            "dress" to "드레스", "clothes" to "의류", "jewellery" to "주얼리",
            "face" to "얼굴", "smile" to "웃음", "group" to "단체 사진",
            "baby" to "아기", "crowd" to "군중과 팀", "text" to "텍스트",
            "code" to "QR 코드 및 바코드"
        ),
        contentGroups = mapOf(
            "Vehicles" to "탈것", "Animals" to "동물", "Nature" to "자연",
            "Places" to "장소와 건물", "Food" to "음식과 음료", "Activities" to "스포츠와 행사",
            "Devices" to "기기와 물건", "Fashion" to "패션", "People" to "사람과 텍스트"
        ),
        engines = mapOf(
            "google_lens" to "사물, 장소, 텍스트를 인식하고 이미지의 출처를 찾아 줍니다",
            "yandex_images" to "이미지 사본과 비슷한 장면을 찾는 데 좋습니다",
            "bing_visual" to "비슷하게 생긴 이미지와 구매 가능한 상품을 찾아 줍니다",
            "tineye" to "이미지가 처음 나타난 곳과 수정된 버전을 찾아 줍니다",
            "google_images" to "기존 역이미지 검색: 카메라 아이콘을 눌러 이미지를 업로드하세요",
            "baidu_images" to "아시아 사이트, 상품, 소셜 미디어 검색에 유용합니다",
            "saucenao" to "애니메이션, 만화, 일러스트의 출처를 찾아 줍니다",
            "other_apps" to "설치된 다른 이미지 검색 앱으로 이미지를 보냅니다"
        ),
        compare = mapOf(
            "stack" to "위아래", "side" to "좌우",
            "slider" to "슬라이더", "swap" to "바꾸기",
            "fit" to "맞춤", "zoomIn" to "확대",
            "zoomOut" to "축소", "resetZoom" to "확대 초기화",
            "hint" to "두 손가락으로 확대 · 두 번 탭", "loadFailed" to "이미지를 불러오지 못했습니다",
            "divider" to "비교 구분선", "diff" to "차이",
            "diffBusy" to "차이 계산 중…", "link" to "확대 연동",
            "unlink" to "확대 분리", "actualSize" to "실제 픽셀",
            "diffPercentPrefix" to "변화: "
        )
    )
}
