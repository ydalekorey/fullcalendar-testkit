package models

import play.api.libs.json._
import com.github.nscala_time.time.Imports._
import play.api.libs.json.Json

case class Event(start: DateTime, end: DateTime, title: String)
object Event {
  implicit val dateTimeWrites = Writes.jodaDateWrites("YYYY-MM-dd'T'HH:mm:ss")
  implicit val eventWrites = Json.writes[Event]
}