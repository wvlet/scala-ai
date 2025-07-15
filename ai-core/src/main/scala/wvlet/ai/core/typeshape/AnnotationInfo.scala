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

package wvlet.ai.core.typeshape

import scala.quoted.*

/**
  * Sealed trait representing different types of annotation parameter values
  * to support proper serialization and type safety.
  */
sealed trait AnnotationValue extends Serializable

object AnnotationValue:
  case class PrimitiveValue(value: Any) extends AnnotationValue
  case class ClassValue(className: String) extends AnnotationValue  
  case class EnumValue(enumClass: String, enumValue: String) extends AnnotationValue
  case class ArrayValue(elements: Seq[AnnotationValue]) extends AnnotationValue
  case class NestedAnnotation(annotation: AnnotationInfo) extends AnnotationValue

/**
  * Runtime representation of annotation metadata captured at compile time.
  * 
  * @param annotationTypeName The fully qualified name of the annotation type
  * @param parameters Map of parameter names to their values
  */
case class AnnotationInfo(
  annotationTypeName: String,
  parameters: Map[String, Any]
) extends Serializable:

  /**
    * Get a parameter value by name
    */
  def getParameter(name: String): Option[Any] = parameters.get(name)

  /**
    * Get a parameter value by name with a default value
    */
  def getParameterOrElse[T](name: String, default: T): T = 
    parameters.get(name).map(_.asInstanceOf[T]).getOrElse(default)

  /**
    * Check if a parameter exists
    */
  def hasParameter(name: String): Boolean = parameters.contains(name)

  /**
    * Get the "value" parameter (common for simple annotations)
    */
  def getValue: Option[Any] = getParameter("value")

  /**
    * Get the "value" parameter as a specific type
    */
  def getValueAs[T]: Option[T] = getValue.map(_.asInstanceOf[T])

  /**
    * Get a parameter value as a specific type with type safety
    */
  def getParameterAs[T](name: String): Option[T] = 
    parameters.get(name).map(_.asInstanceOf[T])

  /**
    * Get all parameter names
    */
  def parameterNames: Set[String] = parameters.keySet

  /**
    * Check if this annotation has no parameters
    */
  def isEmpty: Boolean = parameters.isEmpty

  /**
    * Check if this annotation has parameters
    */
  def nonEmpty: Boolean = parameters.nonEmpty

  /**
    * Get the simple name of the annotation (without package)
    */
  def simpleName: String = 
    val lastDot = annotationTypeName.lastIndexOf('.')
    if lastDot >= 0 then annotationTypeName.substring(lastDot + 1) else annotationTypeName

  override def toString: String = 
    if parameters.isEmpty then
      s"@$simpleName"
    else if parameters.size == 1 && parameters.contains("value") then
      s"@$simpleName(${formatValue(parameters("value"))})"
    else
      val paramStr = parameters.map { case (k, v) => s"$k = ${formatValue(v)}" }.mkString(", ")
      s"@$simpleName($paramStr)"

  private def formatValue(value: Any): String = value match
    case s: String => s""""$s""""
    case other => other.toString

  override def equals(other: Any): Boolean = other match
    case that: AnnotationInfo =>
      annotationTypeName == that.annotationTypeName && parameters == that.parameters
    case _ => false

  override def hashCode(): Int = 
    31 * annotationTypeName.hashCode + parameters.hashCode

end AnnotationInfo

object AnnotationInfo:

  /**
    * Create an AnnotationInfo from a quoted expression (compile-time use)
    * This is a placeholder implementation - will be fully implemented in task 2
    */
  def fromQuoted(annotationExpr: Expr[Any])(using Quotes): AnnotationInfo =
    import quotes.reflect.*
    
    // This is a placeholder implementation - will be implemented in task 2
    // For now, return a basic annotation info
    AnnotationInfo("unknown", Map.empty)

  /**
    * Create a simple annotation with just a string value
    * This is a common pattern for annotations like @description("some text")
    */
  def simple(annotationTypeName: String, value: String): AnnotationInfo = 
    AnnotationInfo(annotationTypeName, Map("value" -> value))

  /**
    * Create a simple annotation with any value type
    */
  def simple(annotationTypeName: String, value: Any): AnnotationInfo = 
    AnnotationInfo(annotationTypeName, Map("value" -> value))

  /**
    * Create an annotation with named parameters
    * This supports complex annotations like @option(name = "user-id", required = true)
    */
  def withParams(annotationTypeName: String, params: Map[String, Any]): AnnotationInfo = 
    AnnotationInfo(annotationTypeName, params)

  /**
    * Create an annotation with key-value parameters using varargs
    * Usage: AnnotationInfo.withKeyValues("MyAnnotation", "name" -> "value", "flag" -> true)
    */
  def withKeyValues(annotationTypeName: String, params: (String, Any)*): AnnotationInfo = 
    AnnotationInfo(annotationTypeName, params.toMap)

  /**
    * Create an annotation with no parameters (marker annotation)
    * This is for annotations like @deprecated or @override
    */
  def marker(annotationTypeName: String): AnnotationInfo = 
    AnnotationInfo(annotationTypeName, Map.empty)

  /**
    * Create an annotation info for boolean flag annotations
    * Usage: AnnotationInfo.flag("required", true)
    */
  def flag(annotationTypeName: String, value: Boolean = true): AnnotationInfo = 
    if value then marker(annotationTypeName) else AnnotationInfo(annotationTypeName, Map("value" -> false))

  /**
    * Helper method to create annotation info for numeric parameters
    */
  def numeric(annotationTypeName: String, value: Number): AnnotationInfo = 
    AnnotationInfo(annotationTypeName, Map("value" -> value))

  /**
    * Helper method to create annotation info with array parameters
    */
  def withArray(annotationTypeName: String, paramName: String, values: Seq[Any]): AnnotationInfo = 
    AnnotationInfo(annotationTypeName, Map(paramName -> values))

  /**
    * Utility method to extract the simple name from a fully qualified annotation name
    */
  def extractSimpleName(fullName: String): String = 
    val lastDot = fullName.lastIndexOf('.')
    if lastDot >= 0 then fullName.substring(lastDot + 1) else fullName

end AnnotationInfo

