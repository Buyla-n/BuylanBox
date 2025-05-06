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
 */
object ChunkUtil {

	@JvmStatic
	fun readCheckType(reader: IntReader, expectedType: Int) {
		val type = reader.readInt()
		if (type != expectedType) {
			throw IOException("Expected chunk of type 0x${expectedType.toString(16)}, read 0x${type.toString(16)}.")
		}
	}
}