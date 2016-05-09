package pages

import org.scalatest.selenium.Page
import play.api.test.Helpers.testServerPort

object CalendarPage extends Page {
  lazy val port = testServerPort
  override val url: String = s"http://localhost:$port/calendar"
}