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

// CI runs `sbt publishLocal`; gate it on the test suite so the tests run (and can fail the build)
// in CI. This lives in build.sbt rather than the workflow because the tests should run before every
// publish anyway, and it needs no `.github/workflows` change.
//
// We check the raw `executeTests` outcome instead of depending on the `test` task, because this
// project dogfoods sbt-github-actions-logger, which replaces `Test / test`'s result logger with a
// no-op — so `test` on its own would not fail the build on a test failure.
val checkTests = taskKey[Unit]("Run the tests and fail the build on any test failure.")
checkTests := {
  val result = (Test / executeTests).value
  if (result.overall != sbt.protocol.testing.TestResult.Passed) {
    sys.error(s"Tests did not pass (overall result: ${result.overall}).")
  }
}

publishLocal := publishLocal.dependsOn(checkTests).value
