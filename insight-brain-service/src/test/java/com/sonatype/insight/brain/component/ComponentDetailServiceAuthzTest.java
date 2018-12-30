/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentDetailServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ComponentDetailService componentDetailService;

  private String hash = "ababababab";

  @Before
  public void before() {
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
  }

  @Test
  public void testGetApplicationDetailsByHash_Unauthenticated() throws Exception {
    assertThat(componentDetailService.getApplicationDetailsByHash(hash)).isEmpty();
  }

  @Test
  public void testGetApplicationDetailsByHash_Unauthorized() throws Exception {
    login();
    assertThat(componentDetailService.getApplicationDetailsByHash(hash)).isEmpty();
  }

  @Test
  public void testGetApplicationDetailsByHash_Authorized() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId1", "artifactId1", "version1"));
    grantReadPermission(app.getId());
    List<ApplicationComponentDetailsDTO> result = componentDetailService.getApplicationDetailsByHash(hash);
    assertThat(result).extracting(dto -> dto.application.getId()).containsExactlyInAnyOrder(app.getId());
  }
}
