package org.thomsonreuters.streetevent

import org.thomsonreuters.streetevent.model.EventStory

import scala.util.matching.Regex._
/**
 * Created by gerrit on 01.11.2014.
 * Takes the body of an extractor and extracts a list of tuples of the form (talker, text).
 * For instance, ("John Kechijan, Thomson Corporation", "Thank you, Carol. [...]")
 */
object EventStoryBodyExtractor {

  case class result(talker: String, text: String)

  val operator = "(.*)\\[([0-9]+)\\]\\s*".r

  def extractBody(eventStory: EventStory) = {
    val body = eventStory.body
    val splitBody = body.split("--------------------------------------------------------------------------------")

    splitBody.map(_.trim)
      .zipWithIndex
      .flatMap(pair => {
      val (str, index) = pair
      str match {
        case operator(name, num) => {
          Some(result(name, splitBody(index+1)))
        }
        case _ => None
      }
    })

  }
}
