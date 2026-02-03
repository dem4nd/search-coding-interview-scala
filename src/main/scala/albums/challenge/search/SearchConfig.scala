package albums.challenge.search

import org.springframework.context.annotation.{Bean, Configuration}
import PriceMatchingMode._

@Configuration
class SearchConfigFactory {

  private val configInstance = SearchConfig(
    priceMatchingMode = resolvePriceMatchingMode(), // ExactNode or FairMode
  )

  @Bean
  def searchConfig(): SearchConfig = configInstance

  private def resolvePriceMatchingMode(): PriceMatchingMode =
    sys.env
      .get("PRICE_MATCHING_MODE")
      .map(_.trim.toUpperCase)
      .collect {
        case "FAIR"  => FairMode
        case "EXACT" => ExactMode
      }
      .getOrElse(ExactMode)
}

final case class SearchConfig(
    priceMatchingMode: PriceMatchingMode,
)
