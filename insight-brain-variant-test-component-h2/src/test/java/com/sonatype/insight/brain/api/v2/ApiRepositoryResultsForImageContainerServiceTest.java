/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerDto;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerRequestDto;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerRequestDto.SearchFilter;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerRequestDto.SearchFilter.FilterableField;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerRequestDto.ViolationStateFilter;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerResponseDto;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.LastPolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryContainerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainerFilter.SortField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainerFilter.SortField.SortableField;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ComponentH2Test
public class ApiRepositoryResultsForImageContainerServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApiRepositoryResultsForImageContainerService repositoryResultsService;

  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private LastPolicyEvaluationDAO lastPolicyEvaluationDAO;

  private PolicyViolationDAO policyViolationDAO;

  private RepositoryManagerDAO repositoryManagerDAO;

  private RepositoryContainerDAO repositoryContainerDAO;

  private RepositoryManager repositoryManager;

  private Organization repositoryManagerOrganization;

  private RepositoryContainer repositoryContainer;

  private Organization containerOrganization;

  private Repository repository;

  private Organization organization;

  private Application application1;

  private Application application2;

  private PolicyEvaluation policyEvaluation1;

  private PolicyEvaluation policyEvaluation2;

  @BeforeEach
  public void setup() {
    Date now = new Date();
    repositoryDAO = lookup(RepositoryDAO.class);
    lastPolicyEvaluationDAO = lookup(LastPolicyEvaluationDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    repositoryContainerDAO = lookup(RepositoryContainerDAO.class);
    repositoryManagerDAO = lookup(RepositoryManagerDAO.class);

    repositoryContainer = RepositoryContainer.SINGLETON;
    containerOrganization = tempEntity.newOrganization("containerOrg");
    repositoryContainerDAO.setRelatedOrganizationIdNotNull(containerOrganization.getId());

    // RepositoryManager setup
    repositoryManager = tempEntity.newRepositoryManager();
    repositoryManagerOrganization = tempEntity.newOrganization("repositoryManagerOrg");
    repositoryManagerOrganization.setRelatedRepositoryManagerId(repositoryManager.getId());
    repositoryManager.setRelatedOrganizationId(repositoryManagerOrganization.getId());
    organizationDAO.update(repositoryManagerOrganization);
    repositoryManagerDAO.update(repositoryManager);
    repository = tempEntity.newRepository(repositoryManager, "publicId");

    // Repository setup
    organization = tempEntity.newOrganization("org");
    organization.setRelatedRepositoryId(repository.getId());
    organization.setParentOrganizationId(repositoryManagerOrganization.getId());
    repository.setRelatedOrganizationId(organization.getId());
    repositoryDAO.update(repository);
    organizationDAO.update(organization);

    // Container Image applications
    application1 = tempEntity.newApplication("app1", "appPublicId1", organization.getId());
    application2 = tempEntity.newApplication("app2", "appPublicId2", organization.getId());

    tempEntity.newPolicyEvaluation(application1.getId(), "proxy", "scanIdOld",
        new Date(now.getTime() - 1000), "abcdef1234abcdef1234abcdef1234abcdef1234");
    policyEvaluation1 = tempEntity.newPolicyEvaluation(application1.getId(), "proxy", "scanId1");
    policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), "proxy", "scanId2");

    // last policy evaluation
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application1.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application2.getId(), "proxy");
  }

  @Test
  public void testGetDetails_NonAggregated_SortByThreatLevelAndObjectName() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    // create policy violations
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

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(6);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(2);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(8);
    assertThat(repositoryResultsDetails.get(3).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(4).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(5).threatLevel).isEqualTo(10);

    assertThat(repositoryResultsDetails.get(0).objectName).isEqualTo("app2");
    assertThat(repositoryResultsDetails.get(1).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(2).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(3).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(4).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(5).objectName).isEqualTo("app2");

    assertThat(repositoryResultsDetails.get(0).applicationPublicId).isEqualTo("appPublicId2");
    assertThat(repositoryResultsDetails.get(1).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(2).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(3).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(4).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(5).applicationPublicId).isEqualTo("appPublicId2");

    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy6");
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy4");
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(3).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(4).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(5).policyName).isEqualTo("policy5");

    assertThat(repositoryResultsDetails.get(0).scanId).isEqualTo("scanId2");
    assertThat(repositoryResultsDetails.get(1).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(2).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(3).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(4).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(5).scanId).isEqualTo("scanId2");
  }

  @Test
  public void testGetDetails_Aggregated_SortByThreatLevelAndObjectName() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    // create policy violations
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEvaluation1, policy4);

    PolicyViolation policyViolation5 = tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation6 = tempEntity.newPolicyViolation(policyEvaluation2, policy6);

    policyViolation1.setThreatLevel(10);
    policyViolation1.setActionTypeId(Action.ID_FAIL);
    policyViolation2.setThreatLevel(8);
    policyViolation2.setActionTypeId(Action.ID_FAIL);
    policyViolation3.setThreatLevel(10);
    policyViolation3.setActionTypeId(Action.ID_FAIL);
    policyViolation4.setThreatLevel(5);
    policyViolation4.setActionTypeId(Action.ID_FAIL);
    policyViolation5.setThreatLevel(10);
    policyViolation5.setActionTypeId(Action.ID_FAIL);
    policyViolation6.setThreatLevel(2);
    policyViolation6.setActionTypeId(Action.ID_FAIL);

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    // ascending order
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.aggregate = true;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(2);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(10);

    assertThat(repositoryResultsDetails.get(0).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(1).objectName).isEqualTo("app2");

    assertThat(repositoryResultsDetails.get(0).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(1).applicationPublicId).isEqualTo("appPublicId2");

    assertThat(repositoryResultsDetails.get(0).policyName).isNull();
    assertThat(repositoryResultsDetails.get(1).policyName).isNull();

    assertThat(repositoryResultsDetails.get(0).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(1).scanId).isEqualTo("scanId2");

    assertThat(repositoryResultsDetails.get(0).violationCount).isEqualTo(4);
    assertThat(repositoryResultsDetails.get(1).violationCount).isEqualTo(2);

    // descending order
    SortField sortField3 = new SortField();
    sortField3.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField3.sortPriority = 1;
    sortField3.asc = false;

    SortField sortField4 = new SortField();
    sortField4.sortableField = SortableField.OBJECT_NAME;
    sortField4.sortPriority = 2;
    sortField4.asc = false;

    RepositoryResultsForImageContainerRequestDto detailsRequest2 = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest2.page = 1;
    detailsRequest2.pageSize = 50;
    detailsRequest2.aggregate = true;
    detailsRequest2.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest2.sortFields = Arrays.asList(sortField3, sortField4);

    RepositoryResultsForImageContainerResponseDto responseDto2 =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest2);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails2 = responseDto2.repositoryResultsDetails;

    assertThat(repositoryResultsDetails2).hasSize(2);
    assertThat(repositoryResultsDetails2.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails2.get(1).threatLevel).isEqualTo(10);

    assertThat(repositoryResultsDetails2.get(0).objectName).isEqualTo("app2");
    assertThat(repositoryResultsDetails2.get(1).objectName).isEqualTo("app1");

    assertThat(repositoryResultsDetails2.get(0).applicationPublicId).isEqualTo("appPublicId2");
    assertThat(repositoryResultsDetails2.get(1).applicationPublicId).isEqualTo("appPublicId1");

    assertThat(repositoryResultsDetails2.get(0).policyName).isNull();
    assertThat(repositoryResultsDetails2.get(1).policyName).isNull();

    assertThat(repositoryResultsDetails2.get(0).scanId).isEqualTo("scanId2");
    assertThat(repositoryResultsDetails2.get(1).scanId).isEqualTo("scanId1");

    assertThat(repositoryResultsDetails2.get(0).violationCount).isEqualTo(2);
    assertThat(repositoryResultsDetails2.get(1).violationCount).isEqualTo(4);
  }

  @Test
  public void testGetDetails_NonAggregated_SortByQuarantineTime() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    // create policy violations
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEvaluation1, policy4);

    PolicyViolation policyViolation5 = tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation6 = tempEntity.newPolicyViolation(policyEvaluation2, policy6);

    LocalDate localDate1 = LocalDate.of(2023, 10, 18);
    LocalDate localDate2 = LocalDate.of(2023, 10, 19);
    LocalDate localDate3 = LocalDate.of(2023, 10, 20);
    LocalDate localDate4 = LocalDate.of(2023, 10, 21);
    LocalDate localDate5 = LocalDate.of(2023, 10, 22);
    ZoneId defaultZoneId = ZoneId.systemDefault();
    Date date1 = Date.from(localDate1.atStartOfDay(defaultZoneId).toInstant());
    Date date2 = Date.from(localDate2.atStartOfDay(defaultZoneId).toInstant());
    Date date3 = Date.from(localDate3.atStartOfDay(defaultZoneId).toInstant());
    Date date4 = Date.from(localDate4.atStartOfDay(defaultZoneId).toInstant());
    Date date5 = Date.from(localDate5.atStartOfDay(defaultZoneId).toInstant());

    policyViolation1.setOpenTime(date1);
    policyViolation2.setOpenTime(date1);
    policyViolation3.setOpenTime(date2);
    policyViolation4.setOpenTime(date3);
    policyViolation5.setOpenTime(date4);
    policyViolation6.setOpenTime(date5);

    policyViolation1.setThreatLevel(10);
    policyViolation2.setThreatLevel(8);
    policyViolation3.setThreatLevel(10);
    policyViolation4.setThreatLevel(5);
    policyViolation5.setThreatLevel(10);
    policyViolation6.setThreatLevel(2);

    policyViolation1.setActionTypeId(FailActionType.ID);
    policyViolation2.setActionTypeId(FailActionType.ID);
    policyViolation3.setActionTypeId(FailActionType.ID);
    policyViolation4.setActionTypeId(FailActionType.ID);
    policyViolation5.setActionTypeId(FailActionType.ID);
    policyViolation6.setActionTypeId(FailActionType.ID);

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.QUARANTINE_TIME;
    sortField1.sortPriority = 1;
    sortField1.asc = false;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField2.sortPriority = 2;
    sortField2.asc = false;

    SortField sortField3 = new SortField();
    sortField3.sortableField = SortableField.OBJECT_NAME;
    sortField3.sortPriority = 3;
    sortField3.asc = true;
    detailsRequest.threatLevelFilters = ImmutableList.of(1, 10);

    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2, sortField3);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(6);
    assertThat(repositoryResultsDetails.get(0).quarantineTime).isEqualTo(date5);
    assertThat(repositoryResultsDetails.get(1).quarantineTime).isEqualTo(date4);
    assertThat(repositoryResultsDetails.get(2).quarantineTime).isEqualTo(date3);
    assertThat(repositoryResultsDetails.get(3).quarantineTime).isEqualTo(date2);
    assertThat(repositoryResultsDetails.get(4).quarantineTime).isEqualTo(date1);
    assertThat(repositoryResultsDetails.get(5).quarantineTime).isEqualTo(date1);

    assertThat(repositoryResultsDetails.get(4).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(5).threatLevel).isEqualTo(8);
  }

  @Test
  public void testGetDetails_NonAggregated_ThreatLevelRange() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    // create policy violations
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

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = false;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.POLICY_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    SortField sortField3 = new SortField();
    sortField3.sortableField = SortableField.OBJECT_NAME;
    sortField3.sortPriority = 3;
    sortField3.asc = true;
    detailsRequest.threatLevelFilters = ImmutableList.of(7, 10);

    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2, sortField3);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(4);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(3).threatLevel).isEqualTo(8);

    assertThat(repositoryResultsDetails.get(0).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(1).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(2).objectName).isEqualTo("app2");
    assertThat(repositoryResultsDetails.get(3).objectName).isEqualTo("app1");

    assertThat(repositoryResultsDetails.get(0).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(1).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(2).applicationPublicId).isEqualTo("appPublicId2");
    assertThat(repositoryResultsDetails.get(3).applicationPublicId).isEqualTo("appPublicId1");

    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy5");
    assertThat(repositoryResultsDetails.get(3).policyName).isEqualTo("policy2");

    assertThat(repositoryResultsDetails.get(0).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(1).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(2).scanId).isEqualTo("scanId2");
    assertThat(repositoryResultsDetails.get(3).scanId).isEqualTo("scanId1");
  }

  @Test
  public void testGetDetails_Aggregated_SearchByPolicyViolationCount() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application1.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application1.getId(), "policy6");

    // create policy violations
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEvaluation1, policy4);

    PolicyViolation policyViolation5 = tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation6 = tempEntity.newPolicyViolation(policyEvaluation2, policy6);

    policyViolation1.setThreatLevel(10);
    policyViolation1.setActionTypeId(Action.ID_FAIL);
    policyViolation2.setThreatLevel(8);
    policyViolation2.setActionTypeId(Action.ID_FAIL);
    policyViolation3.setThreatLevel(10);
    policyViolation3.setActionTypeId(Action.ID_FAIL);
    policyViolation4.setThreatLevel(5);
    policyViolation4.setActionTypeId(Action.ID_FAIL);
    policyViolation5.setThreatLevel(10);
    policyViolation5.setActionTypeId(Action.ID_FAIL);
    policyViolation6.setThreatLevel(2);
    policyViolation6.setActionTypeId(Action.ID_FAIL);

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    // filter by object name
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    SearchFilter searchFilter = new SearchFilter();
    searchFilter.filterableField = FilterableField.OBJECT_NAME;
    searchFilter.value = "1";

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.aggregate = true;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);
    detailsRequest.searchFilters = Arrays.asList(searchFilter);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);

    // filter by violation count
    SortField sortField3 = new SortField();
    sortField3.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField3.sortPriority = 1;
    sortField3.asc = true;

    SortField sortField4 = new SortField();
    sortField4.sortableField = SortableField.OBJECT_NAME;
    sortField4.sortPriority = 2;
    sortField4.asc = true;

    SearchFilter searchFilter1 = new SearchFilter();
    searchFilter1.filterableField = FilterableField.VIOLATION_COUNT;
    searchFilter1.value = "2";

    RepositoryResultsForImageContainerRequestDto detailsRequest1 = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest1.page = 1;
    detailsRequest1.pageSize = 50;
    detailsRequest1.aggregate = true;
    detailsRequest1.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest1.sortFields = Arrays.asList(sortField3, sortField4);
    detailsRequest1.searchFilters = Arrays.asList(searchFilter1);
    RepositoryResultsForImageContainerResponseDto responseDto1 =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest1);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails1 = responseDto1.repositoryResultsDetails;
    assertThat(repositoryResultsDetails1).hasSize(1);
    assertThat(repositoryResultsDetails1.get(0).objectName).isEqualTo("app2");
    assertThat(repositoryResultsDetails1.get(0).applicationPublicId).isEqualTo("appPublicId2");
    assertThat(repositoryResultsDetails1.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails1.get(0).scanId).isEqualTo("scanId2");
  }

  @Test
  public void testGetDetails_Aggregated_NotPolicyEvaluationExist() {
    Repository repository1 = tempEntity.newRepository(repositoryManager, "publicId1");
    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.aggregate = true;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository1.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails).hasSize(0);
  }

  @Test
  public void testGetDetails_Aggregated_ValidatePolicyEvaluationExist() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application1.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application1.getId(), "policy6");

    Application application3 = tempEntity.newApplication("app3", "appPublicId3", organization.getId());
    Application application4 = tempEntity.newApplication("app4", "appPublicId4", organization.getId());

    // should not appear in the details, does not have policy evaluation
    tempEntity.newApplication("app5", "appPublicId5", organization.getId());
    tempEntity.newApplication("app6", "appPublicId6", organization.getId());

    tempEntity.newPolicyEvaluation(application3.getId(), "proxy", "scanId3");
    tempEntity.newPolicyEvaluation(application4.getId(), "proxy", "scanId4");

    // create policy violations
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEvaluation1, policy4);
    PolicyViolation policyViolation5 = tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation6 = tempEntity.newPolicyViolation(policyEvaluation2, policy6);
    PolicyViolation policyViolation7 = tempEntity.newPolicyViolation(policyEvaluation1, policy6);
    PolicyViolation policyViolation8 = tempEntity.newPolicyViolation(policyEvaluation1, policy6);

    policyViolation1.setThreatLevel(10);
    policyViolation1.setActionTypeId(Action.ID_FAIL);
    policyViolation2.setThreatLevel(8);
    policyViolation2.setActionTypeId(Action.ID_FAIL);
    policyViolation3.setThreatLevel(10);
    policyViolation3.setActionTypeId(Action.ID_FAIL);
    policyViolation4.setThreatLevel(5);
    policyViolation4.setActionTypeId(Action.ID_FAIL);
    policyViolation5.setThreatLevel(10);
    policyViolation5.setActionTypeId(Action.ID_FAIL);
    policyViolation6.setThreatLevel(2);
    policyViolation6.setActionTypeId(Action.ID_FAIL);

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    // policyViolation thread level low are not counted
    policyViolation7.setThreatLevel(1);
    policyViolation7.setActionTypeId(Action.ID_FAIL);
    policyViolation8.setThreatLevel(1);
    policyViolation8.setActionTypeId(Action.ID_FAIL);
    policyViolationDAO.update(policyViolation7);
    policyViolationDAO.update(policyViolation8);

    // filter by object name
    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.aggregate = true;
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;
    assertThat(repositoryResultsDetails).hasSize(4);
    assertThat(repositoryResultsDetails.get(0).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(0).violationCount).isEqualTo(4);
    assertThat(repositoryResultsDetails.get(1).objectName).isEqualTo("app2");
    assertThat(repositoryResultsDetails.get(1).violationCount).isEqualTo(2);
    assertThat(repositoryResultsDetails.get(2).objectName).isEqualTo("app3");
    assertThat(repositoryResultsDetails.get(2).violationCount).isEqualTo(0);
    assertThat(repositoryResultsDetails.get(3).objectName).isEqualTo("app4");
    assertThat(repositoryResultsDetails.get(3).violationCount).isEqualTo(0);

  }

  @Test
  public void testGetDetails_NonAggregated_SearchByPolicyName() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application1.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application1.getId(), "policy6");

    // create policy violations
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

    // filter by policy name
    SortField sortField3 = new SortField();
    sortField3.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField3.sortPriority = 1;
    sortField3.asc = true;

    SortField sortField4 = new SortField();
    sortField4.sortableField = SortableField.OBJECT_NAME;
    sortField4.sortPriority = 2;
    sortField4.asc = true;

    SearchFilter searchFilter1 = new SearchFilter();
    searchFilter1.filterableField = FilterableField.POLICY_NAME;
    searchFilter1.value = "2";

    RepositoryResultsForImageContainerRequestDto detailsRequest1 = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest1.page = 1;
    detailsRequest1.pageSize = 50;
    detailsRequest1.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest1.sortFields = Arrays.asList(sortField3, sortField4);
    detailsRequest1.searchFilters = Arrays.asList(searchFilter1);

    RepositoryResultsForImageContainerResponseDto responseDto1 =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest1);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails1 = responseDto1.repositoryResultsDetails;
    assertThat(repositoryResultsDetails1).hasSize(1);
    assertThat(repositoryResultsDetails1.get(0).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails1.get(0).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails1.get(0).threatLevel).isEqualTo(8);
    assertThat(repositoryResultsDetails1.get(0).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails1.get(0).scanId).isEqualTo("scanId1");
  }

  @Test
  public void testGetDetails_NonAggregated_NotViolating() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");

    // create policy violations
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEvaluation1, policy4);

    policyViolation1.setThreatLevel(10);
    policyViolation2.setThreatLevel(8);
    policyViolation3.setThreatLevel(10);
    policyViolation4.setThreatLevel(5);

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_NOT_VIOLATING);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    // application2 have no violations
    assertThat(repositoryResultsDetails).hasSize(1);
  }

  @Test
  public void testGetDetails_NonAggregated_ViolationStateOpen() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    // create policy violations
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

    policyViolation5.setWaiveTime(new Date());
    policyViolation5.setLegacyViolationTime(new Date());
    policyViolation5.setFixTime(new Date());

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_OPEN);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(5);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(2);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(8);
    assertThat(repositoryResultsDetails.get(3).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(4).threatLevel).isEqualTo(10);

    assertThat(repositoryResultsDetails.get(0).objectName).isEqualTo("app2");
    assertThat(repositoryResultsDetails.get(1).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(2).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(3).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(4).objectName).isEqualTo("app1");

    assertThat(repositoryResultsDetails.get(0).applicationPublicId).isEqualTo("appPublicId2");
    assertThat(repositoryResultsDetails.get(1).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(2).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(3).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(4).applicationPublicId).isEqualTo("appPublicId1");

    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy6");
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy4");
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(3).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(4).policyName).isEqualTo("policy3");

    assertThat(repositoryResultsDetails.get(0).scanId).isEqualTo("scanId2");
    assertThat(repositoryResultsDetails.get(1).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(2).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(3).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(4).scanId).isEqualTo("scanId1");
  }

  @Test
  public void testGetDetails_NonAggregated_ViolationStateQuarantined() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    // create policy violations
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

    policyViolation5.setOpenTime(new Date());
    policyViolation5.setActionTypeId("fail");

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_QUARANTINED);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy5");
    assertThat(repositoryResultsDetails.get(0).scanId).isEqualTo("scanId2");
    assertThat(repositoryResultsDetails.get(0).objectName).isEqualTo("app2");
    assertThat(repositoryResultsDetails.get(0).applicationPublicId).isEqualTo("appPublicId2");
  }

  @Test
  public void testGetDetails_NonAggregated_ViolationStateWaived() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    // create policy violations
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

    policyViolation5.setWaiveTime(new Date());

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_WAIVED);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(1);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy5");
    assertThat(repositoryResultsDetails.get(0).scanId).isEqualTo("scanId2");
    assertThat(repositoryResultsDetails.get(0).objectName).isEqualTo("app2");
    assertThat(repositoryResultsDetails.get(0).applicationPublicId).isEqualTo("appPublicId2");
  }

  @Test
  public void testGetDetails_NonAggregated_OpenOrQuarantined() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    // create policy violations
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

    policyViolation5.setWaiveTime(new Date());

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_QUARANTINED,
        ViolationStateFilter.VIOLATION_STATE_OPEN);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    // one violation is excluded from the results
    assertThat(repositoryResultsDetails).hasSize(5);
  }

  @Test
  public void testGetDetails_NonAggregated_OpenOrWaived() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    // create policy violations
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

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_WAIVED,
        ViolationStateFilter.VIOLATION_STATE_OPEN);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(6);
  }

  @Test
  public void testGetDetails_NonAggregated_QuarantinedOrWaived() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    // create policy violations
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

    policyViolation5.setWaiveTime(new Date());

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_WAIVED,
        ViolationStateFilter.VIOLATION_STATE_QUARANTINED);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(1);
  }

  @Test
  public void testGetDetails_NonAggregated_NonViolatingOrWaived() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    // create policy violations
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

    policyViolation5.setWaiveTime(new Date());

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_WAIVED,
        ViolationStateFilter.VIOLATION_STATE_NOT_VIOLATING);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(1);
  }

  @Test
  public void testGetDetails_NonAggregated_RepositoryManagerOwnerType() {
    Repository repository1 = tempEntity.newRepository(repositoryManager, "publicId1");
    Repository repository2 = tempEntity.newRepository(repositoryManager, "publicId3");

    // Repository organization1
    Organization organization1 = tempEntity.newOrganization("org1");
    organization1.setRelatedRepositoryId(repository1.getId());
    repository1.setRelatedOrganizationId(organization1.getId());
    repositoryDAO.update(repository1);
    organizationDAO.update(organization1);

    Organization organization2 = tempEntity.newOrganization("org2");
    organization2.setRelatedRepositoryId(repository2.getId());
    repository2.setRelatedOrganizationId(organization2.getId());
    repositoryDAO.update(repository2);
    organizationDAO.update(organization2);

    Application application3 = tempEntity.newApplication("app3", "appPublicId3", organization1.getId());
    Application application4 = tempEntity.newApplication("app4", "appPublicId4", organization1.getId());

    Application application5 = tempEntity.newApplication("app5", "appPublicId5", organization2.getId());
    Application application6 = tempEntity.newApplication("app6", "appPublicId6", organization2.getId());

    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");
    Policy policy7 = tempEntity.newPolicy(application3.getId(), "policy7");
    Policy policy8 = tempEntity.newPolicy(application4.getId(), "policy8");
    Policy policy9 = tempEntity.newPolicy(application5.getId(), "policy9");
    Policy policy10 = tempEntity.newPolicy(application6.getId(), "policy10");

    // create policy violations
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEvaluation1, policy4);

    PolicyViolation policyViolation5 = tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation6 = tempEntity.newPolicyViolation(policyEvaluation2, policy6);

    PolicyViolation policyViolation7 = tempEntity.newPolicyViolation(policyEvaluation2, policy7);
    PolicyViolation policyViolation8 = tempEntity.newPolicyViolation(policyEvaluation2, policy7);

    PolicyViolation policyViolation9 = tempEntity.newPolicyViolation(policyEvaluation2, policy8);
    PolicyViolation policyViolation10 = tempEntity.newPolicyViolation(policyEvaluation2, policy8);

    PolicyViolation policyViolation11 = tempEntity.newPolicyViolation(policyEvaluation2, policy9);
    PolicyViolation policyViolation12 = tempEntity.newPolicyViolation(policyEvaluation2, policy9);

    PolicyViolation policyViolation13 = tempEntity.newPolicyViolation(policyEvaluation2, policy10);
    PolicyViolation policyViolation14 = tempEntity.newPolicyViolation(policyEvaluation2, policy10);

    policyViolation1.setThreatLevel(10);
    policyViolation2.setThreatLevel(8);
    policyViolation3.setThreatLevel(10);
    policyViolation4.setThreatLevel(5);
    policyViolation5.setThreatLevel(10);
    policyViolation6.setThreatLevel(2);
    policyViolation7.setThreatLevel(0);
    policyViolation8.setThreatLevel(1);
    policyViolation9.setThreatLevel(2);
    policyViolation10.setThreatLevel(3);
    policyViolation11.setThreatLevel(4);
    policyViolation12.setThreatLevel(6);
    policyViolation13.setThreatLevel(7);
    policyViolation14.setThreatLevel(9);

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);
    policyViolationDAO.update(policyViolation7);
    policyViolationDAO.update(policyViolation8);
    policyViolationDAO.update(policyViolation9);
    policyViolationDAO.update(policyViolation10);
    policyViolationDAO.update(policyViolation11);
    policyViolationDAO.update(policyViolation12);
    policyViolationDAO.update(policyViolation13);
    policyViolationDAO.update(policyViolation14);

    tempEntity.newPolicyEvaluation(application3.getId(), "proxy", "scanId3");
    tempEntity.newPolicyEvaluation(application4.getId(), "proxy", "scanId4");
    tempEntity.newPolicyEvaluation(application5.getId(), "proxy", "scanId5");
    tempEntity.newPolicyEvaluation(application6.getId(), "proxy", "scanId6");

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_MANAGER,
            repositoryManager.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(18);
  }

  @Test
  public void testGetDetails_NonAggregated_RepositoryContainerOwnerType() {
    Repository repository1 = tempEntity.newRepository(repositoryManager, "publicId1");
    Repository repository2 = tempEntity.newRepository(repositoryManager, "publicId3");

    // Repository organization1
    Organization organization1 = tempEntity.newOrganization("org1");
    organization1.setRelatedRepositoryId(repository1.getId());
    repository1.setRelatedOrganizationId(organization1.getId());
    repositoryDAO.update(repository1);
    organizationDAO.update(organization1);

    Organization organization2 = tempEntity.newOrganization("org2");
    organization2.setRelatedRepositoryId(repository2.getId());
    repository2.setRelatedOrganizationId(organization2.getId());
    repositoryDAO.update(repository2);
    organizationDAO.update(organization2);

    Application application3 = tempEntity.newApplication("app3", "appPublicId3", organization1.getId());
    Application application4 = tempEntity.newApplication("app4", "appPublicId4", organization1.getId());

    Application application5 = tempEntity.newApplication("app5", "appPublicId5", organization2.getId());
    Application application6 = tempEntity.newApplication("app6", "appPublicId6", organization2.getId());

    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");
    Policy policy7 = tempEntity.newPolicy(application3.getId(), "policy7");
    Policy policy8 = tempEntity.newPolicy(application4.getId(), "policy8");
    Policy policy9 = tempEntity.newPolicy(application5.getId(), "policy9");
    Policy policy10 = tempEntity.newPolicy(application6.getId(), "policy10");

    tempEntity.newPolicyEvaluation(application3.getId(), "proxy", "scanId3");
    tempEntity.newPolicyEvaluation(application4.getId(), "proxy", "scanId4");
    tempEntity.newPolicyEvaluation(application5.getId(), "proxy", "scanId5");
    tempEntity.newPolicyEvaluation(application6.getId(), "proxy", "scanId6");

    // create policy violations
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEvaluation1, policy4);

    PolicyViolation policyViolation5 = tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation6 = tempEntity.newPolicyViolation(policyEvaluation2, policy6);

    PolicyViolation policyViolation7 = tempEntity.newPolicyViolation(policyEvaluation2, policy7);
    PolicyViolation policyViolation8 = tempEntity.newPolicyViolation(policyEvaluation2, policy7);

    PolicyViolation policyViolation9 = tempEntity.newPolicyViolation(policyEvaluation2, policy8);
    PolicyViolation policyViolation10 = tempEntity.newPolicyViolation(policyEvaluation2, policy8);

    PolicyViolation policyViolation11 = tempEntity.newPolicyViolation(policyEvaluation2, policy9);
    PolicyViolation policyViolation12 = tempEntity.newPolicyViolation(policyEvaluation2, policy9);

    PolicyViolation policyViolation13 = tempEntity.newPolicyViolation(policyEvaluation2, policy10);
    PolicyViolation policyViolation14 = tempEntity.newPolicyViolation(policyEvaluation2, policy10);

    policyViolation1.setThreatLevel(10);
    policyViolation2.setThreatLevel(8);
    policyViolation3.setThreatLevel(10);
    policyViolation4.setThreatLevel(5);
    policyViolation5.setThreatLevel(10);
    policyViolation6.setThreatLevel(2);
    policyViolation7.setThreatLevel(0);
    policyViolation8.setThreatLevel(1);
    policyViolation9.setThreatLevel(2);
    policyViolation10.setThreatLevel(3);
    policyViolation11.setThreatLevel(4);
    policyViolation12.setThreatLevel(6);
    policyViolation13.setThreatLevel(7);
    policyViolation14.setThreatLevel(9);

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);
    policyViolationDAO.update(policyViolation7);
    policyViolationDAO.update(policyViolation8);
    policyViolationDAO.update(policyViolation9);
    policyViolationDAO.update(policyViolation10);
    policyViolationDAO.update(policyViolation11);
    policyViolationDAO.update(policyViolation12);
    policyViolationDAO.update(policyViolation13);
    policyViolationDAO.update(policyViolation14);

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY_CONTAINER,
            repositoryContainer.getId(),
            detailsRequest);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = responseDto.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).hasSize(18);
  }

  @Test
  public void testGetDetails_invalidPage() {
    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = -1;

    assertThatThrownBy(
        () -> repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Page and Page size must be greater than 0");
  }

  @Test
  public void testGetDetails_invalidSortPriority() {
    SortField sortField1 =
        new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 2;
    sortField1.asc = false;

    SortField sortField2 =
        new SortField();
    sortField2.sortableField = SortableField.POLICY_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 1;
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    assertThatThrownBy(
        () -> repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("sort priority cannot be the same for different fields");
  }

  @Test
  public void testGetDetails_MissingRequestParameters() {
    assertThatThrownBy(() -> repositoryResultsService.getDetails(OwnerType.REPOSITORY,
        repository.getId(),
        null))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Missing request parameters");
  }

  @Test
  public void testGetDetails_NonAggregated_Pagination() {
    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");

    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");
    Policy policy7 = tempEntity.newPolicy(application2.getId(), "policy7");
    Policy policy8 = tempEntity.newPolicy(application2.getId(), "policy8");

    // create policy violations
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEvaluation1, policy4);

    PolicyViolation policyViolation5 = tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation6 = tempEntity.newPolicyViolation(policyEvaluation2, policy6);
    PolicyViolation policyViolation7 = tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation8 = tempEntity.newPolicyViolation(policyEvaluation2, policy6);
    PolicyViolation policyViolation9 = tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation10 = tempEntity.newPolicyViolation(policyEvaluation2, policy6);
    PolicyViolation policyViolation11 = tempEntity.newPolicyViolation(policyEvaluation2, policy7);
    PolicyViolation policyViolation12 = tempEntity.newPolicyViolation(policyEvaluation2, policy8);

    policyViolation1.setThreatLevel(10);
    policyViolation2.setThreatLevel(8);
    policyViolation3.setThreatLevel(10);
    policyViolation4.setThreatLevel(5);
    policyViolation5.setThreatLevel(10);
    policyViolation6.setThreatLevel(2);
    policyViolation7.setThreatLevel(3);
    policyViolation8.setThreatLevel(7);
    policyViolation9.setThreatLevel(10);
    policyViolation10.setThreatLevel(9);
    policyViolation11.setThreatLevel(10);
    policyViolation12.setThreatLevel(9);

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);
    policyViolationDAO.update(policyViolation7);
    policyViolationDAO.update(policyViolation8);
    policyViolationDAO.update(policyViolation9);
    policyViolationDAO.update(policyViolation10);
    policyViolationDAO.update(policyViolation11);
    policyViolationDAO.update(policyViolation12);

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest1 = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest1.page = 1;
    detailsRequest1.pageSize = 5;
    detailsRequest1.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto1 =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest1);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails1 = responseDto1.repositoryResultsDetails;
    assertThat(repositoryResultsDetails1).hasSize(5);
    assertThat(repositoryResultsDetails1.get(0).threatLevel).isEqualTo(2);
    assertThat(repositoryResultsDetails1.get(1).threatLevel).isEqualTo(3);
    assertThat(repositoryResultsDetails1.get(2).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails1.get(3).threatLevel).isEqualTo(7);
    assertThat(repositoryResultsDetails1.get(4).threatLevel).isEqualTo(8);

    RepositoryResultsForImageContainerRequestDto detailsRequest2 = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest2.page = 2;
    detailsRequest2.pageSize = 5;
    detailsRequest2.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto2 =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest2);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails2 = responseDto2.repositoryResultsDetails;
    assertThat(repositoryResultsDetails2).hasSize(5);
    assertThat(repositoryResultsDetails2.get(0).threatLevel).isEqualTo(9);
    assertThat(repositoryResultsDetails2.get(1).threatLevel).isEqualTo(9);
    assertThat(repositoryResultsDetails2.get(2).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails2.get(2).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails2.get(2).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails2.get(3).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails2.get(3).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails2.get(4).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails2.get(4).objectName).isEqualTo("app2");

    RepositoryResultsForImageContainerRequestDto detailsRequest3 = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest3.page = 3;
    detailsRequest3.pageSize = 5;
    detailsRequest3.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto3 =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest3);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails3 = responseDto3.repositoryResultsDetails;
    assertThat(repositoryResultsDetails3).hasSize(2);
    assertThat(repositoryResultsDetails3.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails3.get(0).objectName).isEqualTo("app2");
    assertThat(repositoryResultsDetails3.get(1).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails3.get(1).objectName).isEqualTo("app2");
    assertThat(repositoryResultsDetails3.get(1).applicationPublicId).isEqualTo("appPublicId2");
  }

  @Test
  public void testGetDetails_Aggregated_Pagination() {
    // Container Image applications
    Application application3 = tempEntity.newApplication("app3", "appPublicId3", organization.getId());
    Application application4 = tempEntity.newApplication("app4", "appPublicId4", organization.getId());
    Application application5 = tempEntity.newApplication("app5", "appPublicId5", organization.getId());
    Application application6 = tempEntity.newApplication("app6", "appPublicId6", organization.getId());
    Application application7 = tempEntity.newApplication("app7", "appPublicId7", organization.getId());
    Application application8 = tempEntity.newApplication("app8", "appPublicId8", organization.getId());
    Application application9 = tempEntity.newApplication("app9", "appPublicId9", organization.getId());
    Application application10 = tempEntity.newApplication("app10", "appPublicId10", organization.getId());
    Application application11 = tempEntity.newApplication("app11", "appPublicId11", organization.getId());
    Application application12 = tempEntity.newApplication("app12", "appPublicId12", organization.getId());

    // policy evaluation
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application1.getId(), "proxy", "scanId1");
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), "proxy", "scanId2");
    PolicyEvaluation policyEvaluation3 = tempEntity.newPolicyEvaluation(application3.getId(), "proxy", "scanId1");
    PolicyEvaluation policyEvaluation4 = tempEntity.newPolicyEvaluation(application4.getId(), "proxy", "scanId2");
    PolicyEvaluation policyEvaluation5 = tempEntity.newPolicyEvaluation(application5.getId(), "proxy", "scanId1");
    PolicyEvaluation policyEvaluation6 = tempEntity.newPolicyEvaluation(application6.getId(), "proxy", "scanId2");
    PolicyEvaluation policyEvaluation7 = tempEntity.newPolicyEvaluation(application7.getId(), "proxy", "scanId1");
    PolicyEvaluation policyEvaluation8 = tempEntity.newPolicyEvaluation(application8.getId(), "proxy", "scanId2");
    PolicyEvaluation policyEvaluation9 = tempEntity.newPolicyEvaluation(application9.getId(), "proxy", "scanId1");
    PolicyEvaluation policyEvaluation10 = tempEntity.newPolicyEvaluation(application10.getId(), "proxy", "scanId2");
    PolicyEvaluation policyEvaluation11 = tempEntity.newPolicyEvaluation(application11.getId(), "proxy", "scanId1");
    PolicyEvaluation policyEvaluation12 = tempEntity.newPolicyEvaluation(application12.getId(), "proxy", "scanId2");

    // last policy evaluation
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application1.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application2.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application3.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application4.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application5.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application6.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application7.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application8.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application9.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application10.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application11.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application12.getId(), "proxy");

    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application2.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application3.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application4.getId(), "policy4");
    Policy policy5 = tempEntity.newPolicy(application5.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application6.getId(), "policy6");
    Policy policy7 = tempEntity.newPolicy(application7.getId(), "policy7");
    Policy policy8 = tempEntity.newPolicy(application8.getId(), "policy8");
    Policy policy9 = tempEntity.newPolicy(application9.getId(), "policy9");
    Policy policy10 = tempEntity.newPolicy(application10.getId(), "policy10");
    Policy policy11 = tempEntity.newPolicy(application11.getId(), "policy11");
    Policy policy12 = tempEntity.newPolicy(application12.getId(), "policy12");

    // create policy violations
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation2, policy3);
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEvaluation3, policy4);
    PolicyViolation policyViolation5 = tempEntity.newPolicyViolation(policyEvaluation4, policy5);
    PolicyViolation policyViolation6 = tempEntity.newPolicyViolation(policyEvaluation5, policy11);
    PolicyViolation policyViolation7 = tempEntity.newPolicyViolation(policyEvaluation6, policy5);
    PolicyViolation policyViolation8 = tempEntity.newPolicyViolation(policyEvaluation7, policy6);
    PolicyViolation policyViolation9 = tempEntity.newPolicyViolation(policyEvaluation8, policy5);
    PolicyViolation policyViolation10 = tempEntity.newPolicyViolation(policyEvaluation9, policy6);
    PolicyViolation policyViolation11 = tempEntity.newPolicyViolation(policyEvaluation10, policy7);
    PolicyViolation policyViolation12 = tempEntity.newPolicyViolation(policyEvaluation11, policy8);
    PolicyViolation policyViolation13 = tempEntity.newPolicyViolation(policyEvaluation12, policy9);
    PolicyViolation policyViolation14 = tempEntity.newPolicyViolation(policyEvaluation12, policy10);
    PolicyViolation policyViolation15 = tempEntity.newPolicyViolation(policyEvaluation6, policy12);
    PolicyViolation policyViolation16 = tempEntity.newPolicyViolation(policyEvaluation4, policy8);

    policyViolation1.setThreatLevel(10);
    policyViolation1.setActionTypeId(Action.ID_FAIL);
    policyViolation2.setThreatLevel(8);
    policyViolation2.setActionTypeId(Action.ID_FAIL);
    policyViolation3.setThreatLevel(10);
    policyViolation3.setActionTypeId(Action.ID_FAIL);
    policyViolation4.setThreatLevel(5);
    policyViolation4.setActionTypeId(Action.ID_FAIL);
    policyViolation5.setThreatLevel(10);
    policyViolation5.setActionTypeId(Action.ID_FAIL);
    policyViolation6.setThreatLevel(2);
    policyViolation6.setActionTypeId(Action.ID_FAIL);
    policyViolation7.setThreatLevel(3);
    policyViolation7.setActionTypeId(Action.ID_FAIL);
    policyViolation8.setThreatLevel(7);
    policyViolation8.setActionTypeId(Action.ID_FAIL);
    policyViolation9.setThreatLevel(10);
    policyViolation9.setActionTypeId(Action.ID_FAIL);
    policyViolation10.setThreatLevel(9);
    policyViolation10.setActionTypeId(Action.ID_FAIL);
    policyViolation11.setThreatLevel(10);
    policyViolation11.setActionTypeId(Action.ID_FAIL);
    policyViolation12.setThreatLevel(9);
    policyViolation12.setActionTypeId(Action.ID_FAIL);
    policyViolation13.setThreatLevel(2);
    policyViolation13.setActionTypeId(Action.ID_FAIL);
    policyViolation14.setThreatLevel(3);
    policyViolation14.setActionTypeId(Action.ID_FAIL);
    policyViolation15.setThreatLevel(4);
    policyViolation15.setActionTypeId(Action.ID_FAIL);
    policyViolation16.setThreatLevel(5);
    policyViolation16.setActionTypeId(Action.ID_FAIL);

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);
    policyViolationDAO.update(policyViolation7);
    policyViolationDAO.update(policyViolation8);
    policyViolationDAO.update(policyViolation9);
    policyViolationDAO.update(policyViolation10);
    policyViolationDAO.update(policyViolation11);
    policyViolationDAO.update(policyViolation12);
    policyViolationDAO.update(policyViolation13);
    policyViolationDAO.update(policyViolation14);
    policyViolationDAO.update(policyViolation15);
    policyViolationDAO.update(policyViolation16);

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest1 = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest1.page = 1;
    detailsRequest1.pageSize = 5;
    detailsRequest1.aggregate = true;
    detailsRequest1.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto1 =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest1);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails1 = responseDto1.repositoryResultsDetails;
    assertThat(repositoryResultsDetails1).hasSize(5);
    assertThat(repositoryResultsDetails1.get(0).threatLevel).isEqualTo(2);
    assertThat(repositoryResultsDetails1.get(1).threatLevel).isEqualTo(3);
    assertThat(repositoryResultsDetails1.get(2).threatLevel).isEqualTo(4);
    assertThat(repositoryResultsDetails1.get(3).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails1.get(4).threatLevel).isEqualTo(7);
    assertThat(responseDto1.hasNextPage).isEqualTo(true);

    RepositoryResultsForImageContainerRequestDto detailsRequest2 = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest2.page = 2;
    detailsRequest2.aggregate = true;
    detailsRequest2.pageSize = 5;
    detailsRequest2.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto2 =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest2);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails2 = responseDto2.repositoryResultsDetails;
    assertThat(repositoryResultsDetails2).hasSize(5);
    assertThat(repositoryResultsDetails2.get(0).threatLevel).isEqualTo(9);
    assertThat(repositoryResultsDetails2.get(1).threatLevel).isEqualTo(9);
    assertThat(repositoryResultsDetails2.get(2).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails2.get(3).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails2.get(4).threatLevel).isEqualTo(10);
    assertThat(responseDto2.hasNextPage).isEqualTo(true);

    RepositoryResultsForImageContainerRequestDto detailsRequest3 = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest3.page = 3;
    detailsRequest3.aggregate = true;
    detailsRequest3.pageSize = 5;
    detailsRequest3.sortFields = Arrays.asList(sortField1, sortField2);

    RepositoryResultsForImageContainerResponseDto responseDto3 =
        repositoryResultsService.getDetails(OwnerType.REPOSITORY,
            repository.getId(),
            detailsRequest3);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails3 = responseDto3.repositoryResultsDetails;
    assertThat(repositoryResultsDetails3).hasSize(2);
    assertThat(repositoryResultsDetails3.get(0).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails3.get(1).threatLevel).isEqualTo(10);
    assertThat(responseDto3.hasNextPage).isEqualTo(false);
  }
}
