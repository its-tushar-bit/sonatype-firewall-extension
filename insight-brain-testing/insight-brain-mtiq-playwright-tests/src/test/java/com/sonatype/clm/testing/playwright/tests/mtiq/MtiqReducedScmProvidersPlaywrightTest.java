/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.pages.MtiqSourceControlConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SourceControlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SourceControlRegressionPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.Organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * In MTIQ the SCM provider dropdown is filtered by
 * {@code productFeaturesSelectors.js#MULTI_TENANT_SCM_PROVIDERS} to exactly four entries:
 * Azure DevOps, Bitbucket, GitHub, GitLab. Self-hosted, GitHub Enterprise, and Gitea variants
 * are absent. The constraint is frontend-only — no backend feature flag controls it.
 */
@Tag("mtiq")
public class MtiqReducedScmProvidersPlaywrightTest
    extends AbstractMtiqSourceControlEditorPlaywrightTest
{
  private SourceControlConfigurationPage scmPage;

  private MtiqSourceControlConfigurationPageAssertions scmAssertions;

  private SourceControlRegressionPage scm;

  @BeforeEach
  public void createOrgAndNavigateToScmEditor() {
    Organization testOrg = tempEntity.newOrganization();
    init(testOrg);
    scm = new SourceControlRegressionPage();
    navigateToOrgSourceControlEditor(testOrg.getId());
    scmPage = new SourceControlConfigurationPage();
    scmAssertions = new MtiqSourceControlConfigurationPageAssertions(scmPage);
  }

  @Override
  protected SourceControlRegressionPage scm() {
    return scm;
  }

  @Override
  protected void navigateToEditor() {
    navigateToOrgSourceControlEditor(currentOwner.getId());
  }

  @Test
  public void testMtiqReducedScmProviders_providerDropdownListsOnlyFourProviders() {
    assertThat(scmPage.pageHeading()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    scmAssertions.shouldListOnlyMtiqProviders();
  }
}
