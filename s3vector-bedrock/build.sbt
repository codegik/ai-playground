val scala3Version = "3.3.4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "s3vector-bedrock",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",
      "org.typelevel" %% "cats-core" % "2.9.0",
      "software.amazon.awssdk" % "s3" % "2.21.0",
      "software.amazon.awssdk" % "bedrockruntime" % "2.21.0",
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )
