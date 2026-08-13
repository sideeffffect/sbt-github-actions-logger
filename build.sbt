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
  // `extraLoggers` was deprecated in sbt 1.4 without a non-deprecated replacement, but it is the
  // only sbt 1.x hook for injecting our logger, so silence just that one deprecation. It must stay
  // `silent` (not `warning`) so `-Xfatal-warnings` below does not escalate it to an error.
  "-Wconf:cat=deprecation&msg=extraLoggers:silent",
  // Fail the build on any (non-silenced) warning, so the warning-free state can't regress.
  "-Xfatal-warnings",
)

libraryDependencies += "org.scalameta" %% "munit" % "1.1.1" % Test
