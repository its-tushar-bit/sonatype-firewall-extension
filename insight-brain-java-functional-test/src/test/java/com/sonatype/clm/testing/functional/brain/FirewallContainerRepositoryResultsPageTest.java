/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallContainerRepositoryResultsPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.LastPolicyEvaluationDAO;
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
import org.junit.Test;

import static com.codeborne.selenide.Condition.*;

public class FirewallContainerRepositoryResultsPageTest
    extends AbstractFunctionalTest
{
  private final FirewallContainerRepositoryResultsPage containerRepositoryResultsPage =
      new FirewallContainerRepositoryResultsPage();

  private PolicyViolationDAO policyViolationDAO;

  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private LastPolicyEvaluationDAO lastPolicyEvaluationDAO;

  @Test
  public void testContent() throws Exception {
    lastPolicyEvaluationDAO = lookup(LastPolicyEvaluationDAO.class);
    repositoryDAO = lookup(RepositoryDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);

    setFeatures(
        LicensedFeature.CONTAINER_IMAGES_EVALUATION,
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.APPLICATION_REPORTS,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.SUCCESS_METRICS);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    hardreset();
    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();

    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repository-public-id");

    // Repository Organization
    final Organization organization = tempEntity.newOrganization("org1");
    organization.setRelatedRepositoryId(repository.getId());
    repository.setRelatedOrganizationId(organization.getId());
    repositoryDAO.update(repository);
    organizationDAO.update(organization);

    // Container Image Applications
    Application application1 = tempEntity.newApplication("app1", "appPublicId1", organization.getId());
    Application application2 = tempEntity.newApplication("app2", "appPublicId2", organization.getId());

    // Policy Evaluation
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application1.getId(), "proxy", "scanId1");
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), "proxy", "scanId2");

    // Last Policy Evaluation
    lastPolicyEvaluationDAO.getByApplicationIdAndStageTypeId(application1.getId(), "proxy");
    lastPolicyEvaluationDAO.getByApplicationIdAndStageTypeId(application2.getId(), "proxy");

    // Policy for Policy Violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application1.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application1.getId(), "policy6");

    // Create Policy Violations
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEvaluation1, policy4);

    PolicyViolation policyViolation5 = tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation6 = tempEntity.newPolicyViolation(policyEvaluation2, policy6);

    policyViolation1.setThreatLevel(10);
    policyViolation2.setThreatLevel(8);
    policyViolation3.setThreatLevel(10);
    policyViolation4.setThreatLevel(5);
    policyViolation5.setThreatLevel(10);
    policyViolation6.setThreatLevel(2);

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    refreshOrOpen(FirewallContainerRepositoryResultsPage.url(repository.getId()));

    containerRepositoryResultsPage.shouldBe(visible);
    containerRepositoryResultsPage.title().shouldHave(text("Repository Results"));

    FirewallContainerRepositoryResultsPage.ContainerRepositoryResultsTable resultsTable =
        new FirewallContainerRepositoryResultsPage.ContainerRepositoryResultsTable();
    resultsTable.table().shouldBe(visible);
  }
}
