package com.chariotsolutions.tohlagom.impl

import play.api.libs.json.{Format, Json}

sealed trait Confirmation

object Confirmation {
  implicit val format: Format[Confirmation] = Json.format
}

case object Accepted extends Confirmation {
  implicit val format: Format[Accepted.type] = Json.format
}

case class Rejected(reason: String) extends Confirmation

object Rejected {
  implicit val format: Format[Rejected] = Json.format
}
