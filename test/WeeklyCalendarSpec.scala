import org.openqa.selenium.{By, WebDriver, WebElement}
import org.scalatestplus.play.{FirefoxFactory, OneBrowserPerTest, OneServerPerTest, PlaySpec}
import com.github.nscala_time.time.Imports._
import models.Event
import org.scalatest.TestData
import pages.CalendarPage
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{Action, Results}
import play.api.routing.Router
import play.api.routing.sird._

import scala.collection.JavaConversions._

/**
  * Created by yuriy on 29.04.16.
  */
class WeeklyCalendarSpec extends PlaySpec with OneServerPerTest with OneBrowserPerTest with FirefoxFactory with WeeklyCalendarSpecInternal {

  override def newAppForTest(testData: TestData) =
    new GuiceApplicationBuilder().additionalRouter(Router.from {
      case GET(p"/events") => Action {
        Results.Ok(
          Json.toJson(
            List(
              Event(DateTime.parse("2016-01-10T02:35:00"), DateTime.parse("2016-01-10T03:35:00"), "First"),
              Event(DateTime.parse("2016-01-12T14:30:00"), DateTime.parse("2016-01-12T17:30:00"), "Second"),
              Event(DateTime.parse("2016-01-12T18:35:00"), DateTime.parse("2016-01-12T19:30:00"), "Fourth"),
              Event(DateTime.parse("2016-01-15T17:30:00"), DateTime.parse("2016-01-15T17:30:00"), "Fifths"),
              Event(DateTime.parse("2016-01-15T18:35:00"), DateTime.parse("2016-01-15T19:30:00"), "Sixths")
            )
          )
        )
      }
    }).build()

  "Dates" must {
    "be parsed" in {

      go to CalendarPage

      clickOn(className("fc-agendaWeek-button"))

      val weekDates = findCalendar("calendar").dateColumns.keySet

      weekDates must have size 7
      weekDates must contain(LocalDate.parse("2016-01-10"))
      weekDates must contain(LocalDate.parse("2016-01-11"))
      weekDates must contain(LocalDate.parse("2016-01-12"))
      weekDates must contain(LocalDate.parse("2016-01-13"))
      weekDates must contain(LocalDate.parse("2016-01-14"))
      weekDates must contain(LocalDate.parse("2016-01-15"))
      weekDates must contain(LocalDate.parse("2016-01-16"))
    }
  }

}

trait WeeklyCalendarSpecInternal {
  def findCalendar(calendarId: String)(implicit driver: WebDriver): WeeklyCalendar = new WeeklyCalendar(calendarId)
}

class WeeklyCalendar(calendarId: String)(implicit driver: WebDriver) {
  val calendarElement = driver.findElement(By.id(calendarId))

  private val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  private def parseDate(e: WebElement): LocalDate = {
    LocalDate.parse(e.getAttribute("data-date"), dateFormat)
  }

  def dateColumns: Map[LocalDate, WebElement] = {
    calendarElement
      .findElements(By.xpath(".//div[@class='fc-bg']/table/tbody/tr/td[@data-date]"))
      .map(td => (parseDate(td), td))
      .toMap
  }

  def timeRows: Map[LocalTime, WebElement] = {
    val timeRows = calendarElement.findElements(By.xpath(".//div[@class='fc-slats']/table/tbody/tr"))
    val minutesInDay = 1440
    val timeInterval = minutesInDay / timeRows.size()
    val

  }

}

