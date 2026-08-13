addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")

// Dogfood: build this plugin with the latest release of itself, so its own CI logs get the
// GitHub Actions groups and annotations it provides.
addSbtPlugin("com.github.sideeffffect" % "sbt-github-actions-logger" % "1.0.0")
