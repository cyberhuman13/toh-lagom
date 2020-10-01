package com.chariotsolutions.tohlagom.impl

import play.api.libs.json.{Format, Json}
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{Effect, ReplyEffect}

/**
 * The current state of the Hero aggregate.
 * The 'valid' field is set to false for deleted aggregates.
 */
case class HeroState(name: String, valid: Boolean) {
  def applyCommand(cmd: HeroCommand): ReplyEffect[HeroEvent, HeroState] = cmd match {
    case CreateHero(name, replyTo) =>
      if (valid) Effect.reply(replyTo)(Rejected("The hero already exists."))
      else Effect.persist(HeroCreated(name.toLowerCase)).thenReply(replyTo) { _ => Accepted }
    case ChangeHero(newName, replyTo) =>
      if (!valid) Effect.reply(replyTo)(Rejected("The hero is in invalid state."))
      else Effect.persist(HeroChanged(newName.toLowerCase, name)).thenReply(replyTo) { _ => Accepted }
    case DeleteHero(replyTo) =>
      if (!valid) Effect.reply(replyTo)(Rejected("The hero has already been deleted."))
      else Effect.persist(HeroDeleted).thenReply(replyTo) { _ => Accepted }
    case FetchHero(replyTo) if valid =>
      Effect.reply(replyTo)(this)
    case _ =>
      Effect.noReply
  }

  def applyEvent(event: HeroEvent): HeroState = event match {
    case HeroCreated(newName) => HeroState(newName, valid = true)
    case HeroChanged(newName, _) => HeroState(newName, valid)
    case HeroDeleted => HeroState(name, valid = false)
    case _ => this
  }
}

object HeroState {
  /**
   * The initial state. This is used if no snapshot state is found.
   */
  def initial = HeroState("", valid = false)

  /**
   * The EventSourcedBehavior instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
   * When sharding actors and distributing them across the cluster, each aggregate is
   * namespaced under a typekey that specifies a name and also the type of the commands
   * that sharded actor can receive.
   */
  val typeKey = EntityTypeKey[HeroCommand]("HeroAggregate")

  /**
   * Persisted entities get snapshotted every configured number of events. This
   * means the state gets stored to the database, so that when the aggregate gets
   * loaded, you don't need to replay all the events, just the ones since the
   * snapshot. Hence, a JSON format needs to be declared so that it can be
   * serialized and deserialized when storing to and from the database.
   */
  implicit val format: Format[HeroState] = Json.format
}
