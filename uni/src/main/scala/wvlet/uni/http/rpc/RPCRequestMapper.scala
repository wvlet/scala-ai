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
package wvlet.uni.http.rpc

import wvlet.uni.http.Request
import wvlet.uni.json.JSON
import wvlet.uni.json.JSON.*
import wvlet.uni.surface.{MethodParameter, MethodSurface, Primitive, Surface}

/**
  * Maps RPC request JSON body to method parameters.
  *
  * Supports two JSON formats:
  *   - Positional args: `[arg1, arg2, arg3]`
  *   - Named args: `{"param1": value1, "param2": value2}`
  */
class RPCRequestMapper:

  /**
    * Bind RPC request JSON body to method parameters.
    *
    * @param request
    *   The incoming HTTP request with JSON body
    * @param methodSurface
    *   The method surface describing the RPC endpoint
    * @param controllerOpt
    *   Optional controller instance for accessing method arg default values
    * @return
    *   A sequence of argument values in the order expected by the method
    */
  def bindParameters(
      request: Request,
      methodSurface: MethodSurface,
      controllerOpt: Option[Any] = None
  ): Seq[Any] =
    val jsonBody = request.content.asString.getOrElse("[]")
    val json     =
      try
        JSON.parse(jsonBody)
      catch
        case e: Exception =>
          throw RPCStatus.INVALID_REQUEST_U1.newException(s"Invalid JSON body: ${e.getMessage}")

    json match
      case arr: JSONArray =>
        // Positional args: [arg1, arg2, ...]
        bindPositionalArgs(arr, methodSurface, controllerOpt)
      case obj: JSONObject =>
        // Named args: {"param1": value1, ...}
        bindNamedArgs(obj, methodSurface, controllerOpt)
      case _ =>
        // Empty or invalid - try with no args
        methodSurface
          .args
          .map { param =>
            getDefaultValue(param, controllerOpt).getOrElse(
              throw RPCStatus
                .INVALID_ARGUMENT_U2
                .newException(s"Required parameter '${param.name}' is missing")
            )
          }

  end bindParameters

  private def bindPositionalArgs(
      arr: JSONArray,
      methodSurface: MethodSurface,
      controllerOpt: Option[Any]
  ): Seq[Any] = methodSurface
    .args
    .zipWithIndex
    .map { case (param, idx) =>
      if idx < arr.size then
        convertJsonValue(arr(idx), param.surface, param.name)
      else
        getDefaultValue(param, controllerOpt).getOrElse(
          throw RPCStatus
            .INVALID_ARGUMENT_U2
            .newException(s"Required parameter '${param.name}' is missing (index ${idx})")
        )
    }

  private def bindNamedArgs(
      obj: JSONObject,
      methodSurface: MethodSurface,
      controllerOpt: Option[Any]
  ): Seq[Any] = methodSurface
    .args
    .map { param =>
      obj.get(param.name) match
        case Some(jsonValue) =>
          convertJsonValue(jsonValue, param.surface, param.name)
        case None =>
          getDefaultValue(param, controllerOpt).getOrElse(
            throw RPCStatus
              .INVALID_ARGUMENT_U2
              .newException(s"Required parameter '${param.name}' is missing")
          )
    }

  private def getDefaultValue(param: MethodParameter, controllerOpt: Option[Any]): Option[Any] =
    // Try method arg default value first (requires controller instance)
    controllerOpt
      .flatMap(ctrl => param.getMethodArgDefaultValue(ctrl))
      .orElse(param.getDefaultValue)
      .orElse {
        // For Option types, treat missing values as None
        if param.surface.isOption then
          Some(None)
        else
          None
      }

  /**
    * Convert a JSON value to the expected Scala type
    */
  private def convertJsonValue(jsonValue: JSONValue, surface: Surface, paramName: String): Any =
    jsonValue match
      case JSONNull =>
        if surface.isOption then
          None
        else
          throw RPCStatus
            .INVALID_ARGUMENT_U2
            .newException(s"Parameter '${paramName}' cannot be null (expected ${surface.name})")

      case JSONBoolean(boolVal) =>
        surface match
          case Primitive.Boolean =>
            boolVal
          case _ if surface.isOption =>
            Some(boolVal)
          case _ =>
            throw RPCStatus
              .INVALID_ARGUMENT_U2
              .newException(s"Parameter '${paramName}' expects ${surface.name}, got boolean")

      case JSONLong(longVal) =>
        convertNumber(longVal, surface, paramName)

      case JSONDouble(doubleVal) =>
        convertNumber(doubleVal, surface, paramName)

      case JSONString(strVal) =>
        surface match
          case Primitive.String =>
            strVal
          case _ if surface.isOption && surface.typeArgs.headOption.exists(_ == Primitive.String) =>
            Some(strVal)
          case _ if surface.isEnum =>
            // Try to parse as enum using reflection
            try
              val cls        = surface.rawType
              val enumValues = cls.getEnumConstants
              if enumValues != null then
                enumValues
                  .find(_.toString == strVal)
                  .getOrElse(
                    throw RPCStatus
                      .INVALID_ARGUMENT_U2
                      .newException(s"Invalid enum value '${strVal}' for parameter '${paramName}'")
                  )
              else
                strVal
            catch
              case e: RPCException =>
                throw e
              case _: Exception =>
                strVal
          case _ =>
            throw RPCStatus
              .INVALID_ARGUMENT_U2
              .newException(s"Parameter '${paramName}' expects ${surface.name}, got string")

      case arr: JSONArray =>
        if surface.isSeq then
          val elemSurface = surface.typeArgs.headOption.getOrElse(Primitive.String)
          arr
            .v
            .zipWithIndex
            .map { case (elem, idx) =>
              convertJsonValue(elem, elemSurface, s"${paramName}[${idx}]")
            }
        else if surface.isOption then
          // Handle Option[Seq[T]] by converting elements if inner type is Seq
          val innerSurface = surface.typeArgs.headOption
          innerSurface match
            case Some(inner) if inner.isSeq =>
              val elemSurface = inner.typeArgs.headOption.getOrElse(Primitive.String)
              Some(
                arr
                  .v
                  .zipWithIndex
                  .map { case (elem, idx) =>
                    convertJsonValue(elem, elemSurface, s"${paramName}[${idx}]")
                  }
              )
            case _ =>
              // Unknown inner type for Option, convert elements and wrap
              Some(arr.v.map(elem => convertJsonValue(elem, Primitive.String, paramName)))
        else
          // Array for non-Seq/non-Option type is a type mismatch
          throw RPCStatus
            .INVALID_ARGUMENT_U2
            .newException(s"Parameter '${paramName}' expects ${surface.name}, got array")

      case obj: JSONObject =>
        // Complex object deserialization requires compile-time generated weavers.
        // For now, only primitive types and sequences are supported.
        // Complex object parameters will be supported with the RPC code generator.
        throw RPCStatus
          .INVALID_ARGUMENT_U2
          .newException(
            s"Parameter '${paramName}' has unsupported type ${surface.name}. " +
              "Complex object parameters are not yet supported in RPC."
          )

  private def convertNumber(n: Number, surface: Surface, paramName: String): Any =
    surface match
      case Primitive.Int =>
        n.intValue()
      case Primitive.Long =>
        n.longValue()
      case Primitive.Double =>
        n.doubleValue()
      case Primitive.Float =>
        n.floatValue()
      case Primitive.Short =>
        n.shortValue()
      case Primitive.Byte =>
        n.byteValue()
      case Primitive.BigInt =>
        BigInt(n.longValue())
      case _ if surface.isOption =>
        val inner = surface.typeArgs.headOption.getOrElse(Primitive.Long)
        Some(convertNumber(n, inner, paramName))
      case _ =>
        throw RPCStatus
          .INVALID_ARGUMENT_U2
          .newException(s"Parameter '${paramName}' expects ${surface.name}, got number")

end RPCRequestMapper
