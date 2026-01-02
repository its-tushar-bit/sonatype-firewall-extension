/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.callflow;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities.MissingReachableComponentVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities.PresentReachableComponentVulnerabilities;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.FileApplicationReportPersistenceService;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Test;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.POLICY_THREATS;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class PolicyViolationReachabilityServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyViolationReachabilityService policyViolationReachabilityService;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private FileApplicationReportPersistenceService applicationReportPersistenceService;

  private static final String CVE_REF_ID = "CVE-2020-13933";

  @Test
  public void testUpdateReachabilityStatusForPolicyViolations_updateOnBothDatabaseAndFileSystem() throws Exception {
    String scanId = "test-scanid";
    Application application = tempEntity.newApplicationWithParent("test-app");
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    PolicyViolation policyViolation = createSecurityPolicyViolation(policyEvaluation, policy, CVE_REF_ID);
    assertThat(policyViolation.getReachabilityStatus()).isNull();
    ApplicationReport reportZip = new ApplicationReport(applicationReportPersistenceService, application, scanId);
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> map = new HashMap<>();
    map.put(
        PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier()),
        new PresentReachableComponentVulnerabilities(Set.of(CVE_REF_ID))
    );

    policyViolationReachabilityService.updateReachabilityStatusForPolicyViolations(application.getId(), scanId, map,
        reportZip);

    List<PolicyViolation> policyViolations =
        policyViolationDAO.getActiveByApplicationIdAndStageId(application.getId(), Stage.ID_BUILD);
    assertThat(policyViolations).hasSize(1);
    assertThat(policyViolations.get(0).getReachabilityStatus()).isEqualTo(ReachabilityStatus.REACHABLE);
    ReportEntry reportEntry = reportZip.getEntry(POLICY_THREATS.getName());
    PolicyThreats policyThreats = JsonUtils.parse(reportEntry.buf, PolicyThreats.class);
    assertThat(policyThreats.aaData.get(0).activeViolations.get(0).reachabilityStatus)
        .isEqualTo(ReachabilityStatus.REACHABLE);
  }

  @Test
  public void testUpdateReachabilityStatusForPolicyViolations_updateOnFileSystemIncludingWaivedPolicy()
      throws IOException
  {
    String scanId = "test-scanid";
    Application application = tempEntity.newApplicationWithParent("test-app");
    Policy policy = tempEntity.newPolicy(application);
    Policy policywaived = tempEntity.newPolicy(application);
    policywaived.setLegacyViolationAllowed(true);
    policyDAO.update(policywaived);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("hash1", policywaived.getId(), application.getId(), "Some comments here");
    PolicyViolation waivedPolicyViolation =
        tempEntity.newWaivedPolicyViolation(policyEvaluation, policywaived, policyWaiver);
    PolicyViolation policyViolation = createSecurityPolicyViolation(policyEvaluation, policy, CVE_REF_ID);
    assertThat(policyViolation.getReachabilityStatus()).isNull();
    assertThat(waivedPolicyViolation.getReachabilityStatus()).isNull();
    ApplicationReport reportZip = new ApplicationReport(applicationReportPersistenceService, application, scanId);
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> map = new HashMap<>();
    map.put(
        PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier()),
        new PresentReachableComponentVulnerabilities(Set.of(CVE_REF_ID))
    );

    policyViolationReachabilityService.updateReachabilityStatusForPolicyViolations(application.getId(), scanId, map,
        reportZip);

    List<PolicyViolation> policyViolations =
        policyViolationDAO.getUnfixedByApplicationIdAndStageId(application.getId(), Stage.ID_BUILD);
    assertThat(policyViolations).hasSize(2);
    assertThat(policyViolations.get(0).getReachabilityStatus()).isEqualTo(ReachabilityStatus.UNKNOWN);
    assertThat(policyViolations.get(0).isWaived()).isTrue();
    assertThat(policyViolations.get(1).getReachabilityStatus()).isEqualTo(ReachabilityStatus.REACHABLE);
    ReportEntry reportEntry = reportZip.getEntry(POLICY_THREATS.getName());
    PolicyThreats policyThreats = JsonUtils.parse(reportEntry.buf, PolicyThreats.class);
    assertThat(policyThreats.aaData.get(0).activeViolations.get(0).reachabilityStatus).isEqualTo(
        ReachabilityStatus.REACHABLE);
    //make sure it includes the waived policy violation
    assertThat(policyThreats.aaData.get(0).allViolations.size()).isEqualTo(2);
  }

  @Test
  public void testUpdateReachabilityStatusForPolicyViolations_NonReachable() throws Exception {
    String scanId = "test-scanid";
    Application application = tempEntity.newApplicationWithParent("test-app");
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    PolicyViolation policyViolation = createSecurityPolicyViolation(policyEvaluation, policy, CVE_REF_ID);
    policyViolationDAO.update(policyViolation);
    ApplicationReport reportFile = new ApplicationReport(applicationReportPersistenceService, application, scanId);
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> map = new HashMap<>();
    map.put(
        PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier()),
        new PresentReachableComponentVulnerabilities(Set.of())
    );

    policyViolationReachabilityService.updateReachabilityStatusForPolicyViolations(application.getId(), scanId,
        map, reportFile);

    List<PolicyViolation> policyViolations =
        policyViolationDAO.getActiveByApplicationIdAndStageId(application.getId(), Stage.ID_BUILD);
    assertThat(policyViolations).hasSize(1);
    assertThat(policyViolations.get(0).getReachabilityStatus()).isEqualTo(ReachabilityStatus.NON_REACHABLE);
  }

  @Test
  public void testtestUpdateReachabilityStatusForPolicyViolations_NoSignatures() throws Exception {
    String scanId = "test-scanid";
    Application application = tempEntity.newApplicationWithParent("test-app");
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    PolicyViolation policyViolation = createSecurityPolicyViolation(policyEvaluation, policy, CVE_REF_ID);
    policyViolationDAO.update(policyViolation);
    ApplicationReport reportFile = new ApplicationReport(applicationReportPersistenceService, application, scanId);
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> reachableVulnerabilitiesByPurlIdentifiers =
        new HashMap<>();
    reachableVulnerabilitiesByPurlIdentifiers.put(
        PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier()),
        MissingReachableComponentVulnerabilities.INSTANCE
    );

    policyViolationReachabilityService.updateReachabilityStatusForPolicyViolations(application.getId(), scanId,
        reachableVulnerabilitiesByPurlIdentifiers, reportFile);

    List<PolicyViolation> policyViolations =
        policyViolationDAO.getActiveByApplicationIdAndStageId(application.getId(), Stage.ID_BUILD);
    assertThat(policyViolations).hasSize(1);
    assertThat(policyViolations.get(0).getReachabilityStatus()).isEqualTo(ReachabilityStatus.UNKNOWN);
  }

  private PolicyViolation createSecurityPolicyViolation(PolicyEvaluation policyEvaluation, Policy policy, String cve) {
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    policyViolation.setComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("com.h2database", "h2", "1.4.200"));

    ConditionFact conditionFact =
        new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "some summary", "some reason");
    conditionFact.setReference(new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, cve));

    ConstraintFact constraintFact = new ConstraintFact("someConstraintId", "some constraint name", "and");
    constraintFact.addConditionFact(conditionFact);

    policyViolation.setConstraintFacts(List.of(constraintFact));
    policyViolationDAO.update(policyViolation);
    return policyViolation;
  }
}
