/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.successmetrics.ComponentCountsDTO;
import com.sonatype.insight.brain.utils.DisplayFieldValueAssertionUtil;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ComponentDetailResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ComponentDetailResource.RESOURCE_PATH);
  }

  @Test
  public void testGetApplicationDetailsByHash() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testGetApplicationDetailsByHash");
    String hash = "ababababab";
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    HttpResponse response = restRequest().path("applications").query("hash", hash).get();
    assertResponseStatus(200, response);
    ApplicationComponentDetailsDTO[] applicationComponentDetailsDTOs = response
        .getBody(ApplicationComponentDetailsDTO[].class);
    assertThat(applicationComponentDetailsDTOs, notNullValue());
    assertThat(applicationComponentDetailsDTOs, arrayWithSize(1));
  }

  @Test
  public void testGetComponentNameByHash() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testGetComponentNameByHash");
    String hash = "ababababab";
    HttpRequest request = restRequest().path("name").query("hash", hash);

    HttpResponse response = request.get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("Unknown component with hash ababababab."));

    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    response = request.get();
    assertResponseStatus(200, response);
    ComponentDisplayName name = response.getBody(ComponentDisplayName.class);
    DisplayFieldValueAssertionUtil.assertDisplayFieldValuesForGAV(name.parts, "groupId", "artifactId", "version");
  }

  @Test
  public void testGetComponentCounts() throws Exception {
    // create two apps in two orgs
    Application app1 = tempEntity.newApplicationWithParent("appId1", "app 1", "test org 1");
    Application app2 = tempEntity.newApplicationWithParent("appId2", "app 2", "test org 2");

    // an evaluation for each app, with one violation each
    ApplicationComponent component1 = tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "scan1",
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "scan2",
        ComponentIdentifier.createMavenCoordinates("groupId2", "artifactId2", "version2"));
    Policy policy1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1", new Date());
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId2", new Date());
    tempEntity.newPolicyViolation(eval1, policy1, "groupId", "artifactId", "version", "scan1", "reason1");
    tempEntity.newPolicyViolation(eval2, policy1, "groupId2", "artifactId2", "version2", "scan2", "reason2");

    Set<String> orgIds = Collections.singleton(app1.getOrganizationId());
    Set<String> appIds = Collections.singleton(app1.getId());

    // test that app ids are passed
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("applicationIds", appIds);
    HttpResponse response = restRequest().path(ComponentDetailResource.GET_COMPONENT_COUNTS).body(requestBody).post();

    assertResponseStatus(200, response);
    assertGetComponentCountResponse(response, component1);

    // test that org ids are passed
    requestBody = new HashMap<>();
    requestBody.put("organizationIds", orgIds);
    response = restRequest().path(ComponentDetailResource.GET_COMPONENT_COUNTS).body(requestBody).post();

    assertResponseStatus(200, response);
    assertGetComponentCountResponse(response, component1);
  }

  private void assertGetComponentCountResponse(HttpResponse response, ApplicationComponent expectedComponent) {
    ComponentCountsDTO componentCountsDTO = response.getBody(ComponentCountsDTO.class);

    assertThat(componentCountsDTO, notNullValue());
    assertThat(componentCountsDTO.componentsPerApplication, is(1));
    assertThat(componentCountsDTO.componentsInTheMostApplications, hasSize(1));
    assertThat(componentCountsDTO.componentsInTheMostApplications.get(0).count, is(1));
    assertThat(componentCountsDTO.componentsInTheMostApplications.get(0).componentDisplayName,
        is(ComponentDisplayNameUtil.fromIdentifier(expectedComponent.getComponentIdentifier()).toString()));
    assertThat(componentCountsDTO.componentsWithTheMostViolations, hasSize(1));
    assertThat(componentCountsDTO.componentsWithTheMostViolations.get(0).count, is(1));
    assertThat(componentCountsDTO.componentsWithTheMostViolations.get(0).componentDisplayName,
        is(ComponentDisplayNameUtil.fromIdentifier(expectedComponent.getComponentIdentifier()).toString()));
  }
}
