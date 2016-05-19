import org.openqa.selenium.{By, Point, WebDriver, WebElement}
import org.scalatestplus.play.{FirefoxFactory, OneBrowserPerTest, OneServerPerTest, PlaySpec}
import com.github.nscala_time.time.Imports._
import models.Event
import org.openqa.selenium.interactions.Actions
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
              Event(DateTime.parse("2016-01-15T17:30:00"), DateTime.parse("2016-01-15T18:30:00"), "Fifths"),
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

  "Events" must {
    "be parsed" in {

      go to CalendarPage

      clickOn(className("fc-agendaWeek-button"))

      val events = findCalendar("calendar").getEvents

      events.contains(WeekEventData(DateTime.parse("2016-01-10T02:35:00"), Duration.standardHours(1), "First"))
      events.contains(WeekEventData(DateTime.parse("2016-01-10T02:35:00"), Duration.standardHours(3), "Second"))
      events.contains(WeekEventData(DateTime.parse("2016-01-10T02:35:00"), Duration.standardMinutes(55), "Fourth"))
      events.contains(WeekEventData(DateTime.parse("2016-01-10T02:35:00"), Duration.standardHours(1), "Fifths"))
      events.contains(WeekEventData(DateTime.parse("2016-01-10T02:35:00"), Duration.standardMinutes(55), "Sixths"))
    }
  }

  "Events" must {
    "be able to move" in {

      go to CalendarPage

      clickOn(className("fc-agendaWeek-button"))

      val calendar = findCalendar("calendar")

      val oldEvent = WeekEventData(DateTime.parse("2016-01-10T02:35:00"), Duration.standardHours(1), "First")
      val movedEvent = oldEvent.copy(startTime = DateTime.parse("2016-01-10T04:45:00"))

      calendar.moveEvent(oldEvent, movedEvent.startTime)

      calendar.containsEvent(movedEvent)

    }
  }

  "Events" must {
    "be able to resize" in {

      go to CalendarPage

      clickOn(className("fc-agendaWeek-button"))

      val calendar = findCalendar("calendar")

      val oldEvent = WeekEventData(DateTime.parse("2016-01-10T02:35:00"), Duration.standardHours(1), "First")
      val movedEvent = oldEvent.copy(duration = Duration.standardHours(2))

      calendar.resizeEvent(oldEvent, movedEvent.duration)

      calendar.containsEvent(movedEvent)

    }
  }

}

trait WeeklyCalendarSpecInternal {
  def findCalendar(calendarId: String)(implicit driver: WebDriver): WeeklyCalendar = new WeeklyCalendar(calendarId)
}

class WeeklyCalendar(calendarId: String)(implicit val driver: WebDriver) {

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

  def dateColumnsElements: List[WebElement] = {
    calendarElement.findElements(By.xpath(".//div[@class='fc-bg']/table/tbody/tr/td[@data-date]")).toList
  }

  def timeIntervalInMinutes(timeSlotsCount: Int): Int = {
    val minutesInDay = 1440
    minutesInDay / timeSlotsCount
  }

  def timeRows: Map[LocalTime, WebElement] = {
    val timeRows = calendarElement.findElements(By.xpath(".//div[@class='fc-slats']/table/tbody/tr"))

    val timeInterval = timeIntervalInMinutes(timeRows.size)

    (0 until timeRows.size).view
      .map(n => LocalTime.Midnight.plusMinutes(n * timeInterval))
      .toList.zip(timeRows).toMap

  }

  def eventContainers: List[WebElement] = {
    calendarElement.findElements(By.xpath(".//div[@class='fc-content-skeleton']/table/tbody/tr/td/div[@class='fc-event-container']")).toList
  }

  def eventElements(eventContainer: WebElement): List[WebElement] = {
    eventContainer.findElements(By.tagName("a")).toList
  }

  def parseEvent(date: LocalDate, eventElement: WebElement): WeekEventData = {
    val timeFormat = DateTimeFormat.forPattern("hh:mm a")
    val timeExp = """\d{1,2}:\d{1,2} [AP]M"""
    val timeRangeRegExp = s"""($timeExp) - ($timeExp)""".r

    val content = eventElement.findElement(By.xpath("./div[@class='fc-content']"))
    val timeRangeRegExp(startTimeString, endTimeString) = content.findElement(By.xpath("./div[@class='fc-time']")).getAttribute("data-full")
    val title = content.findElement(By.xpath("./div[@class='fc-title']")).getText
    val startTime = date.toDateTime(LocalTime.parse(startTimeString, timeFormat))
    val endTime = date.toDateTime(LocalTime.parse(endTimeString, timeFormat))
    WeekEventData(startTime, new Duration(startTime, endTime), title)

  }

  def getEvents: Map[WeekEventData, WebElement] = {
    def getEvents(date: LocalDate, eventContainer: WebElement): List[(WeekEventData, WebElement)] = {
      eventElements(eventContainer).map(e => (parseEvent(date, e), e))
    }
    val dates = dateColumnsElements map parseDate

    dates.zip(eventContainers).flatMap({ case (date, eventContainer) => getEvents(date, eventContainer) }).toMap
  }

  def timeSlotLocation(date: DateTime): Point = {
    val timeSlots = timeRows
    val slotInterval = timeIntervalInMinutes(timeSlots.size)

    val minutesOfHour = date.toLocalTime.getMinuteOfHour
    val time = date.toLocalTime.withMinuteOfHour(minutesOfHour - minutesOfHour % slotInterval)

    val x = dateColumns.get(date.toLocalDate).get.getLocation.x
    val y = timeSlots.get(time).get.getLocation.y
    new Point(x, y)
  }

  def moveEvent(eventData: WeekEventData, toDate: DateTime): Unit = {
    val events = getEvents
    val element = events(eventData).findElement(By.className("fc-time"))
    val oldLocation = element.getLocation
    val newLocation = timeSlotLocation(toDate)

    val moveActions = new Actions(driver)
    moveActions.clickAndHold(element)
    moveActions.moveByOffset(newLocation.x - oldLocation.x, newLocation.y - oldLocation.y)
    moveActions.release()
    moveActions.build().perform()

  }

  def resizeEvent(eventData: WeekEventData, duration: Duration): Unit = {
    val events = getEvents
    val element = events(eventData).findElement(By.className("fc-resizer"))

    val oldLocation = element.getLocation
    val newLocation = timeSlotLocation(eventData.startTime.plus(duration))

    val moveActions = new Actions(driver)
    moveActions.clickAndHold(element)
    moveActions.moveByOffset(newLocation.x - oldLocation.x, newLocation.y - oldLocation.y)
    moveActions.release()
    moveActions.build().perform()

  }

  def containsEvent(eventData: WeekEventData) = getEvents.contains(eventData)

}

case class WeekEventData(startTime: DateTime, duration: Duration, title: String)