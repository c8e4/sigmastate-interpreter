package sigmastate.utxo

import sigmastate.{SByteArray, SType}
import sigmastate.Values._
import sigmastate.interpreter.Interpreter
import sigmastate.serialization.ValueSerializer


class ErgoInterpreter(override val maxCost: Long = CostTable.ScriptLimit) extends Interpreter {
  override type CTX = ErgoContext

  override def specificTransformations(context: ErgoContext, tree: SValue): SValue = tree match {
    case Inputs => ConcreteCollection(context.boxesToSpend.map(BoxConstant.apply))

    case Outputs => ConcreteCollection(context.spendingTransaction.outputs.map(BoxConstant.apply))

    case Self => BoxConstant(context.self)

    case Height => IntConstant(context.currentHeight)

    case LastBlockUtxoRootHash => AvlTreeConstant(context.lastBlockUtxoRoot)

    case t: TaggedVariable[_] =>
      if (context.extension.values.contains(t.id))
        context.extension.values(t.id)
      else
        null
//        Interpreter.error(s"Tagged variable with id=${t.id} not found in context ${context.extension.values}")

    case _ =>
      super.specificTransformations(context, tree)
  }

  override def substDeserialize(context: CTX): PartialFunction[Value[_ <: SType], Option[Value[_ <: SType]]] =
    ({
      case d: DeserializeRegister[_] =>
        context.self.get(d.reg).flatMap { v =>
          v match {
            case eba: EvaluatedValue[SByteArray.type] => Some(ValueSerializer.deserialize(eba.value))
            case _ => None
          }
        }.orElse(d.default)
    }: PartialFunction[Value[_ <: SType], Option[Value[_ <: SType]]]) orElse super.substDeserialize(context)
}