/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.utils

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier
import com.sonatype.insight.brain.model.policy.Condition
import com.sonatype.insight.brain.model.policy.Constraint
import com.sonatype.insight.brain.model.policy.Policy
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType
import com.sonatype.insight.brain.testing.functional.BaseSpec
import com.sonatype.insight.brain.testing.functional.cip.CIPModule
import com.sonatype.insight.brain.testing.functional.cip.VersionGraphModule

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

  static final String ENTITY_FRAMEWORK_DETAILS_FILE = '/canned-hds-responses/componentDetailsEntityFramework.json'

  static final String ENTITY_FRAMEWORK_DETAILS_LIST_FILE = '/canned-hds-responses/componentDetailsListEntityFramework.json'

  static final String PREZI_DETAILS_FILE = '/canned-hds-responses/componentDetailsPrezi.json'

  static final String PREZI_DETAILS_LIST_FILE = '/canned-hds-responses/componentDetailsListPrezi.json'

  static final String LICENSES_FILE = '/canned-hds-responses/licenses.json'

  protected static ComponentDetails JUNIT

  protected static ComponentDetails CATALINA_HOST_MANAGER

  protected static ComponentDetails ENTITY_FRAMEWORK

  protected static ComponentDetails PREZI_DIST

  @Override
  def setupSpec() {
    JUNIT = mockComponentDetails(JUNIT_DETAILS_FILE)
    mockComponentDetailsList(JUNIT_DETAILS_LIST_FILE, JUNIT)
    CATALINA_HOST_MANAGER = mockComponentDetails(CATALINA_HOST_MANAGER_DETAILS_FILE)
    mockComponentDetailsList(CATALINA_HOST_MANAGER_DETAILS_LIST_FILE, CATALINA_HOST_MANAGER)
    ENTITY_FRAMEWORK = mockComponentDetails(ENTITY_FRAMEWORK_DETAILS_FILE)
    mockComponentDetailsList(ENTITY_FRAMEWORK_DETAILS_LIST_FILE, ENTITY_FRAMEWORK)
    PREZI_DIST = mockComponentDetails(PREZI_DETAILS_FILE)
    mockComponentDetailsList(PREZI_DETAILS_LIST_FILE, PREZI_DIST)

    // validation of a license category Policy will trigger this request to populate a cache of licenses
    hdsRule.setResponseForURI('rest/license', this.getClass().getResource(LICENSES_FILE).text, 200)
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
    URL resourceUrl = AbstractComponentDetailsSpec.class.getResource(jsonFilename)
    return new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(resourceUrl, type)
  }

  ComponentDetails mockComponentDetails(String jsonFilename) {
    ComponentDetails hdsComponentResponse = parseJsonFile(jsonFilename, ComponentDetails.class)
    hdsComponentResponse.catalogDate = new Date().minus(366).time  // ensure that catalog data is consistent
    hdsRule.setResponseForURI(createComponentDetailURL(hdsComponentResponse.componentIdentifier), hdsComponentResponse, 200)
    return hdsComponentResponse
  }

  void mockComponentDetailsList(String jsonFilename, ComponentDetails component) {
    ComponentDetailsList hdsComponentListResponse = parseJsonFile(jsonFilename, ComponentDetailsList.class)
    hdsRule.setResponseForURI(createComponentDetailListURL(component.componentIdentifier), hdsComponentListResponse, 200)
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

  protected void verifyVersionGraph(VersionGraphModule versionGraph) {
    assert versionGraph.displayed
    assert versionGraph.labels == ['Popularity', 'Policy Threat', 'Details', 'Security', 'License', 'Quality', 'Other']
    assert versionGraph.chart.@height.toInteger() == 153
  }

  protected void validateMavenComponent(CIPModule cip, ComponentDetails component) {
    assert cip.getNameField('Group') == component.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_GROUP_ID]
    assert cip.getNameField('Artifact') ==
        component.componentIdentifier.coordinates[ComponentIdentifier.MAVEN_ARTIFACT_ID]
    validateComponentCommon(cip, component)
  }

  protected void validateNuGetComponent(CIPModule cip, ComponentDetails component) {
    assert cip.getNameField('ID') == component.componentIdentifier.coordinates[ComponentIdentifier.NUGET_PACKAGE_ID]
    validateComponentCommon(cip, component)
  }

  private void validateComponentCommon(CIPModule cip, ComponentDetails component) {
    assert cip.getNameField('Version') == component.componentIdentifier.coordinates[ComponentIdentifier.VERSION]
    assert cip.declaredLicense == component.declaredLicenses[0].licenseName
    assert cip.observedLicense == component.observedLicenses[0].licenseName
    assert cip.effectiveLicense.split(',').sort().join(',') == component.effectiveLicenses.licenseName.sort().join(',')
    assert cip.matchState == 'exact'
    assert cip.identificationSource == 'Sonatype'
    assert cip.catalogued == '1 year ago'
  }

  abstract String getToolName();
}
