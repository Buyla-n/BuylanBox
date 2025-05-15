package com.buyla.application.parser.axml

import java.io.IOException

/**
 * @author Dmitry Skiba (original Java version)
 * @author Buylan (Kotlin conversion and optimizations)
 *
 */
object ChunkUtil {

	@JvmStatic
	fun readCheckType(reader: IntReader, expectedType: Int) {
		val type = reader.readInt()
		if (type != expectedType) {
			throw IOException(
                "Expected chunk of type 0x${expectedType.toString(16)}, read 0x${
                    type.toString(
                        16
                    )
                }."
            )
		}
	}
}