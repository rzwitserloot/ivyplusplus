# com.zwitserloot.ivyplusplus

`com.zwitserloot.ivyplusplus` is a jar containing [Apache Ivy](http://ant.apache.org/ivy/) as well as a few useful constructs built on top of it.

Aside from ivy itself, you get a few extra tasks built on top of it.

## How to use

In a plain ant file, put the following at the top, and make sure any targets that use any of ivy's stuff include `depends="ensure-ipp"`:

	<project name="whatever" xmlns:ivy="antlib:com.zwitserloot.ivyplusplus">
		<property name="ivy.retrieve.pattern" value="lib/[conf]/[artifact].[ext]" />
		<available file="lib/ivyplusplus.jar" property="ivyplusplus.available" />
		
		<target name="download-ipp" unless="ivyplusplus.available">
			<mkdir dir="lib" />
			<get src="http://projectlombok.org/downloads/ivyplusplus.jar" dest="lib/ivyplusplus.jar" usetimestamp="true" />
		</target>
		
		<target name="ensure-ipp" depends="download-ipp">
			<taskdef classpath="lib/ivyplusplus.jar" resource="com/zwitserloot/ivyplusplus/antlib.xml" uri="antlib:com.zwitserloot.ivyplusplus" />
		</target>
		
		... rest of your targets, properties, etcetera go here.
	</project>

For a more thorough example, just like at the build file of this very project (com.zwitserloot.ivyplusplus eats its own dog food).

## Developing com.zwitserloot.ivyplusplus in eclipse

Run `ant eclipse` first, then just load the main directory of the project as eclipse project.

## Extra Tasks

### `<ivy:eclipsegen>` - creates eclipse project files from your ivy configuration.

Specify your preferred target/source JVM as attribute named 'source'.
Specify the target directory as attribute named 'todir' (default: project dir, which is what you should leave it at unless you know what you are doing or want to test).

Then, specify each source dir with an inner `<source dir="srcdir" />` element, with an optional attribute named `optional="true"` for optional sources.
For annotation processing, `.apt_generated` is automatically added as optional source dir for you.

Specify ivy configuration using inner elements like so: `<conf name="build" sources="contrib" />` - this will add all artifacts that would be downloaded when resolving _build_
to the eclipse classpath, and if a certain dependency would also download some files for the _contrib_ configuration, attaches those as sources. You can specify multiple
configurations, and if a certain artifact is in multiple configurations, only the one from the highest listed 'conf' element is used. `sources` is of course optional.

If you have apt processors, specify them with `<apt location="path/to/processor.jar" />`.

eclipsegen will also generate the project settings (warnings, errors, source and target compatibility, formatters, styles, etcetera) if you want, by including the `<settings>`
element. Put eclipse settings properties inside as plain text, as well as ant resource elements. If any of the following keys aren't defined, they will be added based on
the `source` attribute of eclipsegen:

 * `org.eclipse.jdt.core.compiler.processAnnotations` - disabled for 1.5, enabled for anything above that.
 * `org.eclipse.jdt.core.compiler.source` - set to 'source' value.
 * `org.eclipse.jdt.core.compiler.compliance` - set to 'source' value.
 * `org.eclipse.jdt.core.compiler.codegen.targetPlatform` - set to 'source' value.

To write your own file, configure a project the way you want it, then mix together all the various files in the `.settings` directory. `eclipsegen` knows how to sort each key
back into the appropriate file.

Note that eclipsegen won't itself actually download any of the files, so it would be a good idea to run `<ivy:retieve />` on the needed confs first.

Example:

	<ivy:eclipsegen source="1.5">
		<srcdir dir="src" />
		<srcdir dir="test" />
		<conf name="build" sources="contrib" />
		<conf name="test" sources="contrib" />
		<settings>
			<url url="http://projectlombok.org/downloads/lombok.eclipse.settings" />
			org.eclipse.jdt.core.formatter.lineSplit=100
		</settings>
	</ivy:eclipsegen>


### `<ivy:compile>` - just like `<javac>`, but this task will also copy any non-java, non-class files to the destination directory.

The defaults are also different:
 debug = on
 source = 1.6
 target = 1.6
 encoding = UTF-8

The _destdir_ directory is also created automatically, so you don't have to `<mkdir>` it first.

### `<ivy:show-dep-report>` - creates a dependency report, and then opens your browser to show it.

The last executed `<ivy:resolve>` serves as the configuration for which a dependency report will be generated. By default `build/report` is used as target dir for
both temporary files needed to create and view the report as well as the report itself. Change it with the `todir` attribute.

### `<ivy:loadversion>` - loads version info from a text file.

Set the file containing the version name in the `file` attribute. The property will be read from it by stripping linebreaks and treating the rest as the version.
This version will then be loaded into a property named `version`. You can change the property by setting the `property` attribute.

### `<ivy:git>` - runs git.

Only works if git is locally installed (for windows users, you'd have to be running ant inside cygwin/msys). Set the git command, such as _pull_ in the `command` attribute.
The command will run in the project dir, unless you override this by specifying the optional `dir` attribute. You can add more arguments via a nested `args` element containing
`<arg value="someCommandLineArgHere" />` elements. Fails with a helpful error message if git doesn't exist.

### `<ivy:git-clone>` - runs git clone.

Required attributes: `repository` listing the repository URL and `dest` listing the directory to place the git repository.

### `<ivy:git-pull>` - convenience for `<ivy:git command="pull" />`

### `<ivy:make-maven-repo>` - creates/updates maven-compatible repositories

Attributes:

* `url` - list the base URL where the repository is located. Example: `http://projectlombok.org/mavenrepo`
* `group` - group name. Example: `org.projectlombok`
* `artifact` - artifact name. Example: `lombok`
* `version` - this version. make-maven-repo won't work if this version name is already available from the repository.
* `outfile` - a bzip2 tarball will be produced that must be unpacked in the existing mavenrepo to update it. This describes where to build it.
* `tmpdir` (optional) - where to put the files that will end up being bzip2 tarballed. By default `build/maven`.
* `artifactfile` - Location of the artifact (e.g. jar file). This will be uploaded along with the logistics to be a maven repository.
* `pomfile` - Location of the pom file describing this project. `@VERSION@` will be replaced with the version. artifact and group IDs and the like must match.

Inner elements:

* `sources` - should contain filesets pointing at source files. Will be used to create a source artifact.
