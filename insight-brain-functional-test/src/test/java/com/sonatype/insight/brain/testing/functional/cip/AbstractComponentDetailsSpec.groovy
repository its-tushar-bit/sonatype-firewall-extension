/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.cip

import com.sonatype.insight.brain.testing.functional.BaseSpec

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import static com.sonatype.insight.brain.testing.functional.utils.HDSHelper.createComponentDetailListURL as componentDetailList

import static com.sonatype.insight.brain.testing.functional.utils.HDSHelper.createComponentDetailURL as componentDetail

/**
 * Common elements of testing the component details services exposed by the clm-server to external clients.
 * @since 1.12
 */
abstract class AbstractComponentDetailsSpec
    extends BaseSpec
{
  Map<String, Object> mockComponentDetails(String jsonFilename) {
    Map<String, Object> hdsComponentResponse = parseJsonFile(jsonFilename)
    saasRule.setResponseForURI(componentDetail(hdsComponentResponse), JsonOutput.toJson(hdsComponentResponse), 200)
    return hdsComponentResponse
  }

  Map<String, Object> mockComponentDetailsList(String jsonFilename, Map<String, Object> component) {
    Map<String, Object> hdsComponentListResponse = parseJsonFile(jsonFilename)
    saasRule.setResponseForURI(componentDetailList(component), JsonOutput.toJson(hdsComponentListResponse), 200)
    return hdsComponentListResponse
  }

  Map<String, Object> parseJsonFile(String jsonFilename) {
    new JsonSlurper().parseText(getClass().getResource(jsonFilename).text)
  }
}
