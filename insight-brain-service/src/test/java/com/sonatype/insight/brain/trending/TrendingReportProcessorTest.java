/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.trending;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.trending.ApplicationRiskSummary;
import com.sonatype.insight.brain.model.trending.Applications;
import com.sonatype.insight.brain.model.trending.ComponentsSummary;
import com.sonatype.insight.brain.model.trending.PartialMatch;
import com.sonatype.insight.brain.model.trending.PolicyViolation;
import com.sonatype.insight.brain.model.trending.TrendingReport;
import com.sonatype.insight.brain.model.trending.TrendingReportMetadata;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationLog;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.trending.ReportBuilder.ConstraintFactBuilder;
import com.sonatype.insight.brain.trending.ReportBuilder.PolicyAlertBuilder;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TrendingReportProcessorTest
{
  private TrendingReportProcessor processor;
  private InsightWork insightWork;

  @Rule
  public TemporaryFolder workDir = new TemporaryFolder(new File("target").getAbsoluteFile());

  @Before
  public void createTwentyDayReportProcessor() {
    InsightConfig config = new InsightConfig();
    config.setSonatypeWork(workDir.getRoot().getAbsolutePath());
    insightWork = new InsightWork(config);

    ReportDownloader reportDownloader = null; // no downloading is expected, let them fail with NPE
    PolicyEvaluationUtils policyEvaluationUtils = new PolicyEvaluationUtils(insightWork, reportDownloader);
    processor = new TrendingReportProcessor(insightWork, policyEvaluationUtils);
  }

  @Test
  public void testMetadata() throws Exception {
    long beforeGeneration = new Date().getTime();
    TrendingReport report = processor.calculate();
    long afterGeneration = new Date().getTime();

    TrendingReportMetadata meta = report.getMeta();
    Assert.assertNotNull(meta.getGeneratedBy());
    Assert.assertNotNull(meta.getGeneratedFor());
    Assert.assertTrue(meta.getGeneratedOn() >= beforeGeneration && meta.getGeneratedOn() <= afterGeneration);
    Assert.assertEquals(TrendingReportProcessor.TWENTY_DAYS_MS, meta.getPeriodEnd() - meta.getPeriodStart());
  }

  @Test
  public void testComponentSummary() throws Exception {
    Application application = createApplication("testApp");
    ReportBuilder builder = new ReportBuilder();
    builder.addComponent().setGAV("a", "a", "a").setHash("A").setMatchState(MatchState.EXACT);
    builder.addComponent().setGAV("b", "b", "b").setHash("B").setMatchState(MatchState.SIMILAR);
    builder.addComponent().setGAV("c", "c", "c").setHash("C").setMatchState(MatchState.UNKNOWN);
    createScan(application, builder);

    TrendingReport report = processor.calculate();

    ComponentsSummary components = report.getComponents();
    Assert.assertEquals(1, components.getExact());
    Assert.assertEquals(1, components.getPartial());
    Assert.assertEquals(1, components.getUnknown());
    Assert.assertEquals(3, components.getInApplication());
  }

  @Test
  public void testApplications() throws Exception {
    ReportBuilder builder = new ReportBuilder();
    // 1
    builder.addPolicyAlert("a", 10).addComponentFact("a").addConstraintFact().addConditionFact("a");
    PolicyAlertBuilder policyAlertB = builder.addPolicyAlert("b", 6);
    policyAlertB.addComponentFact("a").addConstraintFact().addConditionFact("b");
    policyAlertB.addComponentFact("b").addConstraintFact().addConditionFact("b");
    builder.addPolicyAlert("c", 2).addComponentFact("c").addConstraintFact().addConditionFact("c");
    builder.addPolicyAlert("d", 0).addComponentFact("d").addConstraintFact().addConditionFact("d");
    createScan(createApplication("testApp1"), builder);
    // 2
    builder = new ReportBuilder();
    builder.addPolicyAlert("e", 9).addComponentFact("e").addConstraintFact().addConditionFact("e");
    createScan(createApplication("testApp2"), builder);
    // 3
    builder = new ReportBuilder();
    builder.addPolicyAlert("f", 8).addComponentFact("f").addConstraintFact().addConditionFact("f");
    createScan(createApplication("testApp3"), builder);
    // 4
    builder = new ReportBuilder();
    builder.addPolicyAlert("g", 7).addComponentFact("g").addConstraintFact().addConditionFact("g");
    createScan(createApplication("testApp4"), builder);
    // 5
    builder = new ReportBuilder();
    builder.addPolicyAlert("h", 6).addComponentFact("h").addConstraintFact().addConditionFact("h");
    createScan(createApplication("testApp5"), builder);
    // 6 cut off
    builder = new ReportBuilder();
    builder.addPolicyAlert("i", 5).addComponentFact("i").addConstraintFact().addConditionFact("i");
    createScan(createApplication("testApp6"), builder);

    TrendingReport report = processor.calculate();

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
    ReportBuilder builder = new ReportBuilder();
    builder.addPolicyAlert("a", 10).addComponentFact("a").addConstraintFact().addConditionFact("a");

    long time = System.currentTimeMillis();
    long ONE_DAY_MS = 86400L * 1000;
    createScan(application, builder, time - (21 * ONE_DAY_MS)); // this is expected to be ignored
    createScan(application, builder, time - (16 * ONE_DAY_MS));
    createScan(application, builder, time - (11 * ONE_DAY_MS));
    createScan(application, builder, time - (6 * ONE_DAY_MS));
    createScan(application, builder, time - (1 * ONE_DAY_MS));

    TrendingReport report = processor.calculate();

    List<PolicyViolation> violations = report.getViolations();
    Assert.assertEquals(1, violations.size());
    Assert.assertArrayEquals(new int[] { 1, 1, 1, 1 }, violations.get(0).getViolations());
  }

  @Test
  public void testPolicyViolationsCategories() throws Exception {
    Application application = createApplication("testApp");
    ReportBuilder builder = new ReportBuilder();
    ConstraintFactBuilder security = builder.addPolicyAlert("a", 0).addComponentFact("a").addConstraintFact();
    security.addConditionFact("security");
    security.addConditionFact("license");
    security.addConditionFact("quality");
    security.addConditionFact("other");
    ConstraintFactBuilder license = builder.addPolicyAlert("b", 1).addComponentFact("b").addConstraintFact();
    license.addConditionFact("license");
    license.addConditionFact("quality");
    license.addConditionFact("other");
    ConstraintFactBuilder quality1 = builder.addPolicyAlert("c1", 2).addComponentFact("c1").addConstraintFact();
    quality1.addConditionFact("age");
    quality1.addConditionFact("other");
    ConstraintFactBuilder quality2 = builder.addPolicyAlert("c2", 3).addComponentFact("c2").addConstraintFact();
    quality2.addConditionFact("popularity");
    quality2.addConditionFact("other");
    ConstraintFactBuilder other = builder.addPolicyAlert("d", 4).addComponentFact("d").addConstraintFact();
    other.addConditionFact("other");
    createScan(application, builder);

    TrendingReport report = processor.calculate();
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
    ReportBuilder builder = new ReportBuilder();
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

    TrendingReport report = processor.calculate();

    List<PartialMatch> partialMatches = report.getPartialMatches();

    Assert.assertEquals(TrendingReportProcessor.PARTIAL_MATCHES_COUNT, partialMatches.size());
    assertPartialMatch(partialMatches.get(0), "a", "a", "1", 6);
    assertPartialMatch(partialMatches.get(1), "a", "b", "2", 5);
    assertPartialMatch(partialMatches.get(2), "a", "c", "3", 4);
    assertPartialMatch(partialMatches.get(3), "a", "d", "4", 3);
    assertPartialMatch(partialMatches.get(4), "a", "e", "5", 2);
  }

  private void assertPartialMatch(PartialMatch partialMatch, String groupId, String artifactId, String version,
      int count)
  {
    // TODO better way to report failures
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
    builder.build(insightWork.getReportDir(application.getId(), scanId));
    PolicyEvaluationLog log = new PolicyEvaluationLog(insightWork.getAuditDir(application.getId()));
    log.add(new Stage(BuildStageType.ID), scanId, false, "nobody", null, time);
  }

  // create application and reports plumbing
  // TODO move to a reusable helper

  private Collection<Organization> organizationsToDelete = new ArrayList<Organization>();

  protected Application createApplication(String appId) {
    OrganizationDAO dao = new OrganizationDAO();
    Organization organization = new Organization();
    organization.setName("DUMMY-ORG-" + appId);
    dao.insert(organization, true);
    organizationsToDelete.add(organization);

    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application application = new Application();
    application.setPublicId(appId);
    application.setName(appId);
    application.setOrganizationId(organization != null ? organization.getId() : null);
    applicationDAO.insert(application);
    return application;
  }

  @After
  public void cleanupDatabase() {
    OrganizationDAO dao = new OrganizationDAO();
    ApplicationDAO applicationDAO = new ApplicationDAO();
    for (Organization organization : organizationsToDelete) {
      for (Application application : applicationDAO.getByOrganizationId(organization.getId())) {
        applicationDAO.delete(application);
      }
      dao.delete(organization);
    }
    organizationsToDelete.clear();
  }
}
