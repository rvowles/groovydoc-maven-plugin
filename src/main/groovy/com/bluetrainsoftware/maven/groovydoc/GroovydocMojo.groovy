package com.bluetrainsoftware.maven.groovydoc

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.ResolutionScope

/**

 * author: Richard Vowles - http://gplus.to/RichardVowles
 */
@Mojo(name="generate", requiresProject = false, requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE)
class GroovydocMojo extends AbstractGroovydocMojo {
	@Override
	void execute() throws MojoExecutionException, MojoFailureException {
		if (project.getPackaging() == "pom")
			return

		generateGroovydoc()
	}
}
