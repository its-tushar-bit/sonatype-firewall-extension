/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.elements.ViolationTrendPlot;
import com.sonatype.clm.testing.functional.elements.ViolationTrendPlot.BarPlot;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ApplicationCountsTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ComponentCountsTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.Header;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.MttrTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationAveragesTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationTrendTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationsByCategoryTile;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsReportScopeDTO;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.WebElementCondition;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ALL_CLASS;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.CRITICAL_CLASS;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.LICENSE_CLASS;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.NO_DATA_INFO_TEXT_MONTHLY;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.OTHER_CLASS;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.QUALITY_CLASS;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.SECURITY_CLASS;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.TOTAL_CLASS;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationTrendTile.DESCRIPTION_TEXT;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationTrendTile.GUIDELINE_TOOLTIP_VALUES;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationTrendTile.HEIGHT_ATTR;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationTrendTile.TITLE_TEXT;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationTrendTile.TRENDS_DELTA_DOWN_CLASS;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationTrendTile.TRENDS_DELTA_UP_CLASS;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationTrendTile.TRENDS_DISCOVERED_CLASS;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationTrendTile.TRENDS_FIXED_CLASS;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;

public class SuccessMetricsChartsTest
    extends AbstractFunctionalTest
{
  private static final DateTime thisMonth = DateTime.parse("2018-08-15T19:36");

  private static final DateTime fourMonthsAgo = thisMonth.minusMonths(4);

  private static final DateTime threeMonthsAgo = fourMonthsAgo.plusDays(30);

  private static final DateTime twoMonthsAgo = threeMonthsAgo.plusDays(30);

  private static final DateTime oneMonthAgo = twoMonthsAgo.plusDays(30);

  private final String browserName = System.getProperty("browser");

  private PolicyViolationDAO policyViolationDAO;

  private SuccessMetricsReport successMetricsReport;

  private void fixViolations(
      PolicyEvaluation evaluation,
      Predicate<PolicyViolation> exclude)
  {
    List<PolicyViolation> policyViolations = policyViolationDAO
        .getUnfixedByApplicationIdAndStageId(evaluation.getApplicationId(), evaluation.getStageTypeId());
    policyViolationDAO.loadConstraintFacts(policyViolations);
    for (PolicyViolation fixedViolation : policyViolations) {
      if (exclude == null || !exclude.test(fixedViolation)) {
        fixedViolation.setFixTime(evaluation.getTime());
        policyViolationDAO.update(fixedViolation);
      }
    }
  }

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @After
  public void tearDown() {
    resetTime();
  }

  @Before
  public void navigate() {
    policyViolationDAO = lookup(PolicyViolationDAO.class);

    // always use the same date to have consistent results in weekly charts
    setTimeTo(thisMonth);
    Application app1 = tempEntity.newApplicationWithParent("app1", "SuccessMetricsChart Test App1");
    Application app2 = tempEntity.newApplicationWithParent("app2", "SuccessMetricsChart Test App2");
    Application app3 = tempEntity.newApplicationWithParent("app3", "SuccessMetricsChart Test App3");

    Policy licensePolicy = tempEntity.newPolicy(app1.getParentOwnerId());
    Policy securityPolicy = tempEntity.newPolicy(app2.getParentOwnerId());
    Policy qualityPolicy = tempEntity.newPolicy(app2.getParentOwnerId());
    Policy otherPolicy = tempEntity.newPolicy(app2.getParentOwnerId());
    Policy app3Policy = tempEntity.newPolicy(app3.getParentOwnerId());

    PolicyEvaluation buildEval4MonthsAgo = tempEntity
        .newPolicyEvaluation(app1.getId(), BuildStageType.ID, "fourMonthsAgo", fourMonthsAgo.toDate());
    PolicyEvaluation releaseEval3MonthsAgo = tempEntity
        .newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "threeMonthsAgo", threeMonthsAgo.toDate());
    PolicyEvaluation buildEval2MonthsAgo = tempEntity
        .newPolicyEvaluation(app2.getId(), BuildStageType.ID, "twoMonthsAgo", twoMonthsAgo.toDate());
    PolicyEvaluation releaseEval2MonthsAgo = tempEntity
        .newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "twoMonthsAgo", twoMonthsAgo.toDate());
    PolicyEvaluation releaseEval1MonthAgo = tempEntity
        .newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "oneMonthAgo", oneMonthAgo.toDate());
    PolicyEvaluation app3Eval1 = tempEntity
        .newPolicyEvaluation(app3.getId(), BuildStageType.ID, "app3Eval1", fourMonthsAgo.toDate());
    PolicyEvaluation app3Eval2 = tempEntity
        .newPolicyEvaluation(app3.getId(), BuildStageType.ID, "app3Eval2", threeMonthsAgo.toDate());

    ApplicationComponent buildComponent = tempEntity
        .newApplicationComponent(app1.getId(), BuildStageType.ID, "shortnamehash",
            ComponentIdentifier.createMavenCoordinates("short", "name", "0.6"));
    ApplicationComponent releaseComponent = tempEntity
        .newApplicationComponent(app2.getId(), ReleaseStageType.ID, "longnamehash",
            ComponentIdentifier.createMavenCoordinates("long.component.name.should.cause.tooltip", "artifact",
                "1.2.3.4"));

    // add a few violations
    tempEntity.newPolicyViolation(buildEval4MonthsAgo, licensePolicy, 7,
        LICENSE, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);

    tempEntity.newPolicyViolation(releaseEval3MonthsAgo, securityPolicy, 8,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(releaseEval3MonthsAgo, licensePolicy, 1,
        LICENSE, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(releaseEval3MonthsAgo, securityPolicy, 9,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(releaseEval3MonthsAgo, securityPolicy, 10,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);

    fixViolations(releaseEval2MonthsAgo, violation -> violation.getThreatLevel() >= 9);

    tempEntity.newPolicyViolation(buildEval2MonthsAgo, licensePolicy, 10,
        LICENSE, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(buildEval2MonthsAgo, licensePolicy, 7,
        LICENSE, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(buildEval2MonthsAgo, qualityPolicy, 7,
        QUALITY, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(buildEval2MonthsAgo, otherPolicy, 7,
        OTHER, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);

    fixViolations(releaseEval1MonthAgo, violation -> violation.getThreatLevel() >= 10);

    tempEntity.newPolicyViolation(app3Eval1, app3Policy, 10,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);

    fixViolations(app3Eval2, null);

    SuccessMetricsReportScopeDTO successMetricsScope = new SuccessMetricsReportScopeDTO();
    successMetricsScope.organizationIds = new HashSet<>(Collections.singletonList(app1.getParentOwnerId()));
    successMetricsScope.applicationIds = new HashSet<>(Arrays.asList(app1.getId(), app2.getId()));

    // Include app2 using its app id and app1 using its parent org id. Do not include app3.
    successMetricsReport = tempEntity.newSuccessMetricsReport("admin", "Test", JsonUtils.format(successMetricsScope));
  }

  @Test
  public void testHeader() {
    refreshOrOpen(SuccessMetricsReportPage.url(successMetricsReport.getId()));

    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage().shouldBeFullyLoaded();

    successMetricsChartsPage.should(appear);
    Header.root().shouldBe(visible);
    eyesWatcher.eyesCheck();
    Header.title().shouldHave(text("Test"));
    String reportUpdated = DateTimeFormat.forPattern("MMM d, YYYY")
        .withLocale(Locale.ENGLISH)
        .print(Ordering.natural().max(LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfWeek(1)));
    Header.description()
        .shouldHave(text("This report contains data for 2 applications, evaluated over the"
            + " past 4 months, aggregated and deduplicated over the source, build, stage release, release, and operate"
            + " stages. Last updated " + reportUpdated));
  }

  @Test
  public void testViolationTrendTile() {
    refreshOrOpen(SuccessMetricsReportPage.url(successMetricsReport.getId()));

    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage().shouldBeFullyLoaded();
    successMetricsChartsPage.should(appear);

    ScrollUtil.scrollIntoView(ViolationTrendTile.root());

    ViolationTrendTile.root().shouldBe(visible);

    ViolationTrendTile.title().shouldHave(TITLE_TEXT);
    ViolationTrendTile.description().shouldHave(DESCRIPTION_TEXT);

    // all violations
    ViolationTrendPlot allViolations = ViolationTrendTile.allViolationsPlot();
    allViolations.shouldBe(visible);

    BarPlot allViolationsDeltaPlot = allViolations.deltaPlot();
    allViolationsDeltaPlot.shouldBe(visible);
    allViolationsDeltaPlot.bar(3).shouldHave(TRENDS_DELTA_UP_CLASS).shouldHave(heightAttrStartingWith("20.66"));
    allViolationsDeltaPlot.bar(7).shouldHave(TRENDS_DELTA_DOWN_CLASS).shouldHave(heightAttrStartingWith("10.33"));

    BarPlot newViolationsDeltaPlot = allViolations.newPlot();
    newViolationsDeltaPlot.shouldBe(visible);
    newViolationsDeltaPlot.bar(3).shouldHave(TRENDS_DISCOVERED_CLASS).shouldHave(heightAttrStartingWith("31"));

    allViolations.waivedPlot().shouldBe(visible);

    BarPlot allViolationsFixedPlot = allViolations.fixedPlot();
    allViolationsFixedPlot.shouldBe(visible);
    allViolationsFixedPlot.bar(3).shouldHave(TRENDS_FIXED_CLASS).shouldHave(heightAttrStartingWith("31"));
    allViolationsFixedPlot.bar(7).shouldHave(TRENDS_FIXED_CLASS).shouldHave(heightAttrStartingWith("15.5"));

    // security violations
    ViolationTrendPlot securityViolations = ViolationTrendTile.securityViolationsPlot();
    securityViolations.shouldBe(visible);

    BarPlot securityViolationsDeltaPlot = securityViolations.deltaPlot();
    securityViolationsDeltaPlot.shouldBe(visible);
    securityViolationsDeltaPlot.bar(3).shouldHave(TRENDS_DELTA_DOWN_CLASS).shouldHave(heightAttrStartingWith("10.33"));
    securityViolationsDeltaPlot.bar(7).shouldHave(TRENDS_DELTA_DOWN_CLASS).shouldHave(heightAttrStartingWith("10.33"));

    securityViolations.newPlot().shouldBe(visible);
    securityViolations.waivedPlot().shouldBe(visible);

    BarPlot securityViolationsFixedPlot = securityViolations.fixedPlot();
    securityViolationsFixedPlot.shouldBe(visible);
    securityViolationsFixedPlot.bar(3).shouldHave(TRENDS_FIXED_CLASS).shouldHave(heightAttrStartingWith("15.5"));
    securityViolationsFixedPlot.bar(7).shouldHave(TRENDS_FIXED_CLASS).shouldHave(heightAttrStartingWith("15.5"));

    // license violations
    ViolationTrendPlot licenseViolations = ViolationTrendTile.licenseViolationsPlot();
    securityViolations.shouldBe(visible);

    BarPlot licenseViolationsDeltaPlot = licenseViolations.deltaPlot();
    licenseViolationsDeltaPlot.shouldBe(visible);
    licenseViolationsDeltaPlot.bar(3).shouldHave(TRENDS_DELTA_UP_CLASS).shouldHave(heightAttrStartingWith("10.33"));

    BarPlot licenseViolationsNewPlot = licenseViolations.newPlot();
    licenseViolationsNewPlot.shouldBe(visible);
    licenseViolationsNewPlot.bar(3).shouldHave(TRENDS_DISCOVERED_CLASS).shouldHave(heightAttrStartingWith("15.5"));

    licenseViolations.waivedPlot().shouldBe(visible);

    BarPlot licenseViolationsFixedPlot = licenseViolations.fixedPlot();
    licenseViolationsFixedPlot.shouldBe(visible);
    licenseViolationsFixedPlot.bar(3).shouldHave(TRENDS_FIXED_CLASS).shouldHave(heightAttrStartingWith("15.5"));

    // quality violations
    ViolationTrendPlot qualityViolations = ViolationTrendTile.qualityViolationsPlot();
    securityViolations.shouldBe(visible);

    BarPlot qualityViolationsDeltaPlot = qualityViolations.deltaPlot();
    qualityViolationsDeltaPlot.shouldBe(visible);
    qualityViolationsDeltaPlot.bar(3).shouldHave(TRENDS_DELTA_UP_CLASS).shouldHave(heightAttrStartingWith("10.33"));

    BarPlot qualityViolationsNewPlot = qualityViolations.newPlot();
    qualityViolationsNewPlot.shouldBe(visible);
    qualityViolationsNewPlot.bar(3).shouldHave(TRENDS_DISCOVERED_CLASS).shouldHave(heightAttrStartingWith("7.75"));

    qualityViolations.waivedPlot().shouldBe(visible);
    qualityViolations.fixedPlot().shouldBe(visible);

    // other violations
    ViolationTrendPlot otherViolations = ViolationTrendTile.otherViolationsPlot();
    securityViolations.shouldBe(visible);

    BarPlot otherViolationsDeltaPlot = otherViolations.deltaPlot();
    otherViolationsDeltaPlot.shouldBe(visible);
    otherViolationsDeltaPlot.bar(3).shouldHave(TRENDS_DELTA_UP_CLASS).shouldHave(heightAttrStartingWith("10.33"));

    BarPlot otherViolationsNewPlot = otherViolations.newPlot();
    otherViolationsNewPlot.shouldBe(visible);
    otherViolationsNewPlot.bar(3).shouldHave(TRENDS_DISCOVERED_CLASS).shouldHave(heightAttrStartingWith("7.75"));

    otherViolations.waivedPlot().shouldBe(visible);
    otherViolations.fixedPlot().shouldBe(visible);

    // tooltips
    ViolationTrendTile.guidelineTooltip.shouldNotBe(visible);
    ViolationTrendTile.deltaBarTooltip.shouldNotBe(visible);
    ViolationTrendTile.newBarTooltip.shouldNotBe(visible);
    ViolationTrendTile.waivedBarTooltip.shouldNotBe(visible);
    ViolationTrendTile.fixedBarTooltip.shouldNotBe(visible);

    allViolationsDeltaPlot.bar(3).hover();

    verifyPlotTooltips(allViolationsDeltaPlot, new String[][]{
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"2", "4", "0", "2"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"1", "0", "0", "1"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"}});

    verifyPlotTooltips(securityViolationsDeltaPlot, new String[][]{
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"1", "0", "0", "1"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"1", "0", "0", "1"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"}});

    verifyPlotTooltips(licenseViolationsDeltaPlot, new String[][]{
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"1", "2", "0", "1"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"}});

    verifyPlotTooltips(qualityViolationsDeltaPlot, new String[][]{
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"1", "1", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"}});

    verifyPlotTooltips(otherViolationsDeltaPlot, new String[][]{
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"1", "1", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"},
      new String[]{"0", "0", "0", "0"}});
  }

  private void verifyPlotTooltips(BarPlot plot, String[][] tooltipValuesPerWeek) {
    for (int i = 0; i < tooltipValuesPerWeek.length; i++) {
      // hide tooltip
      ViolationTrendTile.description().hover();
      ViolationTrendTile.guidelineTooltip.shouldNotBe(visible);

      SelenideElement plotBar = plot.bar(i);
      plotBar.hover();
      // For whatever reason the firefox driver misses the hover point when the violation column has 0 violations and
      // the height of the rectangle is set to 0. The cursor is placed just to the left of the first column. To work
      // around this we nudge the cursor over a bit to roughly the center of the hover point so that the hover kicks in.
      if ("firefox".equals(browserName) && plotBar.getAttribute("height").equals("0")) {
        new Actions(WebDriverRunner.getAndCheckWebDriver()).moveByOffset((i * 10) + 5, 0).perform();
      }
      verifyTooltips(GUIDELINE_TOOLTIP_VALUES[i], tooltipValuesPerWeek[i]);
    }
  }

  private void verifyTooltips(String guidelineTooltip, String[] barTooltips) {
    ViolationTrendTile.guidelineTooltip.shouldBe(visible).shouldHave(text(guidelineTooltip));
    ViolationTrendTile.deltaBarTooltip.shouldBe(visible).shouldHave(text(barTooltips[0]));
    ViolationTrendTile.newBarTooltip.shouldBe(visible).shouldHave(text(barTooltips[1]));
    ViolationTrendTile.waivedBarTooltip.shouldBe(visible).shouldHave(text(barTooltips[2]));
    ViolationTrendTile.fixedBarTooltip.shouldBe(visible).shouldHave(text(barTooltips[3]));
  }

  @Test
  public void testViolationsByCategoryTile() {
    refreshOrOpen(SuccessMetricsReportPage.url(successMetricsReport.getId()));

    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage().shouldBeFullyLoaded();

    successMetricsChartsPage.should(appear);

    ScrollUtil.scrollIntoView(ViolationsByCategoryTile.root());

    ViolationsByCategoryTile.root().shouldBe(visible);

    ViolationsByCategoryTile.title().shouldHave(text("12 Week Open Violation Totals"));

    ViolationsByCategoryTile.description().shouldHave(text("Open violations over the past 12 weeks by policy type."));

    ViolationsByCategoryTile.chart().shouldBe(visible);
    eyesWatcher.eyesCheck(true);

    ElementsCollection points = ViolationsByCategoryTile.points();
    points.shouldHave(size(12 * 5)); // 8 weeks times four categories plus totals

    String[] expectedClassOrdering = {OTHER_CLASS, QUALITY_CLASS, LICENSE_CLASS, SECURITY_CLASS, TOTAL_CLASS};

    for (int i = 0; i < points.size(); i++) {
      points.get(i).shouldBe(visible).shouldHave(cssClass(expectedClassOrdering[i / 12]));
    }

    ElementsCollection lines = ViolationsByCategoryTile.lines();
    lines.shouldHave(size(5));

    for (int i = 0; i < lines.size(); i++) {
      lines.get(i).shouldBe(visible).shouldHave(cssClass(expectedClassOrdering[i]));
    }

    ElementsCollection weeks = ViolationsByCategoryTile.xAxisLabels();
    weeks.shouldHave(size(12));

    String[] expectedWeeks = {
      "28 May", "04 Jun", "11 Jun", "18 Jun", "25 Jun", "02 Jul", "09 Jul", "16 Jul", "23 Jul", "30 Jul", "06 Aug",
      "13 Aug"
    };
    for (int i = 0; i < 12; i++) {
      weeks.get(i).shouldBe(visible).shouldHave(text(expectedWeeks[i]));
    }
  }

  @Test
  public void testViolationAveragesTile() {
    refreshOrOpen(SuccessMetricsReportPage.url(successMetricsReport.getId()));

    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage().shouldBeFullyLoaded();

    ScrollUtil.scrollIntoView(ViolationAveragesTile.root());

    successMetricsChartsPage.should(appear);
    ViolationAveragesTile.root().shouldBe(visible);
    eyesWatcher.eyesCheck();
    ViolationAveragesTile.title()
        .shouldHave(text("Average Number of Violations Discovered Per Month, Per Application"));
    ViolationAveragesTile.averages()
        .shouldHave(text("Lifecycle performed an average of 1 evaluation per month on 2 " +
            "applications over the past 4 months. Lifecycle found an average of 2 policy violations per application, 1 of"
            +
            " which were critical."));
  }

  @Test
  public void testApplicationCountsTile() {
    refreshOrOpen(SuccessMetricsReportPage.url(successMetricsReport.getId()));

    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage().shouldBeFullyLoaded();

    ScrollUtil.scrollIntoView(ApplicationCountsTile.root());

    successMetricsChartsPage.should(appear);
    ApplicationCountsTile.root().shouldHave(visible);
    ApplicationCountsTile.description().shouldBe(visible).shouldHave(text("2 applications contained violations"));
    ApplicationCountsTile.description().shouldHave(text("2 out of"));
    ApplicationCountsTile.description().shouldBe(visible).shouldHave(text("1 contained critical violations."));
    ApplicationCountsTile.securityViolatingApplicationsCount().shouldBe(visible).shouldHave(text("1"));
    ApplicationCountsTile.securityCriticalViolatingApplicationsCount().shouldBe(visible).shouldHave(text("1"));
    ApplicationCountsTile.licenseViolatingApplicationsCount().shouldBe(visible).shouldHave(text("2"));
    ApplicationCountsTile.licenseCriticalViolatingApplicationsCount().shouldBe(visible).shouldHave(text("1"));
    ApplicationCountsTile.qualityViolatingApplicationsCount().shouldBe(visible).shouldHave(text("1"));
    ApplicationCountsTile.qualityCriticalViolatingApplicationsCount().shouldBe(visible).shouldHave(text("0"));
    ApplicationCountsTile.otherViolatingApplicationsCount().shouldBe(visible).shouldHave(text("1"));
    ApplicationCountsTile.otherCriticalViolatingApplicationsCount().shouldBe(visible).shouldHave(text("0"));
  }

  @Test
  public void testMttrTile() {
    refreshOrOpen(SuccessMetricsReportPage.url(successMetricsReport.getId()));

    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage().shouldBeFullyLoaded();

    ScrollUtil.scrollIntoView(MttrTile.root());

    successMetricsChartsPage.should(appear);
    MttrTile.root().shouldBe(visible);
    MttrTile.chart().shouldBe(visible);

    ElementsCollection points = MttrTile.mttrPoints();
    points.shouldHave(size(4));
    points.get(0).should(visible).shouldHave(cssClass(ALL_CLASS));
    points.get(1).should(visible).shouldHave(cssClass(ALL_CLASS));
    points.get(2).should(visible).shouldHave(cssClass(CRITICAL_CLASS));
    points.get(3).should(visible).shouldHave(cssClass(CRITICAL_CLASS));

    ElementsCollection lines = MttrTile.mttrLines();
    lines.shouldHave(size(2));
    lines.get(0).should(visible).shouldHave(cssClass(ALL_CLASS));
    lines.get(1).should(visible).shouldHave(cssClass(CRITICAL_CLASS));

    ElementsCollection months = MttrTile.mttrXAxisLabels();
    months.shouldHave(size(12));

    DateTime mttrMonth = DateTime.now().minusMonths(12);
    for (int i = 0; i < 12; i++) {
      months.get(0).shouldBe(visible).shouldHave(text(mttrMonth.toString("MMM", Locale.ENGLISH)));
      mttrMonth.plusMonths(1);
    }
  }

  @Test
  public void testComponentCountsTile() {
    refreshOrOpen(SuccessMetricsReportPage.url(successMetricsReport.getId()));

    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage().shouldBeFullyLoaded();

    ScrollUtil.scrollIntoView(ComponentCountsTile.root());

    successMetricsChartsPage.should(appear);
    ComponentCountsTile.root().shouldBe(visible);

    ComponentCountsTile.averages()
        .shouldHave(text("On average, there are 1 components per application."));
    ElementsCollection componentsInMostApplications = ComponentCountsTile.componentsInMostApplications();
    componentsInMostApplications.shouldHave(size(2));
    ElementsCollection componentsWithMostViolations = ComponentCountsTile.componentsWithMostViolations();
    componentsWithMostViolations.shouldHave(size(2));

    String[] componentGroupIdsInMostApplications = {
      "long.component.name.should.cause.tooltip : artifact : 1.2.3.4", "short : name : 0.6"
    };
    String expectedApplicationText = "1applications";
    componentsInMostApplications.shouldHave(texts(componentGroupIdsInMostApplications));
    componentsInMostApplications.shouldHave(
        texts(expectedApplicationText, expectedApplicationText));

    componentsInMostApplications.get(0)
        .shouldHave(text("long.component.name.should.cause.tooltip : artifact : 1.2.3.4"))
        .hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("long.component.name.should.cause.tooltip : artifact : 1.2.3.4"));
    eyesWatcher.eyesCheck(null, false, false);

    componentsInMostApplications.get(1)
        .shouldHave(text("short : name : 0.6"))
        .hover();

    // Tooltip is configured to appear after 300ms, so we need to wait at least that long to really make sure its
    // not going to appear. Without this sleep we'd just be testing that it hasn't appeared _yet_.
    Selenide.sleep(1000);
    Tooltip.get().shouldBe(hidden);

    String[] componentGroupIdsWithMostViolations = {
      "short : name : 0.6", "long.component.name.should.cause.tooltip : artifact : 1.2.3.4",
    };
    componentsWithMostViolations.shouldHave(texts(componentGroupIdsWithMostViolations));
    componentsWithMostViolations.shouldHave(texts("5violations", "1violations"));
  }

  /**
   * Test that navigating to a SuccessMetricsReport that has a specific app/org selection, but where that app/org
   * selection has only invalid or unauthorized apps/orgs, causes "No Data" and not a totally unfiltered chart
   */
  @Test
  public void testNonMatchSuccessMetrics() {
    // create a SuccessMetricsReport with only non-existant app and org ids
    SuccessMetricsReportScopeDTO invalidScopeDTO = new SuccessMetricsReportScopeDTO();
    invalidScopeDTO.applicationIds = new HashSet<>(Collections.singletonList("non-existent-app"));
    invalidScopeDTO.organizationIds = new HashSet<>(Collections.singletonList("non-existent-org"));
    SuccessMetricsReport successMetricsReport = tempEntity.newSuccessMetricsReport("admin", "invalid metrics",
        JsonUtils.format(invalidScopeDTO));

    refreshOrOpen(SuccessMetricsReportPage.url(successMetricsReport.getId()));

    SuccessMetricsReportPage.Header.title().shouldHave(text("invalid metrics"));
    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage();
    successMetricsChartsPage.should(appear);
    successMetricsChartsPage.noDataInfoPane().shouldBe(visible).shouldHave(NO_DATA_INFO_TEXT_MONTHLY);
  }

  private static void setTimeTo(DateTime fakeNow) {
    DateTimeUtils.setCurrentMillisFixed(fakeNow.getMillis());
  }

  private static void resetTime() {
    DateTimeUtils.setCurrentMillisSystem();
  }

  private static WebElementCondition heightAttrStartingWith(final String value) {
    return new WebElementCondition("heightAttrStartingWith")
    {
      @Override
      public CheckResult check(Driver driver, WebElement element) {
        var actual = element.getAttribute(HEIGHT_ATTR);
        return actual.startsWith(value)
            ? CheckResult.accepted(actual)
            : CheckResult.rejected("height attribute did not start with " + value, actual);
      }
    };
  }
}
