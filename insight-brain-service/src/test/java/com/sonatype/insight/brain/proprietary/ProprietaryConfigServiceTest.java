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
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class ProprietaryConfigServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ProprietaryConfigService service;

  private Application application;

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent("ProprietaryComponentMatchers");

    tempEntity.newProprietaryConfig(application.getId(), Collections.singletonList("application.package"),
        Collections.singletonList("application.regex"));

    tempEntity.newProprietaryConfig(application.getParentOwnerId(), Collections.singletonList("organization.package"),
        Collections.singletonList("organization.regex"));
  }

  @Test
  public void testGetConfig_GoalEvaluateApplication() {
    ProprietaryConfig config = service.getConfig(Goal.EVALUATE_APPLICATION, application.getPublicId());

    assertThat(config.getRegexes(), contains("application.regex", "organization.regex"));
    assertThat(config.getPackages(), contains("application.package", "organization.package"));
  }

  @Test
  public void testGetConfig_GoalEvaluateComponent() {
    ProprietaryConfig config = service.getConfig(Goal.EVALUATE_COMPONENT, application.getPublicId());

    assertThat(config.getRegexes(), contains("application.regex", "organization.regex"));
    assertThat(config.getPackages(), contains("application.package", "organization.package"));
  }
}
