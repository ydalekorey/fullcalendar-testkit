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
      Event(DateTime.parse("2016-01-10T2:35:00"), DateTime.parse("2016-01-10T3:35:00"), "First")
    )

    Ok(Json.toJson(events))
  }

}

