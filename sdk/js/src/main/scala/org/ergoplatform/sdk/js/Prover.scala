package org.ergoplatform.sdk.js

import org.ergoplatform.sdk
import typings.fleetSdkCommon.boxesMod.Box
import typings.fleetSdkCommon.{commonMod, inputsMod, tokenMod, transactionsMod}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("Prover")
class Prover(_prover: sdk.Prover) {
  import Isos._

  def reduce(
      stateCtx: BlockchainStateContext,
      unsignedTx: transactionsMod.UnsignedTransaction,
      boxesToSpend: js.Array[inputsMod.EIP12UnsignedInput],
      baseCost: Int): ReducedTransaction = {
    val tx =sdk.UnsignedTransaction(
      ergoTx = isoUnsignedTransaction.to(unsignedTx),
      boxesToSpend = isoArrayToIndexed(isoEIP12UnsignedInput).to(boxesToSpend),
      dataInputs = IndexedSeq.empty,
      tokensToBurn = IndexedSeq.empty
    )
    _prover.reduce(
      isoBlockchainStateContext.to(stateCtx),
      tx,
      baseCost
    )
    new ReducedTransaction
  }

  def reduceTransaction(
      unsignedTx: transactionsMod.UnsignedTransaction,
      boxesToSpend: js.Array[inputsMod.EIP12UnsignedInput],
      dataBoxes: js.Array[Box[commonMod.Amount]],
      stateDigest: String,
      baseCost: Int,
      tokensToBurn: js.Array[tokenMod.TokenAmount[commonMod.Amount]]
  ): (ReducedTransaction, Int) = {
    val tx = Isos.isoUnsignedTransaction.to(unsignedTx)
//    val inputs: = boxesToSpend.map(isoEIP12UnsignedInput.to).toArray
    
    (new ReducedTransaction, 0)
  }

}

@JSExportTopLevel("ReducedTransaction")
class ReducedTransaction

@JSExportTopLevel("ProverFactory")
object Prover {
}
