package com.chariotsolutions.tohlagom.impl

import java.util.UUID
import scala.concurrent.duration._
import akka.persistence.typed.PersistenceId
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

class HeroAggregateSpec extends ScalaTestWithActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with AnyWordSpecLike with Matchers {
  "Hero aggregate" should {
    "create a hero with a given name" in {
      val ref = spawn(HeroBehavior.create(PersistenceId("fake-type-hint", newId)))
      val confirmationProbe = createTestProbe[Confirmation]()
      val stateProbe = createTestProbe[HeroState]()
      val name = "aLiCe"

      ref ! CreateHero(name, confirmationProbe.ref)
      confirmationProbe.expectMessage(Accepted)

      ref ! FetchHero(stateProbe.ref)
      stateProbe.expectMessage(HeroState(name.toLowerCase, valid = true))
    }

    "change an existing hero's name" in {
      val ref = spawn(HeroBehavior.create(PersistenceId("fake-type-hint", newId)))
      val confirmationProbe = createTestProbe[Confirmation]()
      val stateProbe = createTestProbe[HeroState]()
      val oldName = "aLiCe"
      val newName = "bOrIs"

      ref ! CreateHero(oldName, confirmationProbe.ref)
      confirmationProbe.expectMessage(Accepted)

      ref ! ChangeHero(newName, confirmationProbe.ref)
      confirmationProbe.expectMessage(Accepted)

      ref ! FetchHero(stateProbe.ref)
      stateProbe.expectMessage(HeroState(newName.toLowerCase, valid = true))
    }

    "fail to change a non-initialized hero" in {
      val ref = spawn(HeroBehavior.create(PersistenceId("fake-type-hint", newId)))
      val confirmationProbe = createTestProbe[Confirmation]()
      val newName = "bOrIs"

      ref ! ChangeHero(newName, confirmationProbe.ref)
      confirmationProbe.expectMessage(Rejected("The hero is in invalid state."))
    }

    "delete an existing hero" in {
      val ref = spawn(HeroBehavior.create(PersistenceId("fake-type-hint", newId)))
      val confirmationProbe = createTestProbe[Confirmation]()
      val stateProbe = createTestProbe[HeroState]()
      val name = "aLiCe"

      ref ! CreateHero(name, confirmationProbe.ref)
      confirmationProbe.expectMessage(Accepted)

      ref ! DeleteHero(confirmationProbe.ref)
      confirmationProbe.expectMessage(Accepted)

      ref ! FetchHero(stateProbe.ref)
      stateProbe.expectNoMessage(timeout.duration)
    }

    "fail delete an already deleted hero" in {
      val ref = spawn(HeroBehavior.create(PersistenceId("fake-type-hint", newId)))
      val confirmationProbe = createTestProbe[Confirmation]()
      val name = "aLiCe"

      ref ! CreateHero(name, confirmationProbe.ref)
      confirmationProbe.expectMessage(Accepted)

      ref ! DeleteHero(confirmationProbe.ref)
      confirmationProbe.expectMessage(Accepted)

      ref ! DeleteHero(confirmationProbe.ref)
      confirmationProbe.expectMessage(Rejected("The hero has already been deleted."))
    }
  }
}
