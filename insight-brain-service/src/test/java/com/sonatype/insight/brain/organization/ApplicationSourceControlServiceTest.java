/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import java.util.List;
import java.util.stream.IntStream;
import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.sonatype.plexus.components.cipher.PlexusCipher;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationSourceControlServiceTest
    extends AbstractComponentTest
{
  private static final String ROOT_TOKEN = "root-token";

  private static final String ENC = "CMMDwoV";

  private static final String REPO_URL = "https://example.com/organization/project";

  @Inject
  private ApplicationSourceControlService applicationSourceControlService;

  @Inject
  private PlexusCipher plexusCipher;

  private Organization org;

  @Before
  public void before() throws Exception {
    org = tempEntity.newOrganization();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipher.encrypt(ROOT_TOKEN, ENC),
        SourceControlProvider.GITHUB);
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_ScmEnabled() {
    final Application app1 = tempEntity.newApplication(org.getId());
    final Application app2 = tempEntity.newApplication(org.getId());

    // Add a source control record for app1 with ASCF enabled, so it shouldn't be in the result list
    // app2 has no source control record, so it should be in the result list
    tempEntity.newSourceControl(app1.getId(), REPO_URL, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, true);

    final List<Application> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled();

    assertThat(appsWithAutomatedSourceControlFeedbackDisabled)
        .extracting("id")
        .contains(app2.getId())
        .doesNotContain(app1.getId());
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_ScmDisabled() {
    final Application app1 = tempEntity.newApplication(org.getId());
    final Application app2 = tempEntity.newApplication(org.getId());

    // Add a source control record for app1 with ASCF disabled, so it should be in the result list
    // app2 has no source control record, so it should be in the result list
    tempEntity.newSourceControl(app1.getId(), REPO_URL, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, false);

    final List<Application> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled();

    assertThat(appsWithAutomatedSourceControlFeedbackDisabled)
        .extracting("id")
        .contains(app2.getId(), app1.getId());
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_AllAppsScmDisabled() {
    // All apps missing source control records = SCM disabled
    final int numTotalApps = 8;
    IntStream.range(0, numTotalApps)
        .forEach(i -> tempEntity.newApplication(org.getId()));

    final List<Application> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled();

    assertThat(appsWithAutomatedSourceControlFeedbackDisabled)
        .hasSize(numTotalApps);
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled_NoApps() {
    final List<Application> appsWithAutomatedSourceControlFeedbackDisabled =
        applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled();

    assertThat(appsWithAutomatedSourceControlFeedbackDisabled)
        .isEmpty();
  }
}
