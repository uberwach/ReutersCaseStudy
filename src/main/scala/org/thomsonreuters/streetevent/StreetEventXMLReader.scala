package org.thomsonreuters.streetevent

import java.io.File
import org.thomsonreuters.streetevent.model.{EventStory, StreetEvent}
import org.thomsonreuters.streetevent.sentiment.StanfordCoreNLPSentimentAnalyser
import scala.xml.{Node, XML}

/**
 * Created by gerrit on 01.11.2014.
 * Accesses StreetEvent Data in XML Format and transforms the XML data into the data structures defined in the streetevent.model package.
 */

object StreetEventXMLReader {

  /**
   * Makes the Street Event reader runnable via command line.
   * @param args path to files or directories
   */
  def main(args: Array[String]) {
    for (arg <- args) {
      // read street event data
      val streetEvents = read(new File(arg))
      // init sentiment analyser
      val sentimentAnalyser = new StanfordCoreNLPSentimentAnalyser()
      // analyse street event data and print it to the output screen
      for (streetEvent <- streetEvents) {
        println("Analysing " + streetEvent.title + " with id " + streetEvent.id)
        println(s"${streetEvent.eventStories.size} event stories found.")
        for (eventStory <- streetEvent.eventStories) {
          val msgExchanges = EventStoryBodyExtractor.extractBody(eventStory)
          println(s"Found ${msgExchanges.size} many messages to analyse in event story ${eventStory.headLine}")
          for (msgExchange <- msgExchanges) {
            println(s"Analyse what ${msgExchange.talker} said:")
            val result = sentimentAnalyser.analyseText(msgExchange.text)
            for (annotation <- result) {
              // TODO: display the annotation
              println(annotation.toString)
            }
          }
        }
      }

    }
  }

  /**
   * Recursively iterates through directories to find StreetEvent xml files.
   * @param f A file or a directory.
   * @return An Array of StreetEvent Data
   */
  def read(f: File): Array[StreetEvent] = {
    if (f.isDirectory) {
      println(s"Parsing directory ${f.getAbsolutePath}")
      f.listFiles().flatMap(file => read(file))
    } else {
      if (f.getName.endsWith(".xml")) {
        println(s"Parsing file ${f.getAbsolutePath}")
        Array(extract(f))
      }
      else {
        // TODO: change to proper logging
        println(s"File ${f.getAbsolutePath} has been ignored. Please keep the parsed directory clean.")
        Array()
      }

    }
  }

  /**
   * Extracts Street Event data structures from XML.
   * @param f Must be an XML File.
   * @return
   */
  private def extract(f: File): StreetEvent = {
    val xmlRoot = XML.loadFile(f)

    // extract EventStory subnodes
    def extractEventStoryFromXML(storyNode: Node): EventStory = {
      EventStory(
        headLine = (storyNode \ "Headline").text,
        body = (storyNode \ "Body").text,
        storyType = (storyNode \ "@storyType").text
      )
    }

    // collect all contained substories
    val containedEventStories = (xmlRoot \ "EventStory").map(storyNode => extractEventStoryFromXML(storyNode))

    StreetEvent(
      id = (xmlRoot \ "@Id").text.toInt,
      typeId = (xmlRoot \ "@eventTypeId").text.toInt,
      title = (xmlRoot \ "eventTitle").text,
      companyTicker = (xmlRoot \ "companyTicker").text,
      eventStories = containedEventStories.toList
    )
  }


}
