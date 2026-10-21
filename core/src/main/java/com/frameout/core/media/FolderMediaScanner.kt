package com.frameout.core.media

import com.frameout.codecpack.ContainerSniffer
import java.io.File

object FolderMediaScanner {
    data class Limits(val maxDepth: Int = 12, val maxDirs: Int = 30_000, val maxResults: Int = 5_000, val budgetMs: Long = 4_000L)

    data class Result(val entries: List<FolderEntry>, val truncated: Boolean)

    private class Node(val dir: File, val canonical: File, val depth: Int)

    private class Walk(val allowVideo: Boolean, val allowImage: Boolean, val limits: Limits, val deadline: Long) {
        val found = ArrayList<FolderEntry>()
        val stack = ArrayDeque<Node>()
        var dirs = 0
        var truncated = false

        fun exhausted(now: Long): Boolean {
            if (dirs >= limits.maxDirs || found.size >= limits.maxResults || now > deadline) truncated = true
            return truncated
        }
    }

    fun scan(roots: List<File>, allowVideo: Boolean, allowImage: Boolean, limits: Limits = Limits(), now: () -> Long = System::currentTimeMillis): Result {
        val walk = Walk(allowVideo, allowImage, limits, now() + limits.budgetMs)
        val seen = HashSet<String>()
        roots.forEach { root -> rootNode(root)?.takeIf { seen.add(it.canonical.path) }?.let(walk.stack::addLast) }
        while (walk.stack.isNotEmpty() && !walk.exhausted(now())) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            visit(walk.stack.removeLast(), walk)
        }
        return Result(walk.found.distinctBy { it.path }, walk.truncated)
    }

    private fun rootNode(root: File): Node? =
        runCatching { if (root.isDirectory) Node(root, root.canonicalFile, 0) else null }.getOrNull()

    private fun visit(node: Node, walk: Walk) {
        walk.dirs++
        val children = node.dir.listFiles() ?: return
        if (children.any { it.name == ".nomedia" }) return
        children.forEach { child ->
            if (child.isDirectory) enqueue(node, child, walk) else accept(child, walk)?.let { walk.found += it }
        }
    }

    private fun enqueue(parent: Node, child: File, walk: Walk) {
        val depth = parent.depth + 1
        if (depth > walk.limits.maxDepth || FolderMediaRules.skipDirectory(child.name, depth)) return
        val canonical = runCatching { child.canonicalFile }.getOrNull() ?: return
        if (canonical.parentFile != parent.canonical) return
        walk.stack.addLast(Node(child, canonical, depth))
    }

    private fun accept(file: File, walk: Walk): FolderEntry? {
        val name = file.name
        if (FolderMediaRules.skipFile(name)) return null
        val ext = ContainerSniffer.extensionOf(name)
        val kind = FolderMediaRules.kindOf(ext) ?: return null
        if (if (kind == MediaKind.Video) !walk.allowVideo else !walk.allowImage) return null
        val size = file.length()
        if (size < FolderMediaRules.MIN_BYTES) return null
        if (FolderMediaRules.needsSniff(ext) && !sniffsAsVideo(file)) return null
        return entryOf(file, name, ext, size, kind)
    }

    private fun entryOf(file: File, name: String, ext: String, size: Long, kind: MediaKind): FolderEntry {
        val parent = file.parentFile
        return FolderEntry(
            path = file.absolutePath,
            name = name,
            mime = FolderMediaRules.mimeOf(ext),
            size = size,
            dateSec = (file.lastModified() / 1000L).coerceAtLeast(1L),
            bucketId = FolderMediaRules.bucketIdOf(parent?.absolutePath.orEmpty()),
            bucketName = parent?.name.orEmpty(),
            kind = kind,
            readable = file.canRead()
        )
    }

    private fun sniffsAsVideo(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val head = ByteArray(ContainerSniffer.HEAD_BYTES)
            var read = 0
            while (read < head.size) {
                val n = input.read(head, read, head.size - read)
                if (n < 0) break
                read += n
            }
            FolderMediaRules.looksLikeVideo(head, read)
        }
    }.getOrDefault(false)
}
