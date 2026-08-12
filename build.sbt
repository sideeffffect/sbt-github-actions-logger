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

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
)
