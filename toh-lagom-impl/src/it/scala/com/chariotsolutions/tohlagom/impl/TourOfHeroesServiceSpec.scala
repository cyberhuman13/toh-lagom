package com.chariotsolutions.tohlagom.impl

import akka.Done
import scala.concurrent.duration._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.matchers.should.Matchers
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.chariotsolutions.tohlagom.cassandra.TourOfHeroesApplication
import com.chariotsolutions.tohlagom.api._

@RequiresCassandra
class TourOfHeroesServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {
  private val server: ServiceTest.TestServer[_ <: TourOfHeroesApplication] =
    ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra) { ctx =>
      new TourOfHeroesApplication(ctx) with LocalServiceLocator
    }

  val client = server.serviceClient.implement[TourOfHeroesService]
  override protected def afterAll() = server.stop()
  val pause = 30 seconds

  "toh-lagom service" should {
    val name1 = "aLiCe"
    val name2 = "bOrIs"
    val name3 = "aNnA"
    val newName = "cOrInNe"
    val prefix = "A"

    "create heroes" in {
      for {
        hero1 <- client.createHero().invoke(NewHero(name1))
        hero2 <- client.createHero().invoke(NewHero(name2))
        hero3 <- client.createHero().invoke(NewHero(name3))
      } yield {
        hero1.name should ===(name1.toTitleCase)
        hero2.name should ===(name2.toTitleCase)
        hero3.name should ===(name3.toTitleCase)
      }
    }

    "fetch all heroes" in {
      Thread.sleep(pause.toMillis)
      client.heroes().invoke().map { heroes =>
        heroes.size should ===(3)
        val match1 = heroes.find(_.name == name1.toTitleCase)
        val match2 = heroes.find(_.name == name2.toTitleCase)
        val match3 = heroes.find(_.name == name3.toTitleCase)
        match1.isDefined should ===(true)
        match2.isDefined should ===(true)
        match3.isDefined should ===(true)
      }
    }

    "search heroes by prefix" in {
      client.search(prefix).invoke().map { heroes =>
        heroes.foreach(_.name.substring(0, prefix.length).toTitleCase should ===(prefix.toTitleCase))
        heroes.size should ===(2)
      }
    }

    "fetch an existing hero" in {
      for {
        heroes <- client.heroes().invoke()
        heroId = heroes.head.id
        heroName = heroes.head.name
        hero <- client.fetchHero(heroId).invoke()
      } yield {
        hero.id should ===(heroId)
        hero.name should ===(heroName.toTitleCase)
      }
    }

    "change a hero's name" in {
      for {
        heroes <- client.heroes().invoke()
        heroId = heroes.head.id
        answer <- client.changeHero().invoke(Hero(heroId, newName))
        _ = Thread.sleep(pause.toMillis)
        hero <- client.fetchHero(heroId).invoke()
      } yield {
        answer should ===(Done)
        hero.id should ===(heroId)
        hero.name should ===(newName.toTitleCase)
      }
    }

    "delete a hero" in {
      for {
        heroes <- client.heroes().invoke()
        heroId = heroes.head.id
        answer <- client.deleteHero(heroId).invoke()
        _ = Thread.sleep(pause.toMillis)
        remaining <- client.heroes().invoke()
      } yield {
        answer should ===(Done)
        remaining.size should ===(2)
        remaining.map(_.id).contains(heroId) should ===(false)
      }
    }
  }
}
