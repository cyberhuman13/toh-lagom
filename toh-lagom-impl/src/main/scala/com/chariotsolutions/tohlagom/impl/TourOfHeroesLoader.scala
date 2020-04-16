package com.chariotsolutions.tohlagom.impl

import com.softwaremill.macwire.wire
import play.api.libs.ws.ahc.AhcWSComponents
import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.chariotsolutions.tohlagom.api._

class TourOfHeroesLoader extends LagomApplicationLoader {
  override def describeService = Some(readDescriptor[TourOfHeroesService])

  def load(context: LagomApplicationContext): LagomApplication =
    new TourOfHeroesApplication(context) {
      def serviceLocator: ServiceLocator = ServiceLocator.NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new TourOfHeroesApplication(context) with LagomDevModeComponents
}

abstract class TourOfHeroesApplication(context: LagomApplicationContext)
  extends LagomApplication(context) with CassandraPersistenceComponents with AhcWSComponents {
  lazy val lagomServer = serverFor[TourOfHeroesService](wire[TourOfHeroesServiceImpl])

  // Initialize the sharding of the Aggregate.
  // The following starts the aggregate Behavior under a given sharding entity typeKey.
  clusterSharding.init(Entity(HeroState.typeKey)(HeroBehavior.create))

  /**
   * Akka serialization, used by both persistence and remoting, needs to have
   * serializers registered for every type serialized or deserialized. While it's
   * possible to use any serializer you want for Akka messages, out of the box
   * Lagom provides support for JSON, via this registry abstraction.
   *
   * The serializers are registered here.
   */
  lazy val jsonSerializerRegistry = new JsonSerializerRegistry {
    def serializers: List[JsonSerializer[_]] = List(
      // state and events can use play-json, but commands should use
      // Jackson because of ActorRef[T] (see application.conf).
      JsonSerializer[HeroEvent],
      JsonSerializer[HeroCreated],
      JsonSerializer[HeroChanged],
      JsonSerializer[HeroDeleted.type],
      JsonSerializer[HeroState],
      // the replies use play-json as well
      JsonSerializer[Hero],
      JsonSerializer[NewHero],
      JsonSerializer[Confirmation],
      JsonSerializer[Accepted.type],
      JsonSerializer[Rejected]
    )
  }
}
