name := "realtime-transcript"

version := "0.1.0"

scalaVersion := "3.5.2"

javacOptions ++= Seq("-source", "25", "-target", "25")

libraryDependencies ++= Seq(
  // Whisper JNI bindings for local inference
  "io.github.givimad" % "whisperjni" % "1.6.2",

  // Audio processing
  "javax.sound" % "javax.sound-midi" % "1.0" % "provided",

  // Language detection (optional, Whisper can detect language)
  "com.optimaize.languagedetector" % "language-detector" % "0.6",

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
