package com.chariotsolutions.tohlagom.mixed

import com.lightbend.lagom.scaladsl.persistence.jdbc.{JdbcReadSide, JdbcSession}
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.chariotsolutions.tohlagom.common
import com.chariotsolutions.tohlagom.impl._

class HeroEventProcessor(readSide: JdbcReadSide) extends ReadSideProcessor[HeroEvent] with common.HeroEventProcessor {
  import common.HeroEventProcessor._

  def buildHandler(): ReadSideProcessor.ReadSideHandler[HeroEvent] = {
    val builder = readSide.builder[HeroEvent](EventProcessorId)

    builder.setGlobalPrepare { connection =>
      JdbcSession.tryWith(connection.prepareStatement(
        s"""CREATE TABLE IF NOT EXISTS $Table (
           |  $IdColumn VARCHAR(10),
           |  $NameColumn VARCHAR(256),
           |  PRIMARY KEY ($IdColumn)
           |)""".stripMargin))(_.execute())
      JdbcSession.tryWith(connection.prepareStatement(
        "CREATE EXTENSION IF NOT EXISTS pg_trgm"))(_.execute())
      JdbcSession.tryWith(connection.prepareStatement(
        s"""CREATE INDEX IF NOT EXISTS fn_prefix
           |  ON $Table USING gin ($NameColumn gin_trgm_ops)
           |""".stripMargin))(_.execute())
    }

    builder.setEventHandler[HeroCreated] { case (connection, element) =>
      JdbcSession.tryWith(
        connection.prepareStatement(s"INSERT INTO $Table ($IdColumn, $NameColumn) VALUES (?, ?)")
      ) { statement =>
        statement.setString(1, element.entityId)
        statement.setString(2, element.event.name)
        statement.execute()
      }
    }

    builder.setEventHandler[HeroChanged] { case (connection, element) =>
      JdbcSession.tryWith(
        connection.prepareStatement(s"UPDATE $Table SET $NameColumn = ? WHERE $IdColumn = ?")
      ) { statement =>
        statement.setString(1, element.event.newName)
        statement.setString(2, element.entityId)
        statement.execute()
      }
    }

    builder.setEventHandler[HeroDeleted.type] { case (connection, element) =>
      JdbcSession.tryWith(
        connection.prepareStatement(s"DELETE FROM $Table WHERE $IdColumn = ?")
      ) { statement =>
        statement.setString(1, element.entityId)
        statement.execute()
      }
    }

    builder.build
  }
}
