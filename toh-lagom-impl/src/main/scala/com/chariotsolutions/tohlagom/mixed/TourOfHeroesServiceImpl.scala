package com.chariotsolutions.tohlagom.mixed

import akka.stream.Materializer
import scala.collection.immutable.VectorBuilder
import scala.concurrent.{ExecutionContext, Future}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.lightbend.lagom.scaladsl.persistence.jdbc.{JdbcReadSide, JdbcSession}
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import com.chariotsolutions.tohlagom.common
import com.chariotsolutions.tohlagom.impl._
import com.chariotsolutions.tohlagom.api._
import common.HeroEventProcessor._

class TourOfHeroesServiceImpl(sharding: ClusterSharding)
                             (readSide: ReadSide, jdbcReadSide: JdbcReadSide, session: JdbcSession)
                             (implicit ec: ExecutionContext, materializer: Materializer)
  extends common.TourOfHeroesServiceImpl(sharding)(readSide) {
  protected lazy val eventProcessor = new HeroEventProcessor(jdbcReadSide)

  protected def performRead(query: String): Future[Seq[Hero]] =
    session.withConnection { connection =>
      JdbcSession.tryWith(connection.prepareStatement(query)) { stmt =>
        JdbcSession.tryWith(stmt.executeQuery()) { row =>
          val summaries = new VectorBuilder[Hero]
          while (row.next()) {
            summaries += Hero(
              row.getString(IdColumn),
              row.getString(NameColumn).toTitleCase
            )
          }
          summaries.result()
        }
      }
    }
}
