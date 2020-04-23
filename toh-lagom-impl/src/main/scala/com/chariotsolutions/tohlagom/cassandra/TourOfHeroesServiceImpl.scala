package com.chariotsolutions.tohlagom.cassandra

import akka.stream.Materializer
import scala.concurrent.{ExecutionContext, Future}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import com.chariotsolutions.tohlagom.common
import com.chariotsolutions.tohlagom.impl._
import com.chariotsolutions.tohlagom.api._
import common.HeroEventProcessor._

class TourOfHeroesServiceImpl(sharding: ClusterSharding)
                             (readSide: ReadSide, cassandraReadSide: CassandraReadSide, session: CassandraSession)
                             (implicit ec: ExecutionContext, materializer: Materializer)
  extends common.TourOfHeroesServiceImpl(sharding)(readSide) {
  protected lazy val eventProcessor = new HeroEventProcessor(cassandraReadSide, session)

  protected def performRead(query: String): Future[Seq[Hero]] =
    session.selectAll(query).map(_.map(row => Hero(
      row.getString(IdColumn),
      row.getString(NameColumn).toTitleCase
    )))
}
