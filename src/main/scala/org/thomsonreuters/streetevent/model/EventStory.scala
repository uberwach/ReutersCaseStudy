package org.thomsonreuters.streetevent.model

/**
 * Created by gerrit on 01.11.2014.
 * This models EventStories as a data structures. Of main usage is the body.
 */

case class EventStory(headLine: String,
                      body: String,
                      storyType: String)
