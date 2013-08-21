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

	@Parameter(property = "destinationDirectory", defaultValue = '${project.basedir}/target/groovydoc')
	File destinationDirectory

	@Parameter(property = "additionalSourceDirectories")
	List<String> additionalSourceDirectories

	List<String> sourceDirectories = []

	@Parameter
	private List<String> packageNames;

	@Parameter
	private List<String> excludePackageNames;

	@Parameter
	private String windowTitle = "Groovy Documentation";
	@Parameter
	private String docTitle = "Groovy Documentation";
	@Parameter
	private String footer = "Groovy Documentation";
	@Parameter
	private String header = "Groovy Documentation";
	@Parameter
	private Boolean privateScope = Boolean.FALSE;
	@Parameter
	private Boolean protectedScope = Boolean.FALSE;
	@Parameter
	private Boolean packageScope = Boolean.FALSE;
	@Parameter
	private Boolean publicScope = Boolean.TRUE;
	@Parameter
	private boolean author = true;
	@Parameter
	private Boolean processScripts = Boolean.TRUE;
	@Parameter
	private Boolean includeMainForScripts = Boolean.TRUE;
	@Parameter
	private boolean useDefaultExcludes;
	@Parameter
	private boolean includeNoSourcePackages;


	private List<String> validExtensions;
	private List<String> sourceFilesToDoc = [];
	private List<LinkArgument> links = new ArrayList<LinkArgument>();

	@Parameter
	private File overviewFile;

	@Parameter
	private File styleSheetFile;

	// dev note: update javadoc comment for #setExtensions(String) if updating below
	@Parameter
	private String extensions = ".java:.groovy:.gv:.gvy:.gsh";

	@Parameter
	private String charset;

	@Parameter
	private String fileEncoding;


	private void checkScopeProperties(Properties properties) {

		// make protected the default scope and check for invalid duplication
		int scopeCount = 0;
		if (packageScope) scopeCount++;
		if (privateScope) scopeCount++;
		if (protectedScope) scopeCount++;
		if (publicScope) scopeCount++;
		if (scopeCount == 0) {
			protectedScope = true;
		} else if (scopeCount > 1) {
			throw new MojoFailureException("Groovydoc: More than one of public, private, package, or protected scopes specified.");
		}
	}

	protected boolean matchesExtension(String name) {
		for(String ext : validExtensions) {
			if (name.endsWith(ext))
				return true
		}

		return false
	}

	/**
	 * These have to be relative otherwise they get referred to as "absolute" files, and all go in the DefaultPackage.
	 *
	 */
	protected void spelunk(File dir, Set<String> files, String prefix) {
		dir.listFiles().each { File f ->
			if (f.isDirectory()) {
				if (!f.name.startsWith(".")) {
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
		properties.setProperty("windowTitle", windowTitle);
		properties.setProperty("docTitle", docTitle);
		properties.setProperty("footer", footer);
		properties.setProperty("header", header);
		checkScopeProperties(properties);
		properties.setProperty("publicScope", publicScope.toString());
		properties.setProperty("protectedScope", protectedScope.toString());
		properties.setProperty("packageScope", packageScope.toString());
		properties.setProperty("privateScope", privateScope.toString());
		properties.setProperty("author", author.toString());
		properties.setProperty("processScripts", processScripts.toString());
		properties.setProperty("includeMainForScripts", includeMainForScripts.toString());
		properties.setProperty("overviewFile", overviewFile != null ? overviewFile.getAbsolutePath() : "");
		properties.setProperty("charset", charset != null ? charset : "");
		properties.setProperty("fileEncoding", fileEncoding != null ? fileEncoding : "");

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
			throw new MojoFailureException("Groovydoc failed", e)
		}
		// try to override the default stylesheet with custom specified one if needed
		if (styleSheetFile != null) {
			try {
				String css = ResourceGroovyMethods.getText(styleSheetFile);
				File outfile = new File(destinationDirectory, "stylesheet.css");
				ResourceGroovyMethods.setText(outfile, css);
			} catch (IOException e) {
				getLog().error("Warning: Unable to copy specified stylesheet '" + styleSheetFile.getAbsolutePath() +
					"'. Using default stylesheet instead. Due to: " + e.getMessage());
			}
		}
	}
}
