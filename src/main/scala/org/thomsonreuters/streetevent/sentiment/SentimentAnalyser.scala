package org.thomsonreuters.streetevent.sentiment

import edu.stanford.nlp.pipeline.Annotation

/**
 * Created by gerrit on 01.11.2014.
 * Interface that describes what we expect from a sentiment analyser.
 */
trait SentimentAnalyser {
  type SentimentResult = List[Annotation]

  def analyseText(text : String) : SentimentResult

  // todo: add retraining?
}
