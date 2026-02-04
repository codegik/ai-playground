name := "realtime-transcript"

version := "0.1.0"

scalaVersion := "3.5.2"

javacOptions ++= Seq("-source", "25", "-target", "25")

libraryDependencies ++= Seq(
  // Vosk for offline speech recognition (alternative to Whisper)
  "net.java.dev.jna" % "jna" % "5.14.0",

  // JSON parsing for Vosk results
  "io.circe" %% "circe-core" % "0.14.9",
  "io.circe" %% "circe-parser" % "0.14.9",

  // Audio processing (built-in Java Sound API)

  // Logging
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
)

// Ensure Java 25 compatibility
initialize := {
  val required = "25"
  val current = sys.props("java.specification.version")
  assert(current == required, s"Java $required required, but found Java $current")
}
