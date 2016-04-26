name := """fullcalendar-testkit"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.nscala-time" %% "nscala-time" % "2.6.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test,
  "org.seleniumhq.selenium" % "selenium-java" % "2.52.0" % Test
)

unmanagedResourceDirectories in Test <+= baseDirectory ( _ /"target/web/public/test" )

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.5.0",
  "org.webjars.bower" % "adminlte" % "2.3.3",
  "org.webjars" % "fullcalendar" % "2.4.0"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"


fork in run := false