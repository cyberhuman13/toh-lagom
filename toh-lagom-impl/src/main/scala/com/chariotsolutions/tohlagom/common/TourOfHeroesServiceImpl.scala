package com.chariotsolutions.tohlagom.common

import akka.{Done, NotUsed}
import akka.stream.Materializer
import akka.persistence.query.PersistenceQuery
import scala.concurrent.{ExecutionContext, Future}
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import com.lightbend.lagom.scaladsl.persistence.{ReadSide, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.chariotsolutions.tohlagom.impl._
import com.chariotsolutions.tohlagom.api._

abstract class TourOfHeroesServiceImpl(sharding: ClusterSharding)(readSide: ReadSide)
                                      (implicit ec: ExecutionContext, materializer: Materializer) extends TourOfHeroesService {
  import HeroEventProcessor._
  readSide.register[HeroEvent](eventProcessor)
  protected def eventProcessor: ReadSideProcessor[HeroEvent]
  protected def performRead(query: String): Future[Seq[Hero]]

  // Looks up the entity for the given ID.
  private def entityRef(id: String): EntityRef[HeroCommand] =
    sharding.entityRefFor(HeroState.typeKey, id)

  def heroes(): ServiceCall[NotUsed, Seq[Hero]] = ServiceCall { _ =>
    performRead(s"SELECT * FROM $Table")
  }

  def search(name: String): ServiceCall[NotUsed, Seq[Hero]] = ServiceCall { _ =>
    performRead(s"SELECT * FROM $Table WHERE $NameColumn LIKE '${name.toLowerCase}%'")
  }

  def fetchHero(id: String): ServiceCall[NotUsed, Hero] = ServiceCall { _ =>
    val heroId = id2str(id.toInt)
    performRead(s"SELECT * FROM $Table WHERE $IdColumn = '$heroId'")
      .map(_.headOption.orNull)
  }

  def createHero(): ServiceCall[NewHero, Hero] = ServiceCall { hero =>
    val heroId = newId
    entityRef(heroId).ask[Confirmation](replyTo => CreateHero(hero.name, replyTo)).map {
      case Accepted => Hero(heroId, hero.name.toTitleCase)
      case _ => throw BadRequest(s"Failed to create a hero with name ${hero.name}.")
    }
  }

  def changeHero(): ServiceCall[Hero, Done] = ServiceCall { hero =>
    val heroId = id2str(hero.id.toInt)
    entityRef(heroId).ask[Confirmation](replyTo => ChangeHero(hero.name, replyTo)).map {
      case Accepted => Done
      case _ => throw BadRequest(s"Failed to change a hero with id $heroId.")
    }
  }

  def deleteHero(id: String): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    val heroId = id2str(id.toInt)
    entityRef(heroId).ask[Confirmation](replyTo => DeleteHero(replyTo)).map {
      case Accepted => Done
      case _ => throw BadRequest(s"Failed to delete a hero with id $heroId.")
    }
  }

  // Because we don't change the front end, this method will not be called.
  // But it is here as an illustration for how to request an aggregate's history.
  private val readJournal = PersistenceQuery(materializer.system)
    .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  def history(id: String, from: Long, to: Long): ServiceCall[NotUsed, List[HeroEvent]] = ServiceCall { _ =>
    val heroId = id2str(id.toInt)
    val source = readJournal.eventsByPersistenceId(heroId, from, to)
    source.runFold(List.empty[HeroEvent]) { (acc, envelope) =>
      envelope.event match {
        case event: HeroEvent => event :: acc
        case _ => acc
      }
    }
  }
}
