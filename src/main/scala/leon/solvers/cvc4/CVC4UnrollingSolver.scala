/* Copyright 2009-2015 EPFL, Lausanne */

package leon
package solvers
package cvc4

import purescala.Definitions._

import unrolling._
import theories._

class CVC4UnrollingSolver(context: LeonContext, program: Program, underlying: Solver)
  extends UnrollingSolver(context, program, underlying, theories = new NoEncoder)
