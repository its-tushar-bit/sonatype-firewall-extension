/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.sonatype.clm.testing.functional.pages.DashboardPage.ACTIVE;

public class DashboardTabNavigationTest
    extends AbstractFunctionalTest
{
  private Organization org;

  private Application app;

  private Policy policy;

  private DashboardFilterDAO dashboardFilterDAO;

  private static final List<ComponentData> COMPONENTS = new ArrayList<>();

  private final List<PolicyWaiver> existingWaivers = new ArrayList<>();

  static {
    for (int i = 0; i < 10; i++) {
      COMPONENTS.add(new ComponentData(
          ComponentIdentifier.createMavenCoordinates("group-" + i, "artifact-" + i, Integer.toString(i)),
          Integer.toString(i)));
    }
  }

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.urlToViolations());
    loginAsAdmin();
  }

  @Before
  public void before() {
    setupData();
    clearFilters();
    refreshOrOpen(DashboardPage.urlToViolations());
  }

  public void setupData() {
    dashboardFilterDAO = lookup(DashboardFilterDAO.class);

    org = tempEntity.newOrganization("DashboardPolicySummarySpec");
    app = tempEntity
        .newApplication("DashboardPolicySummarySpecApp", "DashboardPolicySummarySpecApp", org.getPublicId());
    policy = tempEntity.newPolicy(org);

    DateTime now = DateTime.now();
    for (int weeksAgo = 12; weeksAgo >= 0; weeksAgo--) {
      DateTime time = now.minusWeeks(weeksAgo).minusDays(2);
      switch (weeksAgo) {
        case 12: // introduce 3 violations outside the bounds of the 12 week delta to start with
          PolicyEvaluation seedEval = tempEntity
              .newPolicyEvaluation(app.getId(), BuildStageType.ID, "SeedEval", time.toDate());
          createViolations(seedEval, COMPONENTS.subList(0, 3));
          break;
        case 11:
          // introduce 2 new violations
          PolicyEvaluation twelthWeekEval =
              tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "twelthWeekEval", time.toDate());
          createViolations(twelthWeekEval, COMPONENTS.subList(0, 5));
          break;
        case 10:
        case 9: // nothing happens these weeks
          break;
        case 8: // fix an issue and introduce 2 new ones
          PolicyEvaluation ninthWeekEval =
              tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "ninthWeekEval", time.toDate());
          createViolations(ninthWeekEval, COMPONENTS.subList(1, 7));
          break;
        case 7: // Waive 3 violations
          PolicyEvaluation eigthWeekEval =
              tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "eightWeekEval", time.toDate());
          createViolations(eigthWeekEval, COMPONENTS.subList(4, 7));
          createWaivedViolations(eigthWeekEval, COMPONENTS.subList(1, 4));
          break;
        case 6: // nothing happens this weeks
          break;
        case 5: // Fix one waived violation
          PolicyEvaluation fifthWeekEval =
              tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "fifthWeekEval", time.toDate());
          createViolations(fifthWeekEval, COMPONENTS.subList(4, 7));
          createWaivedViolations(fifthWeekEval, COMPONENTS.subList(2, 4));
          break;
        case 4: // find one, fix one
          PolicyEvaluation fourthWeekEval =
              tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "fourthWeekEval", time.toDate());
          createViolations(fourthWeekEval, COMPONENTS.subList(5, 8));
          createWaivedViolations(fourthWeekEval, COMPONENTS.subList(2, 4));
          break;
        case 3: // fix two, fix one waived violation
          PolicyEvaluation thirdWeekEval =
              tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "thirdWeekEval", time.toDate());
          createViolations(thirdWeekEval, COMPONENTS.subList(7, 8));
          createWaivedViolations(thirdWeekEval, COMPONENTS.subList(3, 4));
          break;
        case 2: // find one, fix one
          PolicyEvaluation secondWeekEval =
              tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "secondWeekEval", time.toDate());
          createViolations(secondWeekEval, COMPONENTS.subList(8, 9));
          createWaivedViolations(secondWeekEval, COMPONENTS.subList(3, 4));
          break;
        case 1: // nothing happens this week
          break;
        case 0: // find one, fix one
          PolicyEvaluation thisWeekEval =
              tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "thisWeekEval", time.toDate());
          createViolations(thisWeekEval, COMPONENTS.subList(9, 10));
          createWaivedViolations(thisWeekEval, COMPONENTS.subList(3, 4));
          break;
        default:
      }
    }
  }

  private void createViolations(PolicyEvaluation evaluation, List<ComponentData> components) {
    for (ComponentData component : components) {
      tempEntity.newPolicyViolation(evaluation, policy, component.componentIdentifier, component.hash, "");
    }
  }

  private void createWaivedViolations(PolicyEvaluation evaluation, List<ComponentData> components) {
    for (ComponentData component : components) {
      PolicyWaiver waiver = findExistingWaiver(component.hash);
      if (waiver == null) {
        waiver = tempEntity.newWaiver(component.hash, policy.getId(), app.getId());
        existingWaivers.add(waiver);
      }
      tempEntity.newWaivedPolicyViolation(evaluation, policy, component.componentIdentifier, component.hash,
          waiver);
    }
  }

  private PolicyWaiver findExistingWaiver(final String hash) {
    for (PolicyWaiver waiver : existingWaivers) {
      if (waiver.getHash().equals(hash)) {
        return waiver;
      }
    }
    return null;
  }

  @Test
  public void testTabNavigation() {
    waitUntilUrl(DashboardPage.urlToViolations());
    DashboardPage.violationsTab().shouldBe(ACTIVE);
    DashboardPage.applicationsTab().shouldNotBe(ACTIVE);
    DashboardPage.waiversTab().shouldNotBe(ACTIVE);

    DashboardPage.componentsTab().shouldNotBe(ACTIVE).click();
    waitUntilUrl(DashboardPage.urlToComponents());
    DashboardPage.componentsTab().shouldBe(ACTIVE);

    DashboardPage.applicationsTab().shouldNotBe(ACTIVE).click();
    waitUntilUrl(DashboardPage.urlToApplications());
    DashboardPage.applicationsTab().shouldBe(ACTIVE);

    DashboardPage.waiversTab().shouldNotBe(ACTIVE).click();
    waitUntilUrl(DashboardPage.urlToWaivers());
    DashboardPage.waiversTab().shouldBe(ACTIVE);

    DashboardPage.violationsTab().shouldNotBe(ACTIVE).click();
    waitUntilUrl(DashboardPage.urlToViolations());
    DashboardPage.violationsTab().shouldBe(ACTIVE);

    Selenide.back();
    waitUntilUrl(DashboardPage.urlToWaivers());
    DashboardPage.waiversTab().shouldBe(ACTIVE);

    Selenide.back();
    waitUntilUrl(DashboardPage.urlToApplications());
    DashboardPage.applicationsTab().shouldBe(ACTIVE);

    Selenide.back();
    waitUntilUrl(DashboardPage.urlToComponents());
    DashboardPage.componentsTab().shouldBe(ACTIVE);

    Selenide.back();
    waitUntilUrl(DashboardPage.urlToViolations());
    DashboardPage.violationsTab().shouldBe(ACTIVE);
  }

  @Test
  public void testTabNavigation_FirewallOnyLicense() {
    uninstallLicense();
    testProductLicense.reset();
    refresh();

    setLicensedProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);

    refreshOrOpen(DashboardPage.urlToWaivers());

    ElementsCollection tabs = new DashboardPage().tabs();
    tabs.shouldHave(size(1));
    tabs.get(0).shouldHave(Condition.text("Waivers"));
  }

  private void clearFilters() {
    dashboardFilterDAO.deleteByUsernameAndRealmId(User.ADMIN_USERNAME, InternalRealm.ID);
  }

  private static class ComponentData
  {
    final ComponentIdentifier componentIdentifier;

    final String hash;

    public ComponentData(ComponentIdentifier componentIdentifier, String hash) {
      this.componentIdentifier = componentIdentifier;
      this.hash = hash;
    }
  }
}
