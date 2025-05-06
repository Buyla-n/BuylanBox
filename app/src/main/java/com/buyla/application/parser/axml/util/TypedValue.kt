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
package com.buyla.application.parser.axml.util

/**
 * @author Dmitry Skiba (original Java version)
 * @author Buylan (Kotlin conversion and optimizations)
 */
class TypedValue {
	var type: Int = 0
	var string: CharSequence? = null
	var data: Int = 0
	var assetCookie: Int = 0
	var resourceId: Int = 0
	var changingConfigurations: Int = 0

	companion object {
		const val TYPE_NULL = 0
		const val TYPE_REFERENCE = 1
		const val TYPE_ATTRIBUTE = 2
		const val TYPE_STRING = 3
		const val TYPE_FLOAT = 4
		const val TYPE_DIMENSION = 5
		const val TYPE_FRACTION = 6
		const val TYPE_FIRST_INT = 16
		const val TYPE_INT_DEC = 16
		const val TYPE_INT_HEX = 17
		const val TYPE_INT_BOOLEAN = 18
		const val TYPE_FIRST_COLOR_INT = 28
		const val TYPE_INT_COLOR_ARGB8 = 28
		const val TYPE_INT_COLOR_RGB8 = 29
		const val TYPE_INT_COLOR_ARGB4 = 30
		const val TYPE_INT_COLOR_RGB4 = 31
		const val TYPE_LAST_COLOR_INT = 31
		const val TYPE_LAST_INT = 31

		const val COMPLEX_UNIT_PX = 0
		const val COMPLEX_UNIT_DIP = 1
		const val COMPLEX_UNIT_SP = 2
		const val COMPLEX_UNIT_PT = 3
		const val COMPLEX_UNIT_IN = 4
		const val COMPLEX_UNIT_MM = 5
		const val COMPLEX_UNIT_SHIFT = 0
		const val COMPLEX_UNIT_MASK = 15
		const val COMPLEX_UNIT_FRACTION = 0
		const val COMPLEX_UNIT_FRACTION_PARENT = 1
		const val COMPLEX_RADIX_23p0 = 0
		const val COMPLEX_RADIX_16p7 = 1
		const val COMPLEX_RADIX_8p15 = 2
		const val COMPLEX_RADIX_0p23 = 3
		const val COMPLEX_RADIX_SHIFT = 4
		const val COMPLEX_RADIX_MASK = 3
		const val COMPLEX_MANTISSA_SHIFT = 8
		const val COMPLEX_MANTISSA_MASK = 0xFFFFFF
	}
}