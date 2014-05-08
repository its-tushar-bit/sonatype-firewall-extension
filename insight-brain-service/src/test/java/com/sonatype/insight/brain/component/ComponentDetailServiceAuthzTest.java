/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ComponentDetailServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ComponentDetailService componentDetailService;

  private String hash = "ababababab";

  @Before
  public void before() {
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, "groupId", "artifactId", "version");
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
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash, "groupId1", "artifactId1", "version1");
    grantReadPermission(app.getId());
    List<ApplicationComponentDetailsDTO> result = componentDetailService.getApplicationDetailsByHash(hash);
    assertThat(result, hasSize(1));
    assertThat(result.get(0).application.getId(), is(app.getId()));
  }
}