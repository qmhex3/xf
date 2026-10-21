package com.frameout.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FolderMediaScannerTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun file(path: String, bytes: ByteArray = ByteArray(2048)): File =
        File(tmp.root, path).also { it.parentFile?.mkdirs(); it.writeBytes(bytes) }

    private fun scan(video: Boolean = true, image: Boolean = true, limits: FolderMediaScanner.Limits = FolderMediaScanner.Limits()) =
        FolderMediaScanner.scan(listOf(tmp.root), video, image, limits)

    private fun names(result: FolderMediaScanner.Result) = result.entries.map { it.name }.sorted()

    @Test
    fun findsVideosAndAnimatedImagesByExtension() {
        file("a/clip.avi"); file("a/movie.MKV"); file("b/anim.gif"); file("b/anim2.apng"); file("b/photo.jpg"); file("b/notes.txt")
        val result = scan()
        assertEquals(listOf("anim.gif", "anim2.apng", "clip.avi", "movie.MKV"), names(result))
        assertEquals(MediaKind.Animated, result.entries.first { it.name == "anim2.apng" }.kind)
        assertEquals("image/apng", result.entries.first { it.name == "anim2.apng" }.mime)
        assertEquals("video/x-msvideo", result.entries.first { it.name == "clip.avi" }.mime)
    }

    @Test
    fun respectsPermissionScopes() {
        file("clip.avi"); file("anim.gif")
        assertEquals(listOf("clip.avi"), names(scan(video = true, image = false)))
        assertEquals(listOf("anim.gif"), names(scan(video = false, image = true)))
        assertTrue(scan(video = false, image = false).entries.isEmpty())
    }

    @Test
    fun skipsHiddenNomediaAndAndroidFolders() {
        file("ok/clip.mp4")
        file(".hidden/clip.mp4")
        file("nomedia/clip.mp4"); file("nomedia/.nomedia", ByteArray(0))
        file("Android/data/clip.mp4")
        file("ok/.trashed-1-clip.mp4")
        assertEquals(listOf("clip.mp4"), names(scan()))
        assertEquals("ok", scan().entries.single().bucketName)
    }

    @Test
    fun ambiguousExtensionsNeedAVideoSignature() {
        file("src/app.ts", "export const x = 1;".toByteArray().copyOf(4096))
        val sync = ByteArray(1500)
        listOf(0, 188, 376).forEach { sync[it] = 0x47 }
        file("cam/stream.ts", sync)
        val found = scan().entries
        assertEquals(listOf("stream.ts"), found.map { it.name })
    }

    @Test
    fun rareVideoContainersAreListed() {
        file("old/clip.rm"); file("old/clip2.RMVB"); file("tv/master.mxf")
        val nut = "nut/multimedia container".toByteArray().copyOf(4096)
        file("misc/a.nut", nut); file("misc/notvideo.nut", ByteArray(4096) { 0x20 })
        val found = scan(video = true, image = false)
        assertEquals(listOf("a.nut", "clip.rm", "clip2.RMVB", "master.mxf"), names(found))
        assertEquals("application/vnd.rn-realmedia", found.entries.first { it.name == "clip.rm" }.mime)
        assertEquals("application/mxf", found.entries.first { it.name == "master.mxf" }.mime)
        assertTrue(found.entries.all { it.kind == MediaKind.Video })
    }

    @Test
    fun bdavTransportStreamWithTsExtensionIsListed() {
        val m2ts = ByteArray(1500)
        listOf(4, 196, 388).forEach { m2ts[it] = 0x47 }
        file("cam/bdav.ts", m2ts)
        assertEquals(listOf("bdav.ts"), names(scan()))
    }

    @Test
    fun ignoresTinyFiles() {
        file("tiny.mp4", ByteArray(100))
        assertTrue(scan().entries.isEmpty())
    }

    @Test
    fun honorsDepthAndResultLimits() {
        file("a/b/c/deep.mp4"); file("top1.mp4"); file("top2.mp4"); file("top3.mp4")
        assertFalse("deep.mp4" in names(scan(limits = FolderMediaScanner.Limits(maxDepth = 2))))
        val capped = scan(limits = FolderMediaScanner.Limits(maxResults = 1))
        assertTrue(capped.truncated)
        assertTrue(capped.entries.isNotEmpty())
    }

    @Test
    fun overlappingRootsDoNotProduceDuplicates() {
        file("x/clip.mp4")
        val result = FolderMediaScanner.scan(listOf(tmp.root, File(tmp.root, "x"), tmp.root), true, true)
        assertEquals(1, result.entries.size)
    }

    @Test
    fun bucketIdMatchesMediaStoreConvention() {
        file("Movies/clip.mp4")
        val entry = scan().entries.single()
        assertEquals(File(tmp.root, "Movies").absolutePath.lowercase().hashCode().toString(), entry.bucketId)
    }

    @Test
    fun syntheticIdsAreNegativeUniqueAndSorted() {
        val base = FolderEntry("/p", "n.mp4", "video/mp4", 5000, 10, "1", "b", MediaKind.Video)
        val entries = (1..200).map { base.copy(path = "/p/$it.mp4", dateSec = (it % 7).toLong() + 1) }
        val numbered = FolderMediaIndex.withIds(entries)
        assertEquals(200, numbered.map { it.id }.toSet().size)
        assertTrue(numbered.all { it.id < 0 })
        assertEquals(numbered, numbered.sortedWith(compareByDescending<FolderEntry> { it.dateSec }.thenByDescending { it.id }))
        assertEquals(numbered.map { it.id }, FolderMediaIndex.withIds(entries.reversed()).map { it.id })
    }
}
