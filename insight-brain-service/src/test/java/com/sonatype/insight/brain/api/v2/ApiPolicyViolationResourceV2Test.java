/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentTransitivePolicyViolationsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiCrossStageViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiEnhancedPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestsApplicableToViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiversApplicableToViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStagePolicyViolationComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.service.PolicyViolationType;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverExclusionDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverDTOTestUtils.assertApiPolicyWaiverDTO;
import static com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverRequestDTOTestUtils.assertPolicyWaiverRequestDTO;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyViolationResourceV2Test
    extends AbstractResourceTest
{
  private PolicyViolationDAO policyViolationDAO;

  private AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO;

  @Before
  public void setUp() {
    policyViolationDAO = lookup(PolicyViolationDAO.class);
    autoPolicyWaiverExclusionDAO = lookup(AutoPolicyWaiverExclusionDAO.class);
  }

  @Test
  public void testGetPolicyViolations() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy orgPolicy = tempEntity.newPolicy(org);
    PolicyEvaluation pe1App1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation pv1App1 = tempEntity.newPolicyViolation(pe1App1, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    HttpResponse response = restRequest().path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .query("p", orgPolicy.getId())
        .get();

    assertResponseStatus(200, response);
    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTO = response
        .getBody(ApiApplicationViolationListDTOV2.class);

    assertThat(apiApplicationViolationListDTO.applicationViolations).hasSize(1);
    ApiApplicationViolationDTOV2 apiApplicationViolationDTO = apiApplicationViolationListDTO.applicationViolations
        .get(0);
    assertThat(apiApplicationViolationDTO.application).isNotNull();
    assertThat(apiApplicationViolationDTO.application.id).isEqualTo(app.getId());
    assertThat(apiApplicationViolationDTO.application.name).isEqualTo(app.getName());
    assertThat(apiApplicationViolationDTO.application.publicId).isEqualTo(app.getPublicId());
    assertThat(apiApplicationViolationDTO.application.contactUserName).isEqualTo(app.getContactInternalName());
    assertThat(apiApplicationViolationDTO.application.organizationId).isEqualTo(app.getOrganizationId());

    assertThat(apiApplicationViolationDTO.policyViolations).hasSize(1);
    ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO = apiApplicationViolationDTO.policyViolations.get(0);
    assertThat(apiPolicyViolationDTO.policyId).isEqualTo(pv1App1.getPolicyId());
    assertThat(apiPolicyViolationDTO.policyName).isEqualTo(pv1App1.getPolicyName());
    assertThat(apiPolicyViolationDTO.policyViolationId).isEqualTo(pv1App1.getId());
    assertThat(apiPolicyViolationDTO.threatLevel).isEqualTo(pv1App1.getThreatLevel());
    assertThat(apiPolicyViolationDTO.reportUrl)
        .isEqualTo("ui/links/application/" + app.getPublicId() + "/report/" + pe1App1.getScanId());
    assertThat(apiPolicyViolationDTO.stageId).isEqualTo(pe1App1.getStageTypeId());
    assertThat(apiPolicyViolationDTO.reportId).isEqualTo("scanId1App1");
    assertThat(apiPolicyViolationDTO.component.hash).isEqualTo(pv1App1.getHash());
    assertThat(apiPolicyViolationDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(pv1App1.getComponentIdentifier());
    assertThat(apiPolicyViolationDTO.component.packageUrl).isEqualTo("pkg:maven/g1/a1@v1");
    assertThat(apiPolicyViolationDTO.component.displayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(pv1App1.getComponentIdentifier()).toString());

    assertThat(apiPolicyViolationDTO.constraintViolations).hasSize(1);
    ApiConstraintViolationDTO apiConstraintViolationDTO = apiPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId).isEqualTo(pv1App1.getConstraintFacts().get(0).getConstraintId());
    assertThat(apiConstraintViolationDTO.constraintName)
        .isEqualTo(pv1App1.getConstraintFacts().get(0).getConstraintName());

    assertThat(apiConstraintViolationDTO.reasons).hasSize(1);
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason)
        .isEqualTo(pv1App1.getConstraintFacts().get(0).getConditionFacts().get(0).getReason());
    assertThat(apiConstraintViolationReasonDTO.reference).isNotNull();
    assertThat(apiConstraintViolationReasonDTO.reference.value)
        .isEqualTo(pv1App1.getConstraintFacts().get(0).getConditionFacts().get(0).getReason());
    assertThat(apiConstraintViolationReasonDTO.reference.type).isEqualTo("SECURITY_VULNERABILITY_REFID");
  }

  @Test
  public void testGetCrossStagePolicyViolationById() throws Exception {
    Date date = new Date();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy orgPolicy = tempEntity.newPolicy(org);
    PolicyEvaluation pe1App1 = tempEntity
        .newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1", false, false, date);
    PolicyViolation pv1App1 = tempEntity.newPolicyViolation(pe1App1, orgPolicy, "g1", "a1", "v1", "h1", "r1");
    String fullPath = ApiPolicyViolationResourceV2.CROSS_STAGE_POLICY_VIOLATION_SUBPATH
        + ApiPolicyViolationResourceV2.VIOLATIONID;

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(fullPath)
        .parameter(pv1App1.getId())
        .get();

    assertResponseStatus(200, response);
    ApiCrossStageViolationDTOV2 resultDTO = response.getBody(ApiCrossStageViolationDTOV2.class);
    assertThat(resultDTO.policyViolationId).isEqualTo(pv1App1.getId());
    assertThat(resultDTO.applicationPublicId).isEqualTo(app.getPublicId());
    assertThat(resultDTO.applicationName).isEqualTo(app.getName());
    assertThat(resultDTO.stageData).containsOnlyKeys(BuildStageType.ID);
    assertThat(resultDTO.displayName.toString()).isEqualTo("g1 : a1 : v1");
    assertThat(resultDTO.stageData).hasEntrySatisfying(BuildStageType.ID, stageData -> {
      assertThat(stageData.mostRecentEvaluationTime).isEqualTo(date);
      assertThat(stageData.mostRecentScanId).isEqualTo("scanId1App1");
    });
  }

  @Test
  public void testGetCrossStagePolicyViolationByConstituentId() throws Exception {
    Date baseDate = new Date();
    Date later = new Date(baseDate.getTime() + 1);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy orgPolicy = tempEntity.newPolicy(org);
    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1", false,
        false, baseDate);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(evaluation1, orgPolicy, "g1", "a1", "v1", "h1", "r1");
    // Equivalent, opened while violation1 was still open
    PolicyEvaluation evaluation2 = tempEntity
        .newPolicyEvaluation(app.getId(), DevelopStageType.ID, "scanId2App1", false, false, later);
    tempEntity.newPolicyViolation(evaluation2, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(ApiPolicyViolationResourceV2.CROSS_STAGE_POLICY_VIOLATION_SUBPATH)
        .query("constituentId", violation1.getId())
        .get();

    assertResponseStatus(200, response);
    ApiCrossStageViolationDTOV2 resultDTO = response.getBody(ApiCrossStageViolationDTOV2.class);
    assertThat(resultDTO.policyViolationId).isEqualTo(violation1.getId());
    assertThat(resultDTO.applicationPublicId).isEqualTo(app.getPublicId());
    assertThat(resultDTO.applicationName).isEqualTo(app.getName());
    assertThat(resultDTO.stageData).hasSize(2);
    assertThat(resultDTO.stageData).containsOnlyKeys(BuildStageType.ID, DevelopStageType.ID);
    assertThat(resultDTO.displayName.toString()).isEqualTo("g1 : a1 : v1");
    assertThat(resultDTO.stageData).hasEntrySatisfying(BuildStageType.ID, stageData -> {
      assertThat(stageData.mostRecentEvaluationTime).isEqualTo(baseDate);
      assertThat(stageData.mostRecentScanId).isEqualTo("scanId1App1");
    });
    assertThat(resultDTO.stageData).hasEntrySatisfying(DevelopStageType.ID, stageData -> {
      assertThat(stageData.mostRecentEvaluationTime).isEqualTo(new Date(baseDate.getTime() + 1));
      assertThat(stageData.mostRecentScanId).isEqualTo("scanId2App1");
    });
  }

  @Test
  public void testGetApplicableWaivers() throws Exception {
    DateTime now = DateTime.now();
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    Policy policy2 = tempEntity.newPolicy(newApp);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    ComponentIdentifier identifierForAllVersionsWaiver =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "2.0", "c1", "jar");
    ComponentIdentifier identifierForAllVersionsWaiver2 =
        ComponentIdentifier.createMavenCoordinates("group", "otherArtifact", "2.0", "c1", "jar");
    String packageUrlAllVersionsWaiver = PackageUrlIdentifier.toPackageUrl(identifierForAllVersionsWaiver);
    String packageUrlAllVersionsWaiver2 = PackageUrlIdentifier.toPackageUrl(identifierForAllVersionsWaiver2);
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "hash");
    violation.setConstraintFacts(constraintFacts);
    policyViolationDAO.update(violation);

    String policyId = policy.getId();
    String policy2Id = policy2.getId();
    String orgId = newOrg.getId();
    String appId = newApp.getId();
    String violationId = violation.getId();

    Date expiredExpiryTime = now.minusMillis(1).toDate();
    Date expiringInFutureExpiryTime = now.plusMinutes(1).toDate();

    tempEntity.newWaiver("hashX", policyId, orgId, constraintFacts, packageUrlAllVersionsWaiver, ALL_VERSIONS, "",
        now.minusDays(10).toDate());
    tempEntity.newWaiver(null, policyId, orgId, constraintFacts, ALL_COMPONENTS, "", now.minusDays(9).toDate(), null);
    tempEntity.newWaiver("hash", policyId, appId, constraintFacts, packageUrlAllVersionsWaiver, EXACT_COMPONENT, "",
        now.minusDays(8).toDate(), expiredExpiryTime); // expired
    tempEntity.newWaiver(null, policyId, appId, constraintFacts, ALL_COMPONENTS, "A comment", now.minusDays(7).toDate(),
        expiringInFutureExpiryTime); // expiring in the future
    tempEntity.newWaiver("hash2", policy2Id, appId, null, packageUrlAllVersionsWaiver2, ALL_VERSIONS, "",
        now.minusDays(2).toDate());
    tempEntity.newWaiver(null, policy2Id, appId, null, ALL_COMPONENTS, "", now.minusDays(1).toDate(),
        now.plusMinutes(1).toDate());
    tempEntity.newWaiver("hash", policy2Id, appId, constraintFacts, packageUrlAllVersionsWaiver, EXACT_COMPONENT, "",
        now.toDate(), now.minusMillis(1).toDate());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(ApiPolicyViolationResourceV2.VIOLATIONID +
            ApiPolicyViolationResourceV2.APPLICABLE_WAIVERS_PATH)
        .parameter(violation.getId())
        .get();

    assertResponseStatus(200, response);
    ApiPolicyWaiversApplicableToViolationDTO apiPolicyWaivers =
        response.getBody(ApiPolicyWaiversApplicableToViolationDTO.class);

    List<ApiPolicyWaiverDTO> activeApplicableWaivers = apiPolicyWaivers.activeWaivers.stream()
        .sorted(Comparator.comparing(apiPolicyWaiverDTO -> apiPolicyWaiverDTO.createTime))
        .collect(Collectors.toList());

    assertThat(activeApplicableWaivers.size()).isEqualTo(3);
    assertApiPolicyWaiverDTO("hashX", policyId, orgId, "NewOrg", "", violationId, null,
        "testuser", "Test User", ALL_VERSIONS, packageUrlAllVersionsWaiver, null, null, activeApplicableWaivers.get(0));
    assertApiPolicyWaiverDTO(null, policyId, orgId, "NewOrg", "", violationId, null,
        "testuser", "Test User", ALL_COMPONENTS, null, null, null, activeApplicableWaivers.get(1));
    assertApiPolicyWaiverDTO(null, policyId, appId, "NewApp", "A comment", violationId, expiringInFutureExpiryTime,
        "testuser", "Test User", ALL_COMPONENTS, null, null, null, activeApplicableWaivers.get(2));

    List<ApiPolicyWaiverDTO> expiredApplicableWaivers = apiPolicyWaivers.expiredWaivers.stream()
        .sorted(Comparator.comparing(apiPolicyWaiverDTO -> apiPolicyWaiverDTO.createTime))
        .collect(Collectors.toList());

    assertThat(expiredApplicableWaivers.size()).isEqualTo(1);
    assertApiPolicyWaiverDTO("hash", policyId, appId, "NewApp", "", violationId, expiredExpiryTime,
        "testuser", "Test User", EXACT_COMPONENT, packageUrlAllVersionsWaiver, null, null,
        expiredApplicableWaivers.get(0));
  }

  @Test
  public void testGetApplicableWaiverRequests() throws Exception {
    Date now = new Date();
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    Policy policyOrg = tempEntity.newPolicy(org);
    Policy policyApp = tempEntity.newPolicy(app);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    ComponentIdentifier identifierForAllVersionsWaiver =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "2.0", "c1", "jar");
    ComponentIdentifier identifierForAllVersionsWaiver2 =
        ComponentIdentifier.createMavenCoordinates("group", "otherArtifact", "2.0", "c1", "jar");
    String packageUrlAllVersions1 = PackageUrlIdentifier.toPackageUrl(identifierForAllVersionsWaiver);
    String packageUrlAllVersions2 = PackageUrlIdentifier.toPackageUrl(identifierForAllVersionsWaiver2);
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policyOrg, identifier, "hash");
    violation.setConstraintFacts(constraintFacts);
    policyViolationDAO.update(violation);

    String policyIdOrg = policyOrg.getId();
    String policyIdApp = policyApp.getId();
    String orgId = org.getId();
    String appId = app.getId();

    Date expiredExpiryTime = DateUtils.addMilliseconds(now, -1);
    Date expiringInFutureExpiryTime = DateUtils.addMinutes(now, 1);

    PolicyWaiverRequest policyWaiverRequest1 =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hashX")
            .setPolicyId(policyIdOrg)
            .setOwnerId(orgId)
            .setConstraintFacts(constraintFacts)
            .setAssociatedPackageUrl(packageUrlAllVersions1)
            .setComponentMatchStrategy(ALL_VERSIONS)
            .setRequestTime(DateUtils.addDays(now, -10)));
    PolicyWaiverRequest policyWaiverRequest2 = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash(null)
        .setPolicyId(policyIdOrg)
        .setOwnerId(orgId)
        .setConstraintFacts(constraintFacts)
        .setComponentMatchStrategy(ALL_COMPONENTS)
        .setRequestTime(DateUtils.addDays(now, -9)));
    PolicyWaiverRequest policyWaiverRequest3 = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("hash")
        .setPolicyId(policyIdOrg)
        .setOwnerId(appId)
        .setConstraintFacts(constraintFacts)
        .setAssociatedPackageUrl(packageUrlAllVersions1)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setExpiryTime(expiredExpiryTime)
        .setRequestTime(DateUtils.addDays(now, -8)));
    PolicyWaiverRequest policyWaiverRequest4 =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash(null)
            .setPolicyId(policyIdOrg)
            .setOwnerId(appId)
            .setConstraintFacts(constraintFacts)
            .setComponentMatchStrategy(ALL_COMPONENTS)
            .setExpiryTime(expiringInFutureExpiryTime)
            .setRequestTime(DateUtils.addDays(now, -7)));
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash2")
        .setPolicyId(policyIdApp)
        .setOwnerId(appId)
        .setConstraintFacts(null)
        .setAssociatedPackageUrl(packageUrlAllVersions2)
        .setComponentMatchStrategy(ALL_VERSIONS)
        .setRequestTime(DateUtils.addDays(now, -2)));
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash(null)
        .setPolicyId(policyIdApp)
        .setOwnerId(appId)
        .setConstraintFacts(null)
        .setComponentMatchStrategy(ALL_COMPONENTS)
        .setExpiryTime(expiringInFutureExpiryTime)
        .setRequestTime(DateUtils.addDays(now, -1)));
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash")
        .setPolicyId(policyIdApp)
        .setOwnerId(appId)
        .setConstraintFacts(constraintFacts)
        .setAssociatedPackageUrl(packageUrlAllVersions1)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setExpiryTime(expiredExpiryTime)
        .setRequestTime(now));

    HttpResponse response = restRequest().path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(ApiPolicyViolationResourceV2.VIOLATIONID + ApiPolicyViolationResourceV2.APPLICABLE_WAIVER_REQUESTS_PATH)
        .parameter(violation.getId())
        .get();

    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestsApplicableToViolationDTO apiPolicyWaiverRequests =
        response.getBody(ApiPolicyWaiverRequestsApplicableToViolationDTO.class);

    List<ApiPolicyWaiverRequestDTO> activeApplicableWaiverRequests =
        apiPolicyWaiverRequests.activeWaiverRequests.stream()
            .sorted(Comparator.comparing(apiPolicyWaiverRequestDTO -> apiPolicyWaiverRequestDTO.requestTime))
            .toList();

    assertThat(activeApplicableWaiverRequests).hasSize(3);
    assertPolicyWaiverRequestDTO(activeApplicableWaiverRequests.get(0), policyWaiverRequest1);
    assertPolicyWaiverRequestDTO(activeApplicableWaiverRequests.get(1), policyWaiverRequest2);
    assertPolicyWaiverRequestDTO(activeApplicableWaiverRequests.get(2), policyWaiverRequest4);

    List<ApiPolicyWaiverRequestDTO> expiredApplicableWaiverRequests =
        apiPolicyWaiverRequests.expiredWaiverRequests.stream()
            .sorted(Comparator.comparing(apiPolicyWaiverRequestDTO -> apiPolicyWaiverRequestDTO.requestTime))
            .toList();

    assertThat(expiredApplicableWaiverRequests).hasSize(1);
    assertPolicyWaiverRequestDTO(expiredApplicableWaiverRequests.get(0), policyWaiverRequest3);
  }

  @Test
  public void testGetSimilarWaivers() throws Exception {
    DateTime now = DateTime.now();
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    ComponentIdentifier identifierForAllVersionsWaiver =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "2.0", "c1", "jar");
    String packageUrlAllVersionsWaiver = PackageUrlIdentifier.toPackageUrl(identifierForAllVersionsWaiver);
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "hash");
    policyViolation.setConstraintFacts(constraintFacts);
    policyViolationDAO.update(policyViolation);

    String policyId = policy.getId();
    String orgId = newOrg.getId();
    String violationId = policyViolation.getId();

    List<ConstraintFact> constraintFactsCopy = new ArrayList<>(policyViolation.getConstraintFacts());
    constraintFactsCopy.add(new ConstraintFact("id", "Test Constraint 2", null));
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hashX",
        policyId,
        orgId,
        constraintFactsCopy,
        packageUrlAllVersionsWaiver,
        ALL_VERSIONS,
        "",
        now.minusDays(10).toDate());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(ApiPolicyViolationResourceV2.VIOLATIONID +
            ApiPolicyViolationResourceV2.SIMILAR_WAIVERS_PATH)
        .parameter(policyViolation.getId())
        .get();

    assertResponseStatus(200, response);
    ApiPolicyWaiverDTO[] apiPolicyWaivers = response.getBody(ApiPolicyWaiverDTO[].class);

    assertThat(apiPolicyWaivers).isNotEmpty();
    assertThat(apiPolicyWaivers).hasSize(1);
    assertApiPolicyWaiverDTO("hashX", policyId, orgId, "NewOrg", "", violationId, null,
        "testuser", "Test User", ALL_VERSIONS, packageUrlAllVersionsWaiver, null, null, apiPolicyWaivers[0]);
    assertThat(policyWaiver.getConstraintFactsJson()).isEqualTo(apiPolicyWaivers[0].constraintFactsJson);
  }

  @Test
  public void testGetTransitivePolicyViolationsByOwnerStageComponent() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    Policy policy = tempEntity.newPolicy();
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e");
    ComponentIdentifier transitive = ComponentIdentifier.createMavenCoordinates("g", "transitive", "v", "", "e");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, transitive, "hash2");
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationResourceV2Test/report", tempDir),
        getCLMServer().getInstance(InsightWork.class));
    ReportHelper.createPolicyThreats(
        getCLMServer().getInstance(InsightWork.class),
        application.getId(),
        scanId,
        Collections.singletonList(policyViolation));

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_OWNER_AND_STAGE_PATH)
        .parameter(application.getType().name().toLowerCase(Locale.ROOT), application.getPublicId(), BuildStageType.ID)
        .query("componentIdentifier", direct)
        .get();

    assertResponseStatus(200, response);
    ApiComponentTransitivePolicyViolationsDTO result =
        response.getBody(ApiComponentTransitivePolicyViolationsDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.componentIdentifier).usingRecursiveComparison()
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(direct));
    assertThat(result.packageUrl).isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(direct).getPackageUrl());
    assertThat(result.hash).isEqualTo("hash1");
    assertThat(result.displayName).isEqualTo("g : direct : v");
    assertThat(result.isInnerSource).isFalse();
    Component expectedComponent = new Component();
    expectedComponent.setHash("hash2");
    expectedComponent.setDisplayName("g : transitive : v");
    assertThat(result.transitivePolicyViolations).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(policyViolation, expectedComponent)));
  }

  @Test
  public void testGetTransitivePolicyViolationsByAppScanComponent() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
    Policy policy = tempEntity.newPolicy();
    ComponentIdentifier direct = ComponentIdentifier.createMavenCoordinates("g", "direct", "v", "", "e");
    ComponentIdentifier transitive = ComponentIdentifier.createMavenCoordinates("g", "transitive", "v", "", "e");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy, transitive, "hash2");
    ReportTestUtils.createReportFile(application.getId(), scanId,
        zipReportDir("/ApiPolicyViolationResourceV2Test/report", tempDir),
        getCLMServer().getInstance(InsightWork.class));
    ReportHelper.createPolicyThreats(
        getCLMServer().getInstance(InsightWork.class),
        application.getId(),
        scanId,
        Collections.singletonList(policyViolation));

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_APP_AND_SCAN_PATH)
        .parameter(application.getType().name().toLowerCase(Locale.ROOT), application.getPublicId(), scanId)
        .query("componentIdentifier", direct)
        .get();

    assertResponseStatus(200, response);
    ApiComponentTransitivePolicyViolationsDTO result =
        response.getBody(ApiComponentTransitivePolicyViolationsDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.componentIdentifier).usingRecursiveComparison()
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(direct));
    assertThat(result.packageUrl).isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(direct).getPackageUrl());
    assertThat(result.hash).isEqualTo("hash1");
    assertThat(result.displayName).isEqualTo("g : direct : v");
    assertThat(result.isInnerSource).isFalse();
    Component expectedComponent = new Component();
    expectedComponent.setHash("hash2");
    expectedComponent.setDisplayName("g : transitive : v");
    assertThat(result.transitivePolicyViolations).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            ApiStagePolicyViolationComponentDTO
                .fromPolicyViolationAndComponent(Pair.of(policyViolation, expectedComponent)));
  }

  @Test
  public void testGetTransitivePolicyViolationsByOwnerStageComponent_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.setEnabled(false);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_OWNER_AND_STAGE_PATH)
        .parameter(OwnerType.APPLICATION.name().toLowerCase(Locale.ROOT), "doesNotExist", "doesNotExist")
        .query("hash", "doesNotExist")
        .get();

    assertResponseStatus(403, response);
    assertThat(response.getBodyText())
        .contains(SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.getId() + " feature is disabled.");
  }

  @Test
  public void testGetTransitivePolicyViolationsByAppScanComponent_FeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.setEnabled(false);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2,
            ApiPolicyViolationResourceV2.TRANSITIVE_VIOLATIONS_BY_APP_AND_SCAN_PATH)
        .parameter(OwnerType.APPLICATION.name().toLowerCase(Locale.ROOT), "doesNotExist", "doesNotExist")
        .query("hash", "doesNotExist")
        .get();

    assertResponseStatus(403, response);
    assertThat(response.getBodyText())
        .contains(SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.getId() + " feature is disabled.");
  }

  @Test
  public void testGetApplicableAutoWaiver() throws Exception {
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    String ownerId = newApp.getId();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(ownerId);
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "hash");
    violation.setConstraintFacts(constraintFacts);
    violation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());

    policyViolationDAO.update(violation);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(ApiPolicyViolationResourceV2.VIOLATIONID +
            ApiPolicyViolationResourceV2.APPLICABLE_AUTO_WAIVER_PATH)
        .parameter(violation.getId())
        .get();

    assertResponseStatus(200, response);

    ApiAutoPolicyWaiverDTO apiPolicyWaivers =
        response.getBody(ApiAutoPolicyWaiverDTO.class);

    assertThat(apiPolicyWaivers).isNotNull();
    assertThat(apiPolicyWaivers.ownerId).isEqualTo(ownerId);
    assertThat(apiPolicyWaivers.ownerName).isEqualTo(newApp.getName());
    assertThat(apiPolicyWaivers.ownerType).isEqualTo(OwnerType.APPLICATION.toString());
    assertThat(apiPolicyWaivers.publicId).isEqualTo(newApp.getPublicId());
    assertThat(apiPolicyWaivers.threatLevel).isEqualTo(7);
    assertThat(apiPolicyWaivers.reachability).isTrue();
    assertThat(apiPolicyWaivers.pathForward).isFalse();
    assertThat(apiPolicyWaivers.creatorId).isEqualTo("fakeCreatorId");
    assertThat(apiPolicyWaivers.creatorName).isEqualTo("fakeCreatorName");
  }

  @Test
  public void testGetApplicableAutoWaiver_NoAutoPolicyWaiverApplied() throws Exception {
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "hash");
    violation.setConstraintFacts(constraintFacts);

    policyViolationDAO.update(violation);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(ApiPolicyViolationResourceV2.VIOLATIONID +
            ApiPolicyViolationResourceV2.APPLICABLE_AUTO_WAIVER_PATH)
        .parameter(violation.getId())
        .get();

    assertResponseStatus(204, response);
  }

  @Test
  public void testGetApplicableAutoWaiver_whenExclusionAppliedOnAppLevelAutoPolicyWaiver() throws Exception {
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    String ownerId = newApp.getId();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(ownerId);
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "hash");
    violation.setConstraintFacts(constraintFacts);
    violation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());

    policyViolationDAO.update(violation);
    AutoPolicyWaiverExclusion autoPolicyWaiverExclusion =
        tempEntity.newAutoPolicyWaiverExclusion(
            ownerId,
            "fakeCreatorId",
            "fakeCreatorName",
            new Date(),
            autoPolicyWaiver.getId(),
            evaluation.getScanId(),
            "hash",
            ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(ApiPolicyViolationResourceV2.VIOLATIONID +
            ApiPolicyViolationResourceV2.APPLICABLE_AUTO_WAIVER_PATH)
        .parameter(violation.getId())
        .get();

    assertResponseStatus(204, response);

    // remove exclusion
    autoPolicyWaiverExclusionDAO.delete(autoPolicyWaiverExclusion);

    response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(ApiPolicyViolationResourceV2.VIOLATIONID +
            ApiPolicyViolationResourceV2.APPLICABLE_AUTO_WAIVER_PATH)
        .parameter(violation.getId())
        .get();

    assertResponseStatus(200, response);

    ApiAutoPolicyWaiverDTO apiPolicyWaivers =
        response.getBody(ApiAutoPolicyWaiverDTO.class);

    assertThat(apiPolicyWaivers).isNotNull();
    assertThat(apiPolicyWaivers.ownerId).isEqualTo(ownerId);
    assertThat(apiPolicyWaivers.threatLevel).isEqualTo(7);
    assertThat(apiPolicyWaivers.reachability).isTrue();
    assertThat(apiPolicyWaivers.pathForward).isFalse();
    assertThat(apiPolicyWaivers.creatorId).isEqualTo("fakeCreatorId");
    assertThat(apiPolicyWaivers.creatorName).isEqualTo("fakeCreatorName");
  }

  @Test
  public void testGetApplicableAutoWaiver_whenExclusionAppliedOnOrgLevelAutoPolicyWaiver() throws Exception {
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    String ownerId = newOrg.getId();
    // org level auto policy waiver
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(ownerId);
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "hash");
    violation.setConstraintFacts(constraintFacts);
    violation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());

    policyViolationDAO.update(violation);

    tempEntity.newAutoPolicyWaiverExclusion(
        ownerId,
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiver.getId(),
        evaluation.getScanId(),
        "hash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(ApiPolicyViolationResourceV2.VIOLATIONID +
            ApiPolicyViolationResourceV2.APPLICABLE_AUTO_WAIVER_PATH)
        .parameter(violation.getId())
        .get();

    assertResponseStatus(204, response);
  }

  // ALL VERSIONS
  @Test
  public void testGetApplicableAutoWaiver_ALL_VERSION_whenExclusionAppliedOnAppLevelAutoPolicyWaiver() throws Exception {
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    String ownerId = newApp.getId();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(ownerId);
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "hash");
    violation.setConstraintFacts(constraintFacts);
    violation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());

    policyViolationDAO.update(violation);

    // different version with no policy violation id
    ComponentIdentifier diffVersionIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "2.0", "c1", "jar");

    tempEntity.newAutoPolicyWaiverExclusion(
        ownerId,
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiver.getId(),
        evaluation.getScanId(),
        "hash",
        ComponentMatcherStrategyForExclusion.ALL_VERSIONS,
        null,
        null,
        null,
        null,
        null,
        null,
        diffVersionIdentifier,
        null);
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(ApiPolicyViolationResourceV2.VIOLATIONID +
            ApiPolicyViolationResourceV2.APPLICABLE_AUTO_WAIVER_PATH)
        .parameter(violation.getId())
        .get();

    assertResponseStatus(204, response);
  }

  @Test
  public void testGetApplicableAutoWaiver_AutoPolicyWaiverNotEnabled() throws Exception {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    String ownerId = newApp.getId();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(ownerId);
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "hash");
    violation.setConstraintFacts(constraintFacts);
    violation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());

    policyViolationDAO.update(violation);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .path(ApiPolicyViolationResourceV2.VIOLATIONID +
            ApiPolicyViolationResourceV2.APPLICABLE_AUTO_WAIVER_PATH)
        .parameter(violation.getId())
        .get();

    assertResponseStatus(403, response);
    assertThat(response.getBodyText())
        .contains("Auto Policy Waivers feature is not enabled");
  }

  @Test
  public void testGetPolicyViolationsWithDifferentTypes() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy orgPolicy = tempEntity.newPolicy(org);

    PolicyEvaluation pe1App1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");

    PolicyViolation activePv = tempEntity.newPolicyViolation(pe1App1, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    PolicyViolation waivedPv = tempEntity.newPolicyViolation(pe1App1, orgPolicy, "g2", "a2", "v2", "h2", "r2");
    waivedPv.setWaiveTime(new Date());
    policyViolationDAO.update(waivedPv);

    PolicyViolation legacyPv = tempEntity.newLegacyPolicyViolation(pe1App1, orgPolicy);

    // Default
    HttpResponse response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .query("p", orgPolicy.getId())
        .get();

    assertResponseStatus(200, response);
    ApiApplicationViolationListDTOV2 result = response.getBody(ApiApplicationViolationListDTOV2.class);
    assertThat(result.applicationViolations).hasSize(1);
    assertThat(result.applicationViolations.get(0).policyViolations)
        .hasSize(1)
        .extracting(pv -> pv.policyViolationId)
        .containsExactly(activePv.getId());

    // explicit active
    response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .query("p", orgPolicy.getId())
        .query("type", PolicyViolationType.ACTIVE.name())
        .get();
    assertResponseStatus(200, response);
    result = response.getBody(ApiApplicationViolationListDTOV2.class);
    assertThat(result.applicationViolations).hasSize(1);
    assertThat(result.applicationViolations.get(0).policyViolations)
        .hasSize(1)
        .extracting(pv -> pv.policyViolationId)
        .containsExactly(activePv.getId());

    // Waived and Legacy only
    response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .query("p", orgPolicy.getId())
        .query("type", PolicyViolationType.WAIVED.name(), PolicyViolationType.LEGACY.name())
        .get();

    assertResponseStatus(200, response);
    result = response.getBody(ApiApplicationViolationListDTOV2.class);
    assertThat(result.applicationViolations).hasSize(1);

    List<ApiEnhancedPolicyViolationDTOV2> violations = result.applicationViolations.get(0).policyViolations;
    assertThat(violations).hasSize(2);

    // waived violation
    assertThat(violations)
        .filteredOn(v -> v.policyViolationId.equals(waivedPv.getId()))
        .hasSize(1)
        .allSatisfy(v -> {
          assertThat(v.isWaived).isTrue();
          assertThat(v.isLegacy).isFalse();
        });

    // legacy violation
    assertThat(violations)
        .filteredOn(v -> v.policyViolationId.equals(legacyPv.getId()))
        .hasSize(1)
        .allSatisfy(v -> {
          assertThat(v.isWaived).isFalse();
          assertThat(v.isLegacy).isTrue();
        });

    response = restRequest()
        .path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
        .query("p", orgPolicy.getId())
        .query("type", PolicyViolationType.ACTIVE.name(),
            PolicyViolationType.WAIVED.name(),
            PolicyViolationType.LEGACY.name())
        .get();

    assertResponseStatus(200, response);
    result = response.getBody(ApiApplicationViolationListDTOV2.class);
    assertThat(result.applicationViolations).hasSize(1);
    assertThat(result.applicationViolations.get(0).policyViolations)
        .hasSize(3)
        .extracting(pv -> pv.policyViolationId)
        .containsExactlyInAnyOrder(activePv.getId(), waivedPv.getId(), legacyPv.getId());
  }
}
