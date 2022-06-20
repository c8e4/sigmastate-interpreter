package sigmastate

import Values._
import org.ergoplatform.{Context, Global, Inputs, Height, LastBlockUtxoRootHash, MinerPubkey, Outputs, Self}
import org.typelevel.paiges.Doc
import sigmastate.serialization.OpCodes
import sigmastate.utxo._
import sigmastate.lang.Terms.{Apply, ApplyTypes, Block, Ident, Lambda, MethodCall, MethodCallLike, Select, ValNode}

/**
 * Brainstorm ideas: create configuration for printer (explicit types for vals/methods)
 */
object PrettyPrintErgoTree {

  def prettyPrint(t: SValue, width: Int = 80, indent: Int = 2): String = createDoc(t)(indent).render(width)

  private def createDoc(t: SValue)(implicit indent: Int): Doc = t match {
    // Values
    case ev: EvaluatedValue[SType] => ev match {
      case c: Constant[SType] => c match {
        case FalseLeaf => Doc.text("false")
        case TrueLeaf => Doc.text("true")
        case ConstantNode(value, tpe) => 
          val suffix = tpe match {
            case SInt => Doc.empty
            case SLong => Doc.char('L')
            case other => Doc.text(".to") + STypeDoc(other)
          }
          Doc.text(s"$value") + suffix
      }
      // TODO: Only used in parser?
      case UnitConstant() => ???
      // TODO: Converted to SGlobal.groupGeneratorMethod
      case GroupGenerator => ??? 
      case ec: EvaluatedCollection[_, _] => ec match {
        case ConcreteCollection(items, elemType) =>
          Doc.text(s"Coll") + wrapWithBrackets(STypeDoc(elemType)) + nTupleDoc(items.map(createDoc))
      }
    }
    case bi: BlockItem => bi match {
      case ValDef(id, _, rhs) =>
        val valName = inferNameFromType(rhs.tpe)
        Doc.text(s"val $valName$id = ") + createDoc(rhs)
    }
    case Tuple(items) => nTupleDoc(items.map(createDoc))
    case ValUse(id, tpe) =>
      val valName = inferNameFromType(tpe)
      Doc.text(s"$valName$id")
    // TODO: See unfinished test
    case ConstantPlaceholder(id, tpe) => ???
    // TODO: Does not make sense for printer?
    case TaggedVariableNode(varId, tpe) => ???
    // TODO: Does not make sense for printer?
    case FalseSigmaProp | TrueSigmaProp => ???
    case FuncValue(args, body) =>
      val prefix = Doc.char('{') + Doc.space + argsWithTypesDoc(args) + Doc.space + Doc.text("=>")
      val suffix = Doc.char('}')
      // if function contains block then hardlines/indentation are needed as bracketBy handles indentation based on
      // width, and we must have newline for block `val foo = ...` items
      val bodyDoc = body match {
        case b: BlockValue => Doc.hardLine + createDoc(b).indent(indent) + Doc.hardLine
        case _ => createDoc(body).bracketBy(Doc.empty, Doc.empty, indent)
      }
      prefix + bodyDoc + suffix
    case BlockValue(items, result) =>
      val prettyItems = items.map(item => createDoc(item))
        Doc.intercalate(Doc.hardLine, prettyItems) +
        Doc.hardLine +
        createDoc(result)
    // Not implemented yet - https://github.com/ScorexFoundation/sigmastate-interpreter/issues/462
    case SomeValue(_) | NoneValue(_) => ???

    // ErgoLike
    case Height => Doc.text("HEIGHT")
    case Inputs => Doc.text("INPUTS")
    case Outputs => Doc.text("OUTPUTS")
    case Self => Doc.text("SELF")
    case Context => Doc.text("CONTEXT")
    case Global => Doc.text("Global")
    // TODO: Not possible to reach following 2 cases until https://github.com/ScorexFoundation/sigmastate-interpreter/issues/799
    case MinerPubkey => Doc.text("minerPubKey")
    case LastBlockUtxoRootHash => Doc.text("LastBlockUtxoRootHash") 

    // Transformers
    case MapCollection(input, mapper) => methodDoc(input, "map", List(mapper))
    case Append(coll1, coll2) => methodDoc(coll1, "append", List(coll2))
    case Slice(input, from, until) => methodDoc(input, "slice", List(from, until))
    case Filter(input, condition) => methodDoc(input, "filter", List(condition))
    case bt: BooleanTransformer[_] => bt match {
      case Exists(input, condition) => methodDoc(input, "exists", List(condition))
      case ForAll(input, condition) => methodDoc(input, "forall", List(condition))
    }
    case Fold(input, zero, foldOp) => methodDoc(input, "fold", List(zero, foldOp))
    case SelectField(input, idx) => methodDoc(input, s"_$idx")
    case ByIndex(input, index, defaultIndex) =>
      defaultIndex match {
        case Some(v) => methodDoc(input, "getOrElse", List(index, v))
        case None => createDoc(input) + wrapWithParens(createDoc(index))
      }
    // TODO: Not used in final ErgoTree representation, will be deleted in the future.
    case SigmaPropIsProven(input) => ???
    case SigmaPropBytes(input) => methodDoc(input, "propBytes")
    case SizeOf(input) => methodDoc(input, "size")
    case ExtractAmount(input) => methodDoc(input, "value")
    case ExtractScriptBytes(input) => methodDoc(input, "propositionBytes")
    case ExtractBytes(input) => methodDoc(input, "bytes")
    case ExtractBytesWithNoRef(input) => methodDoc(input, "bytesWithoutRef")
    case ExtractId(input) => methodDoc(input, "id")
    // TODO: Is it possible to have `None` inside maybeTpe?
    case ExtractRegisterAs(input, registerId, maybeTpe) =>
      createDoc(input) + Doc.text(s".$registerId") + wrapWithBrackets(STypeDoc(maybeTpe.elemType))
    case ExtractCreationInfo(input) => methodDoc(input, "creationInfo")
    // Not implemented by compiler
    case d: Deserialize[_] => d match {
      case DeserializeContext(id, tpe) => ???
      case DeserializeRegister(reg, tpe, default) => ???
    }
    // TODO: Looks like we always have Some(...) in maybeTpe, consider dropping Option wrapper from GetVar definition
    case GetVar(varId, maybeTpe) =>
      Doc.text("getVar") + wrapWithBrackets(STypeDoc(maybeTpe.elemType)) + wrapWithParens(createDoc(varId))
    case OptionGet(input) => methodDoc(input, "get")
    case OptionGetOrElse(input, default) => methodDoc(input, "getOrElse", List(default))
    case OptionIsDefined(x) => methodDoc(x, "isDefined")

    // Terms
    // Following nodes are not part of final ErgoTree
    case Block(_, _) => ???
    case ValNode(_, _, _) => ???
    case Select(_, _, _) => ???
    case Ident(_, _) => ???
    case ApplyTypes(_, _) => ???
    case MethodCallLike(_, _, _, _) => ???
    case Lambda(_, _, _, _) => ???

    case Apply(func, args) => createDoc(func) + nTupleDoc(args.map(createDoc))
    case MethodCall(obj, method, args, map) => methodDoc(obj, method.name, args.toList)

    // Trees
    case BoolToSigmaProp(value) => Doc.text("sigmaProp") + wrapWithParens(createDoc(value))
    case CreateProveDlog(value) => Doc.text("proveDlog") + wrapWithParens(createDoc(value))
    // TODO: Comment says it can be removed as it isn't used anymore. But there is issue to implement it:
    // https://github.com/ScorexFoundation/sigmastate-interpreter/issues/605
    case CreateAvlTree(_, _, _, _) => ???
    case CreateProveDHTuple(gv, hv, uv, vv) => Doc.text("proveDHTuple") + nTupleDoc(List(gv, hv, uv, vv).map(createDoc))
    case st: SigmaTransformer[_, _] => st match {
      case SigmaAnd(items) => Doc.intercalate(Doc.text(" && "), items.map(createDoc))
      case SigmaOr(items) => Doc.intercalate(Doc.text(" || "), items.map(createDoc))
    }
    case OR(input) => Doc.text("anyOf") + wrapWithParens(createDoc(input))
    case XorOf(input) => Doc.text("xorOf") + wrapWithParens(createDoc(input))
    case AND(input) => Doc.text("allOf") + wrapWithParens(createDoc(input))
    case AtLeast(bound, input) => Doc.text("atLeast") + nTupleDoc(List(bound, input).map(createDoc))
    case Upcast(input, tpe) => createDoc(input) + Doc.text(".to") + STypeDoc(tpe)
    case Downcast(input, tpe) => createDoc(input) + Doc.text(".to") + STypeDoc(tpe)
    case LongToByteArray(input) => Doc.text("longToByteArray") + wrapWithParens(createDoc(input))
    case ByteArrayToLong(input) => Doc.text("byteArrayToLong") + wrapWithParens(createDoc(input))
    case ByteArrayToBigInt(input) => Doc.text("byteArrayToBigInt") + wrapWithParens(createDoc(input))
    case nr: NotReadyValueGroupElement => nr match {
      case DecodePoint(input) => Doc.text("decodePoint") + wrapWithParens(createDoc(input))
      case Exponentiate(l, r) => methodDoc(l, "exp", List(r))
      case MultiplyGroup(l, r) => methodDoc(l, "multiply", List(r))
    }
    case ch: CalcHash => ch match {
      case CalcBlake2b256(input) => Doc.text("blake2b256") + wrapWithParens(createDoc(input))
      case CalcSha256(input) => Doc.text("sha256") + wrapWithParens(createDoc(input))
    }
    case SubstConstants(scriptBytes, positions, newValues) =>
      Doc.text("substConstants") + nTupleDoc(List(scriptBytes, positions, newValues).map(createDoc))

    case t: Triple[_, _, _] => t match {
      case ArithOp(l, r, OpCodes.PlusCode) => createDoc(l) + Doc.text(" + ") + createDoc(r)
      case ArithOp(l, r, OpCodes.MinusCode) => createDoc(l) + Doc.text(" - ") + createDoc(r)
      case ArithOp(l, r, OpCodes.MultiplyCode) => createDoc(l) + Doc.text(" * ") + createDoc(r)
      case ArithOp(l, r, OpCodes.DivisionCode) => createDoc(l) + Doc.text(" / ") + createDoc(r)
      case ArithOp(l, r, OpCodes.ModuloCode) => createDoc(l) + Doc.text(" % ") + createDoc(r)
      case ArithOp(l, r, OpCodes.MinCode) => Doc.text("min") + nTupleDoc(List(l, r).map(createDoc))
      case ArithOp(l, r, OpCodes.MaxCode) => Doc.text("max") + nTupleDoc(List(l, r).map(createDoc))
      // TODO: Implement bitwise operations tests after 
      // https://github.com/ScorexFoundation/sigmastate-interpreter/issues/474
      // https://github.com/ScorexFoundation/sigmastate-interpreter/issues/418
      case BitOp(l, r, OpCodes.BitOrCode) => binOpWithPriorityParens(l, r, Doc.char('|'))
      case BitOp(l, r, OpCodes.BitAndCode) => binOpWithPriorityParens(l, r, Doc.char('&'))
      // Possible clash with BinXor?
      case BitOp(l, r, OpCodes.BitXorCode) => binOpWithPriorityParens(l, r, Doc.char('^'))
      case BitOp(l, r, OpCodes.BitInversionCode) => binOpWithPriorityParens(l, r, Doc.char('~'))
      case BitOp(l, r, OpCodes.BitShiftLeftCode) => binOpWithPriorityParens(l, r, Doc.text("<<"))
      case BitOp(l, r, OpCodes.BitShiftRightCode) => binOpWithPriorityParens(l, r, Doc.text(">>"))
      case BitOp(l, r, OpCodes.BitShiftRightZeroedCode) => binOpWithPriorityParens(l, r, Doc.text(">>>"))
      case Xor(l, r) => Doc.text("xor") + nTupleDoc(List(l, r).map(createDoc))
      case sr: SimpleRelation[_] => sr match {
        case GT(l, r) => binOpWithPriorityParens(l, r, Doc.text(" > "))
        case GE(l, r) => binOpWithPriorityParens(l, r, Doc.text(" >= "))
        case EQ(l, r) => binOpWithPriorityParens(l, r, Doc.text(" == "))
        case NEQ(l, r) => binOpWithPriorityParens(l, r, Doc.text(" != "))
        case LT(l, r) => binOpWithPriorityParens(l, r, Doc.text(" < "))
        case LE(l, r) => binOpWithPriorityParens(l, r, Doc.text(" <= "))
      }
      case BinOr(l, r) => binOpWithPriorityParens(l, r, Doc.text(" || "))
      case BinAnd(l, r) => binOpWithPriorityParens(l, r, Doc.text(" && "))
      case BinXor(l, r) => binOpWithPriorityParens(l, r, Doc.text(" ^ "))
    }
    case oneArgOp: OneArgumentOperation[_, _] => oneArgOp match {
      // TODO: Only used in parser. Missing implementation for buildNode. Related to BitOp case above?
      case BitInversion(input) => Doc.char('~') + createDoc(input)
      case Negation(input) => Doc.char('!') + createDoc(input)
    }
    // ModQs + operations are not implemented:
    // https://github.com/ScorexFoundation/sigmastate-interpreter/issues/479
    // https://github.com/ScorexFoundation/sigmastate-interpreter/issues/327
    case ModQ(input) => ???
    case ModQArithOp(l, r, opCode) => ???

    case quad: Quadruple[_, _, _, _] => quad match {
      case If(condition, trueBranch, falseBranch) =>
        Doc.text("if (") + createDoc(condition) + Doc.text(") {") +
          createDoc(trueBranch).bracketBy(Doc.empty, Doc.empty, indent) +
          Doc.text("} else {") +
          createDoc(falseBranch).bracketBy(Doc.empty, Doc.empty, indent) +
          Doc.char('}')
      // Will be removed in the future:
      // https://github.com/ScorexFoundation/sigmastate-interpreter/issues/645
      case TreeLookup(tree, key, proof) => ???
    }
    case LogicalNot(input) => Doc.char('!') + nodeWithPriorityParens(input)
  }

