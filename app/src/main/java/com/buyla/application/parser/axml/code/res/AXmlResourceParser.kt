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

import com.buyla.application.parser.axml.util.TypedValue
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.io.Reader

/**
 * @author Dmitry Skiba (original Java version)
 * @author Buylan (Kotlin conversion and optimizations)
 *
 * @see
 * Binary xml files parser.
 *
 *
 * Parser has only two states:
 * (1) Operational state, which parser obtains after first successful call
 * to next() and retains until open(), close(), or failed call to next().
 * (2) Closed state, which parser obtains after open(), close(), or failed
 * call to next(). In this state methods return invalid values or throw exceptions.
 *
 *
 *
 * * check all methods in closed state
 */
class AXmlResourceParser : XmlPullParser {

	fun open(stream: InputStream?) {
		close()
		if (stream != null) {
			indexReader = IntReader(stream, false)
		}
	}

	fun close() {
		if (!operational) {
			return
		}
		operational = false
		indexReader!!.close()
		indexReader = null
		stringBlocks = null
		resourceIDs = null
		namespaces.reset()
		resetEventInfo()
	}

	override fun next(): Int {
		if (indexReader == null) {
			throw XmlPullParserException("Parser is not opened.", this, null)
		}
		try {
			doNext()
			return event
		} catch (e: IOException) {
			close()
			throw e
		}
	}

	override fun nextToken(): Int {
		return next()
	}

	override fun nextTag(): Int {
		var eventType = next()
		if (eventType == XmlPullParser.TEXT && isWhitespace) {
			eventType = next()
		}
		if (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_TAG) {
			throw XmlPullParserException("Expected start or end tag.", this, null)
		}
		return eventType
	}

	override fun nextText(): String? {
		if (eventType != XmlPullParser.START_TAG) {
			throw XmlPullParserException(
				"Parser must be on START_TAG to read next text.",
				this,
				null
			)
		}
		var eventType = next()
		if (eventType == XmlPullParser.TEXT) {
			val result = getText()
			eventType = next()
			if (eventType != XmlPullParser.END_TAG) {
				throw XmlPullParserException(
					"Event TEXT must be immediately followed by END_TAG.",
					this,
					null
				)
			}
			return result
		} else if (eventType == XmlPullParser.END_TAG) {
			return ""
		} else {
			throw XmlPullParserException(
				"Parser must be on START_TAG or TEXT to read text.",
				this,
				null
			)
		}
	}

	override fun require(type: Int, namespace: String?, name: String?) {
		if (type != eventType ||
			(namespace != null && namespace != getNamespace()) ||
			(name != null && name != getName())
		) {
			throw XmlPullParserException(XmlPullParser.TYPES[type] + " is expected.", this, null)
		}
	}

	override fun getDepth(): Int {
		return namespaces.depth - 1
	}

	override fun getEventType(): Int {
		return event
	}

	override fun getLineNumber(): Int {
		return lineNumberCount
	}

	override fun getName(): String? {
		if (intName == -1 || (event != XmlPullParser.START_TAG && event != XmlPullParser.END_TAG)) {
			return null
		}
		return stringBlocks!!.getString(intName)
	}

	override fun getText(): String? {
		if (intName == -1 || event != XmlPullParser.TEXT) {
			return null
		}
		return stringBlocks!!.getString(intName)
	}

	override fun getTextCharacters(holderForStartAndLength: IntArray): CharArray? {
		val text = getText()
		if (text == null) {
			return null
		}
		holderForStartAndLength[0] = 0
		holderForStartAndLength[1] = text.length
		val chars = CharArray(text.length)
		text.toCharArray(chars, 0, 0, text.length)
		return chars
	}

	override fun getNamespace(): String? {
		return stringBlocks!!.getString(namespaceUri)
	}

	override fun getPrefix(): String? {
		val prefix = namespaces.findPrefix(namespaceUri)
		return stringBlocks!!.getString(prefix)
	}

