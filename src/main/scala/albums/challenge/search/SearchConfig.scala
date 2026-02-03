package albums.challenge.search

import org.springframework.context.annotation.{Bean, Configuration}

@Configuration
class SearchConfigFactory {

  @Bean
  def searchConfig(): SearchConfig =
    SearchConfig(
      priceMatchingMode = PriceMatchingMode.FairMode, // or ExactNode
    )
}

final case class SearchConfig(
    priceMatchingMode: PriceMatchingMode,
)