  // empty args list returns just `name` suffix
  private def methodDoc(input: SValue, name: String, args: List[SValue] = Nil)(implicit indent: Int): Doc = {
    val argsDoc = if (args.nonEmpty) nTupleDoc(args.map(createDoc)) else Doc.empty
    createDoc(input) + Doc.text(s".$name") + argsDoc
  }
  
  private def nodeWithPriorityParens(v: SValue)(implicit indent: Int): Doc = v match {
    case _:BinAnd | _:BinOr | _:BinXor | _:SimpleRelation[_] | _:LogicalNot => wrapWithParens(createDoc(v))
    case _ => createDoc(v)
  }

  private def binOpWithPriorityParens(l: SValue, r: SValue, sep: Doc)(implicit indent: Int): Doc =
    nodeWithPriorityParens(l) + sep + nodeWithPriorityParens(r)

  private def wrapWithParens(d: Doc)(implicit indent: Int): Doc = d.tightBracketBy(Doc.char('('), Doc.char(')'), indent)
  private def wrapWithBrackets(d: Doc)(implicit indent: Int): Doc = d.tightBracketBy(Doc.char('['), Doc.char(']'), indent)

  /* Create argument representation enclosed in brackets with types, e.g. `(str5: String, i1: Int)` */
  private def argsWithTypesDoc(args: Seq[(Int, SType)])(implicit indent: Int): Doc = {
    val argsWithTypes = args.map { 
      case (i, tpe) => 
        val argName = inferNameFromType(tpe)
        Doc.text(s"$argName$i: ") + STypeDoc(tpe)
    }
    nTupleDoc(argsWithTypes)
  }

