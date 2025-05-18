package com.buyla.application.parser.axml

import java.io.IOException

/**
 * @author Dmitry Skiba (original Java version)
 * @author Buylan (Kotlin conversion and optimizations)
 *
 * Block of strings, used in binary xml and arsc.
 *
 *
 * - implement get()
 */
class StringBlock {
	private lateinit var mStringOffsets: IntArray
	private lateinit var mStrings: IntArray
	private lateinit var mStyleOffsets: IntArray
	private lateinit var mStyles: IntArray
	private var mConvert: Boolean = false

	/**
	 * Returns raw string (without any styling information) at specified index.
	 * Fixed utf 8 to 16LE problem
	 */
	fun getString(index: Int): String? {
		if (index < 0 || index >= mStringOffsets.size) {
			return null
		}
		val offset: Int = mStringOffsets[index]

		var currentOffset = offset

		var length = if (mConvert) {
			if ((getByte(mStrings, offset) and 128) != 0) currentOffset++
			val byte1 = getByte(mStrings, offset + 1).toInt()
			if (byte1 and 128 != 0) (byte1 and 127 shl 8) or getByte(mStrings, offset + 2).toInt() else byte1
		} else {
			val value = getShort(mStrings, offset)
			if (value and 32768 != 0) getShort(mStrings, offset + 2) or ((value and 32767) shl 16) else value
		}

		val lengthBlock = offset + if (mConvert) {
			when {
				getByte(mStrings, offset) and 128 == 0 -> 2
				else -> 4
			}
		} else {
			if (getShort(mStrings, offset) and 32768 == 0) 2 else 4
		}

		if (!mConvert) {
			length = length shl 1
		}

		val bytes = ByteArray(length) { i ->
			getByte(mStrings, lengthBlock + i).toByte()
		}

		return String(bytes, 0, length, if (mConvert) Charsets.UTF_8 else Charsets.UTF_16LE)
	}

	private fun getByte(iArr: IntArray, i: Int) = (iArr[i shr 2] ushr ((i and 0x3) shl 3)) and 0xFF

	/**
	 * Not yet implemented.
	 *
	 * Returns string with style information (if any).
	 */
	operator fun get(index: Int): CharSequence? = getString(index)

	companion object {
		private const val CHUNK_TYPE = 0x001C0001

		/**
		 * Reads whole (including chunk type) string block from stream.
		 * Stream must be at the chunk type.
		 */
		@JvmStatic
		@Throws(IOException::class)
		fun read(reader: IntReader): StringBlock {
			ChunkUtil.readCheckType(reader, CHUNK_TYPE)
			val chunkSize = reader.readInt()
			val stringCount = reader.readInt()
			val styleOffsetCount = reader.readInt()
			val flags = reader.readInt()
			val stringsOffset = reader.readInt()
			val stylesOffset = reader.readInt()

			val block = StringBlock()
			block.mStringOffsets = reader.readIntArray(stringCount)
			block.mConvert = (flags and 256) != 0

			if (styleOffsetCount != 0) {
				block.mStyleOffsets = reader.readIntArray(styleOffsetCount)
			}

			val stringsSize = (if (stylesOffset == 0) chunkSize else stylesOffset) - stringsOffset
			if (stringsSize % 4 != 0) {
				throw IOException("String data size is not multiple of 4 ($stringsSize).")
			}
			block.mStrings = reader.readIntArray(stringsSize / 4)
			if (stylesOffset != 0) {
				val stylesSize = chunkSize - stylesOffset
				if (stylesSize % 4 != 0) {
					throw IOException("Style data size is not multiple of 4 ($stylesSize).")
				}
				block.mStyles = reader.readIntArray(stylesSize / 4)
			}
			return block
		}

		private fun getShort(array: IntArray, offset: Int): Int {
			val value = array[offset / 4]
			return if ((offset % 4) / 2 == 0) {
				value and 0xFFFF
			} else {
				value ushr 16
			}
		}
	}
}