	override fun getPositionDescription(): String {
		return "XML line #$lineNumber"
	}

	override fun getNamespaceCount(depth: Int): Int {
		return namespaces.getAccumulatedCount(depth)
	}

	override fun getNamespacePrefix(pos: Int): String? {
		val prefix = namespaces.getPrefix(pos)
		return stringBlocks!!.getString(prefix)
	}

	override fun getNamespaceUri(pos: Int): String? {
		val uri = namespaces.getUri(pos)
		return stringBlocks!!.getString(uri)
	}

	override fun getAttributeCount(): Int {
		if (event != XmlPullParser.START_TAG) {
			return -1
		}
		return attributes.size / ATTRIBUTE_LENGTH
	}

	override fun getAttributeNamespace(index: Int): String? {
		val offset = getAttributeOffset(index)
		val namespace = attributes[offset + ATTRIBUTE_IX_NAMESPACE_URI]
		if (namespace == -1) {
			return ""
		}
		return stringBlocks!!.getString(namespace)
	}

	override fun getAttributePrefix(index: Int): String? {
		val offset = getAttributeOffset(index)
		val uri = attributes[offset + ATTRIBUTE_IX_NAMESPACE_URI]
		val prefix = namespaces.findPrefix(uri)
		if (prefix == -1) {
			return ""
		}
		return stringBlocks!!.getString(prefix)
	}

	override fun getAttributeName(index: Int): String {
		val offset = getAttributeOffset(index)
		val name = attributes[offset + ATTRIBUTE_IX_NAME]
		if (name == -1) {
			return ""
		}
		return stringBlocks!!.getString(name)!!
	}

	fun getAttributeValueType(index: Int): Int {
		val offset = getAttributeOffset(index)
		return attributes[offset + ATTRIBUTE_IX_VALUE_TYPE]
	}

	 fun getAttributeValueData(index: Int): Int {
		val offset = getAttributeOffset(index)
		return attributes[offset + ATTRIBUTE_IX_VALUE_DATA]
	}

	override fun getAttributeValue(index: Int): String {
		val offset = getAttributeOffset(index)
		val valueType = attributes[offset + ATTRIBUTE_IX_VALUE_TYPE]
		if (valueType == TypedValue.TYPE_STRING) {
			val valueString = attributes[offset + ATTRIBUTE_IX_VALUE_STRING]
			return stringBlocks!!.getString(valueString)!!
		}
		return ""
	}

	override fun getAttributeValue(namespace: String?, attribute: String): String {
		val index = findAttribute(namespace, attribute)
		if (index == -1) {
			return ""
		}
		return getAttributeValue(index)
	}

	override fun getAttributeType(index: Int): String {
		return "CDATA"
	}

	override fun isAttributeDefault(index: Int): Boolean {
		return false
	}

	override fun setInput(stream: InputStream?, inputEncoding: String?) {
		throw XmlPullParserException(E_NOT_SUPPORTED)
	}

	override fun setInput(reader: Reader?) {
		throw XmlPullParserException(E_NOT_SUPPORTED)
	}

	override fun getInputEncoding(): String? {
		return null
	}

	override fun getColumnNumber(): Int {
		return -1
	}

	override fun isEmptyElementTag(): Boolean {
		return false
	}

	override fun isWhitespace(): Boolean {
		return false
	}

	override fun defineEntityReplacementText(entityName: String?, replacementText: String?) {
		throw XmlPullParserException(E_NOT_SUPPORTED)
	}

	override fun getNamespace(prefix: String?): String? {
		throw RuntimeException(E_NOT_SUPPORTED)
	}

	override fun getProperty(name: String?): Any? {
		return null
	}

	override fun setProperty(name: String?, value: Any?) {
		throw XmlPullParserException(E_NOT_SUPPORTED)
	}

	override fun getFeature(feature: String?): Boolean {
		return false
	}

	override fun setFeature(name: String?, value: Boolean) {
		throw XmlPullParserException(E_NOT_SUPPORTED)
	}

