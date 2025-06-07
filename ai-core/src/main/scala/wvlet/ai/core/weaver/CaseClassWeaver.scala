package wvlet.ai.core.weaver

import scala.deriving.Mirror
import scala.compiletime.{erasedValue, summonInline}
import wvlet.ai.core.msgpack.spi.{Packer, Unpacker}

// Removed duplicate ObjectWeaver trait.
// The canonical one is in ObjectWeaver.scala

class CaseClassWeaver[A](using m: Mirror.ProductOf[A]) extends ObjectWeaver[A] {

  // Note: elementWeavers are now of type ObjectWeaver from the canonical definition
  private inline def summonElementWeavers[Elems <: Tuple]: List[ObjectWeaver[?]] =
    inline erasedValue[Elems] match {
      case _: (elem *: elemsTail) =>
        summonInline[ObjectWeaver[elem]] :: summonElementWeavers[elemsTail]
      case _: EmptyTuple =>
        Nil
    }

  private val elementWeavers: List[ObjectWeaver[?]] = summonElementWeavers[m.MirroredElemTypes]

  override def pack(packer: Packer, v: A, config: WeaverConfig): Unit = {
    val product = v.asInstanceOf[Product]
    if (product.productArity != elementWeavers.size) {
      // TODO: More specific error handling using WeaverContext
      throw new IllegalArgumentException(s"Element count mismatch. Expected: ${elementWeavers.size}, Got: ${product.productArity}")
    }
    packer.packArrayHeader(elementWeavers.size)
    product.productIterator.zip(elementWeavers).foreach { case (elem, weaver) =>
      // This cast is generally safe due to how elementWeavers is constructed.
      // The individual element's weaver will handle its specific packing.
      (weaver.asInstanceOf[ObjectWeaver[Any]]).pack(packer, elem, config)
    }
  }

  override def unpack(unpacker: Unpacker, context: WeaverContext): Unit = {
    val numElements = unpacker.unpackArrayHeader()
    if (numElements != elementWeavers.size) {
      context.setError(new IllegalArgumentException(s"Element count mismatch. Expected: ${elementWeavers.size}, Got: ${numElements}"))
      // TODO: Potentially consume unexpected fields from unpacker to allow recovery or partial unpack
      return
    }

    val elements = new Array[Any](elementWeavers.size)
    var i = 0
    var failed = false
    while (i < elementWeavers.size && !failed) {
      val weaver = elementWeavers(i)
      // Create a new context for each element to isolate errors and values
      val elementContext = WeaverContext(context.config)
      weaver.unpack(unpacker, elementContext)

      if (elementContext.hasError) {
        context.setError(new RuntimeException(s"Failed to unpack element $i: ${elementContext.getError.get.getMessage}", elementContext.getError.get))
        failed = true
      } else {
        elements(i) = elementContext.getLastValue
      }
      i += 1
    }

    if (!failed) {
      try {
        val instance = m.fromProduct(new Product {
          override def productArity: Int = elements.length
          override def productElement(n: Int): Any = elements(n)
          override def canEqual(that: Any): Boolean = that.isInstanceOf[Product] && that.asInstanceOf[Product].productArity == productArity
        })
        context.setLastValue(instance)
      } catch {
        case e: Throwable =>
          context.setError(new RuntimeException("Failed to instantiate case class from product", e))
      }
    }
    // If failed, context will already have an error set.
  }
}
