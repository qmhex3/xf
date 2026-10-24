package com.frameout.core.quality

import com.frameout.core.db.FrameEntity
import com.frameout.core.db.FrameTagDao
import com.frameout.core.db.FrameTagEntity
import kotlin.math.abs

class TagFamily(val id: String, val en: String, val ar: String, internal val labels: String) {
    val key: String get() = TagFamilies.key(id)

    fun title(lang: String): String = when (lang) {
        "ar" -> ar
        else -> TagText.forLang(lang)?.families?.get(id) ?: en
    }
}

object TagFamilies {
    const val PREFIX = "f:"
    const val OTHER = "other"
    private const val BACKFILL_CHUNK = 400

    val all: List<TagFamily> = listOf(
        TagFamily("vehicles", "Vehicles", "مركبات", "Car|Vehicle|Bus|Van|Motorcycle|Bicycle|Unicycle|Train|Airplane|Airliner|Aircraft|Aviation|Aerospace engineering|Helicopter|Rocket|Boat|Sailboat|Speedboat|Skiff|Canoe|Kayak|Brig|Frigate|Clipper|Submarine|Shipwreck|Tractor|Rickshaw|Bumper|Windshield|Wheel|Tire|Odometer|Skateboard|Longboard|Roller|Sled|Wing|Truck|Ship|Bicycle wheel|Land vehicle|Motor vehicle|Sports car|Automotive design|Street|Traffic"),
        TagFamily("animals", "Animals", "حيوانات", "Dog|Shetland sheepdog|Dalmatian|Basset hound|Cairn terrier|Cavalier|Boxer|Shikoku|Cat|Sphynx|Ragdoll|Pixie-bob|Pet|Bird|Waterfowl|Duck|Penguin|Horse|Bridle|Rein|Cattle|Bull|Herd|Bear|Turtle|Crocodile|Insect|Butterfly|Larva|Gerbil|Seal|Pomacentridae|Fur|Horn|Puppy|Kitten|Mammal|Carnivore|Canidae|Dog breed|Companion dog|Snout|Whiskers|Rabbit|Rodent|Sheep|Pig|Livestock|Terrestrial animal|Working animal|Wildlife|Felidae|Small to medium-sized cats|Big cats|Fawn|Tail|Paw|Muzzle|Deer|Monkey|Primate|Vertebrate|Hound|Terrier|Retriever|Fish|Reptile|Wolf|Fox|Animal|Dog supply|Dog clothes|Sporting group|Toy dog|Cow"),
        TagFamily("people", "People & body", "أشخاص وملامح", "Person|Selfie|Dude|Baby|Grandparent|Crowd|Team|Crew|Community|Smile|Laugh|Skin|Hair|Beard|Moustache|Bangs|Mouth|Ear|Eyelash|Foot|Hand|Nail|Toe|Flesh|Muscle|Tattoo|Sitting|Standing|Interaction|Model|Militia|Army|Sleep|Bathing|People|Human|Human body|Girl|Boy|Man|Woman|Child|Toddler|Face|Head|Forehead|Nose|Cheek|Chin|Eyebrow|Eye|Lip|Neck|Shoulder|Finger|Arm|Leg|Hairstyle|Long hair|Facial hair|Jaw|Black hair|Brown hair|Blond|Facial expression|Gesture|Thumb|Body|Doctor|Police|Soldier|Military"),
        TagFamily("fashion", "Fashion & accessories", "أزياء وإكسسوارات", "Dress|Gown|Sari|Shorts|Swimwear|Wetsuit|Tights|Leggings|Jeans|Denim|Jacket|Outerwear|Blazer|Tuxedo|Polo|Jersey|Mufti|Scarf|Hat|Cap|Beanie|Helmet|Tie|Veil|Shoe|Sneakers|Glasses|Sunglasses|Goggles|Bag|Handbag|Necklace|Bracelet|Bangle|Ring|Jewellery|Lipstick|Strap|Footwear|Clothing|Fashion|Pocket|Watch"),
        TagFamily("food", "Food & drink", "طعام وشراب", "Food|Cuisine|Meal|Lunch|Supper|Bento|Couscous|Pho|Sushi|Pasteles|Pizza|Cheeseburger|Hot dog|Fast food|Cake|Cookie|Pie|Icing|Gelato|Bread|Fruit|Vegetable|Coffee|Cappuccino|Juice|Wine|Alcohol|Cola|Eating|Steaming|Menu|Picnic|Dish|Drink|Dessert|Baked goods|Cup|Saucer"),
        TagFamily("nature", "Nature & landscape", "طبيعة ومناظر", "Mountain|Himalayan|Cliff|Canyon|Cave|Caving|Volcano|Rock|Soil|Sand|Dune|Desert|Prairie|Field|Swamp|Beach|Lake|River|Waterfall|Reef|Underwater|Iceberg|Glacier|Icicle|Ice|Shell|Safari|Farm|Ranch|Barn|Sea|Ocean|Water|Hill|Snow|Coast|Shore|Highland|Wave|Island|Landscape"),
        TagFamily("plants", "Plants & gardens", "نباتات وحدائق", "Plant|Flora|Flower|Petal|Flowerpot|Branch|Twig|Trunk|Garden|Forest|Jungle|Park|Tree|Grass|Leaf|Woody plant"),
        TagFamily("sky", "Sky & weather", "سماء وطقس", "Sky|Sunset|Moon|Star|Comet|Nebula|Space|Aurora|Rainbow|Fog|Storm|Lightning|Cloud|Sun|Sunrise|Night|Rain|Dusk|Afterglow|Horizon"),
        TagFamily("places", "Buildings & places", "مبانٍ وأماكن", "Building|Skyscraper|Skyline|Tower|Lighthouse|Bridge|Pier|Dam|Infrastructure|Road|Asphalt|Roof|Wall|Stairs|Handrail|Construction|Factory|Church|Cathedral|Mosque|Temple|Palace|Castle|Monument|Statue|Ruins|Museum|School|Casino|Stadium|Playground|Circus|Ferris wheel|Aquarium|Bar|Nightclub|City|Urban area|Tower block|House|Window|Door|Hospital|Market|Restaurant|Shop"),
        TagFamily("home", "Home & interior", "منزل وأثاث", "Room|Bedroom|Bathroom|Kitchen|Sink|Countertop|Cabinetry|Drawer|Shelf|Desk|Chair|Couch|Loveseat|Bunk bed|Pillow|Cushion|Curtain|Lampshade|Tile|Brick|Armrest|Bench|Swing|Tableware|Tablecloth|Placemat|Centrepiece|Cutlery|Cookware and bakeware|Porcelain|Furniture|Table|Bed"),
        TagFamily("devices", "Devices & screens", "أجهزة وشاشات", "Computer|Mobile phone|Television|Subwoofer|Screenshot|Web page|Icon|Camera|Laptop|Phone|Screen|Technology|Machine"),
        TagFamily("documents", "Documents & boards", "مستندات ولوحات", "Poster|Presentation|News|Newspaper|Paper|Receipt|Passport|Money|Whiteboard|Blackboard|Class|Product|Book|Sign"),
        TagFamily("music", "Music & dance", "موسيقى ورقص", "Musical instrument|Piano|Musical|Song|Pop music|Concert|Musician|Singer|Deejay|Dance|Ballroom|Guitar|Music"),
        TagFamily("sports", "Sports & outdoors", "رياضة وأنشطة", "Sports|Soccer|Rugby|Softball|Badminton|Archery|Curling|Gymnastics|Skateboarder|Skiing|Snowboarding|Sledding|Surfing|Surfboard|Windsurfing|Wakeboarding|Waterskiing|Swimming|Pool|Scuba diving|Snorkeling|Rafting|Rowing|Tubing|Cycling|Running|Marathon|Race|Racing|Track|Pitch|Net|Competition|Fishing|Camping|Backpacking|Vacation|Rodeo|Bullfighting|Balance|Ball|Football|Tourism|Travel"),
        TagFamily("events", "Celebrations & events", "احتفالات ومناسبات", "Party|Balloon|Prom|Graduation|Mortarboard|Carnival|Fireworks|Sparkler|Bonfire|Fire|Christmas|Hanukkah|Thanksgiving|Santa claus|Wreath|Marriage|Bride|Groom|Event|Clown|Joker|Love|Heart|Wedding|Smoke|Leisure|Fun"),
        TagFamily("toys", "Toys & characters", "ألعاب وشخصيات", "Toy|Stuffed toy|Plush|Lego|Superman|Superhero|Supervillain|Dragon|Dinosaur|Comics|Fiction|Cool|Miniature"),
        TagFamily("crafts", "Arts & crafts", "فنون وحرف", "Watercolor paint|Crochet|Knitting|Quilting|Needlework|Doily|Textile|Cotton|Wool|Leather|Glitter|Pattern|Neon|Monochrome|Metal|Art|Cartoon|Drawing|Painting|Sculpture"),
        TagFamily("objects", "Objects & tools", "أغراض وأدوات", "Clock|Umbrella|Flag|Wheelbarrow|Cage|Chain|Junk|Flap|Tool"),
        TagFamily("other", "Other", "متفرقات", "")
    )

