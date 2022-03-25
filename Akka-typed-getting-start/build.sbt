Test / parallelExecution := false

val akkaVersion = "2.6.18"

scalaVersion := "3.1.1"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked"
)

libraryDependencies ++= Seq(
 // "com.typesafe.akka" %% "akka-actor" % akkaVersion,
 // "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalameta" %% "munit" % "0.7.26" % Test,
  "org.scalatest" %% "scalatest" % "3.2.11" % Test,

  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,

  "ch.qos.logback" % "logback-classic" % "1.2.11" % Test,
  "ch.qos.logback" % "logback-classic" % "1.2.11" % Runtime
)
