package com.github.siasia

import sbt._
import Keys._
import classpath.ClasspathUtilities.makeLoader

object JRubyPlugin extends Plugin {
	lazy val rubyRunnerAttr = AttributeKey[RubyRunner]("ruby-runner-attr")
	lazy val rubyRunner = TaskKey[RubyRunner]("ruby-runner")
	lazy val rubyRunScript = InputKey[Unit]("ruby-run-script")
	lazy val gemDependencies = SettingKey[Seq[String]]("gem-dependencies")
	lazy val gemUpdate = TaskKey[Unit]("gem-update")	

	private def eval[T](key: Project.ScopedKey[sbt.Task[T]], state: State):T =
			EvaluateTask.processResult(
				Project.evaluateTask(key, state) getOrElse
					sys.error("Error getting " + key),
				CommandSupport.logger(state))

	def newRunner(state: State, gems: File, tmp: File) = {
		val si = eval(scalaInstance, state)
		val classpath = Seq(gems, file(IO.classLocation[org.jruby.Main].getPath))
		val loader = makeLoader(classpath, si)
		val runner = RubyRunner(loader, tmp)
		state.put(rubyRunnerAttr, runner)
	}		

	def onLoadTask = (onLoad in Global, target in gemDependencies, taskTemporaryDirectory) {
		(onLoad, gems, tmp) => 
			(state: State) =>
			newRunner(onLoad(state), gems, tmp)
	}

	def rubyRunnerTask = (state) map {
		(state) =>
		state.get(rubyRunnerAttr).get
	}

	def rubyRunScriptTask = inputTask {
		(result) =>
			(result, rubyRunner) map {
				(args, runner) =>
					runner.run(args :_*)
			}
	}

	def gemUpdateTask = (rubyRunner, gemDependencies, target in gemDependencies) map {
		(runner, deps, target) =>
		val args = Seq("-S", "gem", "install", "--no-rdoc", "--no-ri", "-i", target.toString) ++ deps
		runner.run(args :_*)
	}

	def rubySettings0 = Seq(
		onLoad in Global <<= onLoadTask,
		rubyRunner <<= rubyRunnerTask,
		rubyRunScript <<= rubyRunScriptTask,
		gemDependencies := Nil,
		target in gemDependencies <<= target / "gems",
		gemUpdate <<= gemUpdateTask
	)
	def rubySettings = inConfig(Compile)(rubySettings0)
}
