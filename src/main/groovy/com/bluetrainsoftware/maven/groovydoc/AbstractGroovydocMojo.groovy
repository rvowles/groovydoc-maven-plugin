package com.bluetrainsoftware.maven.groovydoc

import groovy.transform.CompileStatic
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.codehaus.groovy.runtime.ResourceGroovyMethods
import org.codehaus.groovy.tools.groovydoc.ClasspathResourceManager
import org.codehaus.groovy.tools.groovydoc.FileOutputTool
import org.codehaus.groovy.tools.groovydoc.GroovyDocTool
import org.codehaus.groovy.tools.groovydoc.LinkArgument
import org.codehaus.groovy.tools.groovydoc.gstringTemplates.GroovyDocTemplateInfo

/**
 * author: Richard Vowles - http://gplus.to/RichardVowles
 */

@CompileStatic
abstract class AbstractGroovydocMojo extends AbstractMojo {
	@Component
	MavenProject project

	@Parameter(property = 'destinationDirectory', defaultValue = '${project.basedir}/target/groovydoc')
	File destinationDirectory

	@Parameter(property = 'additionalSourceDirectories')
	List<String> additionalSourceDirectories

	List<String> sourceDirectories = []

	@Parameter(property = 'run.packageNames')
	private List<String> packageNames

	@Parameter(property = 'run.excludePackageNames')
	private List<String> excludePackageNames

	@Parameter(property = 'run.windowTitle', defaultValue = '${project.groupId}:${project.artifactId}:${project.version}')
	private String windowTitle
	@Parameter(property = 'run.docTitle', defaultValue = '${project.groupId}:${project.artifactId}:${project.version}')
	private String docTitle
	@Parameter(property = 'run.footer', defaultValue = '${project.groupId}:${project.artifactId}:${project.version}')
	private String footer
	@Parameter(property = 'run.header', defaultValue = '${project.groupId}:${project.artifactId}:${project.version}')
	private String header

	@Parameter(property = 'run.scope', defaultValue = 'public')
	private String scope

	@Parameter(property = 'run.author', defaultValue = 'true')
	private boolean author

	@Parameter(property = 'run.processScripts', defaultValue = 'true')
	private boolean processScripts

	@Parameter(property = 'run.includeMainForScripts', defaultValue = 'true')
	private boolean includeMainForScripts

	private List<String> validExtensions
	private List<String> sourceFilesToDoc = []
	private List<LinkArgument> links = new ArrayList<LinkArgument>()

	public AbstractGroovydocMojo() {
		links.add(new LinkArgument(packages: 'java.,org.xml.,javax.,org.xml.', href: 'http://download.oracle.com/javase/6/docs/api'))
		links.add(new LinkArgument(packages: 'java.,org.xml.,javax.,org.xml.', href: 'http://download.oracle.com/javase/6/docs/api'))
		links.add(new LinkArgument(packages: 'groovy.,org.codehaus.groovy.', href: 'http://groovy.codehaus.org/api/'))
	}

	@Parameter(property = 'run.overviewFile')
	private File overviewFile

	@Parameter(property = 'run.stylesheetFile')
	private File styleSheetFile

	// dev note: update javadoc comment for #setExtensions(String) if updating below
	@Parameter(property = 'run.extensions')
	private String extensions = '.java:.groovy:.gv:.gvy:.gsh'

	@Parameter(property = 'run.charset')
	private String charset

	@Parameter(property = 'run.fileEncoding')
	private String fileEncoding

	private List<String> validScopes = ['package', 'private', 'protected', 'public']

	private void checkScopeProperties(Properties properties) {
		if (!validScopes.contains(scope)) {
			throw new MojoFailureException('Groovydoc: More than one of public, private, package, or protected scopes specified.');
		}

		properties.setProperty('publicScope', (scope == 'public').toString());
		properties.setProperty('protectedScope', (scope == 'protected').toString());
		properties.setProperty('packageScope', (scope == 'package').toString());
		properties.setProperty('privateScope', (scope == 'private').toString());

	}

	protected boolean matchesExtension(String name) {
		for(String ext : validExtensions) {
			if (name.endsWith(ext))
				return true
		}

		return false
	}

	/**
	 * These have to be relative otherwise they get referred to as 'absolute' files, and all go in the DefaultPackage.
	 *
	 */
	protected void spelunk(File dir, Set<String> files, String prefix) {
		dir.listFiles().each { File f ->
			if (f.isDirectory()) {
				if (!f.name.startsWith('.')) {
					spelunk(f, files, prefix + f.name + '/')
				}

			} else if (matchesExtension(f.name)) {
				files.add(prefix + f.name)
			}
		}
	}

	protected void parsePackages() {
		Set<String> sourceDirs = new HashSet<>()

		validExtensions = extensions.tokenize(':')

		sourceDirectories.each { String srcDir ->
			spelunk(new File(srcDir), sourceDirs, '')
		}

		sourceFilesToDoc.addAll(sourceDirs)
	}



	protected void generateGroovydoc() {
		String bd = project.basedir.absolutePath

		for(Object srcRoot : project.getCompileSourceRoots()) {
			String src = srcRoot.toString()

			if (src.startsWith(bd)) {
				src = src.substring(bd.length() + 1)
			}

			getLog().debug("adding ${src}")

			sourceDirectories.add(src)
		}

		if (additionalSourceDirectories) {
			sourceDirectories.addAll(additionalSourceDirectories)
		}

		Properties properties = new Properties();
		properties.setProperty('windowTitle', windowTitle);
		properties.setProperty('docTitle', docTitle);
		properties.setProperty('footer', footer);
		properties.setProperty('header', header);
		checkScopeProperties(properties);
		properties.setProperty('author', author.toString());
		properties.setProperty('processScripts', processScripts.toString());
		properties.setProperty('includeMainForScripts', includeMainForScripts.toString());
		properties.setProperty('overviewFile', overviewFile != null ? overviewFile.getAbsolutePath() : '');
		properties.setProperty('charset', charset != null ? charset : '');
		properties.setProperty('fileEncoding', fileEncoding != null ? fileEncoding : '');

		String[] sourcePaths = new String[sourceDirectories.size()]

		GroovyDocTool htmlTool = new GroovyDocTool(
			new ClasspathResourceManager(), // we're gonna get the default templates out of the dist jar file
			sourceDirectories.toArray(sourcePaths),
			GroovyDocTemplateInfo.DEFAULT_DOC_TEMPLATES,
			GroovyDocTemplateInfo.DEFAULT_PACKAGE_TEMPLATES,
			GroovyDocTemplateInfo.DEFAULT_CLASS_TEMPLATES,
			links,
			properties
		)

		parsePackages()

		try {
			htmlTool.add(sourceFilesToDoc);
			FileOutputTool output = new FileOutputTool();
			htmlTool.renderToOutput(output, destinationDirectory.getCanonicalPath()); // TODO push destDir through APIs?
		} catch (Exception e) {
			throw new MojoFailureException('Groovydoc failed', e)
		}
		// try to override the default stylesheet with custom specified one if needed
		if (styleSheetFile != null) {
			try {
				String css = ResourceGroovyMethods.getText(styleSheetFile);
				File outfile = new File(destinationDirectory, 'stylesheet.css');
				ResourceGroovyMethods.setText(outfile, css);
			} catch (IOException e) {
				getLog().error("Warning: Unable to copy specified stylesheet '${styleSheetFile.absolutePath}'. Using default stylesheet instead. Due to: " + e.getMessage(), e);
			}
		}
	}
}
