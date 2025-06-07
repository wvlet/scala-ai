package wvlet.ai.core.weaver

import scala.deriving.Mirror // Keep Mirror for `m`
// erasedValue, summonInline, constValue, error are no longer needed here
import wvlet.ai.core.msgpack.spi.{Packer, Unpacker}

// Removed duplicate ObjectWeaver trait.
// The canonical one is in ObjectWeaver.scala

/**
  * Custom exception for errors occurring during weaver packing.
  * @param message
  *   A description of the error.
  * @param cause
  *   The underlying cause of the error, if any.
  */
case class WeaverPackingException(message: String, cause: Throwable = null)
    extends RuntimeException(message, cause)

// Companion object removed for this attempt

// Constructor now accepts elementWeavers. Mirror m is still needed for fromProduct.
class CaseClassWeaver[A](private val elementWeavers: List[ObjectWeaver[?]])(using
    m: Mirror.ProductOf[A]
) extends ObjectWeaver[A]:

  // Internal buildWeavers and elementWeavers val are removed.

  override def pack(packer: Packer, v: A, config: WeaverConfig): Unit =
    val product = v.asInstanceOf[Product]
    if product.productArity != elementWeavers.size then
      throw WeaverPackingException(
        s"Element count mismatch. Expected: ${elementWeavers.size}, Got: ${product.productArity}"
      )
    packer.packArrayHeader(elementWeavers.size)

    product
      .productIterator
      .zip(elementWeavers)
      .foreach { case (elemValue, weaver) =>
        (weaver.asInstanceOf[ObjectWeaver[Any]]).pack(packer, elemValue, config)
      }

  override def unpack(unpacker: Unpacker, context: WeaverContext): Unit =
    val numElements = unpacker.unpackArrayHeader
    if numElements != elementWeavers.size then
      context.setError(
        new IllegalArgumentException(
          s"Element count mismatch. Expected: ${elementWeavers.size}, Got: ${numElements}"
        )
      )
      // This point is for future consideration of schema evolution or robust error recovery.
      // For now, strict element count matching is enforced.
      return

    val elements = new Array[Any](elementWeavers.size)
    var i        = 0
    var failed   = false

    while i < elementWeavers.size && !failed do
      val weaver         = elementWeavers(i)
      val elementContext = WeaverContext(context.config)
      // Assuming weaver is ObjectWeaver[?] so direct call is not possible without cast
      // However, the element type is unknown here to do a safe cast.
      // This part of unpack will need careful handling if we stick to List[ObjectWeaver[?]]
      (weaver.asInstanceOf[ObjectWeaver[Any]]).unpack(unpacker, elementContext)

      if elementContext.hasError then
        context.setError(
          new RuntimeException(
            s"Failed to unpack element $i: ${elementContext.getError.get.getMessage}",
            elementContext.getError.get
          )
        )
        failed = true
      else
        elements(i) = elementContext.getLastValue
      i += 1

    if !failed then
      try
        val instance = m.fromProduct(
          new Product:
            override def productArity: Int           = elements.length
            override def productElement(n: Int): Any = elements(n)
            override def canEqual(that: Any): Boolean =
              that.isInstanceOf[Product] && that.asInstanceOf[Product].productArity == productArity
        )
        context.setObject(instance)
      catch
        case e: Throwable =>
          context.setError(new RuntimeException("Failed to instantiate case class from product", e))
        // Closing brace for try-catch
      // Closing brace for if (!failed)
    // If failed, context will already have an error set.
    // Closing brace for unpack method
  end unpack

  // Closing brace for CaseClassWeaver class

end CaseClassWeaver
