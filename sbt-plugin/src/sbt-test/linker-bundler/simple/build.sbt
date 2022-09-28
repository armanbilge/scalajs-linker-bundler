enablePlugins(ScalaJSPlugin, ScalaJSLinkerBundlerPlugin, ScalaJSJUnitPlugin)
testOptions += Tests.Argument("-a", "-s", "-v")
