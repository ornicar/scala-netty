lazy val akkaVersion = "2.6.0"

lazy val root = (project in file("."))
.enablePlugins(JavaAppPackaging)
.settings(
  scalaVersion := "2.13.1",
  version := "1",
  name := "scala-netty",
  scalacOptions ++= Seq(
    "-language:implicitConversions",
    "-feature",
    "-deprecation",
    "-Xfatal-warnings"
    ),
  libraryDependencies ++= Seq(
    "io.netty" % "netty-all" % "4.1.43.Final",
    "io.netty" % "netty-transport-native-epoll" % "4.1.43.Final" classifier "linux-x86_64",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
  )
)
