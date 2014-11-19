/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.utils

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.Condition
import com.sonatype.insight.brain.model.policy.Constraint
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType
import com.sonatype.insight.brain.testing.functional.BaseSpec
import com.sonatype.insight.brain.testing.functional.cip.CIPModule
import com.sonatype.insight.mock.UriParamRequestMatcher

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.jetty.util.UrlEncoded
/**
 * Common elements of testing the component details services exposed by the clm-server to external clients.
 * @since 1.12
 */
abstract class AbstractComponentDetailsSpec
extends BaseSpec {

  static final String SELECT_COMPONENT = 'Select a component to view details.'

  static final String JUNIT_DETAILS_FILE = '/canned-hds-responses/componentDetailsJunit.json'

  static final String JUNIT_DETAILS_LIST_FILE = '/canned-hds-responses/componentDetailsListJunit.json'

  static final String CATALINA_HOST_MANAGER_DETAILS_FILE =
  '/canned-hds-responses/componentDetailsCatalinaHostManager.json'

  static final String CATALINA_HOST_MANAGER_DETAILS_LIST_FILE =
  '/canned-hds-responses/componentDetailsListCatalinaHostManager.json'

  static final String LICENSES_FILE = '/canned-hds-responses/licenses.json'

  protected static ComponentDetails JUNIT

  protected static ComponentDetails CATALINA_HOST_MANAGER

  def setupSpec() {
    JUNIT = mockComponentDetails(JUNIT_DETAILS_FILE)
    mockComponentDetailsList(JUNIT_DETAILS_LIST_FILE, JUNIT)
    CATALINA_HOST_MANAGER = mockComponentDetails(CATALINA_HOST_MANAGER_DETAILS_FILE)
    mockComponentDetailsList(CATALINA_HOST_MANAGER_DETAILS_LIST_FILE, CATALINA_HOST_MANAGER)

    // validation of a license category Policy will trigger this request to populate a cache of licenses
    saasRule.setResponseForURI('rest/license', this.getClass().getResource(LICENSES_FILE).text, 200)
  }

  String createComponentDetailURL(ComponentIdentifier componentIdentifier) {
    return "rest/${getToolName()}/componentDetails?componentIdentifier=${getComponentIdentifierParam(componentIdentifier)}"
  }

  String createComponentDetailListURL(ComponentIdentifier componentIdentifier) {
    return "rest/${getToolName()}/componentDetails/list?componentIdentifier=${getComponentIdentifierParam(componentIdentifier)}"
  }

  static String toJson(Object o) {
    try {
      return new ObjectMapper().writeValueAsString(o);
    }
    catch (Exception e) {
      throw new IllegalStateException(e)
    }
  }

  static String getComponentIdentifierParam(ComponentIdentifier identifier) {
    return UrlEncoded.encodeString(toJson(identifier))
  }

  static <T> T parseJsonFile(String jsonFilename,  Class<? extends T> type) {
    return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(getClass().getResource(jsonFilename), type)
  }

  ComponentDetails mockComponentDetails(String jsonFilename) {
    ComponentDetails hdsComponentResponse = parseJsonFile(jsonFilename, ComponentDetails.class)
    hdsComponentResponse.catalogDate = new Date().minus(366).time  // ensure that catalog data is consistent
    saasRule.setResponseForURI(new UriParamRequestMatcher(createComponentDetailURL(hdsComponentResponse.componentIdentifier), toJson(hdsComponentResponse), 200))
    return hdsComponentResponse
  }

  void mockComponentDetailsList(String jsonFilename, ComponentDetails component) {
    ComponentDetailsList hdsComponentListResponse = parseJsonFile(jsonFilename, ComponentDetailsList.class)
    saasRule.setResponseForURI(new UriParamRequestMatcher(createComponentDetailListURL(component.componentIdentifier), toJson(hdsComponentListResponse), 200))
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

  protected void validateCommon(CIPModule cip, ComponentDetails component) {
    assert cip.getNameField('Group') == component.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_GROUP_ID]
    assert cip.getNameField('Artifact') ==
        component.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_ARTIFACT_ID]
    assert cip.getNameField('Version') == component.componentIdentifier.coordinates[ComponentIdentifier.VERSION]
    validateEffectiveLicense(cip, component)
    assert cip.declaredLicense == component.declaredLicenses[0].licenseName
    assert cip.observedLicense == component.observedLicenses[0].licenseName
    assert cip.matchState == 'exact'
    assert cip.identificationSource == 'Sonatype'
  }

  void validateEffectiveLicense(CIPModule cip, ComponentDetails component) {
    List effectLicenseNames = component.effectiveLicenses.licenseName
    effectLicenseNames = effectLicenseNames.sort()
    List cipLicenseNames = cip.effectiveLicense.split(",").sort()
    assert cipLicenseNames.join(",") == effectLicenseNames.join(",")
  }

  abstract String getToolName();
}
