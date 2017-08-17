/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.aggregation.ComponentCountsDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ComponentDetailServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ComponentDetailService componentDetailService;

  private String hash = "ababababab";

  private Set orgIds = null;

  private Set appIds = null;

  @Before
  public void before() {
    ApplicationComponent buildComponent = tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    Policy licensePolicy = tempEntity.newPolicy(app.getParentOwnerId(), "TestLicensePolicy");
    PolicyEvaluation buildEval = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "now", new Date());
    tempEntity.newPolicyViolation(buildEval, licensePolicy, 7, LICENSE, buildComponent.getComponentIdentifier(),
        buildComponent.getHash(), FailActionType.ID);
    orgIds = new HashSet(Arrays.asList(org.getId()));
    appIds = new HashSet(Arrays.asList(app.getId()));
  }

  @Test
  public void testGetApplicationDetailsByHash_Unauthenticated() throws Exception {
    assertThat(componentDetailService.getApplicationDetailsByHash(hash), hasSize(0));
  }

  @Test
  public void testGetApplicationDetailsByHash_Unauthorized() throws Exception {
    login();
    assertThat(componentDetailService.getApplicationDetailsByHash(hash), hasSize(0));
  }

  @Test
  public void testGetApplicationDetailsByHash_Authorized() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId1", "artifactId1", "version1"));
    grantReadPermission(app.getId());
    List<ApplicationComponentDetailsDTO> result = componentDetailService.getApplicationDetailsByHash(hash);
    assertThat(result, hasSize(1));
    assertThat(result.get(0).application.getId(), is(app.getId()));
  }

  @Test
  public void testGetComponentCounts_Organization_Unauthenticated() throws Exception {
    ComponentCountsDTO result = componentDetailService.getComponentCounts(orgIds, null);
    assertThat(result, notNullValue());
    assertThat(result.componentsPerApplication, is(0));
    assertThat(result.componentsInTheMostApplications, hasSize(0));
    assertThat(result.componentsWithTheMostViolations, hasSize(0));
  }

  @Test
  public void testGetComponentCounts_Organization_Unauthorized() throws Exception {
    login();
    ComponentCountsDTO result = componentDetailService.getComponentCounts(orgIds, null);
    assertThat(result, notNullValue());
    assertThat(result.componentsPerApplication, is(0));
    assertThat(result.componentsInTheMostApplications, hasSize(0));
    assertThat(result.componentsWithTheMostViolations, hasSize(0));
  }

  @Test
  public void testGetComponentCounts_Organization_Authorized() throws Exception {
    grantReadPermission(app.getId());
    ComponentCountsDTO result = componentDetailService.getComponentCounts(orgIds, null);
    assertThat(result, notNullValue());
    assertThat(result.componentsPerApplication, is(1));
    assertThat(result.componentsInTheMostApplications.get(0).count, is(1));
    assertThat(result.componentsWithTheMostViolations.get(0).count, is(1));
  }

  @Test
  public void testGetComponentCounts_Application_Unauthenticated() throws Exception {
    ComponentCountsDTO result = componentDetailService.getComponentCounts(null, appIds);
    assertThat(result, notNullValue());
    assertThat(result.componentsPerApplication, is(0));
    assertThat(result.componentsInTheMostApplications, hasSize(0));
    assertThat(result.componentsWithTheMostViolations, hasSize(0));
  }

  @Test
  public void testGetComponentCounts_Application_Unauthorized() throws Exception {
    login();
    ComponentCountsDTO result = componentDetailService.getComponentCounts(null, appIds);
    assertThat(result, notNullValue());
    assertThat(result.componentsPerApplication, is(0));
    assertThat(result.componentsInTheMostApplications, hasSize(0));
    assertThat(result.componentsWithTheMostViolations, hasSize(0));
  }

  @Test
  public void testGetComponentCount_Application_Authorized() throws Exception {
    grantReadPermission(app.getId());
    ComponentCountsDTO result = componentDetailService.getComponentCounts(null, appIds);
    assertThat(result, notNullValue());
    assertThat(result.componentsPerApplication, is(1));
    assertThat(result.componentsInTheMostApplications.get(0).count, is(1));
    assertThat(result.componentsWithTheMostViolations.get(0).count, is(1));
  }
}
