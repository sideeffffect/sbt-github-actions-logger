sbt GitHub Actions logger
=========================

This sbt plugin turns sbt's compiler and test output into
[GitHub Actions workflow commands](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-commands),
so that build results are surfaced natively in the GitHub Actions UI:

- compiler warnings and errors show up as inline **annotations** on the affected lines of code, and
- compilation and test output is folded into collapsible **log groups**.

When the build is not running inside GitHub Actions the plugin stays out of the way and does nothing,
so it is safe to enable globally.

### Installation

Add the plugin to your `project/plugins.sbt`:

```scala
addSbtPlugin("com.github.sideeffffect" % "sbt-github-actions-logger" % "<version>")
```

Alternatively, register it as a global plugin by adding the same line to
`~/.sbt/1.0/plugins/plugins.sbt` (see the
[sbt documentation on plugins](https://www.scala-sbt.org/1.x/docs/Using-Plugins.html)).

The plugin is an `AutoPlugin` and is enabled automatically for all projects that use the `JvmPlugin`
(i.e. essentially every project), so no further configuration is required.

Cross-published for **sbt 1.x** and **sbt 2.x**; `addSbtPlugin` picks the right artifact for your build.

### Usage

To check that the plugin was installed correctly, run:

```
sbt sbt-github-actions-logger
```

The plugin's status (on/off, and whether GitHub Actions was detected) will be printed.

### Acknowledgements

This plugin started as a fork of JetBrains'
[sbt-teamcity-logger](https://github.com/JetBrains/sbt-teamcity-logger), adapted from TeamCity service
messages to GitHub Actions workflow commands. It is distributed under the Apache License 2.0.
