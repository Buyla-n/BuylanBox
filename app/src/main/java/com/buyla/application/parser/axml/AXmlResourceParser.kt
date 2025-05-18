package com.buyla.application.parser.axml

import java.io.IOException
import java.io.InputStream

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
class AXmlResourceParser {

	fun open(stream: InputStream?) {
		close()
		if (stream != null) {
			indexReader = IntReader(stream, false)
			ChunkUtil.readCheckType(indexReader, CHUNK_AXML_FILE)
			indexReader.skipInt()
			stringBlocks = StringBlock.Companion.read(indexReader)
			namespaces.increaseDepth()
			operational = true
		}
	}

	fun close() {
		if (!operational) {
			return
		}
		operational = false
		indexReader.close()
		resourceIDs = IntArray(0)
		namespaces.reset()
		resetEventInfo()
	}

	fun next(): Int {
		try {
			doNext()
			return axmlEvent
		} catch (e: IOException) {
			close()
			throw e
		}
	}

	fun getName(): String? {
		if (intName == -1 || (axmlEvent != startTag && axmlEvent != endTag)) {
			return null
		}
		return stringBlocks.getString(intName)
	}

	fun getText(): String? {
		if (intName == -1 || axmlEvent != typeText) {
			return null
		}
		return stringBlocks.getString(intName)
	}

	fun getAttributePrefix(index: Int): String? {
		val prefix = namespaces.findPrefix(attributes[getAttributeOffset(index) + ATTRIBUTE_IX_NAMESPACE_URI])
		return if (prefix == -1) "" else { stringBlocks.getString(prefix) }
	}

	fun getAttributeName(index: Int): String {
		val name = attributes[getAttributeOffset(index) + ATTRIBUTE_IX_NAME]
		return if (name == -1) "" else stringBlocks.getString(name)!!
	}

	fun getAttributeValue(index: Int): String {
		val offset = getAttributeOffset(index)
		return if (attributes[offset + ATTRIBUTE_IX_VALUE_TYPE] == TypedValue.TYPE_STRING) stringBlocks.getString(attributes[offset + ATTRIBUTE_IX_VALUE_STRING])!! else ""
	}

	fun getDepth() = namespaces.depth - 1
	fun getPrefix() = stringBlocks.getString(namespaces.findPrefix(namespaceUri))
	fun getNamespaceCount(depth: Int) = namespaces.getAccumulatedCount(depth)
	fun getNamespacePrefix(pos: Int) = stringBlocks.getString(namespaces.getPrefix(pos))
	fun getNamespaceUri(pos: Int) = stringBlocks.getString(namespaces.getUri(pos))
	fun getAttributeCount() = if (axmlEvent != startTag) { -1 } else { attributes.size / ATTRIBUTE_LENGTH }
	fun getAttributeValueType(index: Int) = attributes[getAttributeOffset(index) + ATTRIBUTE_IX_VALUE_TYPE]
	fun getAttributeValueData(index: Int) = attributes[getAttributeOffset(index) + ATTRIBUTE_IX_VALUE_DATA]

	class NamespaceStack {
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

		fun getPrefix(index: Int) = get(index, true)
		fun getUri(index: Int) = get(index, false)
		fun findPrefix(uri: Int) = find(uri, false)

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

		var data: IntArray
		var dataLength = 0
		var totalCount: Int = 0
		var depth: Int = 0

		init {
			data = IntArray(32)
		}
	}

	private fun getAttributeOffset(index: Int): Int {
		if (axmlEvent != startTag) {
			throw IndexOutOfBoundsException("Current event is not START_TAG.")
		}
		val offset = index * 5
		if (offset >= attributes.size) {
			throw IndexOutOfBoundsException("Invalid attribute index ($index).")
		}
		return offset
	}

	private fun resetEventInfo() {
		axmlEvent = -1
		lineNumberCount = -1
		intName = -1
		namespaceUri = -1
		attributes = IntArray(0)
		idAttribute = -1
		classAttribute = -1
		styleAttribute = -1
	}

