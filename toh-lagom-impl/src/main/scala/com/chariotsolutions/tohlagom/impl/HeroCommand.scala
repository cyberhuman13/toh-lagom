package com.chariotsolutions.tohlagom.impl

import akka.actor.typed.ActorRef

/**
 * This interface defines all the commands that HeroAggregate supports.
 * We will serialize them using Akka's Jackson support that is able to deal with the replyTo field.
 * (see application.conf)
 */
sealed trait HeroCommand

/**
 * A command to create a hero.
 * It has a reply type of [[Confirmation]], which is sent back to the caller
 * when all the events emitted by this command are successfully persisted.
 */
case class CreateHero(name: String, replyTo: ActorRef[Confirmation]) extends HeroCommand

/**
 * A command to change a hero.
 * It has a reply type of [[Confirmation]], which is sent back to the caller
 * when all the events emitted by this command are successfully persisted.
 */
case class ChangeHero(newName: String, replyTo: ActorRef[Confirmation]) extends HeroCommand

/**
 * A command to delete a hero.
 * It has a reply type of [[Confirmation]], which is sent back to the caller
 * when all the events emitted by this command are successfully persisted.
 */
case class DeleteHero(replyTo: ActorRef[Confirmation]) extends HeroCommand

/**
 * A command to fetch a hero (for unit tests only).
 * It has a reply type of [[HeroState]], which is sent back to the caller
 * when all the events emitted by this command are successfully persisted.
 */
case class FetchHero(replyTo: ActorRef[HeroState]) extends HeroCommand
