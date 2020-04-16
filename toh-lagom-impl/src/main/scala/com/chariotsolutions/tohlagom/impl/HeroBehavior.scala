package com.chariotsolutions.tohlagom.impl

import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.{EventAdapter, EventSeq, PersistenceId}
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter

/**
 * This provides an event-sourced behavior. It has a state, [[HeroState]],
 * which stores the hero's current state.
 *
 * Event-sourced entities are interacted with by sending them commands.
 * Commands get translated to events, and it's the events that get persisted.
 * Each event will have an event handler registered for it, and an event handler
 * simply applies an event to the current state. This will be done when the event
 * is first created, and it will also be done when the aggregate is loaded from
 * the database - each event will be replayed to recreate the state of the aggregate.
 */
object HeroBehavior {
  /**
   * Given a sharding EntityContext, this function produces an Akka Behavior for the aggregate.
   */
  def create(entityContext: EntityContext[HeroCommand]): Behavior[HeroCommand] = {
    val persistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)

    create(persistenceId).eventAdapter(HeroEventAdapter).withTagger(
      // Using Akka Persistence Typed in Lagom requires tagging your events
      // in Lagom-compatible way so Lagom ReadSideProcessors and TopicProducers
      // can locate and follow the event streams.
      AkkaTaggerAdapter.fromLagom(entityContext, HeroEvent.Tag)
    )
  }

  /**
   * This method is extracted to write unit tests that are completely independent to Akka Cluster.
   */
  private[impl] def create(persistenceId: PersistenceId): EventSourcedBehavior[HeroCommand, HeroEvent, HeroState] =
    EventSourcedBehavior.withEnforcedReplies[HeroCommand, HeroEvent, HeroState](
      persistenceId = persistenceId,
      emptyState = HeroState.initial,
      commandHandler = (state, cmd) => state.applyCommand(cmd),
      eventHandler = (state, event) => state.applyEvent(event)
    )
}

object HeroEventAdapter extends EventAdapter[HeroEvent, HeroEvent] {
  def manifest(event: HeroEvent): String = " " // Override the empty string.
  def fromJournal(event: HeroEvent, manifest: String) = EventSeq.single(event)
  def toJournal(event: HeroEvent) = event
}
