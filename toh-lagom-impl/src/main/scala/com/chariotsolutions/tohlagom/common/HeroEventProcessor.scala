package com.chariotsolutions.tohlagom.common

import com.chariotsolutions.tohlagom.impl.HeroEvent
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventShards, AggregateEventTag}

object HeroEventProcessor {
  val Table = "hero"
  val IdColumn = "id"
  val NameColumn = "name"
  val EventProcessorId = "hero-offset"
}

trait HeroEventProcessor {
  def aggregateTags: Set[AggregateEventTag[HeroEvent]] = HeroEvent.Tag match {
    case tagger: AggregateEventTag[HeroEvent] =>
      Set(tagger)
    case shardedTagger: AggregateEventShards[HeroEvent] =>
      shardedTagger.allTags
  }
}
