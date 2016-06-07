package org.thomsonreuters.streetevent.model


/**
 * Created by gerrit on 01.11.2014.
 * This models a StreetEvent as a data structure.
 */
case class StreetEvent(id: Int,
                       typeId: Int,
                       title: String,
                       companyTicker: String,
                       eventStories: List[EventStory])
