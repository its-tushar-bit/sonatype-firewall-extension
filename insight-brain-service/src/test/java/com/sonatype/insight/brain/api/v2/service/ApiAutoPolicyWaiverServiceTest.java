/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import java.util.List;
import javax.inject.Inject;

import com.google.inject.Binder;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverStatusDTO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction;
import com.sonatype.insight.brain.telemetry.AutoPolicyWaiverTelemetryMetrics;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.isNotNull;

public class ApiAutoPolicyWaiverServiceTest
    extends AbstractComponentTest
{
  @Mock
  private AutoPolicyWaiverTelemetryMetrics autoPolicyWaiverTelemetryMetrics;

  @Inject
  private AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private ApiAutoPolicyWaiverService apiAutoPolicyWaiverService;

  @Override
  public void configure(Binder binder) {
    binder.bind(AutoPolicyWaiverTelemetryMetrics.class).toInstance(autoPolicyWaiverTelemetryMetrics);
    super.configure(binder);
  }

  @Test
  public void testAddAutoPolicyWaiver_Application() {
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.threatLevel = 7;
    dto.reachable = true;
    dto.pathForward = false;

    ApiAutoPolicyWaiverDTO resultingDTO =
        apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto);

    assertThat(resultingDTO).isNotNull();
    assertThat(resultingDTO.ownerId).isEqualTo(application.getId());
    assertThat(resultingDTO.threatLevel).isEqualTo(dto.threatLevel);
    assertThat(resultingDTO.reachable).isEqualTo(dto.reachable);
    assertThat(resultingDTO.pathForward).isEqualTo(dto.pathForward);

    Mockito.verify(autoPolicyWaiverTelemetryMetrics).collect(isNotNull(), eq(OwnerType.APPLICATION),
        eq(AutoPolicyWaiverAction.CREATE), isNull());
  }

  @Test
  public void testAddAutoPolicyWaiver_Organization() {
    Organization organization = tempEntity.newOrganization();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.threatLevel = 7;
    dto.reachable = true;
    dto.pathForward = false;

    ApiAutoPolicyWaiverDTO resultingDTO =
        apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), dto);

    assertThat(resultingDTO).isNotNull();
    assertThat(resultingDTO.ownerId).isEqualTo(organization.getId());
    assertThat(resultingDTO.threatLevel).isEqualTo(dto.threatLevel);
    assertThat(resultingDTO.reachable).isEqualTo(dto.reachable);
    assertThat(resultingDTO.pathForward).isEqualTo(dto.pathForward);

    Mockito.verify(autoPolicyWaiverTelemetryMetrics).collect(isNotNull(),
        eq(OwnerType.ORGANIZATION),
        eq(AutoPolicyWaiverAction.CREATE), isNull());
  }

  @Test
  public void testAddAutoPolicyWaiver_InvalidOwnerType() {
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.REPOSITORY, "fakeRepoId", dto)).isInstanceOf(
        IllegalStateException.class).hasMessage("Unknown owner type: repository");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testAddAutoPolicyWaiver_WithThreatLevel_negative() {
    Organization organization = tempEntity.newOrganization();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = organization.getId();
    dto.threatLevel = -1;
    dto.reachable = true;
    dto.pathForward = false;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), dto)).isInstanceOf(
            BadRequestException.class)
        .hasMessage("Invalid threat level: " + dto.threatLevel + ". Value must be between 1 and 10.");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testAddAutoPolicyWaiver_WithThreatLevel_tooLarge() {
    Organization organization = tempEntity.newOrganization();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = organization.getId();
    dto.threatLevel = 11;
    dto.reachable = true;
    dto.pathForward = false;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), dto)).isInstanceOf(
            BadRequestException.class)
        .hasMessage("Invalid threat level: " + dto.threatLevel + ". Value must be between 1 and 10.");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaiver_Application() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverDTO waiver =
        apiAutoPolicyWaiverService.getAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            autoPolicyWaiver.getId());

    assertThat(waiver).isNotNull();
    assertThat(waiver.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());

    assertThat(waiver.ownerId).isEqualTo(autoPolicyWaiver.getOwnerId());
    assertThat(waiver.ownerName).isEqualTo(application.getName());
    assertThat(waiver.ownerType).isEqualTo(OwnerType.APPLICATION.toString());
    assertThat(waiver.publicId).isEqualTo(application.getPublicId());

    assertThat(waiver.threatLevel).isEqualTo(autoPolicyWaiver.getThreatLevel());
    assertThat(waiver.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());

    assertThat(waiver.reachable).isEqualTo(autoPolicyWaiver.isReachable());

    assertThat(waiver.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(waiver.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(waiver.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaiver_Organization() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());

    ApiAutoPolicyWaiverDTO waiver =
        apiAutoPolicyWaiverService.getAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(),
            autoPolicyWaiver.getId());

    assertThat(waiver).isNotNull();
    assertThat(waiver.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());

    assertThat(waiver.ownerId).isEqualTo(autoPolicyWaiver.getOwnerId());
    assertThat(waiver.ownerName).isEqualTo(organization.getName());
    assertThat(waiver.ownerType).isEqualTo(OwnerType.ORGANIZATION.toString());
    assertThat(waiver.publicId).isEqualTo(organization.getPublicId());

    assertThat(waiver.threatLevel).isEqualTo(autoPolicyWaiver.getThreatLevel());
    assertThat(waiver.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());

    assertThat(waiver.reachable).isEqualTo(autoPolicyWaiver.isReachable());

    assertThat(waiver.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(waiver.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(waiver.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaiver_InvalidOwnerType() {
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.getAutoPolicyWaiver(OwnerType.REPOSITORY, "fakeRepoId",
            "fakeWaiverId")).isInstanceOf(IllegalStateException.class).hasMessage("Unknown owner type: repository");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testDeleteAutoPolicyWaiver_Application() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId());
    assertThat(autoPolicyWaiverDAO.getById(autoPolicyWaiver.getId())).isNull();

    Mockito.verify(autoPolicyWaiverTelemetryMetrics).collect(isNotNull(),
        eq(OwnerType.APPLICATION),
        eq(AutoPolicyWaiverAction.DELETE), isNull());
  }

  @Test
  public void testDeleteAutoPolicyWaiver_Organization() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());
    apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(),
        autoPolicyWaiver.getId());
    assertThat(autoPolicyWaiverDAO.getById(autoPolicyWaiver.getId())).isNull();

    Mockito.verify(autoPolicyWaiverTelemetryMetrics).collect(isNotNull(),
        eq(OwnerType.ORGANIZATION),
        eq(AutoPolicyWaiverAction.DELETE), isNull());
  }

  @Test
  public void testDeleteAutoPolicyWaiver_InvalidOwnerType() {
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.REPOSITORY, "fakeRepoId",
            "fakeWaiverId")).isInstanceOf(IllegalStateException.class).hasMessage("Unknown owner type: repository");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testDeleteAutoPolicyWaiver_OwnerIdMismatch() {
    Application application = tempEntity.newApplicationWithParent();
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            autoPolicyWaiver.getId())).isInstanceOf(
        NotFoundException.class).hasMessage(
        "Cannot find an auto policy waiver with ID " + autoPolicyWaiver.getId() + " for " + OwnerType.APPLICATION
            + " with ID " + application.getId());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaivers_Application() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    List<ApiAutoPolicyWaiverDTO> autoPolicyWaiverDTOList =
        apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId());

    assertThat(autoPolicyWaiverDTOList).hasSize(1);
    ApiAutoPolicyWaiverDTO waiver = autoPolicyWaiverDTOList.get(0);
    assertThat(waiver.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(waiver.threatLevel).isEqualTo(autoPolicyWaiver.getThreatLevel());
    assertThat(waiver.reachable).isEqualTo(autoPolicyWaiver.isReachable());
    assertThat(waiver.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(waiver.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(waiver.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(waiver.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
    assertThat(waiver.ownerId).isEqualTo(application.getId());
    assertThat(waiver.ownerName).isEqualTo(application.getName());
    assertThat(waiver.ownerType).isEqualTo(OwnerType.APPLICATION.toString());
    assertThat(waiver.publicId).isEqualTo(application.getPublicId());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaivers_Organization() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());

    List<ApiAutoPolicyWaiverDTO> autoPolicyWaiverDTOList =
        apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.ORGANIZATION, organization.getId());

    assertThat(autoPolicyWaiverDTOList).hasSize(1);
    ApiAutoPolicyWaiverDTO waiver = autoPolicyWaiverDTOList.get(0);
    assertThat(waiver.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(waiver.threatLevel).isEqualTo(autoPolicyWaiver.getThreatLevel());
    assertThat(waiver.reachable).isEqualTo(autoPolicyWaiver.isReachable());
    assertThat(waiver.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(waiver.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(waiver.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(waiver.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
    assertThat(waiver.ownerId).isEqualTo(organization.getId());
    assertThat(waiver.ownerName).isEqualTo(organization.getName());
    assertThat(waiver.ownerType).isEqualTo(OwnerType.ORGANIZATION.toString());
    assertThat(waiver.publicId).isEqualTo(organization.getPublicId());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaivers_NoWaivers() {
    Application application = tempEntity.newApplicationWithParent();

    List<ApiAutoPolicyWaiverDTO> autoPolicyWaiverDTOList =
        apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId());
    assertThat(autoPolicyWaiverDTOList).isEmpty();

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaivers_InvalidOwnerType() {
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.REPOSITORY, "fakeRepoId")).isInstanceOf(
        IllegalStateException.class).hasMessage("Unknown owner type: repository");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testUpdateAutoPolicyWaiver_Application() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO dto = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);

    dto.threatLevel = 10;
    dto.reachable = false;
    dto.pathForward = false;

    ApiAutoPolicyWaiverDTO responseDto =
        apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            dto.autoPolicyWaiverId, dto);
    assertThat(responseDto.threatLevel).isEqualTo(dto.threatLevel);
    assertThat(responseDto.pathForward).isEqualTo(dto.pathForward);

    Mockito.verify(autoPolicyWaiverTelemetryMetrics).collect(isNotNull(),
        eq(OwnerType.APPLICATION),
        eq(AutoPolicyWaiverAction.UPDATE), isNull());
  }

  @Test
  public void testUpdateAutoPolicyWaiver_Organization() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());
    ApiAutoPolicyWaiverDTO dto = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);

    dto.threatLevel = 1;
    dto.reachable = true;
    dto.pathForward = true;

    ApiAutoPolicyWaiverDTO responseDto =
        apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(),
            dto.autoPolicyWaiverId, dto);
    assertThat(responseDto.threatLevel).isEqualTo(dto.threatLevel);
    assertThat(responseDto.pathForward).isEqualTo(dto.pathForward);

    Mockito.verify(autoPolicyWaiverTelemetryMetrics).collect(isNotNull(),
        eq(OwnerType.ORGANIZATION),
        eq(AutoPolicyWaiverAction.UPDATE), isNull());
  }

  @Test
  public void testUpdateAutoPolicyWaiver_InvalidOwnerType() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());
    ApiAutoPolicyWaiverDTO dto = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.REPOSITORY, "fakeRepoId", dto.autoPolicyWaiverId,
            dto)).isInstanceOf(
        IllegalStateException.class).hasMessage("Unknown owner type: repository");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testUpdateAutoPolicyWaiver_ThreatLevelNegative() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO dto = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
    dto.threatLevel = -10;
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            dto.autoPolicyWaiverId, dto)).isInstanceOf(
            BadRequestException.class)
        .hasMessage("Invalid threat level: " + dto.threatLevel + ". Value must be between 1 and 10.");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testUpdateAutoPolicyWaiver_ThreatLevelTooLarge() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO dto = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
    dto.threatLevel = 80;
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            dto.autoPolicyWaiverId, dto)).isInstanceOf(
            BadRequestException.class)
        .hasMessage("Invalid threat level: " + dto.threatLevel + ". Value must be between 1 and 10.");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testUpdateAutoPolicyWaiver_NoChange() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            autoPolicyWaiverDTO.autoPolicyWaiverId, autoPolicyWaiverDTO)).isInstanceOf(BadRequestException.class)
        .hasMessage("No changes made to auto policy waiver configuration");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testUpdateAutoPolicyWaiver_AutoWaiverIdMismatch() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            "mismatchedId", autoPolicyWaiverDTO)).isInstanceOf(BadRequestException.class)
        .hasMessage("Auto policy waiver ID in requst path does not match request body");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testAddAutoPolicyWaiver_AlreadyExists_Application() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.threatLevel = 1;
    dto.reachable = true;
    dto.pathForward = true;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto)).isInstanceOf(
        BadRequestException.class).hasMessage("An auto policy waiver is already configured for " + application.getId());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testAddAutoPolicyWaiver_AlreadyExists_Organization() {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newAutoPolicyWaiver(organization.getId());

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.threatLevel = 1;
    dto.reachable = true;
    dto.pathForward = true;

    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), dto)).isInstanceOf(
            BadRequestException.class)
        .hasMessage("An auto policy waiver is already configured for " + organization.getId());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_AutoWavierDirectlyConfigured_Application() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverStatusDTO responseDTO =
        apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.APPLICATION, application.getId());
    assertThat(responseDTO.isAutoWaiverEnabled).isTrue();
    assertThat(responseDTO.isInherited).isFalse();
    assertThat(responseDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(responseDTO.autoPolicyWaiverOwnerId).isEqualTo(application.getId());
    assertThat(responseDTO.autoPolicyWaiverOwnerName).isEqualTo(application.getName());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_AutoWaiverDirectlyConfigured_Organization() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());
    ApiAutoPolicyWaiverStatusDTO responseDTO =
        apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.ORGANIZATION, organization.getId());
    assertThat(responseDTO.isAutoWaiverEnabled).isTrue();
    assertThat(responseDTO.isInherited).isFalse();
    assertThat(responseDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(responseDTO.autoPolicyWaiverOwnerId).isEqualTo(organization.getId());
    assertThat(responseDTO.autoPolicyWaiverOwnerName).isEqualTo(organization.getName());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_InvalidOwnerType() {
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.REPOSITORY, "fakeRepoId")).isInstanceOf(
        IllegalStateException.class).hasMessage("Unknown owner type: repository");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_NoAutoWavierConfigured_Application() {
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverStatusDTO responseDTO =
        apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.APPLICATION, application.getId());
    assertThat(responseDTO.isAutoWaiverEnabled).isFalse();
    assertThat(responseDTO.isInherited).isNull();
    assertThat(responseDTO.autoPolicyWaiverId).isNull();
    assertThat(responseDTO.autoPolicyWaiverOwnerId).isNull();
    assertThat(responseDTO.autoPolicyWaiverOwnerName).isNull();

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_NoAutoWavierConfigured_Organization() {
    Organization organization = tempEntity.newOrganization();
    ApiAutoPolicyWaiverStatusDTO responseDTO =
        apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.ORGANIZATION, organization.getId());
    assertThat(responseDTO.isAutoWaiverEnabled).isFalse();
    assertThat(responseDTO.isInherited).isNull();
    assertThat(responseDTO.autoPolicyWaiverId).isNull();
    assertThat(responseDTO.autoPolicyWaiverOwnerId).isNull();
    assertThat(responseDTO.autoPolicyWaiverOwnerName).isNull();

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_ConfiguredOnParent_Application() {
    Organization parentOrganization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(parentOrganization.getId());
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(parentOrganization.getId());
    ApiAutoPolicyWaiverStatusDTO responseDTO =
        apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.APPLICATION, application.getId());
    assertThat(responseDTO.isAutoWaiverEnabled).isTrue();
    assertThat(responseDTO.isInherited).isTrue();
    assertThat(responseDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(responseDTO.autoPolicyWaiverOwnerId).isEqualTo(parentOrganization.getId());
    assertThat(responseDTO.autoPolicyWaiverOwnerName).isEqualTo(parentOrganization.getName());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_ConfiguredOnParent_Organization() {
    Organization parentOrganization = tempEntity.newOrganization();
    Organization childOrganization = tempEntity.newOrganization(parentOrganization);
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(parentOrganization.getId());
    ApiAutoPolicyWaiverStatusDTO responseDTO =
        apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.ORGANIZATION, childOrganization.getId());
    assertThat(responseDTO.isAutoWaiverEnabled).isTrue();
    assertThat(responseDTO.isInherited).isTrue();
    assertThat(responseDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(responseDTO.autoPolicyWaiverOwnerId).isEqualTo(parentOrganization.getId());
    assertThat(responseDTO.autoPolicyWaiverOwnerName).isEqualTo(parentOrganization.getName());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_ConfiguredOnGrandParent_Application() {
    Organization grandParentOrganization = tempEntity.newOrganization();
    Organization parentOrganization = tempEntity.newOrganization(grandParentOrganization);
    Application childApplication = tempEntity.newApplication(parentOrganization.getId());
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(grandParentOrganization.getId());
    ApiAutoPolicyWaiverStatusDTO responseDTO =
        apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.APPLICATION, childApplication.getId());
    assertThat(responseDTO.isAutoWaiverEnabled).isTrue();
    assertThat(responseDTO.isInherited).isTrue();
    assertThat(responseDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(responseDTO.autoPolicyWaiverOwnerId).isEqualTo(grandParentOrganization.getId());
    assertThat(responseDTO.autoPolicyWaiverOwnerName).isEqualTo(grandParentOrganization.getName());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_ConfiguredOnGrandParent_Organization() {
    Organization grandParentOrganization = tempEntity.newOrganization();
    Organization parentOrganization = tempEntity.newOrganization(grandParentOrganization);
    Organization childOrganization = tempEntity.newOrganization(parentOrganization);
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(grandParentOrganization.getId());
    ApiAutoPolicyWaiverStatusDTO responseDTO =
        apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.ORGANIZATION, childOrganization.getId());
    assertThat(responseDTO.isAutoWaiverEnabled).isTrue();
    assertThat(responseDTO.isInherited).isTrue();
    assertThat(responseDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(responseDTO.autoPolicyWaiverOwnerId).isEqualTo(grandParentOrganization.getId());
    assertThat(responseDTO.autoPolicyWaiverOwnerName).isEqualTo(grandParentOrganization.getName());

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }

  @Test
  public void testGetApplicableAutoPolicyWaivers() {
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

    ApiAutoPolicyWaiverDTO results = apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(violation.getId());

    assertThat(results.ownerId).isEqualTo(ownerId);
    assertThat(results.threatLevel).isEqualTo(7);
    assertThat(results.reachable).isTrue();
    assertThat(results.pathForward).isFalse();
    assertThat(results.creatorId).isEqualTo("fakeCreatorId");
    assertThat(results.creatorName).isEqualTo("fakeCreatorName");

    Mockito.verifyNoInteractions(autoPolicyWaiverTelemetryMetrics);
  }
}
