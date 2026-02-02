package albums.challenge

import albums.challenge.models.Results
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.web.bind.annotation.{GetMapping, RequestParam, RestController}
import org.springframework.web.servlet.ModelAndView
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.logging.log4j.{LogManager, Logger}

import scala.jdk.CollectionConverters.CollectionHasAsScala

@Configuration
class JacksonConfig {
  @Bean
  def defaultScalaModule(): com.fasterxml.jackson.databind.Module = DefaultScalaModule
}

@SpringBootApplication
@RestController
@EnableCaching
class Application @Autowired() (
    dataService: DataService,
    searchService: SearchService,
) {

  private val logger: Logger = LogManager.getLogger(classOf[Application])

  @GetMapping(Array("/"))
  def index(): ModelAndView = {
    val modelAndView = new ModelAndView()
    modelAndView.setViewName("index.html")
    modelAndView
  }

  @GetMapping(Array("/api/search"))
  def search(
      @RequestParam query: String,
      @RequestParam(defaultValue = "") year: java.util.List[String],
      @RequestParam(defaultValue = "") price: java.util.List[String],
  ): Results = {

    val logQueryMsg = {
      List(
        Some(query)
          .filterNot(_.isEmpty)
          .orElse(Some("<Not specified>"))
          .map(q => s"Query: '$q'"),
        year.asScala.filterNot(_.isEmpty) match {
          case Nil           => Option.empty[String]
          case notEmptyYears => Some(notEmptyYears.mkString("year: ", ", ", ""))
        },
        price.asScala.filterNot(_.isEmpty) match {
          case Nil            => Option.empty[String]
          case notEmptyPrices => Some(notEmptyPrices.mkString("price: ", ", ", ""))
        },
      ).flatten
        .mkString("; ")
    }

    logger.info(s"Search with $logQueryMsg")

    searchService.search(
      dataService.fetch(),
      query,
      year.asScala.toList,
      price.asScala.toList,
    )
  }
}

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
