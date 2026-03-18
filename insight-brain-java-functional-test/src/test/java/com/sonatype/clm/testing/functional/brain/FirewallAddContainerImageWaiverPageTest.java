/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.AddContainerImageWaiverPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallAddContainerImageWaiverPageTest
    extends AbstractFunctionalTest
{
  private final AddContainerImageWaiverPage addContainerImageWaiverPage = new AddContainerImageWaiverPage();

  private final FirewallPage firewallPage = new FirewallPage();

  private Application app;

  private Policy policy;

  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private PolicyEvaluation policyEvaluation;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    setFeatures(
        LicensedFeature.CONTAINER_IMAGES_EVALUATION,
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.APPLICATION_REPORTS);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    repositoryDAO = lookup(RepositoryDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);

    Organization org = tempEntity.newOrganization();
    app = tempEntity.newApplication("ContainerReportTest", "ContainerReportTest", org.getId());
    policy = tempEntity.newPolicy(app.getId());
    policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ProxyStageType.ID, "scan1");
    TreeMap<String, String> coordinates = new TreeMap<>()
    {
      {
        this.put("name", "apk-tools");
        this.put("namespace", "alpine:3.6.5");
        this.put("version", "2.7.6-r0");
      }
    };
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("container", coordinates);
    tempEntity.newPolicyViolation(policyEvaluation, policy, 9, PolicyThreatCategory.SECURITY,
        componentIdentifier, "hash1", FailActionType.ID);
    tempEntity.newPolicyViolation(policyEvaluation, policy, 6, PolicyThreatCategory.SECURITY,
        componentIdentifier, "hash2", FailActionType.ID);
    tempEntity.newPolicyViolation(policyEvaluation, policy, 3, PolicyThreatCategory.SECURITY,
        componentIdentifier, "hash3", FailActionType.ID);
    Repository repository =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.proxy, "docker");
    repository.setRelatedOrganizationId(org.getId());
    repositoryDAO.update(repository);
    org.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(org);

    refreshOrOpen(AddContainerImageWaiverPage.url(app.getPublicId(), "scan1"));
  }

  @After
  public void cleanup() {
    for (PolicyWaiver waiver : policyWaiverDAO.getActiveByOwnerId(app.getId())) {
      policyWaiverDAO.delete(waiver);
    }
  }

  @Test
  public void testInitialContent() {
    refreshOrOpen(AddContainerImageWaiverPage.url(app.getPublicId(), "scan1"));

    addContainerImageWaiverPage.shouldBe(visible);
    addContainerImageWaiverPage.pageTitle().shouldHave(text("Add Waiver"));
    addContainerImageWaiverPage.waiverFormHeader().shouldHave(text("Waiver Configuration"));
    addContainerImageWaiverPage.threatCounter("critical").shouldHave(text("1"));
    addContainerImageWaiverPage.threatCounter("severe").shouldHave(text("1"));
    addContainerImageWaiverPage.threatCounter("moderate").shouldHave(text("1"));
    addContainerImageWaiverPage.violationText().shouldHave(text("3 FAIL VIOLATION"));
    addContainerImageWaiverPage.violationSubText().shouldHave(text("Affecting 1 component"));
    addContainerImageWaiverPage.infoAlert().shouldHave(text("""
        Proceeding to create a waiver will waive all failing policy violations identified in this evaluation.
        After applying this waiver, you can review waived policy violations per component within the
        Container Image Report."""));
    addContainerImageWaiverPage.policyLabel().shouldHave(text("Policies"));
    addContainerImageWaiverPage.policyValue().shouldHave(text(policy.getName()));
    addContainerImageWaiverPage.containerImageLabel().shouldHave(text("alpine"));
    addContainerImageWaiverPage.containerImageValue().shouldHave(text("alpine : 3.6.5"));

    addContainerImageWaiverPage.expiryTimesSelect().shouldBe(visible);
    addContainerImageWaiverPage.expiryTimesSelect().getSelectedOption().shouldHave(text("Never"));
    addContainerImageWaiverPage.expiryTimesOptions().shouldHave(size(8));
    addContainerImageWaiverPage.expiryTimesOptions().get(0).shouldHave(text("Never"));
    addContainerImageWaiverPage.expiryTimesOptions().get(1).shouldHave(text("7 Days"));
    addContainerImageWaiverPage.expiryTimesOptions().get(2).shouldHave(text("14 Days"));
    addContainerImageWaiverPage.expiryTimesOptions().get(3).shouldHave(text("30 Days"));
    addContainerImageWaiverPage.expiryTimesOptions().get(4).shouldHave(text("60 Days"));
    addContainerImageWaiverPage.expiryTimesOptions().get(5).shouldHave(text("90 Days"));
    addContainerImageWaiverPage.expiryTimesOptions().get(6).shouldHave(text("120 Days"));
    addContainerImageWaiverPage.expiryTimesOptions().get(7).shouldHave(text("Custom"));

    addContainerImageWaiverPage.waiverReasonSelect().shouldBe(visible);
    addContainerImageWaiverPage.waiverReasonSelect().getSelectedOption().shouldHave(text("Select a reason"));
    addContainerImageWaiverPage.waiverReasonOptions().get(0).shouldHave(text("Select a reason"));
    addContainerImageWaiverPage.waiverReasonOptions().get(1).shouldHave(text("Acknowledged violation"));
    addContainerImageWaiverPage.waiverReasonOptions().get(2).shouldHave(text("Evaluating component"));
    addContainerImageWaiverPage.waiverReasonOptions().get(3).shouldHave(text("Mitigated externally"));
    addContainerImageWaiverPage.waiverReasonOptions().get(4).shouldHave(text("No upgrade path"));
    addContainerImageWaiverPage.waiverReasonOptions().get(5).shouldHave(text("Not exploitable"));
    addContainerImageWaiverPage.waiverReasonOptions().get(6).shouldHave(text("Not reachable"));
    addContainerImageWaiverPage.waiverReasonOptions().get(7).shouldHave(text("Researching"));
    addContainerImageWaiverPage.waiverReasonOptions().get(8).shouldHave(text("Other"));

    addContainerImageWaiverPage.comments().shouldBe(visible);
    addContainerImageWaiverPage.cancel().shouldBe(visible);
    addContainerImageWaiverPage.submit().shouldBe(visible);
  }

  @Test
  public void testUnsavedChangesModal() {
    refreshOrOpen(AddContainerImageWaiverPage.url(app.getPublicId(), "scan1"));

    addContainerImageWaiverPage.expiryTimesSelect().click();
    addContainerImageWaiverPage.expiryTimesOptions().get(1).click();

    refreshOrOpen(FirewallPage.url());

    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.cancelButton().click();

    firewallPage.shouldNotBe(visible);

    refreshOrOpen(FirewallPage.url());

    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.continueButton().click();

    firewallPage.shouldBe(visible);
  }

  @Test
  public void testAddWaiverWithExpiryTime() {
    refreshOrOpen(AddContainerImageWaiverPage.url(app.getPublicId(), "scan1"));

    addContainerImageWaiverPage.expiryTimesSelect().selectOptionContainingText("7 Days");
    LocalDateTime endOfDay = LocalDate.now().plusDays(7).atTime(23, 59, 59, 999_000_000);
    Date expectedExpiryTime = Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());

    addContainerImageWaiverPage.daysDiffMessage().shouldHave(text("This waiver will expire in 7 days"));
    addContainerImageWaiverPage.submit().click();
    FormMask.seeAndWaitForDismissal();

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers.size()).isEqualTo(4);
    for (PolicyWaiver waiver : waivers) {
      assertThat(waiver.getExpiryTime()).isEqualTo(expectedExpiryTime);
      assertThat(waiver.getWaiverReasonId()).isNull();
      assertThat(waiver.getComment()).isEmpty();
    }
  }

  @Test
  public void testAddWaiverWithCustomExpiryTime() {
    refreshOrOpen(AddContainerImageWaiverPage.url(app.getPublicId(), "scan1"));

    addContainerImageWaiverPage.expiryTimesSelect().selectOptionContainingText("Custom");
    addContainerImageWaiverPage.customExpiryTime().setValue("01:01:2000");
    addContainerImageWaiverPage.customExpiryTimeValidationMessage().shouldHave(text("Date must be in the future"));

    addContainerImageWaiverPage.customExpiryTime().setValue("01:01:9999");
    addContainerImageWaiverPage.customExpiryTimeValidationMessage().shouldNotBe(visible);

    addContainerImageWaiverPage.submit().click();
    FormMask.seeAndWaitForDismissal();

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers.size()).isEqualTo(4);
    for (PolicyWaiver waiver : waivers) {
      assertThat(waiver.getExpiryTime()).isEqualTo("9999-01-01T00:00:00.000");
      assertThat(waiver.getWaiverReasonId()).isNull();
      assertThat(waiver.getComment()).isEmpty();
    }
  }

  @Test
  public void testAddWaiverWithWaiverReason() {
    refreshOrOpen(AddContainerImageWaiverPage.url(app.getPublicId(), "scan1"));
    addContainerImageWaiverPage.shouldBe(visible);

    addContainerImageWaiverPage.waiverReasonSelect().selectOptionContainingText("Acknowledged violation");
    addContainerImageWaiverPage.submit().click();
    FormMask.seeAndWaitForDismissal();

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers.size()).isEqualTo(4);
    for (PolicyWaiver waiver : waivers) {
      assertThat(waiver.getExpiryTime()).isNull();
      assertThat(waiver.getWaiverReasonId())
          .isEqualTo("9b704ef5bc064fc29d7fe08a251ee9a6");
      assertThat(waiver.getComment()).isEmpty();
    }
  }

  @Test
  public void testAddWaiverWithWaiverComment() {
    refreshOrOpen(AddContainerImageWaiverPage.url(app.getPublicId(), "scan1"));

    addContainerImageWaiverPage.comments().setValue("Test comment for waiver");
    addContainerImageWaiverPage.submit().click();
    FormMask.seeAndWaitForDismissal();

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers.size()).isEqualTo(4);
    for (PolicyWaiver waiver : waivers) {
      assertThat(waiver.getExpiryTime()).isNull();
      assertThat(waiver.getWaiverReasonId()).isNull();
      assertThat(waiver.getComment()).isEqualTo("Test comment for waiver");
    }
  }

  @Test
  public void testAddWaiverWithoutAddingFields() {
    refreshOrOpen(AddContainerImageWaiverPage.url(app.getPublicId(), "scan1"));

    addContainerImageWaiverPage.submit().click();
    FormMask.seeAndWaitForDismissal();

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers.size()).isEqualTo(4);
    for (PolicyWaiver waiver : waivers) {
      assertThat(waiver.getExpiryTime()).isNull();
      assertThat(waiver.getWaiverReasonId()).isNull();
      assertThat(waiver.getComment()).isEmpty();
    }
  }
}
