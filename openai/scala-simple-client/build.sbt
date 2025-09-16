val toolkitV = "0.1.28"
val toolkit = "org.typelevel" %% "toolkit" % toolkitV
val toolkitTest = "org.typelevel" %% "toolkit-test" % toolkitV

ThisBuild / scalaVersion := "3.3.4"
libraryDependencies += toolkit
libraryDependencies += (toolkitTest % Test)