	/**
	* Namespace stack, holds prefix+uri pairs, as well as
	* depth information.
	* All information is stored in one int[] array.
	* Array consists of depth frames:
	* Data=DepthFrame*;
	* DepthFrame=Count+[Prefix+Uri]*+Count;
	* Count='count of Prefix+Uri pairs';
	* Yes, count is stored twice, to enable bottom-up traversal.
	* increaseDepth adds depth frame, decreaseDepth removes it.
	* push/pop operations operate only in current depth frame.
	* decreaseDepth removes any remaining (not popped) namespace pairs.
	* findXXX methods search all depth frames starting
	* from the last namespace pair of current depth frame.
	* All functions that operate with int, use -1 as 'invalid value'.
	*
	* !! functions expect 'prefix'+'uri' pairs, not 'uri'+'prefix' !!
	*
	*/
	private class NamespaceStack {
		fun reset() {
			dataLength = 0
			this.totalCount = 0
			this.depth = 0
		}

		val currentCount: Int
			get() {
				if (dataLength == 0) {
					return 0
				}
				val offset = dataLength - 1
				return data[offset]
			}

		fun getAccumulatedCount(depth: Int): Int {
			var depth = depth
			if (dataLength == 0 || depth < 0) {
				return 0
			}
			if (depth > this.depth) {
				depth = this.depth
			}
			var accumulatedCount = 0
			var offset = 0
			while (depth != 0) {
				val count = data[offset]
				accumulatedCount += count
				offset += (2 + count * 2)
				--depth
			}
			return accumulatedCount
		}

		fun push(prefix: Int, uri: Int) {
			if (this.depth == 0) {
				increaseDepth()
			}
			ensureDataCapacity(2)
			val offset = dataLength - 1
			val count = data[offset]
			data[offset - 1 - count * 2] = count + 1
			data[offset] = prefix
			data[offset + 1] = uri
			data[offset + 2] = count + 1
			dataLength += 2
			this.totalCount += 1
		}

		fun pop(): Boolean {
			if (dataLength == 0) {
				return false
			}
			var offset = dataLength - 1
			var count = data[offset]
			if (count == 0) {
				return false
			}
			count -= 1
			offset -= 2
			data[offset] = count
			offset -= (1 + count * 2)
			data[offset] = count
			dataLength -= 2
			this.totalCount -= 1
			return true
		}

		fun getPrefix(index: Int): Int {
			return get(index, true)
		}

		fun getUri(index: Int): Int {
			return get(index, false)
		}

		fun findPrefix(uri: Int): Int {
			return find(uri, false)
		}

		fun increaseDepth() {
			ensureDataCapacity(2)
			val offset = dataLength
			data[offset] = 0
			data[offset + 1] = 0
			dataLength += 2
			this.depth += 1
		}

		fun decreaseDepth() {
			if (dataLength == 0) {
				return
			}
			val offset = dataLength - 1
			val count = data[offset]
			if ((offset - 1 - count * 2) == 0) {
				return
			}
			dataLength -= 2 + count * 2
			this.totalCount -= count
			this.depth -= 1
		}

		fun ensureDataCapacity(capacity: Int) {
			val available = (data.size - dataLength)
			if (available > capacity) {
				return
			}
			val newLength = (data.size + available) * 2
			val newData = IntArray(newLength)
			System.arraycopy(data, 0, newData, 0, dataLength)
			data = newData
		}

		fun find(prefixOrUri: Int, prefix: Boolean): Int {
			if (dataLength == 0) {
				return -1
			}
			var offset = dataLength - 1
			repeat(this.depth) {
				var count = data[offset]
				offset -= 2
				while (count != 0) {
					if (prefix) {
						if (data[offset] == prefixOrUri) {
							return data[offset + 1]
						}
					} else {
						if (data[offset + 1] == prefixOrUri) {
							return data[offset]
						}
					}
					offset -= 2
					--count
				}
			}
			return -1
		}

