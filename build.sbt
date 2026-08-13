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
  // `extraLoggers` was deprecated in sbt 1.4 without a non-deprecated replacement, but it is the
  // only sbt 1.x hook for injecting our logger, so silence just that one deprecation. It must stay
  // `silent` (not `warning`) so `-Xfatal-warnings` below does not escalate it to an error.
  "-Wconf:cat=deprecation&msg=extraLoggers:silent",
  // Scala 3 (the sbt 2.x axis) deprecates `_` for type wildcards in favour of `?`, but `?` is not
  // valid on Scala 2.12 (the sbt 1.x axis), so the shared source must keep `_`. Silence that one
  // Scala 3 deprecation; it matches nothing on Scala 2.12.
  "-Wconf:msg=`_` is deprecated for wildcard:silent",
  // Fail the build on any (non-silenced) warning, so the warning-free state can't regress.
  // (`-Werror` is spelled the same on Scala 2.12 and Scala 3; `-Xfatal-warnings` is a deprecated
  // alias on Scala 3.)
  "-Werror",
)

// ...but not for Scaladoc: its `[[...]]` cross-reference resolution is flaky (e.g. it can't link Java
// types such as xsbti.Problem), and such warnings must not fail `doc` / `publishLocal` / a release.
Compile / doc / scalacOptions := (Compile / scalacOptions).value.filterNot(_ == "-Werror")

libraryDependencies += "org.scalameta" %% "munit" % "1.1.1" % Test

// sbt 2's on-load unused-key lint reports false positives here (keys from sbt-git / sbt-dynver,
// pulled in transitively by sbt-ci-release, that the release machinery uses), so turn it off.
Global / lintUnusedKeysOnLoad := false
