val scala212 = "2.12.20"
// Must match the Scala version sbt 2.0.6 itself is built with. It cannot be the Scala 3 LTS (3.3.x):
// sbt 2.0.6's artifacts carry TASTy 28.8, which an LTS compiler (TASTy 28.3) rejects as "produced by
// a more recent, forwards incompatible release". So the sbt-2 axis is floored at sbt's own version.
val scala3 = "3.8.4"

sbtPlugin := true

name := "sbt-github-actions-logger"

organization := "com.github.sideeffffect"

description := "sbt plugin that turns sbt's compiler and test output into GitHub Actions workflow commands (log groups and annotations)."

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

developers := List(
  Developer(
    "sideeffffect",
    "Ondra Pelech",
    "ondra.pelech@gmail.com",
    url("https://github.com/sideeffffect"),
  ),
)

homepage := Some(url("https://github.com/sideeffffect/sbt-github-actions-logger"))

versionScheme := Some("early-semver")

// Cross-build against sbt 1.x (Scala 2.12) and sbt 2.x (Scala 3).
// `sbt +compile` / `sbt +publishLocal` builds both axes.
scalaVersion := scala212
crossScalaVersions := Seq(scala212, scala3)
pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "2.12" => "1.11.7" // sbt 1.x
    case _      => "2.0.6" // sbt 2.x
  }
}

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
)

libraryDependencies += "org.scalameta" %% "munit" % "1.1.1" % Test
