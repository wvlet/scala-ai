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
package wvlet.uni.io

/**
  * Cross-platform gzip compression utilities.
  *
  * Platform implementations:
  *   - JVM: Uses java.util.zip.GZIPOutputStream/GZIPInputStream
  *   - Scala.js (Node.js): Uses Node's zlib module
  *   - Scala Native: Uses java.util.zip (requires linking to zlib)
  */
object Gzip extends GzipCompat

/**
  * Base trait for gzip operations. Platform-specific implementations extend this trait.
  */
trait GzipApi:
  /**
    * Compresses the given data using gzip.
    *
    * @param data
    *   The data to compress
    * @return
    *   The compressed data
    */
  def compress(data: Array[Byte]): Array[Byte]

  /**
    * Decompresses the given gzip-compressed data.
    *
    * @param data
    *   The compressed data
    * @return
    *   The decompressed data
    */
  def decompress(data: Array[Byte]): Array[Byte]

  /**
    * Compresses a file using gzip.
    *
    * @param source
    *   The source file path
    * @param target
    *   The target file path (typically with .gz extension)
    */
  def compressFile(source: IOPath, target: IOPath): Unit =
    val data       = FileSystem.readBytes(source)
    val compressed = compress(data)
    FileSystem.writeBytes(target, compressed)

  /**
    * Decompresses a gzip file.
    *
    * @param source
    *   The compressed file path
    * @param target
    *   The target file path for decompressed content
    */
  def decompressFile(source: IOPath, target: IOPath): Unit =
    val compressed   = FileSystem.readBytes(source)
    val decompressed = decompress(compressed)
    FileSystem.writeBytes(target, decompressed)

end GzipApi
