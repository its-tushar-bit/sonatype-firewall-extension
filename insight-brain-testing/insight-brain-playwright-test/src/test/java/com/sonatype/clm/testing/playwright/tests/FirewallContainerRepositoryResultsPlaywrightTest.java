/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.FirewallContainerRepositoryResultsPage;
import com.sonatype.clm.testing.playwright.pages.FirewallContainerRepositoryResultsPageAssertions;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright test for the Firewall Container Repository Results page.
 */
public class FirewallContainerRepositoryResultsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String REPOSITORY_PUBLIC_ID = "repository-public-id";

  private static final String EXPECTED_TITLE_TEXT = "Repository Results";

  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private PolicyViolationDAO policyViolationDAO;

  private String repositoryId;

  @Before
  public void openContainerRepositoryResultsAsAdmin() {
    setFeatures(
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.FIREWALL,
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.DASHBOARD,
        LicensedFeature.WAIVERS_DASHBOARD,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION,
        LicensedFeature.SUCCESS_METRICS);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    repositoryDAO = lookup(RepositoryDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);
    seedRepositoryWithApplicationsAndViolations();

    // hard-reset ensures the embedded server picks up the feature flag changes before navigation
    playwrightHardreset();
    playwrightRefreshOrOpen(FirewallContainerRepositoryResultsPage.url(repositoryId));
    playwrightLogin();
  }

  @After
  public void cleanup() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
    reverseProxyServer.reset();
  }

  @Test
  @Category(SanityTest.class)
  public void testContent() {
    FirewallContainerRepositoryResultsPage resultsPage =
        new FirewallContainerRepositoryResultsPage();
    FirewallContainerRepositoryResultsPageAssertions resultsAssertions =
        new FirewallContainerRepositoryResultsPageAssertions(resultsPage);
    resultsAssertions.shouldBeVisible();
    resultsAssertions.shouldShowTitle(EXPECTED_TITLE_TEXT);
    resultsAssertions.shouldShowResultsTable();
  }

  private void seedRepositoryWithApplicationsAndViolations() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    repositoryId = repository.getId();

    Organization organization = tempEntity.newOrganization("org1");
    organization.setRelatedRepositoryId(repository.getId());
    repository.setRelatedOrganizationId(organization.getId());
    repositoryDAO.update(repository);
    organizationDAO.update(organization);

    Application application1 = tempEntity.newApplication("app1", "appPublicId1", organization.getId());
    Application application2 = tempEntity.newApplication("app2", "appPublicId2", organization.getId());

    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(application1.getId(), "proxy", "scanId1");
    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), "proxy", "scanId2");

    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    PolicyViolation pv1 = tempEntity.newPolicyViolation(evaluation1, policy1);
    PolicyViolation pv2 = tempEntity.newPolicyViolation(evaluation1, policy2);
    PolicyViolation pv3 = tempEntity.newPolicyViolation(evaluation1, policy3);
    PolicyViolation pv4 = tempEntity.newPolicyViolation(evaluation1, policy4);
    PolicyViolation pv5 = tempEntity.newPolicyViolation(evaluation2, policy5);
    PolicyViolation pv6 = tempEntity.newPolicyViolation(evaluation2, policy6);

    pv1.setThreatLevel(10);
    policyViolationDAO.update(pv1);
    pv2.setThreatLevel(8);
    policyViolationDAO.update(pv2);
    pv3.setThreatLevel(10);
    policyViolationDAO.update(pv3);
    pv4.setThreatLevel(5);
    policyViolationDAO.update(pv4);
    pv5.setThreatLevel(10);
    policyViolationDAO.update(pv5);
    pv6.setThreatLevel(2);
    policyViolationDAO.update(pv6);
  }
}
