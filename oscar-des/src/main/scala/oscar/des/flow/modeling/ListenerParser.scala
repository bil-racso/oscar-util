package oscar.des.flow.modeling

import oscar.des.engine.Model
import oscar.des.flow.lib._

import scala.collection.immutable.SortedMap
import scala.util.parsing.combinator._

import scala.language.implicitConversions

sealed abstract class ListenerParsingResult
sealed abstract class ParsingSuccess extends ListenerParsingResult
case class DoubleExpressionResult(d:DoubleExpr) extends ParsingSuccess
case class BooleanExpressionResult(b:BoolExpr) extends ParsingSuccess
case class ParsingError(s:String) extends ListenerParsingResult {
  override def toString: String = "Parse Error:\n" + s + "\n"
}

sealed abstract class MultipleParsingResult
case class MultipleParsingSuccess(expressions:List[(String,Expression)]) extends MultipleParsingResult{
  override def toString: String = " MultipleParsingSuccess(\n\t" + expressions.mkString("\n\t") + ")"
}
case class MultipleParsingError(s:String) extends MultipleParsingResult

object ListenerParser{

  def apply(storages:Iterable[Storage],processes:Iterable[ActivableProcess]):ListenerParser = {
    val storagesMap = storages.foldLeft[SortedMap[String,Storage]](SortedMap.empty)(
      (theMap,storage) => theMap + ((storage.name,storage)))
    val processMap = processes.foldLeft[SortedMap[String,ActivableProcess]](SortedMap.empty)(
      (theMap,process) => theMap + ((process.name,process)))
    new ListenerParser(storagesMap, processMap)
  }

  def apply(storages:Iterable[Storage],processes:Iterable[ActivableProcess], expressions:List[(String,String)]): MultipleParsingResult ={
    val myParser = ListenerParser(storages, processes)
    myParser.parseAllListeners(expressions)
  }

  def processCostParser(process:ActivableProcess):ListenerParser = {
    new ListenerParser(SortedMap.empty, SortedMap.empty[String,ActivableProcess]+(("this",process)))
  }

  def storageCostParser(storage:Storage):ListenerParser = {
    new ListenerParser(SortedMap.empty[String,Storage]+(("this",storage)), SortedMap.empty)
  }
}

