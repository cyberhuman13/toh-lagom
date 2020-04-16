package com.chariotsolutions.tohlagom.impl

import play.api.libs.json.{Format, Json}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag}

/**
 * This interface defines all the events that the Hero aggregate supports.
 */
sealed trait HeroEvent extends AggregateEvent[HeroEvent] {
  def aggregateTag = HeroEvent.Tag
}

object HeroEvent {
  implicit val format: Format[HeroEvent] = Json.format
  val NumShards = 2
  val Tag =
    if (NumShards > 1) AggregateEventTag.sharded[HeroEvent](NumShards)
    else AggregateEventTag[HeroEvent]
}

/**
 * Events get stored and loaded from the database, hence a JSON format
 * needs to be declared so that they can be serialized and deserialized.
 */
case class HeroCreated(name: String) extends HeroEvent
case class HeroChanged(newName: String, oldName: String) extends HeroEvent

case object HeroDeleted extends HeroEvent {
  implicit val format: Format[HeroDeleted.type] = Json.format
}

object HeroCreated {
  implicit val format: Format[HeroCreated] = Json.format
}

object HeroChanged {
  implicit val format: Format[HeroChanged] = Json.format
}
