package albums.challenge.search

sealed trait PriceMatchingMode

object PriceMatchingMode {
  case object ExactMode extends PriceMatchingMode
  case object FairMode extends PriceMatchingMode

  val values: List[PriceMatchingMode] = List(ExactMode, FairMode)
}