class ListenerParser(storages:Map[String,Storage],
                     processes:Map[String,ActivableProcess])
  extends ParserWithSymbolTable with ListenersHelper{

  protected override val whiteSpace = """(\s|//.*|(?m)/\*(\*(?!/)|[^*])*\*/)+""".r

  override def skipWhitespace: Boolean = true

  def parseAllListeners(expressions:List[(String,String)]):MultipleParsingResult = {
    MultipleParsingSuccess(expressions.map({
      case (name,expr) =>
        this.apply(expr) match{
          case BooleanExpressionResult(result) =>
            declaredBoolExpr += ((name,result))
            (name,result)
          case DoubleExpressionResult(result) =>
            declaredDoubleExpr += ((name,result))
            (name,result)
          case ParsingError(p) => return MultipleParsingError("Error while parsing " + name + "\n" + p)
        }
    }))
  }

  var declaredBoolExpr:SortedMap[String,BoolExpr] = SortedMap.empty
  var declaredDoubleExpr:SortedMap[String,DoubleExpr] = (SortedMap.empty
  ++ storages.map(nameAndStorage => ("cost of " + nameAndStorage._1, nameAndStorage._2.cost))
  ++ processes.map(nameAndProcess => ("cost of " + nameAndProcess._1, nameAndProcess._2.cost)))

  def apply(input:String):ListenerParsingResult = {
    parseAll(expressionParser, input) match {
      case Success(result:BoolExpr, _) => BooleanExpressionResult(result)
      case Success(result:DoubleExpr, _) => DoubleExpressionResult(result)
      case n:NoSuccess => ParsingError(n.toString)
    }
  }

  def applyAndExpectDouble(input:String):DoubleExpr = {
    apply(input) match{
      case DoubleExpressionResult(r) => r
      case e:ListenerParsingResult =>
        throw new Exception("expected double expression, got " + e.toString())
        null
    }
  }

  def expressionParser:Parser[Expression] = (
    doubleExprParser
      | boolExprParser
      |failure("expected boolean or arithmetic expression"))

  def boolExprParser:Parser[BoolExpr] = (
    "[*]" ~> boolExprParser ^^ {case b => hasAlwaysBeen(b)}
      | "<*>" ~> boolExprParser ^^ {case b => hasBeen(b)}
      | "@" ~> boolExprParser ^^ {case b => becomesTrue(b)}
      | doubleExprParser~(">="|">"|"<="|"<"|"!="|"=")~doubleExprParser ^^ {
      case (a~op~b) => op match{
        case ">" => g(a,b)
        case ">=" => ge(a,b)
        case "<" => l(a,b)
        case "<=" => le(a,b)
        case "=" => eq(a,b)
        case "!=" => neq(a,b)
      }}
    | disjunctionParser)

  def disjunctionParser:Parser[BoolExpr] =
    conjunctionParser ~ opt("|"~>disjunctionParser) ^^ {
      case a~None => a
      case a~Some(b) => or(a,b)}

  def conjunctionParser:Parser[BoolExpr]  =
    atomicBoolExprParser ~ opt("&"~>conjunctionParser) ^^ {
      case a~None => a
      case a~Some(b) => and(a,b)}

  def atomicBoolExprParser:Parser[BoolExpr] = (
    "empty(" ~> storageParser <~")" ^^ {empty(_)}
      | processBoolProbe("running",running)
      | processBoolProbe("anyBatchStarted",anyBatchStarted)
      | "true" ^^^ boolConst(true)
      | "false" ^^^ boolConst(false)
      | boolListener
      | binaryOperatorBB2BParser("and",and)
      | binaryOperatorBB2BParser("or",or)
      | binaryOperatorBB2BParser("since",since)
      | unaryOperatorB2BParser("not",not)
      | "!"~>boolExprParser^^{case (b:BoolExpr) => not(b)}
      | unaryOperatorB2BParser("hasAlwaysBeen",hasAlwaysBeen)
      | unaryOperatorB2BParser("hasBeen",hasBeen)
      | unaryOperatorB2BParser("becomesTrue",becomesTrue)
      | unaryOperatorB2BParser("becomesFalse",becomesFalse)
      | binaryOperatorDD2BParser("g",g)
      | binaryOperatorDD2BParser("ge",ge)
      | binaryOperatorDD2BParser("l",l)
      | binaryOperatorDD2BParser("le",le)
      | binaryOperatorDD2BParser("eq",eq)
      | binaryOperatorDD2BParser("ne",neq)
      | "changed(" ~> (boolExprParser | doubleExprParser) <~")" ^^ {case e:Expression => changed(e)}
      | "ite(" ~> boolExprParser~(","~>boolExprParser)~(","~>boolExprParser <~ ")") ^^{ case i~t~e => booleanITE(i,t,e)}
      | "("~>boolExprParser<~")"
      | failure("expected boolean expression"))

  def binaryTerm:Parser[BoolExpr] = unaryOperatorB2BParser("not",not)

  def doubleExprParser:Parser[DoubleExpr] =
    term ~ opt(("+"|"-")~doubleExprParser) ^^ {
      case a~None => a
      case a~Some("+"~b) => plus(a,b)
      case a~Some("-"~b) => minus(a,b)}

  def term: Parser[DoubleExpr] =
    atomicDoubleExprParser ~ opt(("*"|"/")~term) ^^ {
      case a~None => a
      case a~Some("*"~b) => mult(a,b)
      case a~Some("/"~b) => div(a,b)}

  def atomicDoubleExprParser:Parser[DoubleExpr] = (
    storageDoubleProbe("content",stockLevel)
      | storageDoubleProbe("capacity",stockCapacity)
      | storageDoubleProbe("relativeStockLevel",relativeStockLevel)
      | storageDoubleProbe("totalPut",totalPut)
      | storageDoubleProbe("totalFetch",totalFetch)
      | storageDoubleProbe("totalLosByOverflow",totalLosByOverflow)
      | storageDoubleProbe("cost",(s:Storage) => s.cost)
      | processDoubleProbe("cost",(p:ActivableProcess) => p.cost)
      | processDoubleProbe("completedBatchCount",completedBatchCount)
      | processDoubleProbe("startedBatchCount",startedBatchCount)
      | processDoubleProbe("totalWaitDuration",totalWaitDuration)
      | doubleParser ^^ {d:Double => doubleConst(d)}
      | doubleListener
      | binaryOperatorDD2DParser("plus",plus)
      | binaryOperatorDD2DParser("minus",minus)
      | binaryOperatorDD2DParser("mult",mult)
      | binaryOperatorDD2DParser("div",(a,b) => div(a,b))
      | unaryOperatorD2DParser("opposite",opposite)
      | unaryOperatorD2DParser("delta",delta)
      | unaryOperatorB2DParser("cumulatedDuration",cumulatedDuration)
      | unaryOperatorB2DParser("cumulatedDurationNotStart",culumatedDurationNotStart)
      | "time"^^^ currentTime
      | "tic" ^^^ delta(currentTime)
      | unaryOperatorD2DParser("integral",ponderateWithDuration)
      | ("maxOnHistory("|"max(") ~> doubleExprParser~opt("," ~> boolExprParser)<~")" ^^ {
      case (d~None) => maxOnHistory(d)
      case (d~Some(cond:BoolExpr)) => maxOnHistory(d,cond)}
      | ("minOnHistory("|"min(") ~> doubleExprParser~opt("," ~> boolExprParser)<~")"^^ {
      case (d~None) => minOnHistory(d)
      case (d~Some(cond:BoolExpr)) => minOnHistory(d,cond)}
      | unaryOperatorD2DParser("avg",avgOnHistory)
      | unaryOperatorD2DParser("avgOnHistory",avgOnHistory)
      | "ite(" ~> boolExprParser~(","~>doubleExprParser)~(","~>doubleExprParser<~ ")") ^^{ case i~t~e => doubleITE(i,t,e)}
      |"duration(" ~> boolExprParser <~")" ^^ {case e => duration(e)}
      | "-"~> doubleExprParser ^^ {opposite(_)}
      | "("~>doubleExprParser<~")"
      | failure("expected arithmetic expression"))

  //generic code
  def boolListener:Parser[BoolExpr] = {
    identifier convertStringUsingSymbolTable(declaredBoolExpr, "delcared boolean expression") //^^ {boolSubExpression(_)}
  }
  def doubleListener:Parser[DoubleExpr] =
    identifier convertStringUsingSymbolTable(declaredDoubleExpr, "declared double expression") //^^{doubleSubExpression(_)}

  //probes on storages
  def storageDoubleProbe(probeName:String,constructor:Storage=>DoubleExpr):Parser[DoubleExpr] =
    probeName~>"("~>storageParser <~")" ^^ {constructor(_)}

  def storageParser:Parser[Storage] = identifier convertStringUsingSymbolTable(storages, "storage")

  //probes on processes
  def processDoubleProbe(probeName:String,constructor:ActivableProcess=>DoubleExpr):Parser[DoubleExpr] =
    probeName~>"("~>processParser <~")" ^^ {constructor(_)}
  def processBoolProbe(probeName:String,constructor:ActivableProcess=>BoolExpr):Parser[BoolExpr] =
    probeName~>"("~>processParser <~")" ^^ {constructor(_)}
  def processParser:Parser[ActivableProcess] = identifier convertStringUsingSymbolTable(processes, "process")

  // some generic parsing methods
  def unaryOperatorD2DParser(operatorString:String,constructor:DoubleExpr=>DoubleExpr):Parser[DoubleExpr] =
    operatorString~>"("~>doubleExprParser<~")" ^^ {
      case param => constructor(param)
    }

  def unaryOperatorB2BParser(operatorString:String,constructor:BoolExpr=>BoolExpr):Parser[BoolExpr] =
    operatorString~>"("~>boolExprParser<~")" ^^ {
      case param => constructor(param)
    }

  def unaryOperatorB2DParser(operatorString:String,constructor:BoolExpr=>DoubleExpr):Parser[DoubleExpr] =
    operatorString~>"("~>boolExprParser<~")" ^^ {
      case param => constructor(param)
    }

  def binaryOperatorDD2DParser(operatorString:String,constructor:(DoubleExpr,DoubleExpr)=>DoubleExpr):Parser[DoubleExpr] =
    operatorString~"("~>doubleExprParser~(","~>doubleExprParser<~")") ^^ {
      case param1~param2 => constructor(param1,param2)
    }

  def binaryOperatorDD2BParser(operatorString:String,constructor:(DoubleExpr,DoubleExpr)=>BoolExpr):Parser[BoolExpr] =
    operatorString~>"("~>doubleExprParser~(","~>doubleExprParser<~")") ^^ {
      case param1~param2 => constructor(param1,param2)
    }

  def binaryOperatorBB2BParser(operatorString:String,constructor:(BoolExpr,BoolExpr)=>BoolExpr):Parser[BoolExpr] =
    operatorString~>"("~>boolExprParser~(","~>boolExprParser<~")") ^^ {
      case param1~param2 => constructor(param1,param2)
    }

  def identifierNoSpaceAllowed:Parser[String] = """[a-zA-Z0-9]+""".r ^^ {_.toString}
  def identifierSpaceAllowed:Parser[String] = """\"[a-zA-Z0-9 ]+\"""".r ^^ {_.toString.drop(1).dropRight(1)}
  def identifier:Parser[String] = identifierSpaceAllowed | identifierNoSpaceAllowed

  def doubleParser:Parser[Double] = """[0-9]+(\.[0-9]+)?""".r ^^ {case s:String => println("converting" + s);s.toDouble}
}

