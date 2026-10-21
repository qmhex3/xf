package com.frameout.codecpack

import android.content.Context
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.zip.ZipFile

internal object CodecPackVerifier {
    private val PUBLIC_KEYS_PEM = listOf("""-----BEGIN PUBLIC KEY-----
MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAqneAz2a/rGKxySwg8tOf
Sss2msfqm7JQq6GJvxn/pv9C8pr/BnxYfzUp9p4o90hzArJN4IeVpISovgqolvxx
Z3BGhWksJH9PGp7ELtACsoLAVF1vC9ayANHD90MG5S2ZaZnqUAKLZ2Ij3aJ3QABi
K8gLMA7/ULIGdfRj5+V/Gg4Eu01116hCbm12P0+kOQV6l7UhfGlU2VtHw6Cp7O57
hVbUfdT6SlhChJhTrBqCQjm9PRkGHVcwfi8f4xyqkO2QGhCs8+9GhhvLntZLT+wI
eQ3uzJThN85Hb+PXYjDNvnvkly7o6m4TPRnxzSb1i4E/d2Wb3sHCISkdDmbLm/dx
GhC2BbR2ZaEor0RpAEWlcNXOruLOW+6ZfKdexDqvYr7kkX1oV6kVmNI3EIcrZRXO
vP9MwOvsxUHSCdIz7NOACuZnkbunA27Whbtp7uMMG/K4DP6t0pBghRdj+DZLeUwh
iaSUGqZEEqFDZhvhs2vLADtSBU/opmrN1AfxEvzRTuGLAgMBAAE=
-----END PUBLIC KEY-----""")
    private val allowedRoots = CodecPack.SUPPORTED_ABIS
    private val allowedNames = CodecPack.dependencies.toSet() + CodecPack.WRAPPER + "SHA256SUMS"

    fun isInstalled(context: Context): Boolean {
        val dir = runCatching { CodecPack.abiDir(context) }.getOrNull() ?: return false
        val files = CodecPack.dependencies.map { File(dir, it) } + File(dir, CodecPack.WRAPPER)
        return File(dir, CodecPack.VERIFIED).isFile && files.all { it.isFile && it.canRead() && !it.canWrite() }
    }

    fun verifyArchive(zip: ZipFile): Boolean {
        if (zip.size() > CodecPack.MAX_ENTRIES) return false
        var total = 0L
        val seen = hashSetOf<String>()
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val name = entry.name
            if (!seen.add(name) || name.startsWith("/") || name.contains('\\') || name.contains("..")) return false
            if (entry.isDirectory) continue
            val parts = name.split('/')
            val allowed = when (parts.size) {
                1 -> parts[0] in CodecPackLicenses.TEXT_FILES || parts[0] == "MANIFEST.txt" || parts[0] == CodecPack.SIGNATURE || parts[0] == CodecPack.PAYLOAD
                2 -> parts[0] in allowedRoots && parts[1] in allowedNames
                else -> false
            }
            if (!allowed) return false
            if (entry.size > CodecPack.MAX_ENTRY_BYTES || entry.compressedSize > CodecPack.MAX_ENTRY_BYTES) return false
            if (entry.size > 0) total += entry.size
            if (total > CodecPack.MAX_TOTAL_UNCOMPRESSED) return false
        }
        return true
    }

    private fun verifyWith(pem: String, payload: ByteArray, signatureBytes: ByteArray): Boolean = runCatching {
        val keyData = Base64.decode(pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replace("\\s".toRegex(), ""), Base64.DEFAULT)
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyData))
        Signature.getInstance("SHA256withRSA").run { initVerify(key); update(payload); verify(signatureBytes) }
    }.getOrDefault(false)

    fun verifySignature(zip: ZipFile): Boolean = runCatching {
        val payload = readLimited(zip.getEntry(CodecPack.PAYLOAD) ?: return false, zip)
        val signatureBytes = readLimited(zip.getEntry(CodecPack.SIGNATURE) ?: return false, zip)
        PUBLIC_KEYS_PEM.any { pem -> verifyWith(pem, payload, signatureBytes) }
    }.getOrDefault(false)

    fun verifyChecksums(zip: ZipFile, abi: String, temp: File, files: Array<String>): Boolean = runCatching {
        val sumsEntry = zip.getEntry("$abi/SHA256SUMS") ?: return false
        val payloadEntry = zip.getEntry(CodecPack.PAYLOAD) ?: return false
        val sums = String(readLimited(sumsEntry, zip), Charsets.UTF_8)
        val payload = String(readLimited(payloadEntry, zip), Charsets.UTF_8)
        if (!containedInPayload(sums, payload)) return false
        val checks = sums.lines().associate { line ->
            val parts = line.trim().split(Regex("\\s+"), limit = 2)
            if (parts.size == 2) parts[1] to parts[0] else "" to ""
        }
        files.all { name -> checks[name]?.equals(sha256(File(temp, name)), true) == true }
    }.getOrDefault(false)

    private fun containedInPayload(sums: String, payload: String): Boolean {
        val needle = sums.replace("\r\n", "\n").trim('\n')
        if (needle.isBlank()) return false
        val haystack = payload.replace("\r\n", "\n")
        var from = 0
        while (true) {
            val at = haystack.indexOf(needle, from)
            if (at < 0) return false
            val end = at + needle.length
            val startsLine = at == 0 || haystack[at - 1] == '\n'
            val endsLine = end == haystack.length || haystack[end] == '\n'
            if (startsLine && endsLine) return true
            from = at + 1
        }
    }

    fun verifyLicenseFiles(payload: String, dir: File): Boolean = runCatching {
        CodecPackLicenses.TEXT_FILES.all { name ->
            val line = Regex("(?m)^([0-9a-fA-F]{64})\\s+\\*?" + Regex.escape(name) + "\\s*$").find(payload)
            val file = File(dir, name)
            line != null && file.isFile && line.groupValues[1].equals(sha256(file), true)
        }
    }.getOrDefault(false)

    fun verifyInstalled(dir: File): Boolean = runCatching {
        val marker = File(dir, CodecPack.VERIFIED)
        val checks = marker.readLines().associate { line -> line.trim().split(Regex("\\s+"), limit = 2).let { it[0] to it.getOrElse(1) { "" } } }
        val files = CodecPack.dependencies + CodecPack.WRAPPER
        files.all { name -> checks[name]?.equals(sha256(File(dir, name)), true) == true }
    }.getOrDefault(false)

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun readLimited(entry: java.util.zip.ZipEntry, zip: ZipFile): ByteArray {
        if (entry.size > CodecPack.MAX_ENTRY_BYTES) throw CodecPack.InstallException(CodecPack.InstallError.TooLarge, "entry too large")
        return zip.getInputStream(entry).use { input ->
            java.io.ByteArrayOutputStream().also { output -> copyLimited(input, output) }.toByteArray()
        }
    }

    fun copyLimited(input: java.io.InputStream, output: java.io.OutputStream) {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > CodecPack.MAX_ENTRY_BYTES) throw CodecPack.InstallException(CodecPack.InstallError.TooLarge, "entry too large")
            output.write(buffer, 0, read)
        }
    }
}
