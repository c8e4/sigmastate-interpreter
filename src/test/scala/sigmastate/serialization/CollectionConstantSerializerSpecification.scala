package sigmastate.serialization

import sigmastate.SByte
import sigmastate.Values.{CollectionConstant, ByteArrayConstant}
import sigmastate.serialization.OpCodes.CollectionConstantCode

class CollectionConstantSerializerSpecification extends TableSerializationSpecification {
  
  property("ByteArrayConstant: Serializer round trip") {
    forAll { arr: CollectionConstant[SByte.type] =>
      roundTripTest(arr)
    }
  }

  property("ByteArrayConstant: Deserialize predefined bytes") {
    val value = ByteArrayConstant(Array[Byte](1, 3, 5, 9, 10, 100))
    val bytes = Array[Byte](CollectionConstantCode, 0, 6, 1, 3, 5, 9, 10, 100)
    predefinedBytesTest(bytes, value)
  }

  override def objects = Table(
    ("object", "bytes"),
    (ByteArrayConstant(Array[Byte]()), Array[Byte](CollectionConstantCode, 0, 0)),
    (ByteArrayConstant(Array[Byte](1)), Array[Byte](CollectionConstantCode, 0, 1, 1)),
    (ByteArrayConstant(Array[Byte](1, 2, 3, 4, 5)), Array[Byte](CollectionConstantCode, 0, 5, 1, 2, 3, 4, 5))
  )

  tableRoundTripTest("ByteArrayConstant: Serializer table round trip")
  tablePredefinedBytesTest("ByteArrayConstant: deserialize from predefined bytes")

  checkConsumed(Array[Byte](CollectionConstantCode, 0, 0))
  checkConsumed(Array[Byte](CollectionConstantCode, 0, 1, 1))
  checkConsumed(Array[Byte](CollectionConstantCode, 0, 5, 1, 2, 3, 4, 5))

  def checkConsumed(array: Array[Byte]) {
    val (_, consumed) = ValueSerializer.deserialize(array, 0)
    consumed should equal (array.length)
  }
}
