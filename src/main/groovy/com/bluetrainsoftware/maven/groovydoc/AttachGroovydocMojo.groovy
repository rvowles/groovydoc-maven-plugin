package com.bluetrainsoftware.maven.groovydoc

import groovy.transform.CompileStatic
import org.apache.commons.io.IOUtils
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProjectHelper

import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**

 * author: Richard Vowles - http://gplus.to/RichardVowles
 */
@CompileStatic
@Mojo(name="attach-doc", requiresProject = false, requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE)
class AttachGroovydocMojo extends AbstractGroovydocMojo {
	@Component
	MavenProjectHelper projectHelper

	@Override
	void execute() throws MojoExecutionException, MojoFailureException {
		generateGroovydoc()

		File javadocFile = new File(project.basedir, "target/${project.artifactId}-${project.version}-javadoc.jar")

		getLog().info("Attaching ${javadocFile.absolutePath}")

		JarOutputStream jarFile = new JarOutputStream(new FileOutputStream(javadocFile))

		spelunk(jarFile, destinationDirectory, "")

		jarFile.close()

		projectHelper.attachArtifact(project, javadocFile, 'javadoc')
	}

	protected void spelunk(JarOutputStream jarFile, File dir, String prefix) {
		dir.listFiles().each { File file ->
			if (file.isDirectory()) {
				if (!file.name.startsWith(".")) {
					spelunk(jarFile, file, prefix + file.name + "/")
				}

			} else {
				getLog().debug("add: ${prefix + file.name}")
	      ZipEntry ze = new ZipEntry(prefix + file.name)
				jarFile.putNextEntry(ze)
				IOUtils.copy(new FileReader(file), jarFile)
			}
		}

	}
}
