package sigmastate.serialization.generators

import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen}
import sigmastate.Values.{FalseLeaf, TrueLeaf}
import sigmastate._
import org.ergoplatform.ErgoBox.RegisterIdentifier
import sigmastate.utxo._

trait TransformerGenerators {
  self: ValueGenerators with ConcreteCollectionGenerators =>

  implicit val arbMapCollection: Arbitrary[MapCollection[SLong.type, SLong.type]] = Arbitrary(mapCollectionGen)
  implicit val arbExists: Arbitrary[Exists[SLong.type]] = Arbitrary(existsGen)
  implicit val arbForAll: Arbitrary[ForAll[SLong.type]] = Arbitrary(forAllGen)
  implicit val arbFold: Arbitrary[Fold[SLong.type]] = Arbitrary(foldGen)
  implicit val arbAppend: Arbitrary[Append[SLong.type]] = Arbitrary(appendGen)
  implicit val arbSlice: Arbitrary[Slice[SLong.type]] = Arbitrary(sliceGen)
  implicit val arbWhere: Arbitrary[Where[SLong.type]] = Arbitrary(whereGen)
  implicit val sizeOf: Arbitrary[SizeOf[SLong.type]] = Arbitrary(sizeOfGen)
  implicit val arbExtractAmount: Arbitrary[ExtractAmount] = Arbitrary(extractAmountGen)
  implicit val arbExtractScriptBytes: Arbitrary[ExtractScriptBytes] = Arbitrary(extractScriptBytesGen)
  implicit val arbExtractBytes: Arbitrary[ExtractBytes] = Arbitrary(extractBytesGen)
  implicit val arbExtractBytesWithNoRef: Arbitrary[ExtractBytesWithNoRef] = Arbitrary(extractBytesWithNoRefGen)
  implicit val arbExtractId: Arbitrary[ExtractId] = Arbitrary(extractIdGen)
  implicit val arbExtractRegisterAs: Arbitrary[ExtractRegisterAs[SLong.type]] = Arbitrary(extractRegisterAsGen)
  implicit val arbIntToByteArray: Arbitrary[IntToByteArray] = Arbitrary(intToByteArrayGen)
  implicit val arbByteArrayToBigInt: Arbitrary[ByteArrayToBigInt] = Arbitrary(byteArrayToBigIntGen)
  implicit val arbCalcBlake2b256: Arbitrary[CalcBlake2b256] = Arbitrary(calcBlake2b256Gen)
  implicit val arbCalcSha256: Arbitrary[CalcSha256] = Arbitrary(calcSha256Gen)
  implicit val arbByIndex: Arbitrary[ByIndex[SLong.type]] = Arbitrary(byIndexGen)
  implicit val arbDeserializeContext: Arbitrary[DeserializeContext[SBoolean.type]] = Arbitrary(deserializeContextGen)
  implicit val arbDeserializeRegister: Arbitrary[DeserializeRegister[SBoolean.type]] = Arbitrary(deserializeRegisterGen)

  val mapCollectionGen: Gen[MapCollection[SLong.type, SLong.type]] = for {
    input <- arbCCOfIntConstant.arbitrary
    idByte <- arbByte.arbitrary
    mapper <- arbIntConstants.arbitrary
  } yield MapCollection(input, idByte, mapper)

  val existsGen: Gen[Exists[SLong.type]] = for {
    input <- arbCCOfIntConstant.arbitrary
    idByte <- arbByte.arbitrary
    condition <- Gen.oneOf(TrueLeaf, FalseLeaf)
  } yield Exists(input, idByte, condition)

  val forAllGen: Gen[ForAll[SLong.type]] = for {
    input <- arbCCOfIntConstant.arbitrary
    idByte <- arbByte.arbitrary
    condition <- Gen.oneOf(TrueLeaf, FalseLeaf)
  } yield ForAll(input, idByte, condition)

  val foldGen: Gen[Fold[SLong.type]] = for {
    input <- arbCCOfIntConstant.arbitrary
  } yield Fold.sum(input)

  val sliceGen: Gen[Slice[SLong.type]] = for {
    col1 <- arbCCOfIntConstant.arbitrary
    from <- intConstGen
    until <- intConstGen
  } yield Slice(col1, from, until)

  val whereGen: Gen[Where[SLong.type]] = for {
    col1 <- arbCCOfIntConstant.arbitrary
    id <- Arbitrary.arbitrary[Byte]
    condition <- booleanConstGen
  } yield Where(col1, id, condition)

  val appendGen: Gen[Append[SLong.type]] = for {
    col1 <- arbCCOfIntConstant.arbitrary
    col2 <- arbCCOfIntConstant.arbitrary
  } yield Append(col1, col2)

  val sizeOfGen: Gen[SizeOf[SLong.type]] = for {
    input <- arbCCOfIntConstant.arbitrary
  } yield SizeOf(input)

  val extractAmountGen: Gen[ExtractAmount] = arbTaggedBox.arbitrary.map { b => ExtractAmount(b) }
  val extractScriptBytesGen: Gen[ExtractScriptBytes] = arbTaggedBox.arbitrary.map { b => ExtractScriptBytes(b) }
  val extractBytesGen: Gen[ExtractBytes] = arbTaggedBox.arbitrary.map { b => ExtractBytes(b) }
  val extractBytesWithNoRefGen: Gen[ExtractBytesWithNoRef] = arbTaggedBox.arbitrary.map { b => ExtractBytesWithNoRef(b) }
  val extractIdGen: Gen[ExtractId] = arbTaggedBox.arbitrary.map { b => ExtractId(b) }
  val extractRegisterAsGen: Gen[ExtractRegisterAs[SLong.type]] = for {
    input <- arbTaggedBox.arbitrary
    r <- arbRegisterIdentifier.arbitrary
    dvInt <- arbIntConstants.arbitrary
    dv <- Gen.oneOf(None, Some(dvInt))
  } yield ExtractRegisterAs(input, r, dv)
  val deserializeContextGen: Gen[DeserializeContext[SBoolean.type]] = Arbitrary.arbitrary[Byte].map(b => DeserializeContext(b, SBoolean))

  val deserializeRegisterGen: Gen[DeserializeRegister[SBoolean.type]] = for {
    r <- arbRegisterIdentifier.arbitrary
    default <- booleanConstGen
    isDefined <- Arbitrary.arbitrary[Boolean]
    defaultOpt = if (isDefined) Some(default) else None
  } yield DeserializeRegister(r, SBoolean, defaultOpt)

  val intToByteArrayGen: Gen[IntToByteArray] = arbIntConstants.arbitrary.map { v => IntToByteArray(v) }
  val byteArrayToBigIntGen: Gen[ByteArrayToBigInt] = arbByteArrayConstant.arbitrary.map { v => ByteArrayToBigInt(v) }
  val calcBlake2b256Gen: Gen[CalcBlake2b256] = arbByteArrayConstant.arbitrary.map { v => CalcBlake2b256(v) }
  val calcSha256Gen: Gen[CalcSha256] = arbByteArrayConstant.arbitrary.map { v => CalcSha256(v) }

  val byIndexGen: Gen[ByIndex[SLong.type]] = for {
    input <- arbCCOfIntConstant.arbitrary
    index <- arbInt.arbitrary
  } yield ByIndex(input, index)

}