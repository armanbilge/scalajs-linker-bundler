enablePlugins(ScalaJSPlugin, ScalaJSLinkerBundlerPlugin, ScalaJSJUnitPlugin)
linkerBundlerNodeModules := Some(file(sys.props("node.modules")))
scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.ESModule)) 
testOptions += Tests.Argument("-a", "-s", "-v")