object ParserTester extends App with FactoryHelper{

  val m = new Model
  val aStorage = fIFOStorage(10,Nil,"aStorage",null,false)
  val bStorage = fIFOStorage(10,Nil,"bStorage",null,false)

  val aProcess = singleBatchProcess(m, 5000, Array(), Array((()=>1,aStorage)), null, "aProcess", null, "completedBatchCount(this)")
  val bProcess = singleBatchProcess(m, 5000, Array(), Array((()=>1,aStorage)), null, "bProcess", null)

  val myParser = ListenerParser(List(aStorage,bStorage), List(aProcess,bProcess))

  println("testParseIdentifierWithSpace:" + myParser.parseAll(myParser.identifierSpaceAllowed, "\"coucou gamin\""))
  println("testParseIdentifier:" + myParser.parseAll(myParser.identifier, "\"coucou gamin\""))

  def testOn(s:String){
    println("testing on:" + s)
    println(myParser(s))
    println
  }
  testOn("cost(aProcess) + cost(aStorage)")
  testOn("completedBatchCount(aProcess) /*a comment in the middle*/ * totalPut(aStorage)")
  testOn("-(-(-completedBatchCount(aProcess)) * -totalPut(aStorage))")
  testOn("-(-(-completedBatchCount(aProcess)) + -totalPut(aStorage))")
  testOn("cumulatedDuration(empty(bStorage))")
  testOn("cumulatedDuration(!!!<*>running(bProcess))")
  testOn("cumulatedDuration(!!!<*>running(cProcess))")
  testOn("empty(aStorage) & empty(aStorage) | empty(aStorage)")
  testOn("empty(aStorage) & empty(aStorage) + empty(aStorage)")
  testOn("empty(aStorage) & empty(aStorage) & empty(aStorage)")

