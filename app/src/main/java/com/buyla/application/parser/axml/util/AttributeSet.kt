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
 *
 */
interface AttributeSet {
    fun getAttributeCount(): Int
    fun getAttributeName(index: Int): String
    fun getAttributeValue(index: Int): String
    fun getPositionDescription(): String
    fun getAttributeNameResource(index: Int): Int
    fun getAttributeListValue(index: Int, options: Array<String>, defaultValue: Int): Int
    fun getAttributeBooleanValue(index: Int, defaultValue: Boolean): Boolean
    fun getAttributeResourceValue(index: Int, defaultValue: Int): Int
    fun getAttributeIntValue(index: Int, defaultValue: Int): Int
    fun getAttributeUnsignedIntValue(index: Int, defaultValue: Int): Int
    fun getAttributeFloatValue(index: Int, defaultValue: Float): Float
    fun getIdAttribute(): String
    fun getClassAttribute(): String
    fun getIdAttributeResourceValue(index: Int): Int
    fun getStyleAttribute(): Int
    fun getAttributeValue(namespace: String?, attribute: String): String
    fun getAttributeListValue(namespace: String?, attribute: String, options: Array<String>, defaultValue: Int): Int
    fun getAttributeBooleanValue(namespace: String?, attribute: String, defaultValue: Boolean): Boolean
    fun getAttributeResourceValue(namespace: String?, attribute: String, defaultValue: Int): Int
    fun getAttributeIntValue(namespace: String?, attribute: String, defaultValue: Int): Int
    fun getAttributeUnsignedIntValue(namespace: String?, attribute: String, defaultValue: Int): Int
    fun getAttributeFloatValue(namespace: String?, attribute: String, defaultValue: Float): Float
    fun getAttributeValueType(index: Int): Int
    fun getAttributeValueData(index: Int): Int
}
