package com.chariotsolutions.tohlagom.cassandra

import akka.Done
import scala.concurrent.{ExecutionContext, Future, Promise}
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.datastax.driver.core.PreparedStatement
import com.chariotsolutions.tohlagom.common
import com.chariotsolutions.tohlagom.impl._

class HeroEventProcessor(readSide: CassandraReadSide, session: CassandraSession)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[HeroEvent] with common.HeroEventProcessor {
  import common.HeroEventProcessor._

  // The promises are initialized in builder.setPrepare below.
  // This is safe to prepare the statements this way, because
  // Cassandra driver's PreparedStatement class is threadsafe.
  private val promiseInsert = Promise[PreparedStatement]
  private val promiseUpdate = Promise[PreparedStatement]
  private val promiseDelete = Promise[PreparedStatement]

  private def stmtInsert: Future[PreparedStatement] = promiseInsert.future
  private def stmtUpdate: Future[PreparedStatement] = promiseUpdate.future
  private def stmtDelete: Future[PreparedStatement] = promiseDelete.future

  def buildHandler(): ReadSideProcessor.ReadSideHandler[HeroEvent] = {
    val builder = readSide.builder[HeroEvent](EventProcessorId)

    // The global prepare callback is called at least once on cluster startup.
    builder.setGlobalPrepare(() =>
      for {
        _ <- session.executeCreateTable(
          s"""CREATE TABLE IF NOT EXISTS $Table (
             |  $IdColumn TEXT,
             |  $NameColumn TEXT,
             |  PRIMARY KEY ($IdColumn)
             |)""".stripMargin)
        _ <- session.executeWrite(
          s"""CREATE CUSTOM INDEX IF NOT EXISTS fn_prefix
             |  ON $Table ($NameColumn)
             |  USING 'org.apache.cassandra.index.sasi.SASIIndex'""".stripMargin)
      } yield Done
    )

    // The prepare callback is called once per shard.
    // We use it to construct reusable PreparedStatements.
    builder.setPrepare { _ =>
      val insertStmt = session.prepare(s"INSERT INTO $Table ($IdColumn, $NameColumn) VALUES (?, ?)")
      val updateStmt = session.prepare(s"UPDATE $Table SET $NameColumn = ? WHERE $IdColumn = ?")
      val deleteStmt = session.prepare(s"DELETE FROM $Table WHERE $IdColumn = ?")

      promiseInsert.completeWith(insertStmt)
      promiseUpdate.completeWith(updateStmt)
      promiseDelete.completeWith(deleteStmt)

      for {
        _ <- insertStmt
        _ <- updateStmt
        _ <- deleteStmt
      } yield Done
    }

    builder.setEventHandler[HeroCreated] { element =>
      stmtInsert.map(_.bind).map { stmt =>
        stmt.setString(IdColumn, element.entityId)
        stmt.setString(NameColumn, element.event.name)
        List(stmt)
      }
    }

    builder.setEventHandler[HeroChanged] { element =>
      stmtUpdate.map(_.bind).map { stmt =>
        stmt.setString(IdColumn, element.entityId)
        stmt.setString(NameColumn, element.event.newName)
        List(stmt)
      }
    }

    builder.setEventHandler[HeroDeleted.type] { element =>
      stmtDelete.map(_.bind).map { stmt =>
        stmt.setString(IdColumn, element.entityId)
        List(stmt)
      }
    }

    builder.build
  }
}
