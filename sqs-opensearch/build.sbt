name := "sqs-opensearch"
version := "0.1.0"
scalaVersion := "3.3.1"

javacOptions ++= Seq("-source", "25", "-target", "25")

Test / fork := true
Test / envVars := {
  val tmpDir = System.getProperty("java.io.tmpdir")
  val podmanSocket = s"$tmpDir/podman/podman-machine-default-api.sock"
  val baseEnv = if (new java.io.File(podmanSocket).exists()) {
    Map("DOCKER_HOST" -> s"unix://$podmanSocket")
  } else {
    Map.empty[String, String]
  }
  baseEnv ++ Map("TESTCONTAINERS_RYUK_DISABLED" -> "true")
}

libraryDependencies ++= Seq(
  "software.amazon.awssdk" % "sqs" % "2.20.26",
  "org.opensearch.client" % "opensearch-rest-client" % "2.11.0",
  "org.opensearch.client" % "opensearch-java" % "2.8.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.15.2",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.2",
  "org.slf4j" % "slf4j-simple" % "2.0.9",

  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "org.testcontainers" % "testcontainers" % "1.19.3" % Test,
  "org.testcontainers" % "localstack" % "1.19.3" % Test,
  "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.41.0" % Test
)
