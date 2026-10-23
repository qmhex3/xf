package com.frameout.core.quality.content

internal object ContentTagsMoving {
    private val V = ContentGroup.Vehicles
    private val A = ContentGroup.Animals
    private val N = ContentGroup.Nature

    val vehicles = listOf(
        tag("car", V, "Cars", "سيارات", "Car", "Bumper"),
        tag("heavy", V, "Vans & trucks", "شاحنات وفانات", "Van", "Tractor", "Vehicle"),
        tag("bus", V, "Buses", "حافلات", "Bus"),
        tag("motorcycle", V, "Motorcycles", "دراجات نارية", "Motorcycle"),
        tag("bicycle", V, "Bicycles", "دراجات هوائية", "Bicycle", "Unicycle"),
        tag("train", V, "Trains", "قطارات", "Train"),
        tag("plane", V, "Planes", "طائرات", "Airplane", "Airliner", "Aircraft", "Aviation"),
        tag("helicopter", V, "Helicopters", "مروحيات", "Helicopter"),
        tag("boat", V, "Boats & ships", "قوارب وسفن", "Boat", "Sailboat", "Speedboat", "Skiff", "Canoe", "Kayak", "Brig", "Frigate")
    )

    val animals = listOf(
        tag("dog", A, "Dogs", "كلاب", "Dog", "Shetland sheepdog", "Dalmatian", "Basset hound", "Cairn terrier"),
        tag("cat", A, "Cats", "قطط", "Cat", "Sphynx", "Ragdoll", "Pixie-bob"),
        tag("bird", A, "Birds", "طيور", "Bird", "Waterfowl", "Duck", "Penguin"),
        tag("horse", A, "Horses", "خيول", "Horse", "Bridle", "Rein"),
        tag("cattle", A, "Cattle", "أبقار وثيران", "Cattle", "Bull", "Herd"),
        tag("bear", A, "Bears", "دببة", "Bear"),
        tag("reptile", A, "Reptiles", "سلاحف وتماسيح", "Turtle", "Crocodile"),
        tag("insect", A, "Insects", "حشرات وفراشات", "Insect", "Butterfly", "Larva")
    )

    val nature = listOf(
        tag("mountain", N, "Mountains", "جبال", "Mountain", "Himalayan", "Cliff"),
        tag("beach", N, "Beaches", "شواطئ", "Beach", "Sand"),
        tag("desert", N, "Desert", "صحراء", "Desert", "Dune"),
        tag("forest", N, "Forests", "غابات", "Forest", "Jungle"),
        tag("water", N, "Lakes & rivers", "بحيرات وأنهار", "Lake", "River"),
        tag("waterfall", N, "Waterfalls", "شلالات", "Waterfall"),
        tag("sunset", N, "Sunsets", "غروب", "Sunset"),
        tag("flower", N, "Flowers", "زهور", "Flower", "Petal"),
        tag("ice", N, "Ice & glaciers", "جليد", "Glacier", "Iceberg", "Icicle", "Ice"),
        tag("storm", N, "Storms", "عواصف", "Storm", "Lightning")
    )
}
