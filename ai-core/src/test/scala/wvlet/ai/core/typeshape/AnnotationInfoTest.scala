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

import wvlet.airspec.AirSpec

class AnnotationInfoTest extends AirSpec:

  test("create simple annotation with string value") {
    val annotation = AnnotationInfo.simple("description", "User data model")
    
    annotation.annotationTypeName shouldBe "description"
    annotation.parameters shouldBe Map("value" -> "User data model")
    annotation.getValue shouldBe Some("User data model")
    annotation.getValueAs[String] shouldBe Some("User data model")
    annotation.hasParameter("value") shouldBe true
    annotation.nonEmpty shouldBe true
  }

  test("create annotation with named parameters") {
    val annotation = AnnotationInfo.withParams("option", Map(
      "name" -> "user-id",
      "required" -> true,
      "help" -> "Unique user identifier"
    ))
    
    annotation.annotationTypeName shouldBe "option"
    annotation.getParameter("name") shouldBe Some("user-id")
    annotation.getParameter("required") shouldBe Some(true)
    annotation.getParameter("help") shouldBe Some("Unique user identifier")
    annotation.getParameterAs[String]("name") shouldBe Some("user-id")
    annotation.getParameterAs[Boolean]("required") shouldBe Some(true)
    annotation.parameterNames shouldBe Set("name", "required", "help")
  }

  test("create annotation with key-value parameters using varargs") {
    val annotation = AnnotationInfo.withKeyValues("validate",
      "min" -> 3,
      "max" -> 50,
      "pattern" -> "[a-zA-Z0-9_]+"
    )
    
    annotation.annotationTypeName shouldBe "validate"
    annotation.getParameterAs[Int]("min") shouldBe Some(3)
    annotation.getParameterAs[Int]("max") shouldBe Some(50)
    annotation.getParameterAs[String]("pattern") shouldBe Some("[a-zA-Z0-9_]+")
  }

  test("create marker annotation with no parameters") {
    val annotation = AnnotationInfo.marker("deprecated")
    
    annotation.annotationTypeName shouldBe "deprecated"
    annotation.parameters shouldBe Map.empty
    annotation.isEmpty shouldBe true
    annotation.nonEmpty shouldBe false
    annotation.parameterNames shouldBe Set.empty
  }

  test("create flag annotation") {
    val trueFlag = AnnotationInfo.flag("required", true)
    val falseFlag = AnnotationInfo.flag("required", false)
    
    trueFlag.annotationTypeName shouldBe "required"
    trueFlag.parameters shouldBe Map.empty
    
    falseFlag.annotationTypeName shouldBe "required"
    falseFlag.parameters shouldBe Map("value" -> false)
  }

  test("create numeric annotation") {
    val annotation = AnnotationInfo.numeric("priority", 10)
    
    annotation.annotationTypeName shouldBe "priority"
    annotation.getValue shouldBe Some(10)
    annotation.getValueAs[Int] shouldBe Some(10)
  }

  test("create annotation with array parameters") {
    val annotation = AnnotationInfo.withArray("tags", "values", Seq("web", "api", "service"))
    
    annotation.annotationTypeName shouldBe "tags"
    annotation.getParameter("values") shouldBe Some(Seq("web", "api", "service"))
    annotation.getParameterAs[Seq[String]]("values") shouldBe Some(Seq("web", "api", "service"))
  }

  test("get parameter with default value") {
    val annotation = AnnotationInfo.withParams("config", Map("timeout" -> 30))
    
    annotation.getParameterOrElse("timeout", 60) shouldBe 30
    annotation.getParameterOrElse("retries", 3) shouldBe 3
  }

  test("extract simple name from fully qualified annotation name") {
    val annotation = AnnotationInfo.simple("com.example.annotations.Description", "test")
    
    annotation.simpleName shouldBe "Description"
    annotation.annotationTypeName shouldBe "com.example.annotations.Description"
  }

  test("toString formatting") {
    val markerAnnotation = AnnotationInfo.marker("deprecated")
    markerAnnotation.toString shouldBe "@deprecated"
    
    val simpleAnnotation = AnnotationInfo.simple("description", "User model")
    simpleAnnotation.toString shouldBe """@description("User model")"""
    
    val complexAnnotation = AnnotationInfo.withKeyValues("option",
      "name" -> "user-id",
      "required" -> true
    )
    // Note: Map iteration order may vary, so we check both possible orders
    val result = complexAnnotation.toString
    result should (include("@option(") and include("name = \"user-id\"") and include("required = true"))
  }

  test("equality and hashCode") {
    val annotation1 = AnnotationInfo.simple("test", "value")
    val annotation2 = AnnotationInfo.simple("test", "value")
    val annotation3 = AnnotationInfo.simple("test", "different")
    
    annotation1 shouldBe annotation2
    annotation1.hashCode shouldBe annotation2.hashCode
    annotation1 should not be annotation3
  }

  test("serialization support") {
    val annotation = AnnotationInfo.withKeyValues("complex",
      "string" -> "test",
      "number" -> 42,
      "boolean" -> true,
      "array" -> Seq("a", "b", "c")
    )
    
    // Verify it implements Serializable
    annotation shouldBe a[Serializable]
    
    // Verify all parameter types are preserved
    annotation.getParameterAs[String]("string") shouldBe Some("test")
    annotation.getParameterAs[Int]("number") shouldBe Some(42)
    annotation.getParameterAs[Boolean]("boolean") shouldBe Some(true)
    annotation.getParameterAs[Seq[String]]("array") shouldBe Some(Seq("a", "b", "c"))
  }

  test("AnnotationValue sealed trait hierarchy") {
    import AnnotationValue.*
    
    val primitiveValue = PrimitiveValue("test")
    val classValue = ClassValue("java.lang.String")
    val enumValue = EnumValue("Color", "RED")
    val arrayValue = ArrayValue(Seq(PrimitiveValue("a"), PrimitiveValue("b")))
    val nestedAnnotation = NestedAnnotation(AnnotationInfo.simple("inner", "value"))
    
    // Verify all are Serializable
    primitiveValue shouldBe a[Serializable]
    classValue shouldBe a[Serializable]
    enumValue shouldBe a[Serializable]
    arrayValue shouldBe a[Serializable]
    nestedAnnotation shouldBe a[Serializable]
    
    // Verify they are all AnnotationValue instances
    primitiveValue shouldBe a[AnnotationValue]
    classValue shouldBe a[AnnotationValue]
    enumValue shouldBe a[AnnotationValue]
    arrayValue shouldBe a[AnnotationValue]
    nestedAnnotation shouldBe a[AnnotationValue]
  }

end AnnotationInfoTest