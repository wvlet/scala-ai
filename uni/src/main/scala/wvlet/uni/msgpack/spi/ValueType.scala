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
package wvlet.uni.msgpack.spi;

enum ValueType(val isNumber: Boolean, val isRaw: Boolean):
  def name: String = toString
  case NIL       extends ValueType(false, false)
  case BOOLEAN   extends ValueType(false, false)
  case INTEGER   extends ValueType(true, false)
  case FLOAT     extends ValueType(true, false)
  case STRING    extends ValueType(false, true)
  case BINARY    extends ValueType(false, true)
  case ARRAY     extends ValueType(false, false)
  case MAP       extends ValueType(false, false)
  case EXTENSION extends ValueType(false, false)

  // abstract class ExtValueType
  // Timestamp value should be reported as a first-class value?
  // TODO: But we cannot determine the value type without reading the second or more bytes

//  sealed trait ExtValueType
//  object Ext {
//    case TIMESTAMP extends ExtValueType
//  }
