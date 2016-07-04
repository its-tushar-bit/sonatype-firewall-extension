/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.integration.Goal;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class ProprietaryConfigServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ProprietaryConfigService service;

  private Application application;

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent("ProprietaryComponentMatchers");

    tempEntity.newProprietaryConfig(Organization.ROOT_ORGANIZATION_ID,
        Collections.singletonList("root.organization.package"), Collections.singletonList("root.organization.regex"));

    tempEntity.newProprietaryConfig(application.getId(), Collections.singletonList("application.package"),
        Collections.singletonList("application.regex"));

    tempEntity.newProprietaryConfig(application.getParentOwnerId(), Collections.singletonList("organization.package"),
        Collections.singletonList("organization.regex"));
  }

  @Test
  public void testGetConfig_GoalEvaluateApplication() {
    ProprietaryConfig config = service.getConfig(Goal.EVALUATE_APPLICATION, application.getPublicId());

    assertThat(config.getRegexes(), contains("application.regex", "organization.regex", "root.organization.regex"));
    assertThat(config.getPackages(),
        contains("application.package", "organization.package", "root.organization.package"));
  }

  @Test
  public void testGetConfig_GoalEvaluateComponent() {
    ProprietaryConfig config = service.getConfig(Goal.EVALUATE_COMPONENT, application.getPublicId());

    assertThat(config.getRegexes(), contains("application.regex", "organization.regex", "root.organization.regex"));
    assertThat(config.getPackages(),
        contains("application.package", "organization.package", "root.organization.package"));
  }

  @Test
  public void testGet_InvalidGoal() throws Exception {
    try {
      service.getConfig(Goal.SUMMARIZE_EVALUATION, application.getPublicId());
      fail("Expected exception was not thrown");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("Proprietary Configuration requested for invalid goal: " + Goal.SUMMARIZE_EVALUATION));
    }
  }

  @Test
  public void testGet_NoGoal() throws Exception {
    ProprietaryConfig config = service.getConfig((Goal) null, application.getPublicId());

    assertThat(config.getRegexes(), contains("root.organization.regex"));
    assertThat(config.getPackages(), contains("root.organization.package"));
  }

  @Test
  public void testGet_NoAppId() throws Exception {
    ProprietaryConfig config = service.getConfig(Goal.EVALUATE_APPLICATION, null);

    assertThat(config.getRegexes(), contains("root.organization.regex"));
    assertThat(config.getPackages(), contains("root.organization.package"));
  }

  @Test
  public void testGetConfig_RootOrganization() {
    ProprietaryConfig config = service.getConfig(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);

    assertThat(config.getRegexes(), contains("root.organization.regex"));
    assertThat(config.getPackages(), contains("root.organization.package"));
  }
}
