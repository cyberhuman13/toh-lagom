package com.chariotsolutions.tohlagom.api

import akka.{Done, NotUsed}
import play.api.libs.json.{Format, Json}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}

/**
 * The classes for existing and to-be-created heroes.
 */
case class Hero(id: String, name: String)
case class NewHero(name: String)

/**
 * Formats for converting heroes to and from JSON.
 * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
 */
object Hero {
  implicit val format: Format[Hero] = Json.format
}

object NewHero {
  implicit val format: Format[NewHero] = Json.format
}

trait TourOfHeroesService extends Service {
  /**
   * GET http://localhost:9000/api/heroes
   */
  def heroes(): ServiceCall[NotUsed, Seq[Hero]]

  /**
   * GET http://localhost:9000/api/heroes?name=[name]
   */
  def search(name: String): ServiceCall[NotUsed, Seq[Hero]]

  /**
   * GET http://localhost:9000/api/heroes/[id]
   */
  def fetchHero(id: String): ServiceCall[NotUsed, Hero]

  /**
   * POST http://localhost:9000/api/heroes
   */
  def createHero(): ServiceCall[NewHero, Hero]

  /**
   * PUT http://localhost:9000/api/heroes
   */
  def changeHero(): ServiceCall[Hero, Done]

  /**
   * DELETE http://localhost:9000/api/heroes/[id]
   */
  def deleteHero(heroId: String): ServiceCall[NotUsed, Done]

  final def descriptor: Descriptor = {
    import Service._

    named("toh-lagom").withCalls(
      restCall(Method.GET, "/api/heroes", heroes _),
      restCall(Method.PUT, "/api/heroes", changeHero _),
      restCall(Method.POST, "/api/heroes", createHero _),
      restCall(Method.GET, "/api/heroes/?name", search _),
      restCall(Method.GET, "/api/heroes/:heroId", fetchHero _),
      restCall(Method.DELETE, "/api/heroes/:heroId", deleteHero _)
    ).withAutoAcl(true)
  }
}
