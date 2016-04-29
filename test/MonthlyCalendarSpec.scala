import org.scalatestplus.play.{FirefoxFactory, OneBrowserPerTest, OneServerPerTest, PlaySpec}
import org.openqa.selenium.{By, WebDriver, WebElement}
import org.scalatest.selenium.Page
import play.api.test.Helpers.testServerPort
import com.github.nscala_time.time.Imports._
import models.Event
import org.openqa.selenium.interactions.Actions
import org.scalatest.TestData
import play.api.inject.guice.GuiceApplicationBuilder

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import play.api.libs.json._
import play.api.mvc._
import play.api.routing._
import play.api.routing.sird._

/**
  * Created by yuriy on 10.04.16.
  */

class MonthlyCalendarSpec extends PlaySpec with OneServerPerTest with OneBrowserPerTest with FullCalendarSpec with FirefoxFactory {

  override def newAppForTest(testData: TestData) =
    new GuiceApplicationBuilder().additionalRouter(Router.from {
      case GET(p"/events") => Action {
        Results.Ok(
          Json.toJson(
            List(
              Event(DateTime.parse("2016-01-10T02:35:00"), DateTime.parse("2016-01-10T03:35:00"), "First"),
              Event(DateTime.parse("2016-01-12T14:30:00"), DateTime.parse("2016-01-12T17:30:00"), "Second"),
              Event(DateTime.parse("2016-01-12T17:30:00"), DateTime.parse("2016-01-13T17:30:00"), "Third"),
              Event(DateTime.parse("2016-01-12T18:35:00"), DateTime.parse("2016-01-12T19:30:00"), "Fourth"),
              Event(DateTime.parse("2016-01-15T17:30:00"), DateTime.parse("2016-01-15T17:30:00"), "Fifths"),
              Event(DateTime.parse("2016-01-15T18:35:00"), DateTime.parse("2016-01-15T19:30:00"), "Sixths")
            )
          )
        )
      }
    }).build()

  "Events" must {
    "be parsed" in {

      go to CalendarPage

      val events = findCalendar("calendar").getAllEvents

      events must have size 6

      events must contain key EventData(DateTime.parse("2016-01-10T02:35:00"), 1, "First")
      events must contain key EventData(DateTime.parse("2016-01-12T14:30:00"), 1, "Second")
      events must contain key EventData(DateTime.parse("2016-01-12T17:30:00"), 2, "Third")
      events must contain key EventData(DateTime.parse("2016-01-12T18:35:00"), 1, "Fourth")
      events must contain key EventData(DateTime.parse("2016-01-15T17:30:00"), 1, "Fifths")
      events must contain key EventData(DateTime.parse("2016-01-15T18:35:00"), 1, "Sixths")

    }
  }

  "Event" must {
    "be moved" in {

      go to CalendarPage

      val calendar = findCalendar("calendar")

      calendar.moveEvent(EventData(DateTime.parse("2016-01-10T02:35:00"), 1, "First"), LocalDate.parse("2016-01-14"))

      calendar.containsEvent(EventData(DateTime.parse("2016-01-14T02:35:00"), 1, "First")) must be(true)
    }
  }
}

case class EventData(dateTime: DateTime, duration: Int, title: String)

case class CalendarEvent(eventData: EventData, element: WebElement)

object CalendarPage extends Page {
  lazy val port = testServerPort
  override val url: String = s"http://localhost:$port/calendar"
}

trait FullCalendarSpec {
  def findCalendar(calendarId: String)(implicit driver: WebDriver): Calendar = {

    val calendarElement = driver.findElement(By.id(calendarId))
    new Calendar(calendarElement)
  }
}

class Calendar(val calendarElement: WebElement) {

  private val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  private def parseDate(e: WebElement): LocalDate = {
    LocalDate.parse(e.getAttribute("data-date"), dateFormat)
  }

  private def parseTime(timeText: String): LocalTime = {
    val timeExp = """(\d{1,2}):(\d{1,2})(a|p)""".r
    val timeExp(hours, minutes, partOfDay) = timeText

    val hourOfDay = partOfDay match {
      case "a" => hours.toInt
      case "p" => hours.toInt + 12
    }
    val minuteOfHour = minutes.toInt

    new LocalTime(hourOfDay, minuteOfHour)

  }


  private def weekContainers: List[WebElement] = {
    calendarElement.findElements(By.xpath(".//div[@class='fc-row fc-week fc-widget-content']")).toList
  }

