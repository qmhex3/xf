package android.content
class ContentResolver { fun openFileDescriptor(u: android.net.Uri, m: String): android.os.ParcelFileDescriptor? = null }
class Context { val contentResolver = ContentResolver() }
