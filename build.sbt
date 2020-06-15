scalaVersion := "2.13.2"

name := "oscar-util"
organization := "oscarlib"
version := "5.0.0"

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xdisable-assertions",
  "-language:implicitConversions",
  "-language:postfixOps"
)
