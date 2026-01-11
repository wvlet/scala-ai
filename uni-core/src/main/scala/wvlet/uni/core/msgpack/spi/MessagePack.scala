/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.uni.core.msgpack.spi

import java.io.InputStream
import java.io.OutputStream

import wvlet.uni.core.json.JSON
import wvlet.uni.core.json.JSONScanner
import wvlet.uni.core.json.JSONSource
import wvlet.uni.core.msgpack.json.NestedMessagePackBuilder
import wvlet.uni.core.msgpack.json.StreamMessagePackBuilder

/**
  */
object MessagePack:
  def newPacker(): BufferPacker                   = Compat.newBufferPacker
  def newUnpacker(msgpack: Array[Byte]): Unpacker = Compat.newUnpacker(msgpack)
  def newUnpacker(msgpack: Array[Byte], offset: Int, len: Int): Unpacker = Compat.newUnpacker(
    msgpack,
    offset,
    len
  )

  def fromJSON(json: String): MsgPack           = fromJSON(JSONSource.fromString(json))
  def fromJSON(jsonBytes: Array[Byte]): MsgPack = fromJSON(JSONSource.fromBytes(jsonBytes))
  def fromJSON(json: JSONSource): MsgPack       =
    val context = new StreamMessagePackBuilder()
    JSONScanner.scanAny(json, context)
    context.result
