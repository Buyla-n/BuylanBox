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
	private var mStrings: IntArray? = null
	private var mStyleOffsets: IntArray? = null
	private var mStyles: IntArray? = null

	/**
	 * Returns number of strings in block.
	 */
	val count: Int
		get() = mStringOffsets?.size ?: 0

	/**
	 * Returns raw string (without any styling information) at specified index.
	 */
	fun getString(index: Int): String? {
		if (index < 0 || mStringOffsets == null || index >= mStringOffsets!!.size) {
			return null
		}
		val offset = mStringOffsets!![index]
		val length = getShort(mStrings!!, offset)
		val result = StringBuilder(length)
		var currentOffset = offset
		var remainingLength = length
		while (remainingLength != 0) {
			currentOffset += 2
			result.append(getShort(mStrings!!, currentOffset).toChar())
			remainingLength--
		}
		return result.toString()
	}

	/**
	 * Not yet implemented.
	 *
	 * Returns string with style information (if any).
	 */
	operator fun get(index: Int): CharSequence? = getString(index)

	/**
	 * Returns string with style tags (html-like).
	 */
	fun getHTML(index: Int): String? {
		val raw = getString(index) ?: return null
		val style = getStyle(index) ?: return raw

		val html = StringBuilder(raw.length + 32)
		var offset = 0

		while (true) {
			var i = -1
			for (j in style.indices step 3) {
				if (style[j + 1] == -1) continue
				if (i == -1 || style[i + 1] > style[j + 1]) {
					i = j
				}
			}

			val start = if (i != -1) style[i + 1] else raw.length

			for (j in style.indices step 3) {
				var end = style[j + 2]
				if (end == -1 || end >= start) continue

				if (offset <= end) {
					html.append(raw, offset, end + 1)
					offset = end + 1
				}
				style[j + 2] = -1
				html.append("</${getString(style[j])}>")
			}

			if (offset < start) {
				html.append(raw, offset, start)
				offset = start
			}

			if (i == -1) break

			html.append("<${getString(style[i])}>")
			style[i + 1] = -1
		}

		return html.toString()
	}

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
				if (string[j] != getShort(mStrings!!, offset).toChar()) break
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
		fun read(reader: IntReader): StringBlock {
			ChunkUtil.readCheckType(reader, CHUNK_TYPE)
			val chunkSize = reader.readInt()
			val stringCount = reader.readInt()
			val styleOffsetCount = reader.readInt()
			reader.readInt() // Unknown value (skipped)
			val stringsOffset = reader.readInt()
			val stylesOffset = reader.readInt()

			val block = StringBlock()
			block.mStringOffsets = reader.readIntArray(stringCount)

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