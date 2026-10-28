package com.frameout.app.remote

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/** Paste the values shown in the admin panel (app → "Android settings"). */
object RemoteConfig {
    const val APP_ID = "com.frameout.app"
    const val PUBLIC_KEY_B64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEmT3JBpS3ug7ogpOwFeCitE0JYcRubS1Mjg9na9FP+3V+JywAysZXG1QFrrzTlf6JJ+E8cUKJWUKPIfk5NRieQ=="
    val MIRRORS = listOf("https://xunk-dev.github.io/frameout-privacy/com.frameout.app.json")
    val LINK_HOSTS = setOf("play.google.com")
    const val MAX_MANIFEST = 200_000
    const val MAX_MEDIA = 15_000_000
}

data class Notice(val id: String, val title: String, val body: String, val image: String?, val sha: String?, val link: String?, val start: Long, val end: Long)
data class Promo(val id: String, val type: String, val title: String, val body: String, val media: String?, val sha: String?, val link: String?, val start: Long, val end: Long)
data class Manifest(val seq: Long, val mirrors: List<String>, val notices: List<Notice>, val promos: List<Promo>)

private fun Long.active(now: Long, end: Long) = this <= now && (end == 0L || now <= end)
fun Notice.isActive(now: Long = System.currentTimeMillis()) = start.active(now, end)
fun Promo.isActive(now: Long = System.currentTimeMillis()) = start.active(now, end)

/** Verifies the ECDSA P-256 signature over the exact payload string, then parses it defensively. */
object ManifestVerifier {
    fun verify(envelope: String): Manifest? = runCatching {
        val env = JSONObject(envelope)
        val payload = env.getString("payload")
        val raw = Base64.decode(env.getString("sig"), Base64.DEFAULT)
        val key = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(Base64.decode(RemoteConfig.PUBLIC_KEY_B64, Base64.DEFAULT)))
        val s = Signature.getInstance("SHA256withECDSA")
        s.initVerify(key)
        s.update(payload.toByteArray(Charsets.UTF_8))
        if (!s.verify(rawToDer(raw))) return null
        parse(JSONObject(payload))
    }.getOrNull()

    private fun parse(j: JSONObject): Manifest? {
        if (j.getString("app") != RemoteConfig.APP_ID) return null
        if (j.getLong("expires") < System.currentTimeMillis()) return null
        fun str(o: JSONObject, k: String, n: Int) = o.optString(k, "").trim().take(n)
        fun https(s: String) = s.takeIf { it.startsWith("https://") && it.length <= 500 }
        fun link(s: String) = https(s)?.takeIf { runCatching { URL(it).host in RemoteConfig.LINK_HOSTS }.getOrDefault(false) }
        fun sha(s: String) = s.lowercase().takeIf { it.matches(Regex("[0-9a-f]{64}")) }
        val notices = j.optJSONArray("notices")?.let { a ->
            (0 until minOf(a.length(), 10)).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                val id = str(o, "id", 40).ifEmpty { return@mapNotNull null }
                val img = https(str(o, "image", 500)); val h = sha(str(o, "sha", 64))
                Notice(id, str(o, "title", 80), str(o, "body", 300), img.takeIf { h != null }, h, link(str(o, "link", 500)), o.optLong("start"), o.optLong("end"))
            }
        }.orEmpty()
        val promos = j.optJSONArray("promos")?.let { a ->
            (0 until minOf(a.length(), 10)).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                val id = str(o, "id", 40).ifEmpty { return@mapNotNull null }
                val type = str(o, "type", 8).takeIf { it in setOf("text", "image", "video") } ?: return@mapNotNull null
                val m = https(str(o, "media", 500)); val h = sha(str(o, "sha", 64))
                Promo(id, type, str(o, "title", 80), str(o, "body", 300), m.takeIf { h != null }, h, link(str(o, "link", 500)) ?: return@mapNotNull null, o.optLong("start"), o.optLong("end"))
            }
        }.orEmpty()
        val mirrors = j.optJSONArray("mirrors")?.let { a -> (0 until minOf(a.length(), 8)).mapNotNull { https(a.optString(it)) } }.orEmpty()
        return Manifest(j.getLong("seq"), mirrors, notices, promos)
    }

    // WebCrypto signs as raw r||s; Java expects DER.
    private fun rawToDer(raw: ByteArray): ByteArray {
        require(raw.size == 64)
        fun int(b: ByteArray): ByteArray {
            var x = b.dropWhile { it == 0.toByte() }.toByteArray()
            if (x.isEmpty()) x = byteArrayOf(0)
            if (x[0] < 0) x = byteArrayOf(0) + x
            return byteArrayOf(2, x.size.toByte()) + x
        }
        val r = int(raw.copyOfRange(0, 32)); val s = int(raw.copyOfRange(32, 64))
        return byteArrayOf(0x30, (r.size + s.size).toByte()) + r + s
    }
}

class RemoteStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("remote_content", Context.MODE_PRIVATE)
    var envelope: String?
        get() = p.getString("env", null)
        set(v) { p.edit().putString("env", v).apply() }
    var seq: Long
        get() = p.getLong("seq", 0)
        set(v) { p.edit().putLong("seq", v).apply() }
    var mirrors: List<String>
        get() = p.getString("mirrors", "")!!.split("\n").filter { it.isNotBlank() }
        set(v) { p.edit().putString("mirrors", v.joinToString("\n")).apply() }
    fun shown(id: String) = p.getStringSet("shown", emptySet())!!.contains(id)
    fun markShown(id: String) { p.edit().putStringSet("shown", p.getStringSet("shown", emptySet())!! + id).apply() }
    fun current(): Manifest? = envelope?.let { ManifestVerifier.verify(it) }
}

object RemoteSync {
    /** Tries every mirror; accepts only a correctly signed, unexpired manifest whose seq is not older than the stored one. */
    fun sync(ctx: Context): Manifest? {
        val store = RemoteStore(ctx)
        for (u in (store.mirrors + RemoteConfig.MIRRORS).distinct()) {
            val raw = download(u, RemoteConfig.MAX_MANIFEST)?.toString(Charsets.UTF_8) ?: continue
            val m = ManifestVerifier.verify(raw) ?: continue
            if (m.seq < store.seq) continue
            store.envelope = raw; store.seq = m.seq
            if (m.mirrors.isNotEmpty()) store.mirrors = m.mirrors
            return m
        }
        return store.current()
    }

    /** Downloads media and keeps it only if its SHA-256 matches the signed value. */
    fun media(ctx: Context, url: String, sha: String): File? {
        val f = File(ctx.cacheDir, "remote_$sha")
        if (f.exists()) return f
        val bytes = download(url, RemoteConfig.MAX_MEDIA) ?: return null
        val h = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        if (h != sha) return null
        f.writeBytes(bytes); return f
    }

    private fun download(u: String, max: Int): ByteArray? = runCatching {
        if (!u.startsWith("https://")) return null
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 15000; c.instanceFollowRedirects = true
        c.useCaches = false
        if (c.responseCode != 200) return null
        val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(8192)
        c.inputStream.use { i -> while (true) { val n = i.read(buf); if (n < 0) break; out.write(buf, 0, n); if (out.size() > max) return null } }
        out.toByteArray()
    }.getOrNull()
}
