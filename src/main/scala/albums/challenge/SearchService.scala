package albums.challenge

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
    Results(
      items = entries,
      // Facets for testing UI
      facets = Map(
        "price" -> List(Facet("5 - 10", 1), Facet("15 - 20", 3)),
        "year" -> List(Facet("2008", 1), Facet("2002", 2))
      ),
      query = query
    )
  }
}
