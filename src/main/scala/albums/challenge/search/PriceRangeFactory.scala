package albums.challenge.search

import albums.challenge.search.PriceRange.{rangeDelimeter, rangeSize, roundPrecision}

import scala.math.BigDecimal.RoundingMode

class PriceRangeFactory(
    priceMatchingMode: PriceMatchingMode,
) {

  // creates PriceRange(5 - 10) for 5.00, 9.99
  def range(price: BigDecimal): Option[PriceRange] =
    Option.when(price >= 0)(
      PriceRange(BigDecimal((price / rangeSize).toInt + 1, 0) * rangeSize),
    )

  // creates PriceRange(5 - 10) for 10.00, None for 19
  def rangeByExactUpper(price: BigDecimal): Option[PriceRange] =
    Option.when(price > 0 && ((price / rangeSize).toInt * rangeSize) == price)(
      PriceRange(price.setScale(0, RoundingMode.FLOOR)),
    )

  /** Creates sets of price ranges that the price satisfies. The range selection rule is controlled
    * by the `priceMatchingMode` configuration parameter.
    *
    * When `PriceMatchingMode.ExactMode` is used, the price is matched against a non-overlapping set
    * of ranges using the condition `lower <= price < upper`.
    *
    * Examples:
    *   - `12.99` -> `PriceRange(10 - 15)`
    *   - `10.00` -> `PriceRange(10 - 15)`
    *   - `9.99` -> `PriceRange(5 - 10)`
    *
    * When `PriceMatchingMode.FairMode` is applied, a price may satisfy multiple ranges (one or
    * two). In this mode, the price may be rounded up to one-cent precision, and both the lower and
    * upper range boundaries are inclusive (`lower <= price <= upper`).
    *
    * Examples:
    *   - `12.99` -> `PriceRange(10 - 15)`
    *   - `10.00` -> `PriceRange(5 - 10), PriceRange(10 - 15)`
    *   - `9.99` -> `PriceRange(5 - 10), PriceRange(10 - 15)` - rounded to 10
    *   - `9.90` -> `PriceRange(5 - 10)`
    */
  def ranges(price: BigDecimal): Set[PriceRange] = {
    val upperRangePrice = (price + roundPrecision).setScale(0, RoundingMode.FLOOR)

    priceMatchingMode match {
      case PriceMatchingMode.FairMode =>
        List(
          rangeByExactUpper(price),
          range(price),
          range(upperRangePrice),
        ).flatten.toSet
      case PriceMatchingMode.ExactMode =>
        List(
          range(price),
        ).flatten.toSet
    }
  }

  def parse(s: String): Option[PriceRange] = {
    s.split(rangeDelimeter)
      .lift(1)
      .map(_.trim)
      .map(p => PriceRange(BigDecimal(p).setScale(0)))
  }
}