		fun get(index: Int, prefix: Boolean): Int {
			var index = index
			if (dataLength == 0 || index < 0) {
				return -1
			}
			var offset = 0
			var remainingDepth = this.depth
			while (remainingDepth-- > 0) {
				val count = data[offset]
				if (index >= count) {
					index -= count
					offset += (2 + count * 2)
					continue
				}
				offset += (1 + index * 2)
				if (!prefix) {
					offset += 1
				}
				return data[offset]
			}
			return -1
		}

		private var data: IntArray
		private var dataLength = 0
		var totalCount: Int = 0
			private set
		var depth: Int = 0
			private set

		init {
			data = IntArray(32)
		}
	}

	private fun getAttributeOffset(index: Int): Int {
		if (event != XmlPullParser.START_TAG) {
			throw IndexOutOfBoundsException("Current event is not START_TAG.")
		}
		val offset = index * 5
		if (offset >= attributes.size) {
			throw IndexOutOfBoundsException("Invalid attribute index ($index).")
		}
		return offset
	}

	private fun findAttribute(namespace: String?, attribute: String?): Int {
		if (stringBlocks == null || attribute == null) {
			return -1
		}
		val name = stringBlocks!!.find(attribute)
		if (name == -1) {
			return -1
		}
		val uri = if (namespace != null) stringBlocks!!.find(namespace) else -1
		for (o in attributes.indices) {
			if (name == attributes[o + ATTRIBUTE_IX_NAME] &&
				(uri == -1 || uri == attributes[o + ATTRIBUTE_IX_NAMESPACE_URI])
			) {
				return o / ATTRIBUTE_LENGTH
			}
		}
		return -1
	}

	private fun resetEventInfo() {
		event = -1
		lineNumberCount = -1
		intName = -1
		namespaceUri = -1
		attributes = IntArray(0)
		idAttribute = -1
		classAttribute = -1
		styleAttribute = -1
	}

