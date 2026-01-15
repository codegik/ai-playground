val scala3Version = "3.3.4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "s3vector",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "opensearch" % "2.21.0",
      "software.amazon.awssdk" % "opensearchserverless" % "2.21.0",
      "software.amazon.awssdk" % "auth" % "2.21.0",
      "software.amazon.awssdk" % "bedrockruntime" % "2.21.0",
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",
      "org.typelevel" %% "cats-core" % "2.9.0",
      "org.apache.httpcomponents.client5" % "httpclient5" % "5.2.1",
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )
