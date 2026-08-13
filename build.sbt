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

// TEMPORARY: this project dogfoods the *released* sbt-github-actions-logger, and releases up to and
// including 1.0.0 still swallow the test result (the bug this PR fixes at the source), which would
// make the CI `test` step pass even on a failing test. Restore sbt's default result logger so our
// own CI gates correctly. Remove this once the dogfooded version is bumped to a release that
// contains this fix.
Test / test / testResultLogger := sbt.TestResultLogger.SilentWhenNoTests
