import sbt._
import sbt.Keys._

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "led-scala",
    organization := "org.skyluc",
    libraryDependencies ++= Seq(
      "com.diozero" % "diozero-ws281x-java" % "0.11",
      "com.diozero" % "diozero-core" % "0.11",
      "com.pi4j" % "pi4j-core" % "1.2"
//      "com.github.mbelling" % "rpi-ws281x-java" % "2.0.0"
    ),
    Compile / run / fork := true
  )

lazy val commonSettings = Seq(
  scalaVersion := "2.12.8",
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-Xlog-reflective-calls",
    "-Xlint",
    "-Ywarn-unused",
    "-Ywarn-unused-import",
    "-deprecation",
    "-feature",
    "-language:_",
    "-unchecked"
  ),

  scalacOptions in (Compile, console) --= Seq("-Ywarn-unused", "-Ywarn-unused-import"),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
)
