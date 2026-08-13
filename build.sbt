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

libraryDependencies += "org.scalameta" %% "munit" % "1.1.1" % Test

// This project dogfoods sbt-github-actions-logger, which (when running inside GitHub Actions)
// replaces the test-result logger with a no-op so a failed suite doesn't also print a bare
// "exit code 1". For our own build we want a failed test to fail the build, so restore sbt's
// default result logger, which fails the `test` task on any test failure.
Test / test / testResultLogger := sbt.TestResultLogger.SilentWhenNoTests
