package com.chariotsolutions.tohlagom.cassandra

import com.softwaremill.macwire.wire
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.chariotsolutions.tohlagom.common
import com.chariotsolutions.tohlagom.api._

class TourOfHeroesLoader extends common.TourOfHeroesLoader {
  // The production mode load.
  def load(context: LagomApplicationContext): LagomApplication =
    new TourOfHeroesApplication(context) with AkkaDiscoveryComponents

  // The development mode load.
  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new TourOfHeroesApplication(context) with LagomDevModeComponents
}

abstract class TourOfHeroesApplication(context: LagomApplicationContext)
  extends LagomApplication(context) with common.TourOfHeroesApplication with CassandraPersistenceComponents {
  // The wire[T] macro injects all nevessary constructor arguments.
  lazy val lagomServer = serverFor[TourOfHeroesService](wire[TourOfHeroesServiceImpl])
}
