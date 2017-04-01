/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.TrendRow;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.TrendsModal;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Selenide;
import com.google.common.base.Predicate;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.elements.TrendDelta.DOWN;
import static com.sonatype.clm.testing.functional.elements.TrendDelta.UP;
import static com.sonatype.clm.testing.functional.pages.DashboardPage.ACTIVE;
import static com.sonatype.clm.testing.functional.pages.DashboardPage.APPLICATIONS_URL;
import static com.sonatype.clm.testing.functional.pages.DashboardPage.COMPONENTS_URL;
import static com.sonatype.clm.testing.functional.pages.DashboardPage.VIOLATIONS_URL;
import static com.sonatype.clm.testing.functional.pages.TrendsModal.INVERSE;
import static com.sonatype.clm.testing.functional.pages.TrendsModal.NATURAL;
import static com.sonatype.clm.testing.functional.pages.TrendsModal.NEUTRAL;

public class DashboardTabNavigationAndTrendsTest
    extends AbstractFunctionalTest
{
  private static Organization org;

  private static Application app;

  private static Policy policy;

  private static List<ComponentData> COMPONENTS = new ArrayList<>();

  static final List<PolicyWaiver> existingWaivers = new ArrayList<>();

  static {
    for (int i = 0; i < 10; i++) {
      COMPONENTS.add(new ComponentData(
          ComponentIdentifier.createMavenCoordinates("group-" + i, "artifact-" + i, Integer.toString(i)),
          Integer.toString(i)));
    }
  }

  static final String[] NEW_ROW_DELTAS = {"2", "0", "0", "2", "0", "0", "0", "1", "0", "1", "0", "1"};

  static final String[] FIXED_ROW_DELTAS = {"0", "0", "0", "1", "0", "0", "1", "1", "3", "1", "0", "1"};

  static final String[] UNRESOLVED_ROW_DELTAS = {"2", "0", "0", "1", "-3", "0", "0", "0", "-2", "0", "0", "0"};

  static final String[] WAIVED_ROW_DELTAS = {"0", "0", "0", "0", "3", "0", "-1", "0", "-1", "0", "0", "0"};

  @BeforeClass
  public static void beforeClass() throws Exception {
    setupData();
    refreshOrOpen(DashboardPage.VIOLATIONS_URL);
    loginAsAdmin();
  }

  @Before
  public void before() {
    clearFilters();
    refreshOrOpen(DashboardPage.VIOLATIONS_URL);
  }

  public static void setupData() {
    org = staticTempEntity.newOrganization("DashboardPolicySummarySpec");
    app = staticTempEntity
        .newApplication("DashboardPolicySummarySpecApp", "DashboardPolicySummarySpecApp", org.getPublicId());
    policy = staticTempEntity.newPolicy(org.getPublicId(), "DashboardPolicySummarySpec");

    DateTime now = DateTime.now();
    for (int weeksAgo = 12; weeksAgo >= 0; weeksAgo--) {
      DateTime time = now.minusWeeks(weeksAgo).minusDays(2);
      switch (weeksAgo) {
        case 12: // introduce 3 violations outside the bounds of the 12 week delta to start with
          PolicyEvaluation seedEval = staticTempEntity
              .newPolicyEvaluation(app.getId(), BuildStageType.ID, "SeedEval", time.toDate());
          createViolations(seedEval, COMPONENTS.subList(0, 3));
          break;
        case 11:
          // introduce 2 new violations
          PolicyEvaluation twelthWeekEval = staticTempEntity.
              newPolicyEvaluation(app.getId(), BuildStageType.ID, "twelthWeekEval", time.toDate());
          createViolations(twelthWeekEval, COMPONENTS.subList(0, 5));
          break;
        case 10:
        case 9: // nothing happens these weeks
          break;
        case 8: // fix an issue and introduce 2 new ones
          PolicyEvaluation ninthWeekEval = staticTempEntity.
              newPolicyEvaluation(app.getId(), BuildStageType.ID, "ninthWeekEval", time.toDate());
          createViolations(ninthWeekEval, COMPONENTS.subList(1, 7));
          break;
        case 7: // Waive 3 violations
          PolicyEvaluation eigthWeekEval = staticTempEntity.
              newPolicyEvaluation(app.getId(), BuildStageType.ID, "eightWeekEval", time.toDate());
          createViolations(eigthWeekEval, COMPONENTS.subList(4, 7));
          createWaivedViolations(eigthWeekEval, COMPONENTS.subList(1, 4));
          break;
        case 6: // nothing happens this weeks
          break;
        case 5: // Fix one waived violation
          PolicyEvaluation fifthWeekEval = staticTempEntity.
              newPolicyEvaluation(app.getId(), BuildStageType.ID, "fifthWeekEval", time.toDate());
          createViolations(fifthWeekEval, COMPONENTS.subList(4, 7));
          createWaivedViolations(fifthWeekEval, COMPONENTS.subList(2, 4));
          break;
        case 4: // find one, fix one
          PolicyEvaluation fourthWeekEval = staticTempEntity.
              newPolicyEvaluation(app.getId(), BuildStageType.ID, "fourthWeekEval", time.toDate());
          createViolations(fourthWeekEval, COMPONENTS.subList(5, 8));
          createWaivedViolations(fourthWeekEval, COMPONENTS.subList(2, 4));
          break;
        case 3: // fix two, fix one waived violation
          PolicyEvaluation thirdWeekEval = staticTempEntity.
              newPolicyEvaluation(app.getId(), BuildStageType.ID, "thirdWeekEval", time.toDate());
          createViolations(thirdWeekEval, COMPONENTS.subList(7, 8));
          createWaivedViolations(thirdWeekEval, COMPONENTS.subList(3, 4));
          break;
        case 2: // find one, fix one
          PolicyEvaluation secondWeekEval = staticTempEntity.
              newPolicyEvaluation(app.getId(), BuildStageType.ID, "secondWeekEval", time.toDate());
          createViolations(secondWeekEval, COMPONENTS.subList(8, 9));
          createWaivedViolations(secondWeekEval, COMPONENTS.subList(3, 4));
          break;
        case 1: // nothing happens this week
          break;
        case 0: // find one, fix one
          PolicyEvaluation thisWeekEval = staticTempEntity.
              newPolicyEvaluation(app.getId(), BuildStageType.ID, "thisWeekEval", time.toDate());
          createViolations(thisWeekEval, COMPONENTS.subList(9, 10));
          createWaivedViolations(thisWeekEval, COMPONENTS.subList(3, 4));
          break;
      }
    }
  }

  private static void createViolations(PolicyEvaluation evaluation, List<ComponentData> components) {
    for (ComponentData component : components) {
      staticTempEntity.newPolicyViolation(evaluation, policy, component.componentIdentifier, component.hash, "");
    }
  }

  private static void createWaivedViolations(PolicyEvaluation evaluation, List<ComponentData> components) {
    for (ComponentData component : components) {
      PolicyWaiver waiver = findExistingWaiver(component.hash);
      if (waiver == null) {
        waiver = staticTempEntity.newWaiver(component.hash, policy.getId(), app.getId());
        existingWaivers.add(waiver);
      }
      staticTempEntity.newWaivedPolicyViolation(evaluation, policy, component.componentIdentifier, component.hash,
          waiver);
    }
  }

  private static PolicyWaiver findExistingWaiver(final String hash) {
    for (PolicyWaiver waiver : existingWaivers) {
      if (waiver.getHash().equals(hash)) {
        return waiver;
      }
    }
    return null;
  }

  @Test
  public void testTabNavigation() {
    waitUntilUrl(VIOLATIONS_URL);
    DashboardPage.violationsTab().shouldBe(ACTIVE);
    DashboardPage.applicationsTab().shouldNotBe(ACTIVE);
    DashboardPage.componentsTab().shouldNotBe(ACTIVE).click();
    waitUntilUrl(COMPONENTS_URL);
    DashboardPage.componentsTab().shouldBe(ACTIVE);

    DashboardPage.applicationsTab().shouldNotBe(ACTIVE).click();
    waitUntilUrl(APPLICATIONS_URL);
    DashboardPage.applicationsTab().shouldBe(ACTIVE);

    DashboardPage.violationsTab().shouldNotBe(ACTIVE).click();
    waitUntilUrl(VIOLATIONS_URL);
    DashboardPage.violationsTab().shouldBe(ACTIVE);

    Selenide.back();
    waitUntilUrl(APPLICATIONS_URL);
    DashboardPage.applicationsTab().shouldBe(ACTIVE);

    Selenide.back();
    waitUntilUrl(COMPONENTS_URL);
    DashboardPage.componentsTab().shouldBe(ACTIVE);

    Selenide.back();
    waitUntilUrl(VIOLATIONS_URL);
    DashboardPage.violationsTab().shouldBe(ACTIVE);
  }

  @Test
  public void testCalculateTrendsModal() {
    DashboardPage.viewDropdown().click();
    DashboardPage.calculateTrendsLink().click();

    final TrendsModal trendsModal = DashboardPage.trendsModal();
    trendsModal.shouldBe(visible);
    Selenide.Wait().withMessage("Trends table didn't show up").until(new Predicate<WebDriver>()
    {
      @Override
      public boolean apply(WebDriver input) {
        return $(trendsModal.contentsTable()).exists();
      }
    });

    trendsModal.rows().shouldHave(CollectionCondition.size(4));

    TrendRow discoveredRow = trendsModal.discoveredRow();
    TrendRow fixedRow = trendsModal.fixedRow();
    TrendRow pendingRow = trendsModal.pendingRow();
    TrendRow waivedRow = trendsModal.waivedRow();

    discoveredRow.category().has(text("Discovered"));
    fixedRow.category().has(text("Fixed"));
    pendingRow.category().has(text("Pending"));
    waivedRow.category().has(text("Waived"));

    discoveredRow.count().has(text("10"));
    fixedRow.count().has(text("8"));
    pendingRow.count().has(text("1"));
    waivedRow.count().has(text("1"));

    discoveredRow.averageAge().is(empty);
    fixedRow.averageAge().has(text("1m"));
    pendingRow.averageAge().has(text("2d"));
    waivedRow.averageAge().has(text("1m"));

    discoveredRow.ninetyPercentileAge().is(empty);
    fixedRow.ninetyPercentileAge().has(text("2m"));
    pendingRow.ninetyPercentileAge().has(text("2d"));
    waivedRow.ninetyPercentileAge().has(text("1m"));

    discoveredRow.delta().value().has(text("7"));
    fixedRow.delta().value().has(text("8"));
    pendingRow.delta().value().has(text("-2"));
    waivedRow.delta().value().has(text("1"));

    discoveredRow.shouldBe(NEUTRAL).delta().chevron().shouldBe(UP);
    fixedRow.shouldBe(NATURAL).delta().chevron().shouldBe(UP);
    pendingRow.shouldBe(INVERSE).delta().chevron().shouldBe(DOWN);
    waivedRow.shouldBe(INVERSE).delta().chevron().shouldBe(UP);

    discoveredRow.barChartPoints().shouldHave(texts(NEW_ROW_DELTAS));
    fixedRow.barChartPoints().shouldHave(texts(FIXED_ROW_DELTAS));
    pendingRow.barChartPoints().shouldHave(texts(UNRESOLVED_ROW_DELTAS));
    waivedRow.barChartPoints().shouldHave(texts(WAIVED_ROW_DELTAS));

    trendsModal.closeButton().click();
    trendsModal.shouldNotBe(visible);
  }

  @Test
  public void filteringAllDataOutShouldResultInEmptyTrendsModal() {
    DashboardFilters.policyTypeFilter().twisty().click();
    DashboardFilters.policyTypeFilter().license().click();
    DashboardFilters.apply();

    DashboardPage.viewDropdown().click();
    DashboardPage.calculateTrendsLink().click();

    final TrendsModal trendsModal = DashboardPage.trendsModal();
    trendsModal.shouldBe(visible);
    Selenide.Wait().withMessage("Trends table didn't show up").until(new Predicate<WebDriver>()
    {
      @Override
      public boolean apply(WebDriver input) {
        return $(trendsModal.contentsTable()).exists();
      }
    });

    TrendRow discoveredRow = trendsModal.discoveredRow();
    TrendRow fixedRow = trendsModal.fixedRow();
    TrendRow pendingRow = trendsModal.pendingRow();
    TrendRow waivedRow = trendsModal.waivedRow();
    
    discoveredRow.count().has(text("0"));
    fixedRow.count().has(text("0"));
    pendingRow.count().has(text("0"));
    waivedRow.count().has(text("0"));

    discoveredRow.delta().value().has(text("0"));
    fixedRow.delta().value().has(text("0"));
    pendingRow.delta().value().has(text("0"));
    waivedRow.delta().value().has(text("0"));

    discoveredRow.delta().chevron().shouldNotBe(UP, DOWN);
    fixedRow.delta().chevron().shouldNotBe(UP, DOWN);
    pendingRow.delta().chevron().shouldNotBe(UP, DOWN);
    waivedRow.delta().chevron().shouldNotBe(UP, DOWN);

    String[] emptyPoints = new String[12];
    Arrays.fill(emptyPoints, "0");

    discoveredRow.barChartPoints().shouldHave(texts(emptyPoints));
    fixedRow.barChartPoints().shouldHave(texts(emptyPoints));
    pendingRow.barChartPoints().shouldHave(texts(emptyPoints));
    waivedRow.barChartPoints().shouldHave(texts(emptyPoints));

    trendsModal.closeButton().click();
    trendsModal.shouldNotBe(visible);
  }

  private void clearFilters() {
    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    List<DashboardFilter> filters = dashboardFilterDAO.getByUsername("admin");
    for (DashboardFilter filter : filters) {
      dashboardFilterDAO.delete(filter);
    }
  }

  private static class ComponentData
  {
    ComponentIdentifier componentIdentifier;

    String hash;

    public ComponentData(ComponentIdentifier componentIdentifier, String hash) {
      this.componentIdentifier = componentIdentifier;
      this.hash = hash;
    }
  }
}
