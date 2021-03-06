package leon.comparison

import leon.comparison.Utils._
import leon.purescala.Common.Tree
import leon.purescala.Definitions.{CaseClassDef, ClassDef}
import leon.purescala.Expressions.{CaseClassPattern, _}
import leon.purescala.Types.{ClassType, TypeTree}

/**
  * Created by joachimmuth on 02.06.16.
  *
  * This object contains all the scoring method to handle different cases
  */
object Scores {

  /**
    * compare only the MatchCases conditions, not the returned value, which are compared through main compareExp
    * function
    *
    * @return
    */
  def scoreMatchExpr(x: MatchExpr, y: MatchExpr): Double = {
    def scoreMatchCase(casesB: Seq[MatchCase], cases: Seq[MatchCase]): Double = {
      val mapCasesB = toMap(casesB)
      val mapCases = toMap(cases)
      scoreMapMatchCase(mapCasesB, mapCases)
    }

    /**
      * Compare the map containing the MatchCases
      * NOW: just check how many similar key they have
      * NEXT: take into account the order; some MatchCase can be non exclusive, meaning that it "eats" the next one:
      * case x if x < 4 => ...
      * case x if x < 10 => ...
      * doesn't have the same meaning if inverted.
      *
      * NEXT: take into account invoked expression
      *
      * @param mapCasesB
      * @param mapCases
      * @return
      */
    def scoreMapMatchCase(mapCasesB: Map[Tree, Expr], mapCases: Map[Tree, Expr]): Double = {
      val all = mapCasesB.keys.size + mapCases.keys.size.toDouble
      val diff = ((mapCasesB.keySet -- mapCases.keySet) ++ (mapCases.keySet -- mapCasesB.keySet)).size.toDouble
      (all - diff) / all
    }

    def toMap(cases: Seq[MatchCase]) = {
      cases.map(a => a.pattern match {
        case InstanceOfPattern(_, ct) => ct -> a.rhs
        case CaseClassPattern(_, ct, _) => ct -> a.rhs
        case _ => a.pattern -> a.rhs
      }).toMap
    }

    scoreMatchCase(x.cases, y.cases)
  }


  /**
    * One hard piece is to compare two case clase, because it cannot be normalized like value
    *
    * @return
    */

  def scoreClassDef(classA: ClassDef, classB: ClassDef): Double = {
    (classA, classB) match {
      case (a, b) if (a.isAbstract && b.isAbstract) =>
        if (a.knownCCDescendants.size == b.knownCCDescendants.size) 1.0
        else 0.0
      case (a: CaseClassDef, b: CaseClassDef) =>
        scoreCaseClassDef(a, b)
      case _ => 0.0

    }
  }

  def compareTypeTree(typeA: TypeTree, typeB: TypeTree): Double = (typeA, typeB) match {
    case (a: ClassType, b: ClassType) => scoreClassDef(a.classDef, b.classDef)
    case _ => 0.0
  }

  def scoreCaseClass(a: CaseClass, b: CaseClass): Double = {
    scoreCaseClassDef(a.ct.classDef, b.ct.classDef)
  }


  /**
    * Compare two CaseClass definition taking into account different parameter:
    * - the number of arguments of it's own type
    * - the number of arguments of it's parent type
    * - the other arguments of primitive types
    *
    * NEXT: take into account matching between parents
    * NEXT: take into account others parameters ?
    *
    * @param a
    * @param b
    * @return
    */
  def scoreCaseClassDef(a: CaseClassDef, b: CaseClassDef): Double = {
    val ownTypeA: Int = argumentsOfOwnType(a)
    val ownTypeB: Int = argumentsOfOwnType(b)
    val scoreOwnType = percent(ownTypeA, ownTypeB)

    val parentTypeA: Int = argumentsOfParentType(a)
    val parentTypeB: Int = argumentsOfParentType(b)
    val scoreParentType = percent(parentTypeA, parentTypeB)

    val otherTypeA = a.fields.map(_.getType).filterNot(_.isInstanceOf[ClassDef])
    val otherTypeB = b.fields.map(_.getType).filterNot(_.isInstanceOf[ClassDef])
    val scoreOtherType = scoreSeqType(otherTypeA, otherTypeB)

    // arithmetic mean instead of geometric, we don't want to have 0.0% if one of these criteria don't match
    val score: Double = mean(List(scoreOwnType, scoreParentType, scoreOtherType))

    score
  }


  def scoreSeqType(a: Seq[TypeTree], b: Seq[TypeTree]): Double = {
    val intersect = a.intersect(b).size
    matchScore(intersect, a.size, b.size)
  }

  /**
    * Count how many occurrences of its own type appear in its arguments
    * (to be improved if multiples types)
    *
    * @param a the case class
    * @return
    */
  def argumentsOfOwnType(a: CaseClassDef): Int =
    a.fields.map(_.getType).count(a.tparams.map(_.tp.getType).contains(_))


  /**
    * Count how many occurrences of its parent type appear in its arguments
    *
    * @param a
    * @return
    */
  def argumentsOfParentType(a: CaseClassDef): Int = a match {
    case _ if a.hasParent => a.fields.map(_.getType).count(_ == a.parent.get.getType)
    case _ => 0
  }


}
