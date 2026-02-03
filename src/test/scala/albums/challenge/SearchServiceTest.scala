package albums.challenge

import albums.challenge.models.{Entry, Facet}
import albums.challenge.search.{
  PriceMatchingMode,
  PriceRange,
  PriceRangeFactory,
  SearchConfig,
  SearchService,
}

class SearchServiceTest extends munit.FunSuite {
  val searchServiceExact = new SearchService(
    SearchConfig(priceMatchingMode = PriceMatchingMode.ExactMode),
  )
  val searchServiceFair = new SearchService(
    SearchConfig(priceMatchingMode = PriceMatchingMode.FairMode),
  )

  val entry1 = Entry(
    "Legend: The Best of Bob Marley and the Wailers (Remastered)",
    9.99f,
    "2002-01-01T00:00:00-07:00",
    "",
    "",
  )
  val entry2 = Entry(
    "The Very Best of The Doors",
    19.99f,
    "2008-01-29T00:00:00-07:00",
    "",
    "",
  )
  val entry3 = Entry(
    "The Best of Earth Wind & Fire Vol. 1",
    5f,
    "1978-11-23T00:00:00-07:00",
    "",
    "",
  )
  val entry4 = Entry(
    "The Least Worst of Type O Negative",
    15.99f,
    "1994-11-13T00:00:00-07:00",
    "",
    "",
  )
  val entry5 = Entry(
    "The Best of Sade",
    20f,
    "1983-01-01T00:00:00-07:00",
    "",
    "",
  )
  val entries = List(entry1, entry2, entry3, entry4, entry5)

  test("Empty search") {
    assertEquals(
      searchServiceExact.search(entries, "").items,
      entries,
    )
  }

  test("Search by general keyword") {
    assertEquals(
      searchServiceExact.search(entries, "the").items,
      entries,
    )
  }

  test("Search by exact keyword") {
    assertEquals(
      searchServiceExact.search(entries, "doors").items,
      List(entry2),
    )
  }

  test("Price facet generation with non-inclusive upper boundary") {
    assertEquals(
      searchServiceExact.search(entries, "best").facets.get("price"),
      Some(List(Facet("5 - 10", 2), Facet("15 - 20", 1), Facet("20 - 25", 1))),
    )
  }

  test("Price facet generation with inclusive upper boundary") {
    assertEquals(
      searchServiceFair.search(entries, "best").facets.get("price"),
      Some(
        List(
          Facet("0 - 5", 1),
          Facet("5 - 10", 2),
          Facet("10 - 15", 1),
          Facet("15 - 20", 2),
          Facet("20 - 25", 2),
        ),
      ),
    )
  }

  test("Year facet generation") {
    assertEquals(
      searchServiceExact.search(entries, "best").facets.get("year"),
      Some(List(Facet("2008", 1), Facet("2002", 1), Facet("1983", 1), Facet("1978", 1))),
    )
  }

  test("Filter multiple facet values") {
    val result =
      searchServiceExact.search(entries, "best", List("2002", "2008"))

    assertEquals(
      result.items,
      List(entry1, entry2),
    )
    assertEquals(
      result.facets.get("year"),
      Some(List(Facet("2008", 1), Facet("2002", 1), Facet("1983", 1), Facet("1978", 1))),
    )
    assertEquals(
      result.facets.get("price"),
      Some(List(Facet("5 - 10", 1), Facet("15 - 20", 1))),
    )
  }

  test("Filter multiple facets") {
    val result =
      searchServiceExact.search(entries, "best", List("2002"), List("5 - 10"))

    assertEquals(
      result.items,
      List(entry1),
    )
    // available years corresponds price filter "5 - 10"
    assertEquals(
      result.facets.get("year"),
      Some(List(Facet("2002", 1), Facet("1978", 1))),
    )
    // available years corresponds year filter "2002"
    assertEquals(
      result.facets.get("price"),
      Some(List(Facet("5 - 10", 1))),
    )
  }

  test("Filter returns zero count") {
    val result = searchServiceExact.search(
      entries,
      "best",
      List("2002", "2008"),
      List("15 - 20"),
    )

    assertEquals(
      result.items,
      List(entry2),
    )
    assertEquals(
      result.facets.get("year"),
      Some(List(Facet("2008", 1))),
    )
    assertEquals(
      result.facets.get("price"),
      Some(List(Facet("5 - 10", 1), Facet("15 - 20", 1))),
    )
  }

  test("Ranges generation for PriceMatchingMode.ExactMode") {
    assertEquals(
      searchServiceExact.rangeFactory.ranges(BigDecimal(1299, 2)), // 12.99
      Set(PriceRange(BigDecimal(15, 0))),
    )

    assertEquals(
      searchServiceExact.rangeFactory.ranges(BigDecimal(10, 0)), // 10
      Set(PriceRange(BigDecimal(15, 0))),
    )

    assertEquals(
      searchServiceExact.rangeFactory.ranges(BigDecimal(999, 2)), // 9.99
      Set(PriceRange(BigDecimal(10, 0))),
    )
  }

  test("Ranges generation for PriceMatchingMode.FairMode") {
    assertEquals(
      searchServiceFair.rangeFactory.ranges(BigDecimal(1299, 2)), // 12.99
      Set(PriceRange(BigDecimal(15, 0))),
    )

    assertEquals(
      searchServiceFair.rangeFactory.ranges(BigDecimal(10, 0)), // 10
      Set(PriceRange(BigDecimal(10, 0)), PriceRange(BigDecimal(15, 0))),
    )

    assertEquals(
      searchServiceFair.rangeFactory.ranges(BigDecimal(999, 2)), // 9.99
      Set(PriceRange(BigDecimal(10, 0)), PriceRange(BigDecimal(15, 0))),
    )

    assertEquals(
      searchServiceFair.rangeFactory.ranges(BigDecimal(990, 2)), // 9.90
      Set(PriceRange(BigDecimal(10, 0))),
    )
  }
}
