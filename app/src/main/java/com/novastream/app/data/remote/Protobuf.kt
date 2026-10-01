package com.novastream.app.data.remote

/**
 * Minimal, dependency-free protobuf wire-format reader.
 * Enough to decode the Keiyoushi/Mihon `index.pb` extension index.
 */
object Protobuf {

    data class Field(
        val number: Int,
        val wireType: Int,
        val varint: Long = 0L,
        val bytes: ByteArray? = null,
    ) {
        val asString: String? get() = bytes?.toString(Charsets.UTF_8)
        val asBool: Boolean get() = varint != 0L
    }

    /** Decode a flat protobuf message into its fields. */
    fun decode(data: ByteArray): List<Field> {
        val out = ArrayList<Field>()
        var i = 0
        while (i < data.size) {
            val (tag, ni) = readVarint(data, i) ?: break
            i = ni
            val fieldNumber = (tag shr 3).toInt()
            val wireType = (tag and 0x7).toInt()
            when (wireType) {
                0 -> {
                    val (v, nj) = readVarint(data, i) ?: break
                    i = nj
                    out.add(Field(fieldNumber, wireType, varint = v))
                }
                1 -> { i += 8 }
                2 -> {
                    val (len, nj) = readVarint(data, i) ?: break
                    i = nj
                    val end = i + len.toInt()
                    if (end > data.size) break
                    out.add(Field(fieldNumber, wireType, bytes = data.copyOfRange(i, end)))
                    i = end
                }
                5 -> { i += 4 }
                else -> break
            }
        }
        return out
    }

    private fun readVarint(data: ByteArray, start: Int): Pair<Long, Int>? {
        var result = 0L
        var shift = 0
        var i = start
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF
            result = result or ((b.toLong() and 0x7F) shl shift)
            i++
            if (b and 0x80 == 0) return result to i
            shift += 7
            if (shift > 63) return null
        }
        return null
    }
}
