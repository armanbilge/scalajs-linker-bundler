val sjsVersion = "1.11.0"

libraryDependencies += "org.scala-js" %% "scalajs-linker" % sjsVersion

Compile / unmanagedSourceDirectories +=
  baseDirectory.value.getParentFile / "core" / "src" / "main" / "scala"

addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.4.13")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.11.0")
