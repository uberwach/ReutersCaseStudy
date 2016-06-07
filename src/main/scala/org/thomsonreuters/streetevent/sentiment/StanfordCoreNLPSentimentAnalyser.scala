package org.thomsonreuters.streetevent.sentiment

import edu.stanford.nlp.pipeline.Annotation
import org.thomsonreuters.streetevent.sentiment.pipeline.SentimentPipelineAdapter
import scala.collection.JavaConversions._

/**
 * Created by gerrit on 01.11.2014.
 * This uses the stanford implementation of sentiment analysis.
 * TODO: change SentimentResult and add transformation from List[nlp.stanford.Annotation] to new SentimentResult here.
 */
class StanfordCoreNLPSentimentAnalyser extends SentimentAnalyser{

  override def analyseText(text: String): SentimentResult = {
    SentimentPipelineAdapter.parse(text).toList
  }
}
