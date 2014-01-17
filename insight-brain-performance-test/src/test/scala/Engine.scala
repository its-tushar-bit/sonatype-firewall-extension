/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import com.excilys.ebi.gatling.app.Gatling
import com.excilys.ebi.gatling.core.config.GatlingPropertiesBuilder

object Engine extends App {
	
	val props = new GatlingPropertiesBuilder
	props.dataDirectory(IDEPathHelper.dataDirectory.toString)
	props.resultsDirectory(IDEPathHelper.resultsDirectory.toString)
	props.requestBodiesDirectory(IDEPathHelper.requestBodiesDirectory.toString)
	props.binariesDirectory(IDEPathHelper.mavenBinariesDirectory.toString)

	Gatling.fromMap(props.build)
}