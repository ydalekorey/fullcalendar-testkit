package controllers

import javax.inject._

import com.github.nscala_time.time.Imports._
import models.Event
import play.api._
import play.api.libs.json._
import play.api.mvc._

/**
  * Created by yuriy on 10.04.16.
  */
@Singleton
class CalendarController @Inject() extends Controller {
  def calendar = Action {
    Ok(views.html.calendar())
  }

  def events = Action {

    val events = List(
      Event(DateTime.parse("2016-01-10T02:30:00"), DateTime.parse("2016-01-10T03:15:00"), "First"),
      Event(DateTime.parse("2016-01-12T14:15:00"), DateTime.parse("2016-01-12T17:30:00"), "Second"),
      Event(DateTime.parse("2016-01-12T17:30:00"), DateTime.parse("2016-01-13T17:45:00"), "Third"),
      Event(DateTime.parse("2016-01-12T18:45:00"), DateTime.parse("2016-01-12T19:30:00"), "Fourth"),
      Event(DateTime.parse("2016-01-15T17:30:00"), DateTime.parse("2016-01-15T17:30:00"), "Fifths"),
      Event(DateTime.parse("2016-01-15T18:45:00"), DateTime.parse("2016-01-15T19:00:00"), "Sixths")
    )

    Ok(Json.toJson(events))
  }

}