	private fun doNext() {

		if (axmlEvent == endDocument) {
			return
		}

		val event = axmlEvent
		resetEventInfo()

		while (true) {
			if (decreaseDepth) {
				decreaseDepth = false
				namespaces.decreaseDepth()
			}

			// Fake END_DOCUMENT event.
			if (event == endTag && namespaces.depth == 1 && namespaces.currentCount == 0) {
				axmlEvent = endDocument
				break
			}
            val chunkType: Int = if (event == startDocument) {
                // Fake event, see CHUNK_XML_START_TAG handler.
                CHUNK_XML_START_TAG
            } else {
                indexReader.readInt()
            }

			when {
				chunkType == CHUNK_RESOURCE_IDS -> {
					val chunkSize = indexReader.readInt()
					if (chunkSize < 8 || (chunkSize % 4) != 0) {
						throw IOException("Invalid resource ids size ($chunkSize).")
					}
					resourceIDs = indexReader.readIntArray(chunkSize / 4 - 2)
					continue
				}
				chunkType !in CHUNK_XML_FIRST..CHUNK_XML_LAST -> {
					throw IOException("Invalid chunk type ($chunkType).")
				}
				chunkType == CHUNK_XML_START_TAG && event == -1 -> {
					axmlEvent = startDocument
					break
				}
			}

			indexReader.skipInt()
			val lineNumber = indexReader.readInt()
			indexReader.skipInt()

			if (chunkType == CHUNK_XML_START_NAMESPACE || chunkType == CHUNK_XML_END_NAMESPACE) {
				if (chunkType == CHUNK_XML_START_NAMESPACE) {
					val prefix = indexReader.readInt()
					val uri = indexReader.readInt()
					namespaces.push(prefix, uri)
				} else {
					indexReader.skipInt() //prefix
					indexReader.skipInt() //uri
					namespaces.pop()
				}
				continue
			}

			lineNumberCount = lineNumber

			when(chunkType) {
				CHUNK_XML_TEXT -> {
					intName = indexReader.readInt()
					indexReader.skipInt()
					indexReader.skipInt()
					axmlEvent = typeText
					break
				}
				CHUNK_XML_END_TAG -> {
					namespaceUri = indexReader.readInt()
					intName = indexReader.readInt()
					axmlEvent = endTag
					decreaseDepth = true
					break
				}
				CHUNK_XML_START_TAG -> {
					namespaceUri = indexReader.readInt()
					intName = indexReader.readInt()
					//flags?
					indexReader.skipInt()
					var attributeCount = indexReader.readInt()
					idAttribute = (attributeCount ushr 16) - 1
					attributeCount = attributeCount and 0xFFFF
					classAttribute = indexReader.readInt()
					styleAttribute = (classAttribute ushr 16) - 1
					classAttribute = (classAttribute and 0xFFFF) - 1
					attributes = indexReader.readIntArray(attributeCount * ATTRIBUTE_LENGTH)
					var i = ATTRIBUTE_IX_VALUE_TYPE
					while (i < attributes.size) {
						attributes[i] = (attributes[i] ushr 24)
						i += ATTRIBUTE_LENGTH
					}
					namespaces.increaseDepth()
					axmlEvent = startTag
					break
				}
			}
		}
	}

	lateinit var indexReader: IntReader
	var operational = false
	lateinit var stringBlocks: StringBlock
	var resourceIDs: IntArray = IntArray(0)
	val namespaces = NamespaceStack()
	var decreaseDepth = false
	var axmlEvent = 0
	var lineNumberCount = 0
	var intName = 0
	var namespaceUri = 0
	var attributes: IntArray = IntArray(0)
	var idAttribute = 0
	var classAttribute = 0
	var styleAttribute = 0
	val startDocument: Int = 0
	val endDocument: Int = 1
	val startTag: Int = 2
	val endTag: Int = 3
	val typeText: Int = 4

	init {
		resetEventInfo()
	}

	companion object {
		const val ATTRIBUTE_IX_NAMESPACE_URI = 0
		const val ATTRIBUTE_IX_NAME = 1
		const val ATTRIBUTE_IX_VALUE_STRING = 2
		const val ATTRIBUTE_IX_VALUE_TYPE = 3
		const val ATTRIBUTE_IX_VALUE_DATA = 4
		const val ATTRIBUTE_LENGTH = 5
		const val CHUNK_AXML_FILE = 0x00080003
		const val CHUNK_RESOURCE_IDS = 0x00080180
		const val CHUNK_XML_FIRST = 0x00100100
		const val CHUNK_XML_START_NAMESPACE = 0x00100100
		const val CHUNK_XML_END_NAMESPACE = 0x00100101
		const val CHUNK_XML_START_TAG = 0x00100102
		const val CHUNK_XML_END_TAG = 0x00100103
		const val CHUNK_XML_TEXT = 0x00100104
		const val CHUNK_XML_LAST = 0x00100104
	}
}