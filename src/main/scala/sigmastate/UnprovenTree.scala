package sigmastate

import java.math.BigInteger

import com.google.common.primitives.Shorts
import scapi.sigma.DLogProtocol.{FirstDLogProverMessage, ProveDlog}
import scapi.sigma.{FirstDiffieHellmanTupleProverMessage, FirstProverMessage, ProveDiffieHellmanTuple}
import sigmastate.Values.{SigmaBoolean, Value}
import sigmastate.serialization.ValueSerializer


object ConjectureType extends Enumeration {
  val AndConjecture = Value(0)
  val OrConjecture = Value(1)
}

//Proof tree

trait ProofTree extends Product

trait ProofTreeLeaf extends ProofTree {
  val proposition: SigmaBoolean
  val commitmentOpt: Option[FirstProverMessage[_]]
}

trait ProofTreeConjecture extends ProofTree {
  val conjectureType: ConjectureType.Value
  val children: Seq[ProofTree]
}


sealed trait UnprovenTree extends ProofTree {
  val proposition: SigmaBoolean

  val simulated: Boolean

  def real: Boolean = !simulated

  val challengeOpt: Option[Array[Byte]]

  def withChallenge(challenge: Array[Byte]): UnprovenTree

  def withSimulated(newSimulated: Boolean): UnprovenTree
}

sealed trait UnprovenLeaf extends UnprovenTree with ProofTreeLeaf

sealed trait UnprovenConjecture extends UnprovenTree with ProofTreeConjecture {
}

case class CAndUnproven(override val proposition: CAND,
                        override val challengeOpt: Option[Array[Byte]] = None,
                        override val simulated: Boolean,
                        children: Seq[ProofTree]) extends UnprovenConjecture {

  override val conjectureType = ConjectureType.AndConjecture

  override def withChallenge(challenge: Array[Byte]) = this.copy(challengeOpt = Some(challenge))

  override def withSimulated(newSimulated: Boolean) = this.copy(simulated = newSimulated)
}

case class COrUnproven(override val proposition: COR,
                       override val challengeOpt: Option[Array[Byte]] = None,
                       override val simulated: Boolean,
                       children: Seq[ProofTree]) extends UnprovenConjecture {

  override val conjectureType = ConjectureType.OrConjecture

  override def withChallenge(challenge: Array[Byte]) = this.copy(challengeOpt = Some(challenge))

  override def withSimulated(newSimulated: Boolean) = this.copy(simulated = newSimulated)
}

case class UnprovenSchnorr(override val proposition: ProveDlog,
                           override val commitmentOpt: Option[FirstDLogProverMessage],
                           randomnessOpt: Option[BigInteger],
                           override val challengeOpt: Option[Array[Byte]] = None,
                           override val simulated: Boolean) extends UnprovenLeaf {

  override def withChallenge(challenge: Array[Byte]) = this.copy(challengeOpt = Some(challenge))

  override def withSimulated(newSimulated: Boolean) = this.copy(simulated = newSimulated)
}

case class UnprovenDiffieHellmanTuple(override val proposition: ProveDiffieHellmanTuple,
                                      override val commitmentOpt: Option[FirstDiffieHellmanTupleProverMessage],
                                      randomnessOpt: Option[BigInteger],
                                      override val challengeOpt: Option[Array[Byte]] = None,
                                      override val simulated: Boolean
                                     ) extends UnprovenLeaf {
  override def withChallenge(challenge: Array[Byte]) = this.copy(challengeOpt = Some(challenge))

  override def withSimulated(newSimulated: Boolean) = this.copy(simulated = newSimulated)
}


object FiatShamirTree {
  val internalNodePrefix = 0: Byte
  val leafPrefix = 1: Byte

  def toBytes(tree: ProofTree): Array[Byte] = {

    def traverseNode(node: ProofTree): Array[Byte] = node match {
      case l: ProofTreeLeaf =>
        val propBytes = ValueSerializer.serialize(l.proposition)
        val commitmentBytes = l.commitmentOpt.get.bytes
        leafPrefix +:
          ((Shorts.toByteArray(propBytes.length.toShort) ++ propBytes) ++
            (Shorts.toByteArray(commitmentBytes.length.toShort) ++ commitmentBytes))

      case c: ProofTreeConjecture =>
        val childrenCountBytes = Shorts.toByteArray(c.children.length.toShort)
        val conjBytes = Array(internalNodePrefix, c.conjectureType.id.toByte) ++ childrenCountBytes

        c.children.foldLeft(conjBytes) { case (acc, ch) =>
          acc ++ traverseNode(ch)
        }
    }

    traverseNode(tree)
  }
}