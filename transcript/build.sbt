name := "realtime-transcript"

version := "0.1.0"

scalaVersion := "3.5.2"

javacOptions ++= Seq("-source", "25", "-target", "25")

libraryDependencies ++= Seq(
  // JSON parsing for results
  "com.google.code.gson" % "gson" % "2.10.1",
  
  // Audio processing (built-in Java Sound API)
  
  // Logging
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
)

// Add Vosk native library as unmanaged dependency
unmanagedJars in Compile += file("lib/vosk-0.3.45.jar")

// Ensure Java 25 compatibility
initialize := {
  val required = "25"
  val current = sys.props("java.specification.version")
  assert(current == required, s"Java $required required, but found Java $current")
}
