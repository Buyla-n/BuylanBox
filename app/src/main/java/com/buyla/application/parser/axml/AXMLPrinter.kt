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
package com.buyla.application.parser.axml

import com.buyla.application.parser.axml.code.res.AXmlResourceParser
import com.buyla.application.parser.axml.util.TypedValue
import org.xmlpull.v1.XmlPullParser
import java.io.FileInputStream

/**
 * @author Dmitry Skiba (original Java version)
 * @author Buylan (Kotlin conversion and optimizations)
 *
 * This is an usage of AXMLParser class.
 *
 * Prints xml document from Android's binary xml file.
 */
object AXMLPrinter {

fun main(args: Array<String>): String {
	if (args.isEmpty()) {
		return "Usage: AXMLPrinter <binary xml file>"
	}
	return try {
		val parser = AXmlResourceParser()
		val startTime = System.currentTimeMillis()  // 记录开始时间
		parser.open(FileInputStream(args[0]))
		return buildString {
			var indent = ""
			val indentStep = "    "

			while (true) {
				val type = parser.next()
				if (type == XmlPullParser.END_DOCUMENT) {
					break
				}
				when (type) {
					XmlPullParser.START_DOCUMENT -> {
						//append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
					}
					XmlPullParser.START_TAG -> {
						append("$indent<${getNamespacePrefix(parser.prefix)}${parser.name}")
						indent += indentStep

						// Namespaces
						val namespaceCountBefore = parser.getNamespaceCount(parser.depth - 1)
						val namespaceCount = parser.getNamespaceCount(parser.depth)
						for (i in namespaceCountBefore until namespaceCount) {
							append("\n${indent}xmlns:${parser.getNamespacePrefix(i)}=\"${parser.getNamespaceUri(i)}\"")
						}

						// Attributes
						for (i in 0 until parser.attributeCount) {
							append("\n$indent${getNamespacePrefix(parser.getAttributePrefix(i))}${parser.getAttributeName(i)}=\"${getAttributeValue(parser, i)}\"")
						}
						append(">\n")
					}
					XmlPullParser.END_TAG -> {
						indent = indent.dropLast(indentStep.length)
						append("$indent</${getNamespacePrefix(parser.prefix)}${parser.name}>\n")
					}
					XmlPullParser.TEXT -> {
						append("$indent${parser.text}\n")
					}
				}
			}

			val elapsed = System.currentTimeMillis() - startTime  // 计算耗时
			println("Success, took: ${elapsed} ms")
		}
	} catch (e: Exception) {
		"Error: ${e.message}"
	}
}

private fun getNamespacePrefix(prefix: String?): String {
	return if (prefix.isNullOrEmpty()) "" else "$prefix:"
}

private fun getAttributeValue(parser: AXmlResourceParser, index: Int): String {
	val type = parser.getAttributeValueType(index)
	val data = parser.getAttributeValueData(index)
	return when (type) {
		TypedValue.TYPE_STRING -> parser.getAttributeValue(index)
		TypedValue.TYPE_ATTRIBUTE -> "?${getPackage(data)}${data.toHexString()}"
		TypedValue.TYPE_REFERENCE -> "@${getPackage(data)}${data.toHexString()}"
		TypedValue.TYPE_FLOAT -> java.lang.Float.intBitsToFloat(data).toString()
		TypedValue.TYPE_INT_HEX -> "0x${data.toHexString()}"
		TypedValue.TYPE_INT_BOOLEAN -> if (data != 0) "true" else "false"
		TypedValue.TYPE_DIMENSION -> "${complexToFloat(data)}${DIMENSION_UNITS[data and TypedValue.COMPLEX_UNIT_MASK]}"
		TypedValue.TYPE_FRACTION -> "${complexToFloat(data)}${FRACTION_UNITS[data and TypedValue.COMPLEX_UNIT_MASK]}"
		in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT -> "#%08X".format(data)
		in TypedValue.TYPE_FIRST_INT..TypedValue.TYPE_LAST_INT -> data.toString()
            else -> "<0x${data.toHexString()}, type 0x${type.toString(16).padStart(2, '0')}>"
	}
}

private fun Int.toHexString(): String = this.toString(16).padStart(8, '0').uppercase()

private fun getPackage(id: Int): String {
	return if (id ushr 24 == 1) "android:" else ""
}

private fun complexToFloat(complex: Int): Float {
	return (complex and 0xFFFFFF00.toInt()) * RADIX_MULTS[(complex shr 4) and 3]
}

private val RADIX_MULTS = floatArrayOf(
		0.00390625F, 3.051758E-005F, 1.192093E-007F, 4.656613E-010F
)
private val DIMENSION_UNITS = arrayOf(
		"px", "dp", "sp", "pt", "in", "mm", "", ""
)
private val FRACTION_UNITS = arrayOf(
		"%", "%p", "", "", "", "", "", ""
)
}