	private fun doNext() {
		// Delayed initialization.
		if (stringBlocks == null) {
			ChunkUtil.readCheckType(indexReader!!, CHUNK_AXML_FILE)
			/*chunkSize*/
			indexReader!!.skipInt()
			stringBlocks = StringBlock.Companion.read(indexReader!!)
			namespaces.increaseDepth()
			operational = true
		}

		if (event == XmlPullParser.END_DOCUMENT) {
			return
		}

		val event = event
		resetEventInfo()

		while (true) {
			if (decreaseDepth) {
				decreaseDepth = false
				namespaces.decreaseDepth()
			}


			// Fake END_DOCUMENT event.
			if (event == XmlPullParser.END_TAG && namespaces.depth == 1 && namespaces.currentCount == 0) {
				this@AXmlResourceParser.event = XmlPullParser.END_DOCUMENT
				break
			}
            val chunkType: Int = if (event == XmlPullParser.START_DOCUMENT) {
                // Fake event, see CHUNK_XML_START_TAG handler.
                CHUNK_XML_START_TAG
            } else {
                indexReader!!.readInt()
            }

			if (chunkType == CHUNK_RESOURCE_IDS) {
				val chunkSize = indexReader!!.readInt()
				if (chunkSize < 8 || (chunkSize % 4) != 0) {
					throw IOException("Invalid resource ids size ($chunkSize).")
				}
				resourceIDs = indexReader!!.readIntArray(chunkSize / 4 - 2)
				continue
			}

			if (chunkType < CHUNK_XML_FIRST || chunkType > CHUNK_XML_LAST) {
				throw IOException("Invalid chunk type ($chunkType).")
			}

			// Fake START_DOCUMENT event.
			if (chunkType == CHUNK_XML_START_TAG && event == -1) {
				this@AXmlResourceParser.event = XmlPullParser.START_DOCUMENT
				break
			}


			// Common header.
			/*chunkSize*/
			indexReader!!.skipInt()
			val lineNumber = indexReader!!.readInt()
			/*0xFFFFFFFF*/
			indexReader!!.skipInt()

			if (chunkType == CHUNK_XML_START_NAMESPACE ||
				chunkType == CHUNK_XML_END_NAMESPACE
			) {
				if (chunkType == CHUNK_XML_START_NAMESPACE) {
					val prefix = indexReader!!.readInt()
					val uri = indexReader!!.readInt()
					namespaces.push(prefix, uri)
				} else {
					/*prefix*/
					indexReader!!.skipInt()
					/*uri*/
					indexReader!!.skipInt()
					namespaces.pop()
				}
				continue
			}

			lineNumberCount = lineNumber

			if (chunkType == CHUNK_XML_START_TAG) {
				namespaceUri = indexReader!!.readInt()
				intName = indexReader!!.readInt()
				/*flags?*/
				indexReader!!.skipInt()
				var attributeCount = indexReader!!.readInt()
				idAttribute = (attributeCount ushr 16) - 1
				attributeCount = attributeCount and 0xFFFF
				classAttribute = indexReader!!.readInt()
				styleAttribute = (classAttribute ushr 16) - 1
				classAttribute = (classAttribute and 0xFFFF) - 1
				attributes = indexReader!!.readIntArray(attributeCount * ATTRIBUTE_LENGTH)
				var i = ATTRIBUTE_IX_VALUE_TYPE
				while (i < attributes.size) {
					attributes[i] = (attributes[i] ushr 24)
					i += ATTRIBUTE_LENGTH
				}
				namespaces.increaseDepth()
				this@AXmlResourceParser.event = XmlPullParser.START_TAG
				break
			}

			if (chunkType == CHUNK_XML_END_TAG) {
				namespaceUri = indexReader!!.readInt()
				intName = indexReader!!.readInt()
				this@AXmlResourceParser.event = XmlPullParser.END_TAG
				decreaseDepth = true
				break
			}

			if (chunkType == CHUNK_XML_TEXT) {
				intName = indexReader!!.readInt()
				indexReader!!.skipInt()
				indexReader!!.skipInt()
				this@AXmlResourceParser.event = XmlPullParser.TEXT
				break
			}
		}
	}

	/**
	* All values are essentially indices, e.g. m_name is
	* an index of name in m_strings.
	*/
	private var indexReader: IntReader? = null
	private var operational = false

	var stringBlocks: StringBlock? = null
	private var resourceIDs: IntArray?
	private val namespaces = NamespaceStack()

	private var decreaseDepth = false

	private var event = 0
	private var lineNumberCount = 0
	private var intName = 0
	private var namespaceUri = 0
	private var attributes: IntArray
	private var idAttribute = 0
	private var classAttribute = 0
	private var styleAttribute = 0

	init {
		resetEventInfo()
	}

	companion object {
		private const val E_NOT_SUPPORTED = "Method is not supported."

		private const val ATTRIBUTE_IX_NAMESPACE_URI = 0
		private const val ATTRIBUTE_IX_NAME = 1
		private const val ATTRIBUTE_IX_VALUE_STRING = 2
		private const val ATTRIBUTE_IX_VALUE_TYPE = 3
		private const val ATTRIBUTE_IX_VALUE_DATA = 4
		private const val ATTRIBUTE_LENGTH = 5

		private const val CHUNK_AXML_FILE = 0x00080003
		private const val CHUNK_RESOURCE_IDS = 0x00080180
		private const val CHUNK_XML_FIRST = 0x00100100
		private const val CHUNK_XML_START_NAMESPACE = 0x00100100
		private const val CHUNK_XML_END_NAMESPACE = 0x00100101
		private const val CHUNK_XML_START_TAG = 0x00100102
		private const val CHUNK_XML_END_TAG = 0x00100103
		private const val CHUNK_XML_TEXT = 0x00100104
		private const val CHUNK_XML_LAST = 0x00100104
	}
}