    private val byId: Map<String, TagFamily> = all.associateBy { it.id }

    private val byLabel: Map<String, String> by lazy {
        HashMap<String, String>().also { out ->
            all.forEach { family ->
                family.labels.split('|').filter { it.isNotEmpty() }.forEach { out[it.lowercase()] = family.id }
            }
        }
    }

    private val byObject: Map<String, String> = mapOf(
        "o:fashion_good" to "fashion",
        "o:food" to "food",
        "o:home_good" to "home",
        "o:place" to "places",
        "o:plant" to "plants"
    )

    fun key(id: String): String = PREFIX + id

    fun isFamily(tag: String): Boolean = tag.startsWith(PREFIX)

    fun isDetail(tag: String): Boolean = tag.startsWith(TagRules.LABEL) || tag.startsWith(TagRules.OBJECT)

    fun visible(tag: String): Boolean = !isDetail(tag)

    fun familyOfLabel(label: String): String = byLabel[label.trim().lowercase()] ?: OTHER

    fun familyOf(tag: String): String? = when {
        tag.startsWith(TagRules.LABEL) -> familyOfLabel(tag.substring(TagRules.LABEL.length))
        tag.startsWith(TagRules.OBJECT) -> byObject[tag]
        else -> null
    }

    fun title(tag: String, lang: String): String? = if (isFamily(tag)) byId[tag.substring(PREFIX.length)]?.title(lang) else null

