package albums.challenge.search

import albums.challenge.search.PriceRange.{rangeBoundaryToString, rangeDelimeter, rangeSize}

import scala.math.BigDecimal.RoundingMode

case class PriceRange(upperBoundary: BigDecimal) {
  private def lowerBoundary: BigDecimal = upperBoundary - rangeSize
  def view: String = {
    s"${rangeBoundaryToString(lowerBoundary)}$rangeDelimeter${rangeBoundaryToString(upperBoundary)}"
  }
}

object PriceRange {

  val rangeSize: BigDecimal = BigDecimal(5)
  val roundPrecision: BigDecimal = BigDecimal(1, 2) // 0.01

  private def rangeBoundaryToString(v: BigDecimal): BigDecimal = v.setScale(0, RoundingMode.HALF_UP)

  val rangeDelimeter = " - "
}
