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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
  * Scala Native implementation of gzip compression using java.util.zip. Requires linking to zlib
  * (-lz).
  */
trait GzipCompat extends GzipApi:
  private val BufferSize = 8192

  override def compress(data: Array[Byte]): Array[Byte] =
    val baos = ByteArrayOutputStream()
    val gzip = GZIPOutputStream(baos)
    try
      gzip.write(data)
      gzip.finish()
      baos.toByteArray
    finally
      gzip.close()

  override def decompress(data: Array[Byte]): Array[Byte] =
    val bais = ByteArrayInputStream(data)
    val gzip = GZIPInputStream(bais)
    try
      val baos   = ByteArrayOutputStream()
      val buffer = Array.ofDim[Byte](BufferSize)
      var len    = gzip.read(buffer)
      while len > 0 do
        baos.write(buffer, 0, len)
        len = gzip.read(buffer)
      baos.toByteArray
    finally
      gzip.close()

end GzipCompat