  private def dayCellsInWeekBackgroundTable(week: WebElement): List[(LocalDate, WebElement)] = {
    week.findElements(By.xpath(".//div[@class='fc-bg']/table/tbody/tr/td"))
      .map(td => (parseDate(td), td)).toList
  }

  private def datesInWeek(weekContainer: WebElement): List[LocalDate] = {
    weekContainer.findElements(By.xpath(".//div[@class='fc-content-skeleton']/table/thead/tr/td"))
      .map(parseDate).toList
  }

  def rowsWithEvents(weekContainer: WebElement): List[WebElement] = {
    weekContainer.findElements(By.xpath(".//div[@class='fc-content-skeleton']/table/tbody/tr")).toList
  }

  private def availableDatesForEventsInNextRow(row: WebElement, dates: List[LocalDate]) = {
    val cells = row.findElements(By.tagName("td")).toList

    @tailrec
    def loop(cells: List[WebElement], dates: List[LocalDate], remainingDates: List[LocalDate]): List[LocalDate] = {

      cells match {
        case Nil => remainingDates
        case head :: tail if Option(head.getAttribute("rowspan")).isDefined => loop(tail, dates.tail, remainingDates)
        case head :: tail =>
          val allocatedCells = Option(head.getAttribute("colspan")).getOrElse("1").toInt
          loop(tail, dates.drop(allocatedCells), remainingDates ++ dates.take(allocatedCells))
      }
    }
    loop(cells, dates, List.empty)
  }

  private def parseEventsInRow(row: WebElement, dates: List[LocalDate]) = {
    val cells = row.findElements(By.tagName("td")).toList

    def parseEvent(td: WebElement, date: LocalDate): CalendarEvent = {
      val duration = Option(td.getAttribute("colspan")).getOrElse("1").toInt
      val anchor = td.findElement(By.tagName("a"))
      val title = anchor.findElement(By.xpath("./div[@class='fc-content']/span[@class='fc-title']")).getText
      val time = parseTime(anchor.findElement(By.xpath("./div[@class='fc-content']/span[@class='fc-time']")).getText)

      CalendarEvent(EventData(dateTime = date.toDateTime(time), duration = duration, title = title), element = anchor)
    }

    @tailrec
    def loop(cells: List[WebElement], dates: List[LocalDate], events: List[CalendarEvent]): List[CalendarEvent] = {

      cells match {
        case Nil => events.reverse
        case head :: tail if Option(head.getAttribute("class")).contains("fc-event-container") =>
          val event = parseEvent(head, dates.head)
          loop(tail, dates.drop(event.eventData.duration), event :: events)
        case head :: tail =>
          loop(tail, dates.tail, events)
      }
    }
    loop(cells, dates, List.empty)
  }

  private def parseEventsInWeekContainer(weekContainer: WebElement): List[CalendarEvent] = {

    val dates = datesInWeek(weekContainer)
    val rows = rowsWithEvents(weekContainer)

    @tailrec
    def loop(rows: List[WebElement], availableDates: List[LocalDate], events: List[CalendarEvent]): List[CalendarEvent] = {
      rows match {
        case Nil => events
        case head :: tail =>
          val datesForNextRow = availableDatesForEventsInNextRow(head, availableDates)
          val parsedEvents = parseEventsInRow(head, availableDates)
          loop(tail, datesForNextRow, events ++ parsedEvents)
      }
    }
    loop(rows, dates, Nil)
  }

  def getAllEvents(implicit driver: WebDriver): Map[EventData, WebElement] = {
    weekContainers.flatMap(parseEventsInWeekContainer).map(ce => (ce.eventData, ce.element)).toMap
  }

  private def getAllCells(implicit driver: WebDriver): Map[LocalDate, WebElement] = {
    weekContainers.flatMap(dayCellsInWeekBackgroundTable).toMap
  }

  def getEventElement(eventData: EventData)(implicit driver: WebDriver): WebElement = {
    getAllEvents(driver)(eventData)
  }

  def containsEvent(eventData: EventData)(implicit driver: WebDriver) : Boolean = {
    getAllEvents(driver) contains eventData
  }

  private def getCell(date: LocalDate)(implicit driver: WebDriver): WebElement = {
    getAllCells(driver)(date)
  }

  def moveEvent(eventData: EventData, toDate: LocalDate)(implicit driver: WebDriver) = {
    val event = getEventElement(eventData)
    val cell = getCell(toDate)

    val moveActions = new Actions(driver)
    moveActions.clickAndHold(event)
    moveActions.moveToElement(cell)
    moveActions.release()
    moveActions.build().perform()
  }

}
