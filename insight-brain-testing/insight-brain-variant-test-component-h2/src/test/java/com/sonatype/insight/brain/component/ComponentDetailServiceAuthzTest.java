/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class ComponentDetailServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ComponentDetailService componentDetailService;

  private final String hash = "ababababab";

  @BeforeEach
  public void before() {
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
  }

  @Test
  public void testGetApplicationDetailsByHash_Unauthenticated() {
    assertThat(componentDetailService.getApplicationDetailsByHash(hash)).isEmpty();
  }

  @Test
  public void testGetApplicationDetailsByHash_Unauthorized() {
    login();
    assertThat(componentDetailService.getApplicationDetailsByHash(hash)).isEmpty();
  }

  @Test
  public void testGetApplicationDetailsByHash_Authorized() {
    Application app1 = tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId1", "artifactId1", "version1"));
    grantReadPermission(app.getId());
    List<ApplicationComponentDetailsDTO> result = componentDetailService.getApplicationDetailsByHash(hash);
    assertThat(result).extracting(dto -> dto.application.getId()).containsExactlyInAnyOrder(app.getId());
  }
}
