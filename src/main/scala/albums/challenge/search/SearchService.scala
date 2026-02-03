package albums.challenge.search

import albums.challenge.models.{Entry, Facet, Results}
import org.apache.logging.log4j.{LogManager, Logger}
import org.springframework.stereotype.Service

import java.time.OffsetDateTime
import scala.math.BigDecimal.RoundingMode
import scala.util.{Failure, Success, Try}

@Service
class SearchService(
    config: SearchConfig,
) {

  private val logger: Logger = LogManager.getLogger(classOf[SearchService])

  val rangeFactory = new PriceRangeFactory(config.priceMatchingMode)

  def enrichedEntry(original: Entry): EnrichedEntry = {
    val year = Try(OffsetDateTime.parse(original.releaseDate)) match {
      case Success(date) => Some(date.getYear)
      case Failure(_) =>
        logger.error(s"Error parsing date: ${original.releaseDate}")
        Option.empty[Int]
    }
    val priceNormalized = BigDecimal(original.price.toString).setScale(2, RoundingMode.HALF_UP)
    EnrichedEntry(original, year, rangeFactory.ranges(priceNormalized))
  }

  def search(
      entries: List[Entry],
      query: String,
      year: List[String] = List.empty,
      price: List[String] = List.empty,
  ): Results = {
    val q = query.trim.toLowerCase
    val filteredByQuery =
      if (q.isEmpty) entries
      else entries.filter(_.title.toLowerCase.contains(q))

    val inputYears = year
      .map(_.toInt)
      .toSet
    val inputRanges = price
      .flatMap(rangeFactory.parse)
      .toSet

    // Entries enriched with parsed release year and corresponding price ranges.
    // In some cases, an entry may belong to two adjacent ranges if its price
    // can be rounded up to the higher range.
    val augmentedEntries = filteredByQuery.map(enrichedEntry)

    val filteredByYears =
      augmentedEntries.filter(e => inputYears.exists(e.yearParsed.contains) || inputYears.isEmpty)

    val filteredByRanges = augmentedEntries.filter(e =>
      inputRanges.exists(e.priceRanges.contains) || inputRanges.isEmpty,
    )

    // Collect entry counts for each range item in a single pass over the collection
    val priceFacets = filteredByYears
      .foldLeft(Map.empty[PriceRange, Int]) { case (mapPrices, entry) =>
        entry.priceRanges.foldLeft(mapPrices) { case (m, r) =>
          m + (r -> m.get(r).fold(1)(_ + 1))
        }
      }
      .toList
      .sortBy(_._1.upperBoundary)
      .map { case (range, count) => Facet(range.view, count) }

    // Collect entry counts for each year in a single pass over the collection
    val yearFacets = filteredByRanges
      .foldLeft(Map.empty[Int, Int]) { case (mapYears, entry) =>
        entry.yearParsed
          .fold(mapYears)(y => mapYears + (y -> mapYears.get(y).fold(1)(_ + 1)))

      }
      .toList
      .sortBy(_._1)(Ordering[Int].reverse)
      .map { case (year, count) => Facet(year.toString, count) }

    val facetsMap = List(
      "price" -> priceFacets,
      "year" -> yearFacets,
    ).filterNot(_._2.isEmpty).toMap

    val entriesMatchingAllCriteria = filteredByYears
      .intersect(filteredByRanges)
      .map(_.original)

    Results(
      items = entriesMatchingAllCriteria,
      facets = facetsMap,
      query = query,
    )
  }
}