  testOn("cumulatedDuration(!running(bProcess))")
  testOn("cumulatedDurationNotStart(not(running(aProcess)))")
  testOn("max(content(aStorage))")
  testOn("min(content(aStorage))")
  testOn("avg(relativeStockLevel(bStorage))")
  testOn("avg(content(aStorage))")
  testOn("integral(content(bStorage))")

  val expressionList = List(
    ("a","completedBatchCount(aProcess) /*a comment in the middle*/ * totalPut(aStorage)"),
    ("b","-(-(-completedBatchCount(aProcess)) * -totalPut(aStorage))"),
    ("c","-(-(-completedBatchCount(aProcess)) + -totalPut(aStorage))"),
    ("d","integral(content(bStorage))"),
    ("e","b * c + d + cost(aProcess)"))
  println(myParser.parseAllListeners(expressionList))
}

trait ParserWithSymbolTable extends RegexParsers{
  class parserWithSymbolTable(identifierParser: Parser[String]) {
    def convertStringUsingSymbolTable[U](symbolTable: Map[String,U], symbolType: String): Parser[U] = new Parser[U] {
      def apply(in: Input) = identifierParser(in) match {
        case Success(x, in1) => symbolTable.get(x) match {
          case Some(u: U) => Success(u, in1)
          case None => Failure("" + x + " is not a known " + symbolType + ": (" + symbolTable.keys.mkString(",") + ") (add quotes around identifiers with white spaces)", in)
        }
        case f: Failure => f
        case e: Error => e
      }
    }
  }

  implicit def addSymbolTableFeature(identifierParser:Parser[String]):parserWithSymbolTable = new parserWithSymbolTable(identifierParser)
}