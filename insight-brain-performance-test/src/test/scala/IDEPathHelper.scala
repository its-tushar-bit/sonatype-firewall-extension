/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import scala.tools.nsc.io.File
import scala.tools.nsc.io.Path.string2path

object IDEPathHelper {

	val gatlingConfUrl = getClass.getClassLoader.getResource("application.conf").getPath
	val projectRootDir = File(gatlingConfUrl).parents(2)

	val mavenSourcesDirectory = projectRootDir / "src" / "test" / "scala"
	val mavenResourcesDirectory = projectRootDir / "src" / "test" / "resources"
	val mavenTargetDirectory = projectRootDir / "target"
	val mavenBinariesDirectory = mavenTargetDirectory / "test-classes"

	val dataDirectory = mavenResourcesDirectory / "data"
	val requestBodiesDirectory = mavenResourcesDirectory / "request-bodies"

	val recorderOutputDirectory = mavenSourcesDirectory
	val resultsDirectory = mavenTargetDirectory / "results"
}