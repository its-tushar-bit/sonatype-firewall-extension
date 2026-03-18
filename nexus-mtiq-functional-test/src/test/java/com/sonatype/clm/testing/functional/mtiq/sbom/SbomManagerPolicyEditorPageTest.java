/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.sbom;

import com.codeborne.selenide.Selenide;
import com.sonatype.clm.testing.functional.elements.OwnerDetailSidebar;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection;

import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection.allRadioTextSbomManager;

import com.codeborne.selenide.WebDriverRunner;

public class SbomManagerPolicyEditorPageTest
    extends AbstractMtiqFunctionalTest
{
  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  protected static final String COMPLIANCE = new ComplianceStageType().getId();

  private Organization rootOrganization;

  private Organization org1;

  private Application app1;

  private PolicyDAO policyDAO;

  @Before
  public void init() {
    setFeatures(LicensedFeature.POLICY_MANAGEMENT, LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_GRANDFATHERING, LicensedFeature.ENFORCEMENT, LicensedFeature.SBOM_MANAGER,
        LicensedFeature.ORGS_AND_APPS, LicensedFeature.NOTIFICATIONS);
    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(true);
    policyDAO = lookup(PolicyDAO.class);
    rootOrganization = lookup(OrganizationDAO.class).getById(ROOT_ORGANIZATION_ID);
    org1 = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    app1 = tempEntity.newApplication(YE_OLE_ORGANIZATION + "_app1", "app1", org1.getId());
    createPolicy(rootOrganization.getId(), "rootOrgPolicy1");
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Test
  public void testEditPolicyPage_RootOrgRendersLocalPolicySuccessfully() {
    logout();
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    loginAsAdmin();
    refreshOrOpen(OwnerSummaryPage.sbomManagerUrl(OwnerType.ORGANIZATION, ROOT_ORGANIZATION_ID));
    OwnerSummaryPage.policyTile().localPolicyList().row(1).click();
    Assert.assertTrue(WebDriverRunner.getWebDriver().getCurrentUrl().contains("/sbomManager"));

    summarySectionElementsAreDisabled();
    inheritanceSectionIsRenderedCorrectly(rootOrganization.getName());
    constraintSectionElementsAreDisabled();
    PolicyEditorPage.actionsSection().title().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().table().shouldNotBe(visible);
    PolicyEditorPage.notificationsSection()
        .headers()
        .get(0)
        .shouldBe(visible)
        .shouldHave(text(COMPLIANCE));
    PolicyEditorPage.deleteButton().shouldBe(hidden);
    OwnerDetailSidebar.policyGroup().shouldBe(visible);
  }

  @Test
  public void testEditPolicyPage_OrgRendersInheritedPolicySuccessfully() {
    logout();
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    loginAsAdmin();
    refreshOrOpen(OwnerSummaryPage.sbomManagerUrl(OwnerType.ORGANIZATION, org1.getId()));
    OwnerSummaryPage.policyTile().policyList(1).row(1).click();
    inheritanceSectionIsRenderedCorrectly(rootOrganization.getName());
    testOrgRendersCorrectly();
  }

  @Test
  public void testEditPolicyPage_OrgRendersLocalPolicySuccessfully() {
    logout();
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    loginAsAdmin();
    createPolicy(org1.getId(), "orgPolicy1");
    refreshOrOpen(OwnerSummaryPage.sbomManagerUrl(OwnerType.ORGANIZATION, org1.getId()));
    OwnerSummaryPage.policyTile().policyList(0).row(1).click();
    inheritanceSectionIsRenderedCorrectly(org1.getName());
    testOrgRendersCorrectly();
  }

  @Test
  public void testEditPolicyPage_AppRendersInheritedPolicySuccessfully() {
    logout();
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    loginAsAdmin();
    refreshOrOpen(OwnerSummaryPage.sbomManagerUrl(OwnerType.APPLICATION, "app1"));
    OwnerSummaryPage.policyTile().policyList(2).row(1).click();

    inheritanceSectionIsRenderedCorrectly(rootOrganization.getName());
    testAppRendersCorrectly();
  }

  @Test
  public void testEditPolicyPage_AppRendersLocalPolicySuccessfully() {
    logout();
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    loginAsAdmin();
    createPolicy(app1.getId(), "appPolicy1");
    refreshOrOpen(OwnerSummaryPage.sbomManagerUrl(OwnerType.APPLICATION, "app1"));
    OwnerSummaryPage.policyTile().policyList(0).row(1).click();

    PolicyEditorPage.inheritanceSection().shouldNotBe(visible);
    testAppRendersCorrectly();
  }

  @Test
  public void testEditPolicyPage_appPolicyRedirectsLifecycleSuccessfullyWithSbomLicenseOnly() {
    logout();
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    loginAsAdmin();
    refreshOrOpen(OwnerSummaryPage.sbomManagerUrl(OwnerType.APPLICATION, "app1"));
    OwnerSummaryPage.policyTile().policyList(2).row(1).click();
    Assert.assertTrue(WebDriverRunner.getWebDriver().getCurrentUrl().contains("/sbomManager"));

    String href = PolicyEditorPage.linkToLifecycle().getAttribute("href");
    Assert.assertEquals(href, "https://links.sonatype.com/nexus-lifecycle-sbom");
  }

  @Test
  public void testEditPolicyPage_orgPolicyRedirectsToLifecycleSuccessfullyWithSbomLicenseOnly() {
    logout();
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    loginAsAdmin();
    refreshOrOpen(OwnerSummaryPage.sbomManagerUrl(OwnerType.ORGANIZATION, org1.getId()));
    OwnerSummaryPage.policyTile().policyList(1).row(1).click();
    Assert.assertTrue(WebDriverRunner.getWebDriver().getCurrentUrl().contains("/sbomManager"));

    String href = PolicyEditorPage.linkToLifecycle().getAttribute("href");
    Assert.assertEquals(href, "https://links.sonatype.com/nexus-lifecycle-sbom");
  }

  @Test
  public void testEditPolicyPage_orgPolicyRedirectsToLifecycleSuccessfullyWithSbomAndLifecycleLicense() {
    logout();
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    loginAsAdmin();
    refreshOrOpen(OwnerSummaryPage.sbomManagerUrl(OwnerType.ORGANIZATION, org1.getId()));
    OwnerSummaryPage.policyTile().policyList(1).row(1).click();
    Assert.assertTrue(WebDriverRunner.getWebDriver().getCurrentUrl().contains("/sbomManager"));

    // Wait for page to fully load.
    Selenide.sleep(1000);
    String href = PolicyEditorPage.linkToLifecycle().getAttribute("href");
    String expectedPath = "iq-test/assets/index.html#/management/edit/organization/" + org1.getId() + "/policy/";
    Assert.assertTrue(href.contains(expectedPath));
  }

  @Test
  public void testEditPolicyPage_appPolicyRedirectsToLifecycleSuccessfullyWithSbomAndLifecycleLicense() {
    logout();
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    loginAsAdmin();
    refreshOrOpen(OwnerSummaryPage.sbomManagerUrl(OwnerType.APPLICATION, "app1"));
    OwnerSummaryPage.policyTile().policyList(2).row(1).click();
    Assert.assertTrue(WebDriverRunner.getWebDriver().getCurrentUrl().contains("/sbomManager"));

    // Wait for page to fully load.
    Selenide.sleep(1000);
    String href = PolicyEditorPage.linkToLifecycle().getAttribute("href");
    String expectedPath = "iq-test/assets/index.html#/management/edit/application/app1/policy/";
    Assert.assertTrue(href.contains(expectedPath));
  }

  private void summarySectionElementsAreDisabled() {
    SummarySection summarySection = PolicyEditorPage.summarySection();
    summarySection.policyName().input().shouldBe(visible, disabled);
    summarySection.threatLevel().shouldHave(cssClass("disabled"));
    summarySection.legacyViolationTitle().shouldBe(hidden);
    summarySection.legacyViolationCheckbox().shouldBe(hidden);
  }

  private void inheritanceSectionIsRenderedCorrectly(String ownerName) {
    PolicyInheritsToSection inheritanceSection = PolicyEditorPage.inheritanceSection();
    inheritanceSection.allChildrenInheritRadio().shouldBe(visible, disabled);
    inheritanceSection.allChildrenInheritRadio().shouldHave(allRadioTextSbomManager(ownerName));
    inheritanceSection.specifiedChildrenInheritRadio().shouldNotBe(visible);
    inheritanceSection.policyActionsOverrideCheckbox().shouldNotBe(visible);
    inheritanceSection.policyNotificationsOverrideCheckbox().shouldBe(visible);
    inheritanceSection.policyNotificationsOverrideCheckbox()
        .label()
        .shouldHave(
            text("Allow notification overrides at organization and application levels"));
  }

  private void constraintSectionElementsAreDisabled() {
    ConstraintSection constraintsSection = PolicyEditorPage.constraintSection();
    constraintsSection.addConstraintButton().shouldBe(visible, disabled);
    constraintsSection.constraintSummary(0).editConstraintButton().shouldBe(visible, disabled);
    constraintsSection.constraintSummary(0).deleteConstraintButton().shouldBe(visible, disabled);
  }

  private Policy createPolicy(String ownerId, String policyName) {
    Policy policy = tempEntity.newPolicy(ownerId, policyName, 1);
    Constraint constraint1 = new Constraint(policy.getId() + "1", "First Constraint with One Condition", null);
    constraint1.addCondition(new Condition(AgeInDaysConditionType.ID, "older than", "730"));
    policy.setConstraints(Collections.singletonList(constraint1));

    policyDAO.update(policy);
    return policy;
  }

  private void testOrgRendersCorrectly() {
    Assert.assertTrue(WebDriverRunner.getWebDriver().getCurrentUrl().contains("/sbomManager"));

    summarySectionElementsAreDisabled();
    constraintSectionElementsAreDisabled();
    PolicyEditorPage.actionsSection().title().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().table().shouldNotBe(visible);
    PolicyEditorPage.notificationsSection()
        .headers()
        .get(0)
        .shouldBe(visible)
        .shouldHave(text(COMPLIANCE));
    PolicyEditorPage.deleteButton().shouldBe(hidden);
    OwnerDetailSidebar.policyGroup().shouldBe(hidden);
  }

  private void testAppRendersCorrectly() {
    Assert.assertTrue(WebDriverRunner.getWebDriver().getCurrentUrl().contains("/sbomManager"));

    summarySectionElementsAreDisabled();
    constraintSectionElementsAreDisabled();
    PolicyEditorPage.actionsSection().title().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().table().shouldNotBe(visible);
    PolicyEditorPage.notificationsSection()
        .headers()
        .get(0)
        .shouldBe(visible)
        .shouldHave(text(COMPLIANCE));
    PolicyEditorPage.deleteButton().shouldBe(hidden);
    OwnerDetailSidebar.policyGroup().shouldBe(hidden);
  }
}
