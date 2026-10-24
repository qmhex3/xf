package com.frameout.core.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

class DatasetArchiveTest {
    private fun frames(count: Int, ext: String = "jpg"): List<DatasetFrame> {
        val dir = File(System.getProperty("java.io.tmpdir"), "dataset_test_${System.nanoTime()}").apply { mkdirs() }
        return (1..count).map { i ->
            val file = File(dir, "f$i.$ext").apply { writeBytes(byteArrayOf(i.toByte(), 1, 2)) }
            DatasetFrame(StagedFrame("x$i.$ext", file, i.toLong(), i * 10, i, i * 1_000_000L, 1920, 1080), 50 + i, 12.5 * i)
        }
    }

    private fun names(format: DatasetFormat, count: Int): Map<String, String> {
        val out = ByteArrayOutputStream()
        DatasetArchive.write(format, frames(count), "Clip", 0L, out)
        val result = LinkedHashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                result[entry.name] = String(zip.readBytes(), Charsets.UTF_8)
            }
        }
        return result
    }

    @Test
    fun colmapHasFlatImagesAndScripts() {
        val files = names(DatasetFormat.Colmap, 3)
        assertTrue("images/frame_000001.jpg" in files)
        assertTrue("images/frame_000003.jpg" in files)
        assertTrue(files.getValue("run_colmap.sh").contains("automatic_reconstructor"))
        assertTrue(files.getValue("run_colmap.bat").contains("%~dp0"))
        assertFalse(files.keys.any { it.startsWith("labels/") })
    }

    @Test
    fun yoloHasSplitsLabelsAndYaml() {
        val files = names(DatasetFormat.Yolo, 10)
        assertEquals(2, files.keys.count { it.startsWith("images/val/") })
        assertEquals(8, files.keys.count { it.startsWith("images/train/") })
        assertEquals(10, files.keys.count { it.startsWith("labels/") })
        assertEquals("", files.getValue("labels/val/frame_000010.txt"))
        assertTrue(files.getValue("data.yaml").contains("train: images/train"))
    }

    @Test
    fun cocoListsImagesPerSplit() {
        val files = names(DatasetFormat.Coco, 5)
        val train = files.getValue("annotations/instances_train.json")
        val val0 = files.getValue("annotations/instances_val.json")
        assertTrue(train.contains("\"file_name\": \"frame_000001.jpg\""))
        assertTrue(val0.contains("\"file_name\": \"frame_000005.jpg\""))
        assertTrue(train.contains("\"annotations\": []"))
        assertTrue(train.contains("\"width\": 1920"))
    }

    @Test
    fun csvCarriesSharpnessAndScore() {
        val files = names(DatasetFormat.Colmap, 2)
        val lines = files.getValue("frames.csv").removePrefix(CsvText.BOM).split(CsvText.EOL)
        assertEquals(DatasetText.HEADER, lines[0])
        assertEquals("1,images/frame_000001.jpg,,1000,00:01.000,51,12.50,1920,1080", lines[1])
    }

    @Test
    fun splitNeverEmptiesValForTinySets() {
        assertFalse(DatasetSplit.isVal(0, 1))
        assertTrue(DatasetSplit.isVal(1, 2))
        assertTrue(DatasetSplit.isVal(3, 4))
        assertFalse(DatasetSplit.isVal(0, 4))
    }

    @Test
    fun jsonStringEscapes() {
        assertEquals("\"a\\\"b\\\\c\\n\"", DatasetText.jsonString("a\"b\\c\n"))
    }

    @Test
    fun rejectedFramesGoToOwnFolderOnly() {
        val all = frames(6)
        val mixed = all.mapIndexed { i, f -> if (i >= 4) DatasetFrame(f.staged, f.sharpness, f.score, rejected = true) else f }
        val out = ByteArrayOutputStream()
        DatasetArchive.write(DatasetFormat.Yolo, mixed, "Clip", 0L, out)
        val files = LinkedHashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            while (true) { val e = zip.nextEntry ?: break; files[e.name] = String(zip.readBytes(), Charsets.UTF_8) }
        }
        assertTrue("rejected/frame_000005.jpg" in files)
        assertTrue("rejected/frame_000006.jpg" in files)
        assertEquals(4, files.keys.count { it.startsWith("labels/") })
        assertEquals(4, files.keys.count { it.startsWith("images/") })
        assertTrue(files.getValue("frames.csv").contains("rejected/frame_000005.jpg,rejected"))
        assertTrue(files.getValue("README.txt").contains("2 frames were rejected"))
    }
}
