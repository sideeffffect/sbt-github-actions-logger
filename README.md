SBT GitHub Actions logger
=============

This plugin extends SBT standard output with service messages that GitHub Actions build server uses to present build results.

### Installation

Add the following to your project/plugins.sbt file:

```scala
addSbtPlugin("com.github.sideeffffect" % "sbt-github-actions-logger" % "1.1.0")
```

or register plugin as a global plugin for your SBT according to [SBT documentation](http://www.scala-sbt.org/0.13.0/docs/Getting-Started/Using-Plugins)

Plugin is compatible with SBT version 0.13.x.


### Using

To be sure that plugin was installed correctly you can use `sbt-github-actions-logger`. Plugin status will be displayed.
