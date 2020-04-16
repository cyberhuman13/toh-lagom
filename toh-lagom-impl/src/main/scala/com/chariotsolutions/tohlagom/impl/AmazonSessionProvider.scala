package com.chariotsolutions.tohlagom.impl

import akka.actor.ActorSystem
import java.net.InetSocketAddress
import scala.concurrent.{ExecutionContext, Future}
import akka.persistence.cassandra.ConfigSessionProvider
import com.typesafe.config.Config

class AmazonSessionProvider(system: ActorSystem, config: Config) extends ConfigSessionProvider(system, config) {
  override def lookupContactPoints(clusterId: String)(implicit ec: ExecutionContext) = {
    val region = config.getString("aws-region")
    val address = InetSocketAddress.createUnresolved(s"cassandra.$region.amazonaws.com", 9142)
    Future.successful(List(address))
  }
}
