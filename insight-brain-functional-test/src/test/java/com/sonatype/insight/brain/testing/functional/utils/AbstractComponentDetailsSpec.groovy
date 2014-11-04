/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.utils

import java.util.Map.Entry

import com.sonatype.insight.brain.model.policy.Condition
import com.sonatype.insight.brain.model.policy.Constraint
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType
import com.sonatype.insight.brain.testing.functional.BaseSpec
import com.sonatype.insight.brain.testing.functional.cip.CIPModule
import com.sonatype.insight.mock.UriParamRequestMatcher

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
/**
 * Common elements of testing the component details services exposed by the clm-server to external clients.
 * @since 1.12
 */
abstract class AbstractComponentDetailsSpec
    extends BaseSpec
{

  static final String SELECT_COMPONENT = 'Select a component to view details.'

  static final String JUNIT_DETAILS_FILE = '/canned-hds-responses/componentDetailsJunit.json'

  static final String JUNIT_DETAILS_LIST_FILE = '/canned-hds-responses/componentDetailsListJunit.json'

  static final String CATALINA_HOST_MANAGER_DETAILS_FILE =
      '/canned-hds-responses/componentDetailsCatalinaHostManager.json'

  static final String CATALINA_HOST_MANAGER_DETAILS_LIST_FILE =
      '/canned-hds-responses/componentDetailsListCatalinaHostManager.json'

  static final String LICENSES_FILE = '/canned-hds-responses/licenses.json'

  protected static Map<String, Object> JUNIT

  protected static Map<String, Object> CATALINA_HOST_MANAGER

  def setupSpec() {
    JUNIT = mockComponentDetails(JUNIT_DETAILS_FILE).asImmutable()
    mockComponentDetailsList(JUNIT_DETAILS_LIST_FILE, JUNIT)
    CATALINA_HOST_MANAGER = mockComponentDetails(CATALINA_HOST_MANAGER_DETAILS_FILE).asImmutable()
    mockComponentDetailsList(CATALINA_HOST_MANAGER_DETAILS_LIST_FILE, CATALINA_HOST_MANAGER)

    // validation of a license category Policy will trigger this request to populate a cache of licenses
    saasRule.setResponseForURI('rest/license', this.getClass().getResource(LICENSES_FILE).text, 200)
  }

  String createComponentDetailURL(Map<String,Object> componentIdentifier) {
    String url = "rest/${getToolName()}/componentDetails/${componentIdentifier.format}?"
    for (Entry<String,String> entry : componentIdentifier.coordinates) {
      url += "${entry.getKey()}=${entry.getValue()}&"
    }
    return url.substring(0, url.length() - 1)
  }

  String createComponentDetailListURL(Map<String,Object> componentIdentifier) {
    String url = "rest/${getToolName()}/componentDetails/${componentIdentifier.format}/list?"
    for (Entry<String,String> entry : componentIdentifier.coordinates) {
      if (entry.getValue() != null) {
        url += "${entry.getKey()}=${entry.getValue()}&"
      }
    }
    return url.substring(0, url.length() - 1)
  }

  static Map<String, Object> parseJsonFile(String jsonFilename) {
    new JsonSlurper().parseText(getClass().getResource(jsonFilename).text)
  }

  Map<String, Object> mockComponentDetails(String jsonFilename) {
    Map<String, Object> hdsComponentResponse = parseJsonFile(jsonFilename)
    hdsComponentResponse.catalogDate = new Date().minus(366).time  // ensure that catalog data is consistent
    saasRule.setResponseForURI(new UriParamRequestMatcher(createComponentDetailURL(hdsComponentResponse.identifier), JsonOutput.toJson(hdsComponentResponse), 200))
    return hdsComponentResponse
  }

  Map<String, Object> mockComponentDetailsList(String jsonFilename, Map<String, Object> component) {
    Map<String, Object> hdsComponentListResponse = parseJsonFile(jsonFilename)
    saasRule.setResponseForURI(new UriParamRequestMatcher(createComponentDetailListURL(component.identifier), JsonOutput.toJson(hdsComponentListResponse), 200))
    return hdsComponentListResponse
  }

  /**
   * Create a policy targeting a specific license.
   */
  Policy createLicensePolicy(String applicationId, String policyName, String licenseId) {
    Policy policy = new Policy()
    policy.with {
      name = policyName
      ownerId = applicationId
      threatLevel = 10
      addConstraint(new Constraint(name: policyName,
      conditions: [new Condition(LicenseConditionType.ID, 'is', licenseId)]))
    }
    temporaryEntity.newPolicy(policy)
    return policy
  }

  protected void validateCommon(CIPModule cip, Map<String, Object> component) {
    assert cip.getNameField('Group') == component.groupId
    assert cip.getNameField('Artifact') == component.artifactId
    assert cip.getNameField('Version') == component.version
    validateEffectiveLicense(cip, component)
    assert cip.declaredLicense == component.declaredLicenses[0].licenseName
    assert cip.observedLicense == component.observedLicenses[0].licenseName
    assert cip.matchState == 'exact'
    assert cip.identificationSource == 'Sonatype'
  }

  void validateEffectiveLicense(CIPModule cip, Map<String, Object> component) {
    List effectLicenseNames = component.effectiveLicenses.licenseName
    effectLicenseNames = effectLicenseNames.sort()
    List cipLicenseNames = cip.effectiveLicense.split(",").sort()
    assert cipLicenseNames.join(",") == effectLicenseNames.join(",")
  }

  abstract String getToolName();
}
