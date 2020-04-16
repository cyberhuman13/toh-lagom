package com.chariotsolutions.tohlagom

import akka.util.Timeout
import scala.util.Random
import scala.concurrent.duration._

package object impl {
  implicit val timeout = Timeout(5 seconds)
  def id2str(id: Int): String = f"$id%010d"
  def newId: String = id2str(Random.nextInt(Int.MaxValue))

  implicit class StringExt(str: String) {
    def toTitleCase = if (null == str) null else
      str.split(" ").map(_.toLowerCase.capitalize).mkString(" ")
  }
}
