package albums.challenge

import albums.challenge.SearchServiceHelper.{AugmentedEntry, PriceRange}
import albums.challenge.models.{Entry, Facet, Results}
import org.springframework.stereotype.Service

@Service
class SearchService {
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

    val enteredYears = year.map(_.toInt)
    val enteredRanges = price
      .flatMap(_.split("-").lift(1))
      .map(_.trim)
      .map(upper => PriceRange(upper.toInt))

    val augmentedEntries = filteredByQuery.map(AugmentedEntry.apply)

    val filteredByYears = augmentedEntries.filter(e =>
      enteredYears.exists(e.yearParsed.contains) || enteredYears.isEmpty)

    val filteredByRanges = augmentedEntries.filter(e =>
      enteredRanges.exists(e.priceRange.contains) || enteredRanges.isEmpty)

    val priceFacets = filteredByYears.flatMap(_.priceRange)
      .distinct
      .sortBy(_.upperBoundary)
      .map { r =>
        Facet(r.view, filteredByYears.count(e => e.priceRange.contains(r)))
      }

    val yearFacets = filteredByRanges
      .filter(_.yearParsed.nonEmpty)
      .groupBy(_.yearParsed)
      .toList
      .sortBy(v => v._1.map(_.*(-1)))
      .flatMap { case (yOpt, ee) =>
        yOpt.map(y => Facet(y.toString, ee.size))
      }

    val facetsMap = List(
      "price" -> priceFacets,
      "year" -> yearFacets
    ).filterNot(_._2.isEmpty)
      .toMap

    Results(
      items = filteredByYears.intersect(filteredByRanges)
        .sortBy(_.original.price)
        .map(_.original),
      facets = facetsMap,
      query = query
    )
  }
}