  private def STypeDoc(tpe: SType)(implicit indent: Int): Doc = tpe match {
    case SBoolean => Doc.text("Boolean")
    case SByte => Doc.text("Byte")
    case SShort => Doc.text("Short")
    case SInt => Doc.text("Int")
    case SLong => Doc.text("Long")
    case SBigInt => Doc.text("BigInt")
    case SBox => Doc.text("Box")
    case SSigmaProp => Doc.text("SigmaProp")
    case SCollectionType(elemType) => Doc.text("Coll") + wrapWithBrackets(STypeDoc(elemType))
    case STuple(items) => nTupleDoc(items.map(STypeDoc))
    case SContext => Doc.text("Context")
    case SOption(elemType) => Doc.text("Option") + wrapWithBrackets(STypeDoc(elemType))
    case SGroupElement => Doc.text("GroupElement")
    case SPreHeader => Doc.text("PreHeader")
    case SString => Doc.text("String")
    case SAny => Doc.text("Any")
    case SAvlTree => Doc.text("AvlTree")
    case SUnit => Doc.text("Unit")
    case SHeader => Doc.text("Header")
    // TODO: Not tested
    case SGlobal => Doc.text("Global")
    case SFunc(tDom, tRange, tpeParams) => nTupleDoc(tDom.map(STypeDoc)) + Doc.text(" => ") + STypeDoc(tRange)
    // TODO: Are all nodes replaced after bind/typing phase? So it cannot be part of final tree?
    case NoType => Doc.empty
    // Not used in final ergo tree
    case STypeApply(name, args) => ???
    case STypeVar(name) => ???
  }

  // Create `(item1, item2, ...)`
  private def nTupleDoc(items: Seq[Doc])(implicit indent: Int): Doc =
    wrapWithParens(
      Doc.intercalate(Doc.text(", "), items))

  private def inferNameFromType(tpe: SType): String = tpe match {
    case SBoolean => "bool"
    case SByte => "b"
    case SShort => "s"
    case SInt => "i"
    case SLong => "l"
    case SBigInt => "bi"
    case SBox => "box"
    case SSigmaProp => "prop"
    case SCollectionType(elemType) => "coll"
    case STuple(items) => "tuple"
    case SContext => "ctx"
    case SOption(elemType) => "opt"
    case SGroupElement => "ge"
    case SPreHeader => "preHeader"
    case SString => "str"
    case SAny => "any"
    case SAvlTree => "avlTree"
    case SUnit => "unit"
    case SHeader => "header"
    case SGlobal => "global"
    case SFunc(tDom, tRange, tpeParams) => "func"
    // TODO: Are all nodes replaced after bind/typing phase? So it cannot be part of final tree?
    case NoType => ???
    // Not used in final ergo tree
    case STypeApply(name, args) => ???
    case STypeVar(name) => ???
  }

}