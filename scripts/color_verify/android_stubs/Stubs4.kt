package android.system
object Os {
    class Stat { var st_size = 0L }
    fun fstat(fd: java.io.FileDescriptor) = Stat()
    fun pread(fd: java.io.FileDescriptor, b: ByteArray, o: Int, l: Int, off: Long) = -1
}
