package albums.challenge

import albums.challenge.models.Entry
import org.apache.logging.log4j.{LogManager, Logger}

import java.time.OffsetDateTime
import scala.util.{Failure, Success, Try}

object SearchServiceHelper {

  private val logger: Logger = LogManager.getLogger(classOf[SearchServiceHelper.type])

  val rangeSize: Int = 5
  val roundPrecision: Float = 0.01f

  case class PriceRange(upperBoundary: Int) {
    private def lowerBoundary: Int = upperBoundary - rangeSize
    def view: String = s"$lowerBoundary - $upperBoundary"
  }

  object PriceRange {
    def exactRange(price: Float): Option[PriceRange] =
      Option.when(price >= 0.0f)(
        PriceRange(((price.toInt / rangeSize) + 1) * rangeSize)
      )

    def ranges(price: Float): Set[PriceRange] =
      List(
        exactRange(price),
        exactRange((price + roundPrecision).toInt.toFloat)
      )
        .flatten
        .toSet
  }

  case class AugmentedEntry(original: Entry, yearParsed: Option[Int], priceRange: Set[PriceRange])

  object AugmentedEntry {
    def apply(original: Entry): AugmentedEntry = {
      val year = Try(OffsetDateTime.parse(original.releaseDate)) match {
        case Success(date) => Some(date.getYear)
        case Failure(parseFailure) =>
          logger.error(s"Error parsing date: ${original.releaseDate}")
          Option.empty[Int]
      }
      AugmentedEntry(original, year, PriceRange.ranges(original.price))
    }
  }
}
