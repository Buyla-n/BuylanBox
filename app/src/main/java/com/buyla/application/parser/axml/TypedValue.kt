package com.buyla.application.parser.axml

/**
 * @author Dmitry Skiba (original Java version)
 * @author Buylan (Kotlin conversion and optimizations)
 */
object TypedValue {
	const val TYPE_REFERENCE = 1
	const val TYPE_ATTRIBUTE = 2
	const val TYPE_STRING = 3
	const val TYPE_FLOAT = 4
	const val TYPE_DIMENSION = 5
	const val TYPE_FRACTION = 6
	const val TYPE_FIRST_INT = 16
	const val TYPE_INT_HEX = 17
	const val TYPE_INT_BOOLEAN = 18
	const val TYPE_FIRST_COLOR_INT = 28
	const val TYPE_LAST_COLOR_INT = 31
	const val TYPE_LAST_INT = 31
	const val COMPLEX_UNIT_MASK = 15
}