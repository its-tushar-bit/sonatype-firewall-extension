/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.trending;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.trending.ApplicationRiskSummary;
import com.sonatype.insight.brain.model.trending.Applications;
import com.sonatype.insight.brain.model.trending.ComponentRiskSummary;
import com.sonatype.insight.brain.model.trending.ComponentsSummary;
import com.sonatype.insight.brain.model.trending.DiffData;
import com.sonatype.insight.brain.model.trending.PartialMatch;
import com.sonatype.insight.brain.model.trending.PoliciesSummary;
import com.sonatype.insight.brain.model.trending.PolicyViolation;
import com.sonatype.insight.brain.model.trending.TrendingReport;
import com.sonatype.insight.brain.model.trending.TrendingReportMetadata;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.trending.TrendingReportProcessor.ProgressMonitor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TrendingReportProcessorTest
{
  private static final long ONE_DAY_MS = TimeUnit.DAYS.toMillis(1);

  private TrendingReportProcessor processor;
  private InsightWork insightWork;

  @Rule
  public TemporaryFolder workDir = new TemporaryFolder(new File("target").getAbsoluteFile());

  @Rule
  public TemporaryEntity temporaryEntity = new TemporaryEntity();

  @Before
  public void createTwentyDayReportProcessor() {
    InsightConfig config = new InsightConfig();
    config.setSonatypeWork(workDir.getRoot().getAbsolutePath());
    insightWork = new InsightWork(config);

    processor = new TrendingReportProcessor(insightWork);
  }

  @Test
  public void testMetadata() throws Exception {
    long beforeGeneration = new Date().getTime();
    TrendingReport report = calculateReport();
    long afterGeneration = new Date().getTime();

    TrendingReportMetadata meta = report.getMeta();
    Assert.assertTrue(meta.getGeneratedOn() >= beforeGeneration && meta.getGeneratedOn() <= afterGeneration);
    Assert.assertEquals(TrendingReportProcessor.TWENTY_DAYS_MS, meta.getPeriodEnd() - meta.getPeriodStart());
  }

  @Test
  public void testComponentSummary() throws Exception {
    Application application = createApplication("testApp");
    ReportBuilder builder = newReportBuilder();
    builder.addComponent().setGAV("a", "a", "a").setHash("A").setMatchState(MatchState.EXACT);
    builder.addComponent().setGAV("b", "b", "b").setHash("B").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("c", "c", "c").setHash("C").setMatchState(MatchState.UNKNOWN);
    builder.addComponent().setGAV("d", "d", "d").setHash("D").setProprietary(false).setMatchState(MatchState.UNKNOWN);
    createScan(application, builder);

    TrendingReport report = calculateReport();

    ComponentsSummary components = report.getComponents();
    Assert.assertEquals(1, components.getExact());
    Assert.assertEquals(1, components.getPartial());
    Assert.assertEquals(2, components.getUnknown());
    Assert.assertEquals(0, components.getProprietary());
    Assert.assertEquals(4, components.getInApplication());

    builder = newReportBuilder();
    builder.addComponent().setGAV("a", "a", "a").setHash("A").setMatchState(MatchState.EXACT);
    builder.addComponent().setGAV("b", "b", "b").setHash("B").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("c", "c", "c").setHash("C").setMatchState(MatchState.UNKNOWN);
    builder.addComponent().setGAV("d", "d", "d").setHash("D").setProprietary(true).setMatchState(MatchState.UNKNOWN);
    createScan(application, builder);

    report = calculateReport();

    components = report.getComponents();
    Assert.assertEquals(1, components.getExact());
    Assert.assertEquals(1, components.getPartial());
    Assert.assertEquals(1, components.getUnknown());
    Assert.assertEquals(1, components.getProprietary());
    Assert.assertEquals(4, components.getInApplication());
  }

  private ReportBuilder newReportBuilder() {
    return new ReportBuilder(temporaryEntity);
  }

  @Test
  public void testApplications() throws Exception {
    ReportBuilder builder = newReportBuilder();
    // 1
    builder.addPolicyViolation("a", 10, "a");
    builder.addPolicyViolation("b", 6, "a");
    builder.addPolicyViolation("b", 6, "b");
    builder.addPolicyViolation("c", 2, "c");
    builder.addPolicyViolation("d", 0, "d");
    createScan(createApplication("testApp1"), builder);
    // 2
    builder = newReportBuilder();
    builder.addPolicyViolation("e", 9, "e");
    createScan(createApplication("testApp2"), builder);
    // 3
    builder = newReportBuilder();
    builder.addPolicyViolation("f", 8, "f");
    createScan(createApplication("testApp3"), builder);
    // 4
    builder = newReportBuilder();
    builder.addPolicyViolation("g", 7, "g");
    createScan(createApplication("testApp4"), builder);
    // 5
    builder = newReportBuilder();
    builder.addPolicyViolation("h", 6, "h");
    createScan(createApplication("testApp5"), builder);
    // 6 cut off
    builder = newReportBuilder();
    builder.addPolicyViolation("i", 5, "i");
    createScan(createApplication("testApp6"), builder);

    TrendingReport report = calculateReport();

    Applications applications = report.getApplications();

    Assert.assertEquals(6, applications.getTotal());

    List<ApplicationRiskSummary> risks = applications.getRisks();
    Assert.assertEquals(TrendingReportProcessor.APPLICATION_RISKS_COUNT, risks.size());

    Assert.assertEquals("testApp1", risks.get(0).getName());
    Assert.assertEquals(1, risks.get(0).getCritical());
    Assert.assertEquals(2, risks.get(0).getSevere());
    Assert.assertEquals(1, risks.get(0).getModerate());
    Assert.assertEquals(1, risks.get(0).getNone());

    Assert.assertEquals("testApp2", risks.get(1).getName());
    Assert.assertEquals("testApp3", risks.get(2).getName());
    Assert.assertEquals("testApp4", risks.get(3).getName());
    Assert.assertEquals("testApp5", risks.get(4).getName());
  }

  @Test
  public void testPolicyViolationsPeriods() throws Exception {
    Application application = createApplication("testApp");

    long time = System.currentTimeMillis();
    ReportBuilder builder = newReportBuilder();
    builder.addPolicyViolation("a", 10, "a");
    createScan(application, builder, time - (40 * ONE_DAY_MS)); // this is expected to be ignored
    builder = newReportBuilder();
    builder.addPolicyViolation("a", 10, "a");
    createScan(application, builder, time - (21 * ONE_DAY_MS)); // this is expected to be ignored
    builder = newReportBuilder();
    builder.addPolicyViolation("a", 10, "a");
    createScan(application, builder, time - (16 * ONE_DAY_MS));
    builder = newReportBuilder();
    builder.addPolicyViolation("a", 10, "a");
    createScan(application, builder, time - (11 * ONE_DAY_MS));
    builder = newReportBuilder();
    builder.addPolicyViolation("a", 10, "a");
    createScan(application, builder, time - (6 * ONE_DAY_MS));
    builder = newReportBuilder();
    builder.addPolicyViolation("a", 10, "a");
    createScan(application, builder, time - (1 * ONE_DAY_MS)); // this is expected to be ignored
    builder = newReportBuilder();
    builder.addPolicyViolation("a", 10, "a");
    createScan(application, builder, time);

    TrendingReport report = calculateReport();

    List<PolicyViolation> violations = report.getViolations();
    Assert.assertEquals(1, violations.size());
    Assert.assertArrayEquals(new int[] { 1, 1, 1, 1 }, violations.get(0).getViolations());
  }

  @Test
  public void testPolicyViolationsPeriods_veryOldReport() throws Exception {
    Application application = createApplication("testApp");
    ReportBuilder builder = newReportBuilder();
    builder.addPolicyViolation("a", 10, "a");

    long time = System.currentTimeMillis();
    createScan(application, builder, time - (40 * ONE_DAY_MS));

    TrendingReport report = calculateReport();

    List<PolicyViolation> violations = report.getViolations();
    Assert.assertEquals(1, violations.size());
    Assert.assertArrayEquals(new int[] { 1, 1, 1, 1 }, violations.get(0).getViolations());
  }

  @Test
  public void testPolicyViolationsPeriods_beforeReportStart() throws Exception {
    Application application = createApplication("testApp");
    ReportBuilder builder = newReportBuilder();
    builder.addPolicyViolation("a", 10, "a");

    long time = System.currentTimeMillis();
    createScan(application, builder, time - (21 * ONE_DAY_MS));

    TrendingReport report = calculateReport();

    List<PolicyViolation> violations = report.getViolations();
    Assert.assertEquals(1, violations.size());
    Assert.assertArrayEquals(new int[] { 1, 1, 1, 1 }, violations.get(0).getViolations());
  }

  @Test
  public void testPolicyViolationsPeriods_endOfReport() throws Exception {
    Application application = createApplication("testApp");
    ReportBuilder builder = newReportBuilder();
    builder.addPolicyViolation("a", 10, "a");

    long time = System.currentTimeMillis();
    createScan(application, builder, time);

    TrendingReport report = calculateReport();

    List<PolicyViolation> violations = report.getViolations();
    Assert.assertEquals(1, violations.size());
    Assert.assertArrayEquals(new int[] { 0, 0, 0, 1 }, violations.get(0).getViolations());
  }

  @Test
  public void testPolicyViolationsPeriods_emptyEvaluation() throws Exception {
    Application application = createApplication("testApp");
    ReportBuilder builder = newReportBuilder();
    builder.addPolicyViolation("a", 10, "a");

    long time = System.currentTimeMillis();
    createScan(application, builder, time - (1 * ONE_DAY_MS));
    createScan(application, null, time); // empty report, should be ignored

    TrendingReport report = calculateReport();

    List<PolicyViolation> violations = report.getViolations();
    Assert.assertEquals(1, violations.size());
    Assert.assertArrayEquals(new int[] { 0, 0, 0, 1 }, violations.get(0).getViolations());
  }

  @Test
  public void testPolicyViolationsCategories() throws Exception {
    Application application = createApplication("testApp");
    ReportBuilder builder = newReportBuilder();
    builder.addPolicyViolation("a", 0, PolicyThreatCategory.SECURITY, "a");
    builder.addPolicyViolation("b", 1, PolicyThreatCategory.LICENSE, "b");
    builder.addPolicyViolation("c1", 2, PolicyThreatCategory.QUALITY, "c1");
    builder.addPolicyViolation("c2", 3, PolicyThreatCategory.QUALITY, "c2");
    builder.addPolicyViolation("d", 4, PolicyThreatCategory.OTHER, "d");
    createScan(application, builder);

    TrendingReport report = calculateReport();
    List<PolicyViolation> violations = report.getViolations();
    Assert.assertEquals(5, violations.size());
    assertPolicyViolations(violations, "a", "security", 0);
    assertPolicyViolations(violations, "b", "license", 1);
    assertPolicyViolations(violations, "c1", "quality", 2);
    assertPolicyViolations(violations, "c2", "quality", 3);
    assertPolicyViolations(violations, "d", "other", 4);
  }

  @Test
  public void testPartialMatches() throws Exception {
    Application application = createApplication("testApp");
    ReportBuilder builder = newReportBuilder();
    builder.addComponent().setGAV("a", "a", "1").setHash("1").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "a", "1").setHash("2").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "a", "1").setHash("3").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "a", "1").setHash("4").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "a", "1").setHash("5").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "a", "1").setHash("6").setMatchState(MatchState.SIMILAR);
    //
    builder.addComponent().setGAV("a", "b", "2").setHash("1").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "b", "2").setHash("2").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "b", "2").setHash("3").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "b", "2").setHash("4").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "b", "2").setHash("5").setMatchState(MatchState.SIMILAR);
    //
    builder.addComponent().setGAV("a", "c", "3").setHash("1").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "c", "3").setHash("2").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "c", "3").setHash("3").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "c", "3").setHash("4").setMatchState(MatchState.SIMILAR);
    //
    builder.addComponent().setGAV("a", "d", "4").setHash("1").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "d", "4").setHash("2").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "d", "4").setHash("3").setMatchState(MatchState.SIMILAR);
    //
    builder.addComponent().setGAV("a", "e", "5").setHash("1").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("a", "e", "5").setHash("2").setMatchState(MatchState.SIMILAR);
    // this should be cut off
    builder.addComponent().setGAV("a", "f", "6").setHash("1").setMatchState(MatchState.SIMILAR);

    createScan(application, builder);

    TrendingReport report = calculateReport();

    List<PartialMatch> partialMatches = report.getPartialMatches();

    Assert.assertEquals(TrendingReportProcessor.PARTIAL_MATCHES_COUNT, partialMatches.size());
    assertPartialMatch(partialMatches.get(0), "a", "a", "1", 6);
    assertPartialMatch(partialMatches.get(1), "a", "b", "2", 5);
    assertPartialMatch(partialMatches.get(2), "a", "c", "3", 4);
    assertPartialMatch(partialMatches.get(3), "a", "d", "4", 3);
    assertPartialMatch(partialMatches.get(4), "a", "e", "5", 2);
  }

  @Test
  public void testDiffData() throws Exception {
    Application application = createApplication("testApp");
    long now = System.currentTimeMillis();

    ReportBuilder builder = newReportBuilder();
    // previous
    builder.addPolicyViolation("a", 0, PolicyThreatCategory.SECURITY, "a");
    builder.addPolicyViolation("b", 10, PolicyThreatCategory.LICENSE, "b");
    createScan(application, builder, now - (21 * ONE_DAY_MS));
    // now
    builder = newReportBuilder();
    builder.addPolicyViolation("a", 0, PolicyThreatCategory.SECURITY, "a");
    createScan(application, builder, now - (1 * ONE_DAY_MS)); // previous

    TrendingReport report = calculateReport();

    Map<PolicyThreatCategory, List<DiffData>> diffData = report.getDiffData();

    assertDiffData(diffData.get(PolicyThreatCategory.SECURITY), new int[] { 0, 0, 0, 1 }, new int[] { 0, 0, 0, 1 });
    assertDiffData(diffData.get(PolicyThreatCategory.LICENSE), new int[] { 1, 0, 0, 0 }, new int[] { 0, 0, 0, 0 });
    assertDiffData(diffData.get(PolicyThreatCategory.QUALITY), new int[] { 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0 });
    assertDiffData(diffData.get(PolicyThreatCategory.OTHER), new int[] { 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0 });
  }

  @Test
  public void testDiffData_noScansDuringReportPeriod() throws Exception {
    Application application = createApplication("testApp");
    long now = System.currentTimeMillis();

    ReportBuilder builder = newReportBuilder();
    // previous
    builder.addPolicyViolation("a", 0, PolicyThreatCategory.SECURITY, "a");
    createScan(application, builder, now - (21 * ONE_DAY_MS));

    TrendingReport report = calculateReport();

    Map<PolicyThreatCategory, List<DiffData>> diffData = report.getDiffData();

    assertDiffData(diffData.get(PolicyThreatCategory.SECURITY), new int[] { 0, 0, 0, 0 }, new int[] { 0, 0, 0, 1 });
  }

  @Test
  public void testDiffData_noScansBeforeReportPeriod() throws Exception {
    Application application = createApplication("testApp");
    long now = System.currentTimeMillis();

    ReportBuilder builder = newReportBuilder();
    // previous
    builder.addPolicyViolation("a", 0, PolicyThreatCategory.SECURITY, "a");
    createScan(application, builder, now - (1 * ONE_DAY_MS));

    TrendingReport report = calculateReport();

    Map<PolicyThreatCategory, List<DiffData>> diffData = report.getDiffData();

    assertDiffData(diffData.get(PolicyThreatCategory.SECURITY), new int[] { 0, 0, 0, 0 }, new int[] { 0, 0, 0, 1 });
  }

  @Test
  public void testComponentRisks() throws Exception {
    Application application = createApplication("testApp");
    ReportBuilder builder = newReportBuilder();
    // critical x2
    builder.addPolicyViolation("a", 10, PolicyThreatCategory.SECURITY, "a", "a", "a", "a");
    builder.addPolicyViolation("a1", 10, PolicyThreatCategory.LICENSE, "a", "a", "a", "a");
    builder.addPolicyViolation("a2", 10, PolicyThreatCategory.SECURITY, "a", "a", "a", "a");
    // critical
    builder.addPolicyViolation("b", 10, PolicyThreatCategory.SECURITY, "b", "b", "b", "b");
    // severe x2
    builder.addPolicyViolation("c", 6, PolicyThreatCategory.SECURITY, "c", "c", "c", "c");
    builder.addPolicyViolation("c2", 6, PolicyThreatCategory.SECURITY, "c", "c", "c", "c");
    // severe
    builder.addPolicyViolation("d", 6, PolicyThreatCategory.SECURITY, "d", "d", "d", "d");
    // moderate
    builder.addPolicyViolation("e", 3, PolicyThreatCategory.SECURITY, "e", "e", "e", "e");
    // none
    builder.addPolicyViolation("f", 0, PolicyThreatCategory.SECURITY, "f", "f", "f", "f");
    createScan(application, builder);

    TrendingReport report = calculateReport();
    Map<String, List<ComponentRiskSummary>> risks = report.getTopPolicyViolations();
    List<ComponentRiskSummary> securityRisks = risks.get("security");
    Assert.assertEquals(TrendingReportProcessor.COMPONENT_RISKS_COUNT, securityRisks.size());
    assertComponentRisk(securityRisks.get(0), "a", "a", "a", 2, 0, 0, 0);
    assertComponentRisk(securityRisks.get(1), "b", "b", "b", 1, 0, 0, 0);
    assertComponentRisk(securityRisks.get(2), "c", "c", "c", 0, 2, 0, 0);
    assertComponentRisk(securityRisks.get(3), "d", "d", "d", 0, 1, 0, 0);
    assertComponentRisk(securityRisks.get(4), "e", "e", "e", 0, 0, 1, 0);
    // license
    assertComponentRisk(risks.get("license").get(0), "a", "a", "a", 1, 0, 0, 0);
    // all
    assertComponentRisk(risks.get("all").get(0), "a", "a", "a", 3, 0, 0, 0);
  }

  @Test
  public void testPoliciesSummary() throws Exception {
    Application application = createApplication("testApp");
    ReportBuilder builder = newReportBuilder();
    // critical
    builder.addPolicyViolation("a", 10, PolicyThreatCategory.SECURITY,"a", "a", "a", "a");
    // severe x2
    builder.addPolicyViolation("b", 6, PolicyThreatCategory.SECURITY,"b", "b", "b", "b");
    builder.addPolicyViolation("c", 6, PolicyThreatCategory.SECURITY,"c", "c", "c", "c");
    // moderate
    builder.addPolicyViolation("d", 3, PolicyThreatCategory.SECURITY, "d", "d", "d", "d");
    // none
    builder.addPolicyViolation("e", 0, PolicyThreatCategory.SECURITY, "e", "e", "e", "e");
    createScan(application, builder);

    TrendingReport report = calculateReport();
    PoliciesSummary policiesSummary = report.getPolicies();
    Assert.assertEquals(1, policiesSummary.getCritical());
    Assert.assertEquals(2, policiesSummary.getSevere());
    Assert.assertEquals(1, policiesSummary.getModerate());
    Assert.assertEquals(1, policiesSummary.getNone());
    Assert.assertEquals(5, policiesSummary.getTotal());
  }

  @Test
  public void testProgressMonitor() throws Exception {
    createApplication("testApp1");
    createApplication("testApp2");
    createApplication("testApp");

    final List<String> ticks = new ArrayList<String>();
    processor.calculate(new ProgressMonitor()
    {
      @Override
      public void tick(int total, int current) {
        ticks.add(String.format("t=%d;c=%d", total, current));
      }
    });

    Assert.assertEquals(Arrays.asList("t=3;c=0", "t=3;c=1", "t=3;c=2", "t=3;c=3"), ticks);
  }

  private TrendingReport calculateReport() throws IOException {
    return processor.calculate(new ProgressMonitor()
    {
      @Override
      public void tick(int total, int current) {
      }
    });
  }

  private void assertComponentRisk(ComponentRiskSummary componentRisk, String groupId, String artifactId,
      String version, int critical, int severe, int moderate, int none)
  {
    Assert.assertEquals(groupId, componentRisk.getGroupId());
    Assert.assertEquals(artifactId, componentRisk.getArtifactId());
    Assert.assertEquals(version, componentRisk.getVersion());
    Assert.assertEquals(critical, componentRisk.getCritical());
    Assert.assertEquals(severe, componentRisk.getSevere());
    Assert.assertEquals(moderate, componentRisk.getModerate());
    Assert.assertEquals(none, componentRisk.getNone());
  }

  private void assertDiffData(List<DiffData> diffData, int[] expectedPrevious, int[] expectedCurrent) {
    Assert.assertEquals(TrendingReportProcessor.THREAT_LEVELS.length, diffData.size());
    Assert.assertEquals(TrendingReportProcessor.THREAT_LEVELS.length, expectedPrevious.length);
    Assert.assertEquals(TrendingReportProcessor.THREAT_LEVELS.length, expectedCurrent.length);

    for (int level = 0; level < TrendingReportProcessor.THREAT_LEVELS.length; level++) {
      Assert.assertEquals(TrendingReportProcessor.THREAT_LEVELS[level], diffData.get(level).getThreat());
      Assert.assertEquals(expectedCurrent[level], diffData.get(level).getViolations());
      Assert.assertEquals(expectedPrevious[level], diffData.get(level).getPreviousViolations());
    }
  }

  private void assertPartialMatch(PartialMatch partialMatch, String groupId, String artifactId, String version,
      int count)
  {
    Assert.assertEquals(groupId, partialMatch.getGroupId());
    Assert.assertEquals(artifactId, partialMatch.getArtifactId());
    Assert.assertEquals(version, partialMatch.getVersion());
    Assert.assertEquals(count, partialMatch.getCount());
  }

  private void assertPolicyViolations(List<PolicyViolation> violations, String policyName, String category,
      int threatLevel)
  {
    for (PolicyViolation violation : violations) {
      if (policyName.equals(violation.getName())) {
        Assert.assertEquals("policy " + policyName, category, violation.getCategory());
        Assert.assertEquals("policy " + policyName, threatLevel, violation.getThreat());
        return;
      }
    }
    Assert.fail("policy " + policyName);
  }

  private void createScan(Application application, ReportBuilder builder) throws IOException {
    createScan(application, builder, System.currentTimeMillis());
  }

  private void createScan(Application application, ReportBuilder builder, long time) throws IOException {
    String scanId = UUID.randomUUID().toString().replace("-", "");
    if (builder != null) {
      builder.build(application, scanId, new Date(time), insightWork.getReportDir(application.getId(), scanId));
    }
  }

  protected Application createApplication(String appId) {
    return temporaryEntity.newApplication(appId, appId, temporaryEntity.newOrganization().getId());
  }
}
