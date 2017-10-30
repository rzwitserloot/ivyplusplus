# com.zwitserloot.ivyplusplus

`com.zwitserloot.ivyplusplus` is a jar containing [Apache Ivy](http://ant.apache.org/ivy/) as well as a few useful constructs built on top of it.

Aside from ivy itself, you get a few extra tasks and a command line tool that creates a new project by filling out a skeleton build.xml and ivy configuration.

## How to use

Run `java -jar ivyplusplus.jar --help` for more information on how to create a skeleton project.

_Supported since ipp 1.5_

For a more thorough example, just like at the build file of this very project (com.zwitserloot.ivyplusplus eats its own dog food).

## Developing com.zwitserloot.ivyplusplus in eclipse

Run `ant eclipse` first, then just load the main directory of the project as eclipse project.

## Developing com.zwitserloot.ivyplusplus in intellij

Run `ant intellij` first, then just load the main directory of the project as intellij project.

## Extra Tasks

### `<ivy:intellijgen>` - creates intellij project files from your ivy configuration.

Specify your preferred target/source JVM as attribute named 'source'.
Specify the target directory as attribute named 'todir' (default: project dir, which is what you should leave it at unless you know what you are doing or want to test).

First specify all the configurations you need with inner `<conf name="build" sources="contrib" />` constructs. Each such configuration will be turned into a library set. 'sources' is optional, of course. All artifacts that would be downloaded when resolving _build_ will be added to this library set, and all artifacts that would be downloaded when resolving _contrib_ are added to this library set as sources.

Finally, create intellij modules with inner `<module name="someModule" depends="conf1, conf2">` entries. These modules will be dependent on the listed library sets (which you just made using `<conf />`). The `<module>` tag should include nested `<srcdir dir="srcdir">` entries.

To enable annotation processing, include `<apt enabled="true">` inside your `<ivy:intellijgen>` task.

intellijgen will also generate the project settings (warnings, errors, source and target compatibility, formatters, styles, etcetera) if you want, by including the `<settings>` element. Put ant resource elements inside.

To write your own file, configure a project the way you want it, then mix together all the various `<component>` elements in the files in your `.idea` directory. `intellijgen` knows how to sort each element back into the appropriate file.

Note that intellij won't itself actually download any of the files, so it would be a good idea to run `<ivy:retieve />` on the needed confs first.

Example:

	<ivy:intellijgen source="1.5">
		<conf name="build" sources="contrib" />
		<conf name="test" sources="contrib" />
		<module name="lombok" depends="build, test">
			<srcdir dir="src" />
			<srcdir dir="test" />
		</module>
		<settings>
			<url url="http://projectlombok.org/downloads/lombok.intellij.settings" />
		</settings>
		<apt enabled="true" />
	</ivy:intellijgen>

_Supported since ipp 1.4_

### `<ivy:eclipsegen>` - creates eclipse project files from your ivy configuration.

Specify your preferred target/source JVM as attribute named 'source'.
Specify the target directory as attribute named 'todir' (default: project dir, which is what you should leave it at unless you know what you are doing or want to test).

Then, specify each source dir with an inner `<srcdir dir="srcdir" />` element, with an optional attribute named `optional="true"` for optional sources.
For annotation processing, `.apt_generated` is automatically added as optional source dir for you.

Specify ivy configuration using inner elements like so: `<conf name="build" sources="contrib" />` - this will add all artifacts that would be downloaded when resolving _build_
to the eclipse classpath, and if a certain dependency would also download some files for the _contrib_ configuration, attaches those as sources. You can specify multiple
configurations, and if a certain artifact is in multiple configurations, only the one from the highest listed 'conf' element is used. `sources` is of course optional.

If you have apt processors, specify them with `<apt location="path/to/processor.jar" />`.

If you have separate jar files, you can specify these with `<lib location="path/to/jar.jar" />`.

To set up the eclipse project so that sibling/child projects also under active development are registered as project dependencies instead of dependencies on the ivy artifacts, use `<local org="your.org" name="projname" />`; this will look for `../your.org.projname/.project` relative to the current directory, and, _only if that file exists_, it will replace any ivy dependency on the stated org/name pair (of any version!) with that project. If that file does not exist, no warning or error is generated and the normal dependency is inserted. This way, a 'fresh' clone from a source repo compiles cleanly, but you can replace any dependency with tandem development on that project by just checking it out into the same workspace and rerunning 'ant eclipse'.

eclipsegen will also generate the project settings (warnings, errors, source and target compatibility, formatters, styles, etcetera) if you want, by including the `<settings>`
element. Put eclipse settings properties inside as plain text, as well as ant resource elements. If any of the following keys aren't defined, they will be added based on
the `source` attribute of eclipsegen:

 * `org.eclipse.jdt.core.compiler.processAnnotations` - disabled for 1.5, enabled for anything above that.
 * `org.eclipse.jdt.core.compiler.source` - set to 'source' value.
 * `org.eclipse.jdt.core.compiler.compliance` - set to 'source' value.
 * `org.eclipse.jdt.core.compiler.codegen.targetPlatform` - set to 'source' value.

To write your own file, configure a project the way you want it, then mix together all the various files in the `.settings` directory. `eclipsegen` knows how to sort each key back into the appropriate file.

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

_Supported since ipp 1.0_
	
### `<ivy:ensureippversion>` - Error out or set a property if available version of ivyplusplus is not sufficient.

Ivy takes care of version control, but who will take care of Ivy's own version control? With this task you can
error out (or set a property) if the cached ivyplusplus.jar is a version that's not equal to/higher than what you need.

Example: `<ivy:ensureippversion version="1.5" property="ipp.minimumVersionOkay">`

the `property` set in the attribute will be set if the version available is equal to or higher than the version put
in the mandatory `version` attribute. Alternative usage is to omit `property`. In that case, a build error will occur if `version` is higher than what's available.

_Supported since ipp 1.4_

### `<ivy:compile>` - just like `<javac>`, but this task will also copy any non-java, non-class files to the destination directory.

The defaults are also different:
 debug = on
 source = 1.8
 target = 1.8
 encoding = UTF-8
 includeAntRuntime = false

The _destdir_ directory is also created automatically, so you don't have to `<mkdir>` it first.

(since ipp 1.20: You can also set ecj="true" to use ecj instead. Useful if you want to compile with old source/target).

_Supported since ipp 1.0_

### `<ivy:cachedunjar>` - similar to unjar, except will not unpack jars that don't need to be unpacked.

While `cachedunjar` is similar to `unjar`, it supports only file resources, either via a `source` attribute or nested `<fileset>` elements. You must specify a `dest` attribute
just like with `unjar`. In addition, you must specify a file via the `marker` attribute. This file is used to track the state of the unpacked jars; a 'savefile' of sorts.

Example:

	<ivy:cachedunjar dest="build/depsToPack" marker="build/depsToPack.marker">
		<fileset dir="lib/runtime" includes="*.jar" />
	</ivy:cachedunjar>

_Supported since ipp 1.7_

### `<ivy:show-dep-report>` - creates a dependency report, and then opens your browser to show it.

The last executed `<ivy:resolve>` serves as the configuration for which a dependency report will be generated. By default `build/report` is used as target dir for
both temporary files needed to create and view the report as well as the report itself. Change it with the `todir` attribute.

_Supported since ipp 1.0_

### `<ivy:loadversion>` - loads version info from a text file.

Set the file containing the version name in the `file` attribute. The property will be read from it by stripping linebreaks and treating the rest as the version.
This version will then be loaded into a property named `version`. You can change the property by setting the `property` attribute.

_Supported since ipp 1.3_

### `<ivy:git>` - runs git.

Only works if git is locally installed (for windows users, you'd have to be running ant inside cygwin/msys). Set the git command, such as _pull_ in the `command` attribute.
The command will run in the project dir, unless you override this by specifying the optional `dir` attribute. You can add more arguments via a nested `args` element containing
`<arg value="someCommandLineArgHere" />` elements. Fails with a helpful error message if git doesn't exist.

_Supported since ipp 1.0_

### `<ivy:git-clone>` - runs git clone.

Required attributes: `repository` listing the repository URL and `dest` listing the directory to place the git repository.

_Supported since ipp 1.0_

### `<ivy:git-pull>` - convenience for `<ivy:git command="pull" />`

_Supported since ipp 1.0_

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

_Supported since ipp 1.3_

NB: make-maven-repo is no longer under active development since sonatype changed their policy on how maven artifacts are to be added to maven central.
