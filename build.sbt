sbtPlugin := true

libraryDependencies <++= (sbtVersion) {
	v =>
		Seq(
			"com.github.siasia" %% "plugin-commons" % (v+"-0.1"),
			"org.jruby" % "jruby-complete" % "1.6.4"
		)
}
