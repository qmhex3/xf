package com.frameout.core.export

object DatasetCoco {
    fun annotations(entries: List<DatasetEntry>, split: String, project: String, createdAt: String): String {
        val own = entries.filter { it.split == split }
        val images = own.mapIndexed { i, e -> image(i + 1, e) }.joinToString(",\n")
        return "{\n" +
            "  \"info\": {\"description\": ${DatasetText.jsonString(project)}, \"version\": \"1.0\", \"date_created\": ${DatasetText.jsonString(createdAt)}},\n" +
            "  \"licenses\": [],\n" +
            "  \"categories\": [{\"id\": 1, \"name\": \"object\", \"supercategory\": \"none\"}],\n" +
            "  \"images\": [\n$images\n  ],\n" +
            "  \"annotations\": []\n" +
            "}\n"
    }

    private fun image(id: Int, e: DatasetEntry): String {
        val s = e.frame.staged
        return "    {\"id\": $id, \"file_name\": ${DatasetText.jsonString(e.base + "." + e.frame.extension)}, " +
            "\"width\": ${s.width}, \"height\": ${s.height}, \"time_ms\": ${s.ptsUs / 1000L}, " +
            "\"sharpness\": ${e.frame.sharpness}, \"sharpness_score\": ${DatasetText.score(e.frame.score)}}"
    }
}
