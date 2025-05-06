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

import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * @author Dmitry Skiba (original Java version)
 * @author Buylan (Kotlin conversion and optimizations)
 *
 * Simple helper class that allows reading of integers.
 *
 * TODO:
 *  * implement buffering
 */
class IntReader(private var stream: InputStream?, private var bigEndian: Boolean) {
	private var position: Int = 0

	fun reset(stream: InputStream?, bigEndian: Boolean) {
		this.stream = stream
		this.bigEndian = bigEndian
		this.position = 0
	}

	fun close() {
		stream?.let {
			try {
				it.close()
			} catch (e: IOException) {
				// Ignore
			}
		}
		reset(null, false)
	}

	fun getStream(): InputStream? = stream

	fun isBigEndian(): Boolean = bigEndian
	fun setBigEndian(bigEndian: Boolean) {
		this.bigEndian = bigEndian
	}

	@Throws(IOException::class)
	fun readByte(): Int = readInt(1)

	@Throws(IOException::class)
	fun readShort(): Int = readInt(2)

	@Throws(IOException::class)
	fun readInt(): Int = readInt(4)

	@Throws(IOException::class)
	fun readInt(length: Int): Int {
		if (length < 0 || length > 4) {
			throw IllegalArgumentException()
		}
		var result = 0
		if (bigEndian) {
			for (i in (length - 1) * 8 downTo 0 step 8) {
				val b = stream?.read() ?: throw EOFException()
				position += 1
				result = result or (b shl i)
			}
		} else {
			val totalBits = length * 8
			for (i in 0 until totalBits step 8) {
				val b = stream?.read() ?: throw EOFException()
				position += 1
				result = result or (b shl i)
			}
		}
		return result
	}

	@Throws(IOException::class)
	fun readIntArray(length: Int): IntArray {
		val array = IntArray(length)
		readIntArray(array, 0, length)
		return array
	}

	@Throws(IOException::class)
	fun readIntArray(array: IntArray, offset: Int, length: Int) {
		var currentOffset = offset
		var remaining = length
		while (remaining > 0) {
			array[currentOffset++] = readInt()
			remaining--
		}
	}

	@Throws(IOException::class)
	fun readByteArray(length: Int): ByteArray {
		val array = ByteArray(length)
		val read = stream?.read(array) ?: throw EOFException()
		position += read
		if (read != length) {
			throw EOFException()
		}
		return array
	}

	@Throws(IOException::class)
	fun skip(bytes: Int) {
		if (bytes <= 0) {
			return
		}
		val skipped = stream?.skip(bytes.toLong()) ?: throw EOFException()
		position += skipped.toInt()
		if (skipped != bytes.toLong()) {
			throw EOFException()
		}
	}

	@Throws(IOException::class)
	fun skipInt() = skip(4)

	@Throws(IOException::class)
	fun available(): Int = stream?.available() ?: throw IOException("Stream is null")

	fun getPosition(): Int = position
}