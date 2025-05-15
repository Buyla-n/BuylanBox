package com.buyla.application.parser.axml

import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * @author Dmitry Skiba (original Java version)
 * @author Buylan (Kotlin conversion and optimizations)
 *
 * Simple helper class that allows reading of integers.
 *
 *
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
			} catch (_: IOException) {
				// Ignore
			}
		}
		reset(null, false)
	}

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
}