    private val PERSON_FAMILIES = setOf("people", "fashion")

    fun withFamilies(hits: List<TagHit>): List<TagHit> {
        val base = hits.filterNot { isFamily(it.tag) }
        val carWithoutPeople = SceneReasoner.vehicleDominant(base) && !SceneReasoner.hasPersonEvidence(base)
        val derived = base.mapNotNull { hit ->
            val id = familyOf(hit.tag) ?: return@mapNotNull null
            if (carWithoutPeople && id in PERSON_FAMILIES) return@mapNotNull null
            TagHit(key(id), hit.confidence, hit.boxes)
        }
        if (derived.isEmpty()) return base
        return base + TagRules.merge(derived)
    }

    suspend fun backfill(dao: FrameTagDao, projectId: Long): Int {
        val rows = dao.detailRowsWithoutFamily(projectId, TagRules.LABEL, TagRules.OBJECT, PREFIX)
        if (rows.isEmpty()) return 0
        val created = rows.groupBy { it.frameId }.flatMap { (frameId, group) ->
            val hits = group.map { TagHit(it.tag, it.confidence, NormBoxCodec.decode(it.boxes)) }
            withFamilies(hits).filter { isFamily(it.tag) }.map {
                FrameTagEntity(frameId = frameId, projectId = projectId, tag = it.tag, confidence = it.confidence, boxes = NormBoxCodec.encode(it.boxes))
            }
        }
        created.chunked(BACKFILL_CHUNK).forEach { dao.insertTags(it) }
        return created.size
    }
}

object CoverStack {
    const val DEPTH = 2
    const val POOL = 24

    fun behind(candidates: List<FrameEntity>, top: FrameEntity, depth: Int = DEPTH): List<FrameEntity> {
        val pool = candidates.filter { it.id != top.id }.distinctBy { it.id }
        if (pool.size <= depth) return pool
        val high = maxOf(top.index, pool.maxOf { it.index })
        val low = minOf(top.index, pool.minOf { it.index })
        val gap = ((high - low) / (depth + 1)).coerceAtLeast(1)
        val picked = ArrayList<FrameEntity>(depth)
        for (frame in pool) {
            if (picked.size >= depth) break
            val apart = abs(frame.index - top.index) >= gap && picked.all { abs(it.index - frame.index) >= gap }
            if (apart) picked += frame
        }
        for (frame in pool) {
            if (picked.size >= depth) break
            if (frame !in picked) picked += frame
        }
        return picked
    }
}
