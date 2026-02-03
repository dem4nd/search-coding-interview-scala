package albums.challenge.search

import albums.challenge.models.Entry

case class EnrichedEntry(original: Entry, yearParsed: Option[Int], priceRanges: Set[PriceRange])
