/*
 * Copyright 2008 Android4ME
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.buyla.application.parser.axml.code.res

import java.io.IOException
import java.nio.charset.StandardCharsets


/**
 * @author Dmitry Skiba (original Java version)
 * @author Buylan (Kotlin conversion and optimizations)
 *
 * Block of strings, used in binary xml and arsc.
 *
 * TODO:
 * - implement get()
 */
class StringBlock private constructor() {
	private var mStringOffsets: IntArray? = null
	private lateinit var mStrings: IntArray
	private var mStyleOffsets: IntArray? = null
	private var mStyles: IntArray? = null
	// To fix utf problem
	private var isUtf8: Boolean = false

	/**
	 * Returns raw string (without any styling information) at specified index.
	 * Fixed utf 8 to 16LE problem
	 */
	fun getString(index: Int): String? {
		if (index < 0 || mStringOffsets == null || index >= mStringOffsets!!.size) {
			return null
		}
		val offset: Int = mStringOffsets!![index]
		var length = getStringLength(mStrings, offset)
		val lengthFieldSize = offset + getLengthFieldSize(mStrings, offset)
		val charset = if (this.isUtf8) StandardCharsets.UTF_8 else StandardCharsets.UTF_16LE
		if (!this.isUtf8) {
			length = length shl 1
		}
		val originalString =
			String(getByteArray(mStrings, lengthFieldSize, length), 0, length, charset)
		return originalString
	}

	private fun getStringLength(array: IntArray?, offset: Int): Int {
		var offset = offset
		if (!this.isUtf8) {
			val value = getShort(array!!, offset)
			if ((32768 and value) != 0) {
				return getShort(array, offset + 2) or ((value and 32767) shl 16)
			}
			return value
		}
		if ((getByte(array!!, offset) and 128) != 0) {
			offset++
		}
		val nextOffset = offset + 1
		val byte1: Int = getByte(array, nextOffset).toInt()
		return if ((byte1 and 128) != 0) ((byte1 and 127) shl 8) or getByte(
			array,
			nextOffset + 1
		).toInt() else byte1
	}

	private fun getByteArray(array: IntArray, offset: Int, length: Int): ByteArray {
		val bytes = ByteArray(length)
		for (i in 0..<length) {
			bytes[i] = getByte(array, offset + i).toByte()
		}
		return bytes
	}

	// Determines the size of the length field based on the encoding
	private fun getLengthFieldSize(array: IntArray?, offset: Int): Int {
		if (!this.isUtf8) {
			return if ((32768 and getShort(array!!, offset)) != 0) 4 else 2
		}
		val size = if ((getByte(array!!, offset) and 128) != 0) 2 + 1 else 2
		return if ((getByte(array, offset) and 128) != 0) size + 1 else size
	}

	private fun getByte(iArr: IntArray, i: Int): Int {
		return (iArr[i / 4] ushr ((i % 4) * 8)) and 255
	}

	/**
	 * Not yet implemented.
	 *
	 * Returns string with style information (if any).
	 */
	operator fun get(index: Int): CharSequence? = getString(index)

	/**
	 * Finds index of the string.
	 * Returns -1 if the string was not found.
	 */
	fun find(string: String?): Int {
		if (string == null || mStringOffsets == null) return -1

		for (i in mStringOffsets!!.indices) {
			var offset = mStringOffsets!![i]
			val length = getShort(mStrings!!, offset)
			if (length != string.length) continue

			var j = 0
			while (j < length) {
				offset += 2
				if (string[j] != getShort(mStrings, offset).toChar()) break
				j++
			}

			if (j == length) return i
		}

		return -1
	}

	/**
	 * Returns style information - array of int triplets,
	 * where in each triplet:
	 *  * first int is index of tag name ('b','i', etc.)
	 *  * second int is tag start index in string
	 *  * third int is tag end index in string
	 */
	private fun getStyle(index: Int): IntArray? {
		if (mStyleOffsets == null || mStyles == null || index >= mStyleOffsets!!.size) {
			return null
		}

		val offset = mStyleOffsets!![index] / 4
		var count = 0
		var i = offset
		while (i < mStyles!!.size) {
			if (mStyles!![i] == -1) break
			count++
			i++
		}

		if (count == 0 || count % 3 != 0) return null

		val style = IntArray(count)
		i = offset
		var j = 0
		while (i < mStyles!!.size && j < style.size) {
			if (mStyles!![i] == -1) break
			style[j++] = mStyles!![i++]
		}

		return style
	}

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
			block.isUtf8 = (flags and 256) != 0

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