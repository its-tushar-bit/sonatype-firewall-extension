/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.autowaivers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverAdapter;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverStatusDTO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.List;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiAutoPolicyWaiverServiceTest
    extends AbstractComponentTest
{
  @Mock
  private TelemetrySender telemetrySender;

  @Inject
  private AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private ApiAutoPolicyWaiverService apiAutoPolicyWaiverService;

  @Test
  public void testAddAutoPolicyWaiver_Application() {
    Application application = tempEntity.newApplicationWithParent();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.threatLevel = 7;
    dto.reachability = true;
    dto.pathForward = false;

    ApiAutoPolicyWaiverDTO resultingDTO =
        apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto);

    assertThat(resultingDTO).isNotNull();
    assertThat(resultingDTO.ownerId).isEqualTo(application.getId());
    assertThat(resultingDTO.threatLevel).isEqualTo(dto.threatLevel);
    assertThat(resultingDTO.reachability).isEqualTo(dto.reachability);
    assertThat(resultingDTO.pathForward).isEqualTo(dto.pathForward);

    ArgumentCaptor<List<TelemetryData>> captor = ArgumentCaptor.forClass(List.class);
    verify(telemetrySender).send(captor.capture());
    List<TelemetryData> telemetryData = captor.getValue();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TelemetryPurpose.AUTO_POLICY_WAIVER);
    assertThat(telemetryData.get(0).getAttributes().get("auto_policy_waiver_action")).isEqualTo("CREATE");
  }

  @Test
  public void testAddAutoPolicyWaiver_Organization() {
    Organization organization = tempEntity.newOrganization();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.threatLevel = 7;
    dto.reachability = true;
    dto.pathForward = false;

    ApiAutoPolicyWaiverDTO resultingDTO =
        apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), dto);

    assertThat(resultingDTO).isNotNull();
    assertThat(resultingDTO.ownerId).isEqualTo(organization.getId());
    assertThat(resultingDTO.threatLevel).isEqualTo(dto.threatLevel);
    assertThat(resultingDTO.reachability).isEqualTo(dto.reachability);
    assertThat(resultingDTO.pathForward).isEqualTo(dto.pathForward);

    ArgumentCaptor<List<TelemetryData>> captor = ArgumentCaptor.forClass(List.class);
    verify(telemetrySender).send(captor.capture());
    List<TelemetryData> telemetryData = captor.getValue();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TelemetryPurpose.AUTO_POLICY_WAIVER);
    assertThat(telemetryData.get(0).getAttributes().get("auto_policy_waiver_action")).isEqualTo("CREATE");
  }

  @Test
  public void testAddAutoPolicyWaiver_InvalidOwnerType() {
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    assertThatThrownBy(() -> apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.REPOSITORY, "fakeRepoId", dto))
        .isInstanceOf(
            IllegalStateException.class)
        .hasMessage("Unknown owner type: repository");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testAddAutoPolicyWaiver_WithThreatLevel_negative() {
    Organization organization = tempEntity.newOrganization();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = organization.getId();
    dto.threatLevel = -1;
    dto.reachability = true;
    dto.pathForward = false;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), dto))
            .isInstanceOf(
                BadRequestException.class)
            .hasMessage("Invalid threat level: " + dto.threatLevel + ". Value must be between 1 and 10.");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testAddAutoPolicyWaiver_WithThreatLevel_tooLarge() {
    Organization organization = tempEntity.newOrganization();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = organization.getId();
    dto.threatLevel = 11;
    dto.reachability = true;
    dto.pathForward = false;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), dto))
            .isInstanceOf(
                BadRequestException.class)
            .hasMessage("Invalid threat level: " + dto.threatLevel + ". Value must be between 1 and 10.");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testAddAutoPolicyWaiver_WhenBothOptionsAreFalse() {
    Organization organization = tempEntity.newOrganization();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = organization.getId();
    dto.threatLevel = 7;
    dto.reachability = false;
    dto.pathForward = false;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), dto))
            .isInstanceOf(
                BadRequestException.class)
            .hasMessage("Path forward and reachability cannot both be false");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testAddAutoPolicyWaiver_WhenBothOptionsAreNull() {
    Organization organization = tempEntity.newOrganization();
    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = organization.getId();
    dto.threatLevel = 7;
    dto.reachability = null;
    dto.pathForward = null;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), dto))
            .isInstanceOf(
                BadRequestException.class)
            .hasMessage("Path forward and reachability cannot both be false");

    verifyNoInteractions(telemetrySender);
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

    assertThat(waiver.reachability).isEqualTo(autoPolicyWaiver.hasReachability());

    assertThat(waiver.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(waiver.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(waiver.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());

    verifyNoInteractions(telemetrySender);
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

    assertThat(waiver.reachability).isEqualTo(autoPolicyWaiver.hasReachability());

    assertThat(waiver.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(waiver.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(waiver.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testGetAutoPolicyWaiver_InvalidOwnerType() {
    assertThatThrownBy(() -> apiAutoPolicyWaiverService.getAutoPolicyWaiver(OwnerType.REPOSITORY, "fakeRepoId",
        "fakeWaiverId")).isInstanceOf(IllegalStateException.class).hasMessage("Unknown owner type: repository");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testDeleteAutoPolicyWaiver_Application() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId());
    assertThat(autoPolicyWaiverDAO.getById(autoPolicyWaiver.getId())).isNull();

    ArgumentCaptor<List<TelemetryData>> captor = ArgumentCaptor.forClass(List.class);
    verify(telemetrySender).send(captor.capture());
    List<TelemetryData> telemetryData = captor.getValue();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TelemetryPurpose.AUTO_POLICY_WAIVER);
    assertThat(telemetryData.get(0).getAttributes().get("auto_policy_waiver_action")).isEqualTo("DELETE");
  }

  @Test
  public void testDeleteAutoPolicyWaiver_Organization() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());
    apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(),
        autoPolicyWaiver.getId());
    assertThat(autoPolicyWaiverDAO.getById(autoPolicyWaiver.getId())).isNull();

    ArgumentCaptor<List<TelemetryData>> captor = ArgumentCaptor.forClass(List.class);
    verify(telemetrySender).send(captor.capture());
    List<TelemetryData> telemetryData = captor.getValue();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TelemetryPurpose.AUTO_POLICY_WAIVER);
    assertThat(telemetryData.get(0).getAttributes().get("auto_policy_waiver_action")).isEqualTo("DELETE");
  }

  @Test
  public void testDeleteAutoPolicyWaiver_InvalidOwnerType() {
    assertThatThrownBy(() -> apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.REPOSITORY, "fakeRepoId",
        "fakeWaiverId")).isInstanceOf(IllegalStateException.class).hasMessage("Unknown owner type: repository");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testDeleteAutoPolicyWaiver_OwnerIdMismatch() {
    Application application = tempEntity.newApplicationWithParent();
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            autoPolicyWaiver.getId())).isInstanceOf(
                NotFoundException.class)
                .hasMessage(
                    "Cannot find an auto policy waiver with ID " + autoPolicyWaiver.getId() + " for "
                        + OwnerType.APPLICATION
                        + " with ID " + application.getId());

    verifyNoInteractions(telemetrySender);
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
    assertThat(waiver.reachability).isEqualTo(autoPolicyWaiver.hasReachability());
    assertThat(waiver.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(waiver.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(waiver.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(waiver.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
    assertThat(waiver.ownerId).isEqualTo(application.getId());
    assertThat(waiver.ownerName).isEqualTo(application.getName());
    assertThat(waiver.ownerType).isEqualTo(OwnerType.APPLICATION.toString());
    assertThat(waiver.publicId).isEqualTo(application.getPublicId());

    verifyNoInteractions(telemetrySender);
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
    assertThat(waiver.reachability).isEqualTo(autoPolicyWaiver.hasReachability());
    assertThat(waiver.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(waiver.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(waiver.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(waiver.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
    assertThat(waiver.ownerId).isEqualTo(organization.getId());
    assertThat(waiver.ownerName).isEqualTo(organization.getName());
    assertThat(waiver.ownerType).isEqualTo(OwnerType.ORGANIZATION.toString());
    assertThat(waiver.publicId).isEqualTo(organization.getPublicId());

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testGetAutoPolicyWaivers_NoWaivers() {
    Application application = tempEntity.newApplicationWithParent();

    List<ApiAutoPolicyWaiverDTO> autoPolicyWaiverDTOList =
        apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId());
    assertThat(autoPolicyWaiverDTOList).isEmpty();

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testGetAutoPolicyWaivers_InvalidOwnerType() {
    assertThatThrownBy(() -> apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.REPOSITORY, "fakeRepoId"))
        .isInstanceOf(
            IllegalStateException.class)
        .hasMessage("Unknown owner type: repository");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testUpdateAutoPolicyWaiver_Application() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO dto = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);

    dto.threatLevel = 10;
    dto.reachability = false;
    dto.pathForward = true;

    ApiAutoPolicyWaiverDTO responseDto =
        apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            dto.autoPolicyWaiverId, dto);
    assertThat(responseDto.threatLevel).isEqualTo(dto.threatLevel);
    assertThat(responseDto.pathForward).isEqualTo(dto.pathForward);

    ArgumentCaptor<List<TelemetryData>> captor = ArgumentCaptor.forClass(List.class);
    verify(telemetrySender).send(captor.capture());
    List<TelemetryData> telemetryData = captor.getValue();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TelemetryPurpose.AUTO_POLICY_WAIVER);
    assertThat(telemetryData.get(0).getAttributes().get("auto_policy_waiver_action")).isEqualTo("UPDATE");
  }

  @Test
  public void testUpdateAutoPolicyWaiver_Organization() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());
    ApiAutoPolicyWaiverDTO dto = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);

    dto.threatLevel = 1;
    dto.reachability = true;
    dto.pathForward = true;

    ApiAutoPolicyWaiverDTO responseDto =
        apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(),
            dto.autoPolicyWaiverId, dto);
    assertThat(responseDto.threatLevel).isEqualTo(dto.threatLevel);
    assertThat(responseDto.pathForward).isEqualTo(dto.pathForward);

    ArgumentCaptor<List<TelemetryData>> captor = ArgumentCaptor.forClass(List.class);
    verify(telemetrySender).send(captor.capture());
    List<TelemetryData> telemetryData = captor.getValue();
    assertThat(telemetryData).hasSize(1);
    assertThat(telemetryData.get(0).getPurpose()).isEqualTo(TelemetryPurpose.AUTO_POLICY_WAIVER);
    assertThat(telemetryData.get(0).getAttributes().get("auto_policy_waiver_action")).isEqualTo("UPDATE");
  }

  @Test
  public void testUpdateAutoPolicyWaiver_InvalidOwnerType() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());
    ApiAutoPolicyWaiverDTO dto = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
    assertThatThrownBy(() -> apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.REPOSITORY, "fakeRepoId",
        dto.autoPolicyWaiverId,
        dto)).isInstanceOf(
            IllegalStateException.class).hasMessage("Unknown owner type: repository");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testUpdateAutoPolicyWaiver_ThreatLevelNegative() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO dto = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
    dto.threatLevel = -10;
    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            dto.autoPolicyWaiverId, dto)).isInstanceOf(
                BadRequestException.class)
                .hasMessage("Invalid threat level: " + dto.threatLevel + ". Value must be between 1 and 10.");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testUpdateAutoPolicyWaiver_ThreatLevelTooLarge() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO dto = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
    dto.threatLevel = 80;
    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            dto.autoPolicyWaiverId, dto)).isInstanceOf(
                BadRequestException.class)
                .hasMessage("Invalid threat level: " + dto.threatLevel + ". Value must be between 1 and 10.");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testUpdateAutoPolicyWaiver_NoChange() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            autoPolicyWaiverDTO.autoPolicyWaiverId, autoPolicyWaiverDTO)).isInstanceOf(BadRequestException.class)
                .hasMessage("No changes made to auto policy waiver configuration");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testUpdateAutoPolicyWaiver_AutoWaiverIdMismatch() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            "mismatchedId", autoPolicyWaiverDTO)).isInstanceOf(BadRequestException.class)
                .hasMessage("Auto policy waiver ID in request path does not match request body");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testAddAutoPolicyWaiver_AlreadyExists_Application() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    List<ApiAutoPolicyWaiverDTO> autoPolicyWaivers =
        apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId());
    assertThat(autoPolicyWaivers).hasSize(1);

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.threatLevel = 1;
    dto.reachability = true;
    dto.pathForward = true;

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO =
        apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto);

    assertThat(apiAutoPolicyWaiverDTO).isNotNull();
    assertThat(autoPolicyWaiver.getId()).isNotEqualTo(apiAutoPolicyWaiverDTO.autoPolicyWaiverId);

    autoPolicyWaivers = apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId());
    assertThat(autoPolicyWaivers).hasSize(2);

    assertThat(autoPolicyWaivers.get(0).autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(autoPolicyWaivers.get(1).autoPolicyWaiverId).isEqualTo(apiAutoPolicyWaiverDTO.autoPolicyWaiverId);
  }

  @Test
  public void testAddAutoPolicyWaiver_AlreadyExists_Application_And_Scope() {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    assertThat(apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId()))
        .hasSize(1);

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.threatLevel = autoPolicyWaiver.getThreatLevel();
    dto.reachability = autoPolicyWaiver.hasReachability();
    dto.pathForward = autoPolicyWaiver.hasPathForward();

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Only one auto policy waiver is allowed for a given owner and scope " +
                "(not reachable/no path forward combination)");

    assertThat(apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId()))
        .hasSize(1);

    // changing thread level still does not permit an additional waiver
    dto.threatLevel = 5;

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Only one auto policy waiver is allowed for a given owner and scope " +
                "(not reachable/no path forward combination)");

    assertThat(apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId()))
        .hasSize(1);
  }

  @Test
  public void testAddAutoPolicyWaiver_AlreadyExists_Organization() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());

    List<ApiAutoPolicyWaiverDTO> autoPolicyWaivers =
        apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.ORGANIZATION, organization.getId());
    assertThat(autoPolicyWaivers).hasSize(1);

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.threatLevel = 1;
    dto.reachability = true;
    dto.pathForward = true;

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO =
        apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), dto);
    assertThat(apiAutoPolicyWaiverDTO).isNotNull();
    assertThat(autoPolicyWaiver.getId()).isNotEqualTo(apiAutoPolicyWaiverDTO.autoPolicyWaiverId);

    autoPolicyWaivers = apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.ORGANIZATION, organization.getId());
    assertThat(autoPolicyWaivers).hasSize(2);

    assertThat(autoPolicyWaivers.get(0).autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(autoPolicyWaivers.get(1).autoPolicyWaiverId).isEqualTo(apiAutoPolicyWaiverDTO.autoPolicyWaiverId);
  }

  @Test
  public void testAddAutoPolicyWaiver_AlreadyExists_Organization_And_Scope() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId(), 8, true, true);

    assertThat(apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.ORGANIZATION, organization.getId()))
        .hasSize(1);

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.threatLevel = autoPolicyWaiver.getThreatLevel();
    dto.reachability = autoPolicyWaiver.hasReachability();
    dto.pathForward = autoPolicyWaiver.hasPathForward();

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), dto))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Only one auto policy waiver is allowed for a given owner and scope " +
                "(not reachable/no path forward combination)");

    assertThat(apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.ORGANIZATION, organization.getId()))
        .hasSize(1);

    // changing thread level still does not permit an additional waiver
    dto.threatLevel = 5;

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.ORGANIZATION, organization.getId(), dto))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Only one auto policy waiver is allowed for a given owner and scope " +
                "(not reachable/no path forward combination)");

    assertThat(apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.ORGANIZATION, organization.getId()))
        .hasSize(1);
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

    verifyNoInteractions(telemetrySender);
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

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_InvalidOwnerType() {
    assertThatThrownBy(() -> apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.REPOSITORY, "fakeRepoId"))
        .isInstanceOf(
            IllegalStateException.class)
        .hasMessage("Unknown owner type: repository");

    verifyNoInteractions(telemetrySender);
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

    verifyNoInteractions(telemetrySender);
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

    verifyNoInteractions(telemetrySender);
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

    verifyNoInteractions(telemetrySender);
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

    verifyNoInteractions(telemetrySender);
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

    verifyNoInteractions(telemetrySender);
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

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testGetApplicableAutoPolicyWaiver() {
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

    ApiAutoPolicyWaiverDTO result = apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(violation.getId());

    assertThat(result.ownerId).isEqualTo(ownerId);
    assertThat(result.threatLevel).isEqualTo(7);
    assertThat(result.reachability).isTrue();
    assertThat(result.pathForward).isFalse();
    assertThat(result.creatorId).isEqualTo("fakeCreatorId");
    assertThat(result.creatorName).isEqualTo("fakeCreatorName");

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testGetApplicableAutoPolicyWaiver_EXACT_COMPONENT_withExclusionHashMatched() {
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    String ownerId = newApp.getId();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(ownerId);
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "fakeHash");
    violation.setConstraintFacts(constraintFacts);
    violation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());

    policyViolationDAO.update(violation);
    // add exclusion with policy violation Id
    tempEntity.newAutoPolicyWaiverExclusion(
        ownerId,
        "creatorId",
        "creatorName",
        new Date(),
        autoPolicyWaiver.getId(),
        evaluation.getScanId(),
        "fakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT,
        violation.getId(),
        violation.getThreatLevel(),
        null,
        null,
        null,
        policy.getId(),
        identifier,
        violation.getConstraintFacts());

    ApiAutoPolicyWaiverDTO result = apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(violation.getId());

    assertThat(result).isNull();
  }

  @Test
  public void testGetApplicableAutoPolicyWaiver_EXACT_COMPONENT_withExclusionHashNotMatched() {
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    String ownerId = newApp.getId();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(ownerId);
    // different "hash" in the exclusion
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "Diffhash");
    violation.setConstraintFacts(constraintFacts);
    violation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());

    policyViolationDAO.update(violation);
    // add exclusion with diff hash not, and violation id null
    tempEntity.newAutoPolicyWaiverExclusion(
        ownerId,
        "creatorId",
        "creatorName",
        new Date(),
        autoPolicyWaiver.getId(),
        evaluation.getScanId(),
        "otherFakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT,
        null,
        violation.getThreatLevel(),
        null,
        null,
        null,
        policy.getId(),
        identifier,
        violation.getConstraintFacts());

    ApiAutoPolicyWaiverDTO result = apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(violation.getId());

    assertThat(result.ownerId).isEqualTo(ownerId);
    assertThat(result.threatLevel).isEqualTo(7);
    assertThat(result.reachability).isTrue();
    assertThat(result.pathForward).isFalse();
    assertThat(result.creatorId).isEqualTo("fakeCreatorId");
    assertThat(result.creatorName).isEqualTo("fakeCreatorName");
  }

  @Test
  public void testGetApplicableAutoPolicyWaiver_EXACT_COMPONENT_withExclusionHashNotMatchedButViolationIdMatched() {
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    String ownerId = newApp.getId();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(ownerId);
    // same "hash" in the exclusion
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "fakeHash");
    violation.setConstraintFacts(constraintFacts);
    violation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());

    policyViolationDAO.update(violation);
    // add exclusion with policy violation
    tempEntity.newAutoPolicyWaiverExclusion(
        ownerId,
        "creatorId",
        "creatorName",
        new Date(),
        autoPolicyWaiver.getId(),
        evaluation.getScanId(),
        "diffHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT,
        violation.getId(),
        violation.getThreatLevel(),
        null,
        null,
        null,
        policy.getId(),
        identifier,
        violation.getConstraintFacts());

    ApiAutoPolicyWaiverDTO result = apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(violation.getId());

    assertThat(result).isNull();
  }

  // DEFAULT
  @Test
  public void testGetApplicableAutoPolicyWaiver_DEFAULT() {
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    String ownerId = newApp.getId();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(ownerId);
    // same "hash" in the exclusion
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "fakeHash");
    violation.setConstraintFacts(constraintFacts);
    violation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());

    policyViolationDAO.update(violation);
    // add exclusion with policy violation
    tempEntity.newAutoPolicyWaiverExclusion(
        ownerId,
        "creatorId",
        "creatorName",
        new Date(),
        autoPolicyWaiver.getId(),
        evaluation.getScanId(),
        "diffHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT,
        violation.getId(),
        violation.getThreatLevel(),
        null,
        null,
        null,
        policy.getId(),
        identifier,
        violation.getConstraintFacts());
    ApiAutoPolicyWaiverDTO result = apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(violation.getId());

    assertThat(result).isNull();
  }

  // ALL_VERSIONS AND EXACT With NO Violation
  @Test
  public void testGetApplicableAutoPolicyWaiver_NoViolationIdInExclusion() {
    List<ConstraintFact> constraintFacts = tempEntity.createArbitraryConstraintFacts();
    Organization newOrg = tempEntity.newOrganization("NewOrg");
    Application newApp = tempEntity.newApplication("NewApp", "AppPublicId", newOrg.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(newApp.getId(), BuildStageType.ID, "scanId");
    Policy policy = tempEntity.newPolicy(newOrg);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0", "c1", "jar");
    String ownerId = newApp.getId();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(ownerId);
    // same "hash" in the exclusion
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, identifier, "fakeHash");
    violation.setConstraintFacts(constraintFacts);
    violation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());

    policyViolationDAO.update(violation);
    // add exclusion with different version and NO policy violation
    ComponentIdentifier diffVersionIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "2.0", "c1", "jar");
    tempEntity.newAutoPolicyWaiverExclusion(
        ownerId,
        "creatorId",
        "creatorName",
        new Date(),
        autoPolicyWaiver.getId(),
        evaluation.getScanId(),
        "differentHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT,
        null,
        violation.getThreatLevel(),
        null,
        null,
        null,
        policy.getId(),
        diffVersionIdentifier,
        violation.getConstraintFacts());

    ApiAutoPolicyWaiverDTO result = apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(violation.getId());

    assertThat(result.ownerId).isEqualTo(ownerId);
    assertThat(result.threatLevel).isEqualTo(7);
    assertThat(result.reachability).isTrue();
    assertThat(result.pathForward).isFalse();
    assertThat(result.creatorId).isEqualTo("fakeCreatorId");
    assertThat(result.creatorName).isEqualTo("fakeCreatorName");

    // add exclusion with ALL VERSIONS and NO policy violation
    tempEntity.newAutoPolicyWaiverExclusion(
        ownerId,
        "creatorId",
        "creatorName",
        new Date(),
        autoPolicyWaiver.getId(),
        evaluation.getScanId(),
        "diffHash",
        ComponentMatcherStrategyForExclusion.ALL_VERSIONS,
        violation.getId(),
        violation.getThreatLevel(),
        null,
        null,
        null,
        policy.getId(),
        diffVersionIdentifier,
        violation.getConstraintFacts());

    result = apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(violation.getId());

    assertThat(result).isNull();
  }

  /**
   * CLM-40943 — when the Report page calls this endpoint for a hosted-repo violation
   * (which lives in {@code repository_policy_violation}, a different table), we return
   * {@code null} (no applicable auto-waiver) rather than 404. Auto-waivers are an
   * application-scan concept; hosted-repo violations don't support them.
   */
  @Test
  public void testGetApplicableAutoPolicyWaiver_HostedRepoViolation_returnsNull() {
    Repository repository = tempEntity.newRepository("rm1", "r1", "maven2");
    RepositoryPolicyViolation repoViolation = tempEntity.newRepositoryPolicyViolation(
        repository.getId(), "outer.zip!/inner.jar");

    ApiAutoPolicyWaiverDTO result =
        apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver(repoViolation.getId());

    assertThat(result).isNull();
  }

  /**
   * CLM-40943 — for a truly unknown violation ID (not in {@code policy_violation} or
   * {@code repository_policy_violation}), preserve the pre-existing 404 behavior so callers
   * can still distinguish "doesn't exist" from "exists but no waiver applies."
   */
  @Test
  public void testGetApplicableAutoPolicyWaiver_UnknownViolationId_throws404() {
    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver("definitely-not-a-real-id"))
            .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void testAllMethods_AutoPolicyWaiverIsDisabled() {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverDTO dto = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);

    final String disabledAutoWaiversMessage = "Auto Policy Waivers feature is not enabled";
    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.addAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(), dto))
            .isInstanceOf(
                UnauthorizedException.class)
            .hasMessage(disabledAutoWaiversMessage);

    assertThatThrownBy(() -> apiAutoPolicyWaiverService.getAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
        autoPolicyWaiver.getId())).isInstanceOf(UnauthorizedException.class).hasMessage(disabledAutoWaiversMessage);

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.deleteAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            autoPolicyWaiver.getId())).isInstanceOf(UnauthorizedException.class).hasMessage(disabledAutoWaiversMessage);

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.getAutoPolicyWaivers(OwnerType.APPLICATION, application.getId())).isInstanceOf(
            UnauthorizedException.class).hasMessage(disabledAutoWaiversMessage);

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.updateAutoPolicyWaiver(OwnerType.APPLICATION, application.getId(),
            dto.autoPolicyWaiverId, dto))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(disabledAutoWaiversMessage);

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.getAutoPolicyWaiverStatus(OwnerType.APPLICATION, application.getId()))
            .isInstanceOf(
                UnauthorizedException.class)
            .hasMessage(disabledAutoWaiversMessage);

    assertThatThrownBy(() -> apiAutoPolicyWaiverService.getApplicableAutoPolicyWaiver("fakeViolationId")).isInstanceOf(
        UnauthorizedException.class).hasMessage(disabledAutoWaiversMessage);

    assertThatThrownBy(
        () -> apiAutoPolicyWaiverService.getApplicableAutoWaivers(OwnerType.APPLICATION, application.getId()))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage(disabledAutoWaiversMessage);

    verifyNoInteractions(telemetrySender);
  }

  @Test
  public void testGetApplicableAutoWaivers_CorrectlyFetchesAllApplicableAutoWaivers_AtEachOwnerLevel() {
    // Hierarchy = org1 -> org2 -> app
    final Organization org1 = tempEntity.newOrganization();
    final Organization org2 = tempEntity.newOrganization(org1);
    final Application app = tempEntity.newApplication(org2.getId());

    // Org1 auto waivers:
    // NPF
    // Not Reachable
    final AutoPolicyWaiver waiver1 = tempEntity.newAutoPolicyWaiver(org1.getId(), 10, false, true);
    final AutoPolicyWaiver waiver2 = tempEntity.newAutoPolicyWaiver(org1.getId(), 7, true, false);

    // Org2 auto waivers:
    // Not Reachable + NPF
    // NPF (overrides org1 auto waiver)
    final AutoPolicyWaiver waiver3 = tempEntity.newAutoPolicyWaiver(org2.getId(), 4, true, true);
    final AutoPolicyWaiver waiver4 = tempEntity.newAutoPolicyWaiver(org2.getId(), 5, false, true);

    // App auto waivers:
    // Not Reachable (overrides org1 auto waiver)
    final AutoPolicyWaiver waiver5 = tempEntity.newAutoPolicyWaiver(app.getId(), 8, true, false);

    // The applicable auto waivers for org1 should be:
    // waiver1: NPF (from org1)
    // waiver2: Not Reachable (from org1)
    List<ApiAutoPolicyWaiverStatusDTO> applicableAutoWaivers =
        apiAutoPolicyWaiverService.getApplicableAutoWaivers(OwnerType.ORGANIZATION, org1.getId());
    assertThat(applicableAutoWaivers)
        .hasSize(2);
    ApiAutoPolicyWaiverStatusDTO dto = applicableAutoWaivers.get(0);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isFalse();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(waiver2.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(org1.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(org1.getName());
    assertThat(dto.createTime).isNotNull();
    assertThat(dto.threatLevel).isEqualTo(waiver2.getThreatLevel());
    assertThat(dto.hasNotReachable).isEqualTo(waiver2.hasReachability());
    assertThat(dto.hasNoPathForward).isEqualTo(waiver2.hasPathForward());
    assertThat(dto.scopesOperatorAny).isEqualTo(waiver2.getScopesOperatorAny());
    dto = applicableAutoWaivers.get(1);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isFalse();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(waiver1.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(org1.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(org1.getName());
    assertThat(dto.createTime).isNotNull();
    assertThat(dto.threatLevel).isEqualTo(waiver1.getThreatLevel());
    assertThat(dto.hasNotReachable).isEqualTo(waiver1.hasReachability());
    assertThat(dto.hasNoPathForward).isEqualTo(waiver1.hasPathForward());
    assertThat(dto.scopesOperatorAny).isEqualTo(waiver1.getScopesOperatorAny());

    // The applicable auto waivers for org2 should be:
    // waiver3: Not Reachable + NPF (from org2)
    // waiver4: NPF (from org2)
    // waiver2: Not Reachable (from org1)
    applicableAutoWaivers =
        apiAutoPolicyWaiverService.getApplicableAutoWaivers(OwnerType.ORGANIZATION, org2.getId());
    assertThat(applicableAutoWaivers)
        .hasSize(3);
    dto = applicableAutoWaivers.get(0);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isFalse();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(waiver3.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(org2.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(org2.getName());
    assertThat(dto.createTime).isNotNull();
    assertThat(dto.threatLevel).isEqualTo(waiver3.getThreatLevel());
    assertThat(dto.hasNotReachable).isEqualTo(waiver3.hasReachability());
    assertThat(dto.hasNoPathForward).isEqualTo(waiver3.hasPathForward());
    assertThat(dto.scopesOperatorAny).isEqualTo(waiver3.getScopesOperatorAny());
    dto = applicableAutoWaivers.get(1);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isTrue();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(waiver2.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(org1.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(org1.getName());
    assertThat(dto.createTime).isNotNull();
    assertThat(dto.threatLevel).isEqualTo(waiver2.getThreatLevel());
    assertThat(dto.hasNotReachable).isEqualTo(waiver2.hasReachability());
    assertThat(dto.hasNoPathForward).isEqualTo(waiver2.hasPathForward());
    assertThat(dto.scopesOperatorAny).isEqualTo(waiver2.getScopesOperatorAny());
    dto = applicableAutoWaivers.get(2);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isFalse();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(waiver4.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(org2.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(org2.getName());
    assertThat(dto.createTime).isNotNull();
    assertThat(dto.threatLevel).isEqualTo(waiver4.getThreatLevel());
    assertThat(dto.hasNotReachable).isEqualTo(waiver4.hasReachability());
    assertThat(dto.hasNoPathForward).isEqualTo(waiver4.hasPathForward());
    assertThat(dto.scopesOperatorAny).isEqualTo(waiver4.getScopesOperatorAny());

    // The applicable auto waivers for app should be:
    // waiver3: Not Reachable + NPF (from org2)
    // waiver4: NPF (from org2)
    // waiver5: Not Reachable (from app)
    applicableAutoWaivers =
        apiAutoPolicyWaiverService.getApplicableAutoWaivers(OwnerType.APPLICATION, app.getId());
    assertThat(applicableAutoWaivers)
        .hasSize(3);
    dto = applicableAutoWaivers.get(0);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isTrue();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(waiver3.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(org2.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(org2.getName());
    assertThat(dto.createTime).isNotNull();
    assertThat(dto.threatLevel).isEqualTo(waiver3.getThreatLevel());
    assertThat(dto.hasNotReachable).isEqualTo(waiver3.hasReachability());
    assertThat(dto.hasNoPathForward).isEqualTo(waiver3.hasPathForward());
    assertThat(dto.scopesOperatorAny).isEqualTo(waiver3.getScopesOperatorAny());
    dto = applicableAutoWaivers.get(1);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isFalse();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(waiver5.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(app.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(app.getName());
    assertThat(dto.createTime).isNotNull();
    assertThat(dto.threatLevel).isEqualTo(waiver5.getThreatLevel());
    assertThat(dto.hasNotReachable).isEqualTo(waiver5.hasReachability());
    assertThat(dto.hasNoPathForward).isEqualTo(waiver5.hasPathForward());
    assertThat(dto.scopesOperatorAny).isEqualTo(waiver5.getScopesOperatorAny());
    dto = applicableAutoWaivers.get(2);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isTrue();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(waiver4.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(org2.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(org2.getName());
    assertThat(dto.createTime).isNotNull();
    assertThat(dto.threatLevel).isEqualTo(waiver4.getThreatLevel());
    assertThat(dto.hasNotReachable).isEqualTo(waiver4.hasReachability());
    assertThat(dto.hasNoPathForward).isEqualTo(waiver4.hasPathForward());
    assertThat(dto.scopesOperatorAny).isEqualTo(waiver4.getScopesOperatorAny());
  }

  @Test
  public void testGetApplicableAutoWaivers_NoApplicableAutoWaivers_AtEachOwnerLevel() {
    // Hierarchy = org1 -> org2 -> app
    final Organization org1 = tempEntity.newOrganization();
    final Organization org2 = tempEntity.newOrganization(org1);
    final Application app = tempEntity.newApplication(org2.getId());

    List<ApiAutoPolicyWaiverStatusDTO> applicableAutoWaivers =
        apiAutoPolicyWaiverService.getApplicableAutoWaivers(OwnerType.ORGANIZATION, org1.getId());
    assertThat(applicableAutoWaivers)
        .isEmpty();

    applicableAutoWaivers =
        apiAutoPolicyWaiverService.getApplicableAutoWaivers(OwnerType.ORGANIZATION, org2.getId());
    assertThat(applicableAutoWaivers)
        .isEmpty();

    applicableAutoWaivers =
        apiAutoPolicyWaiverService.getApplicableAutoWaivers(OwnerType.APPLICATION, app.getId());
    assertThat(applicableAutoWaivers)
        .isEmpty();
  }

  @Test
  public void testGetApplicableAutoWaivers_InvalidOwnerType() {
    assertThatThrownBy(() -> apiAutoPolicyWaiverService.getApplicableAutoWaivers(OwnerType.REPOSITORY, "fakeRepoId"))
        .isInstanceOf(
            IllegalStateException.class)
        .hasMessage("Unknown owner type: repository");

    verifyNoInteractions(telemetrySender);
  }
}
