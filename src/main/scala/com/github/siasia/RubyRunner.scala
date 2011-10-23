package com.github.siasia

import org.jruby.Main
import java.io.File

object RubyRunner { 
	def apply(loader: ClassLoader, tmp: File): RubyRunner = {
		val runner = LazyLoader.makeInstance[RubyRunner, RubyRunnerImpl](loader, Seq("org.jruby"))
		runner.setLoader(loader)
		runner.setTemp(tmp)
		runner
	}
}

trait RubyRunner {
	private var loader: ClassLoader = null
	def setLoader(loader: ClassLoader){ this.loader = loader }
	private var temp: File = null
	def setTemp(temp: File){ this.temp = temp }
	protected def withEnv[T](t: => T): T = {
		val thread = Thread.currentThread
		val oldLoader = thread.getContextClassLoader
		thread.setContextClassLoader(loader)
		val oldTemp = System.getProperty("java.io.tmpdir")
		System.setProperty("java.io.tmpdir", temp.toString)
		try {	t	} finally {
			thread.setContextClassLoader(oldLoader)
			System.setProperty("java.io.tmpdir", oldTemp)
		}
	}
	def run(args: String*): Unit
}

class RubyRunnerImpl extends RubyRunner {
	def run(args: String*): Unit = withEnv {
		val main = new Main
		val result = main.run(args.toArray).getStatus
		if(result != 0)
			sys.error("ruby failed. Exit code: "+result)
	}
}
