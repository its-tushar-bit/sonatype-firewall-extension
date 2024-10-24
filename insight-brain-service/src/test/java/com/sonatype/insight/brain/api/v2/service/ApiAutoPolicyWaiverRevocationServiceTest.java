/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationDTO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverRevocationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class ApiAutoPolicyWaiverRevocationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private AutoPolicyWaiverRevocationDAO autoPolicyWaiverRevocationDAO;

  @Inject
  private ApiAutoPolicyWaiverRevocationService apiAutoPolicyWaiverRevocationService;

  @Test
  public void testAddAutoPolicyWaiverRevocation_Application() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorName",
        "fakeCreatorId",
        new Date(),
        waiver.getId(),
        "3klajsf9aslkjh",
        "associatedPackageUrl",
        "scanId"
    );
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    ApiAutoPolicyWaiverRevocationDTO resultingDto = apiAutoPolicyWaiverRevocationService
        .addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);
    assertThat(resultingDto).isNotNull();
    assertThat(resultingDto.ownerId).isEqualTo(revocation.getOwnerId());
    assertThat(resultingDto.autoPolicyWaiverId).isEqualTo(revocation.getAutoPolicyWaiverId());
    assertThat(resultingDto.hash).isEqualTo(revocation.getHash());
    assertThat(resultingDto.scanId).isEqualTo(revocation.getScanId());
    assertThat(resultingDto.associatedPackageUrl).isEqualTo(revocation.getAssociatedPackageUrl());
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Organization() {
    Organization org = tempEntity.newOrganization();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(org.getId());
    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        org.getId(),
        "fakeCreatorName",
        "fakeCreatorId",
        new Date(),
        waiver.getId(),
        "asdfijh33asldkfj",
        "associatedPackageUrl",
        "scanId"
    );
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    ApiAutoPolicyWaiverRevocationDTO resultingDto = apiAutoPolicyWaiverRevocationService
        .addAutoPolicyWaiverRevocation(OwnerType.ORGANIZATION, org.getId(), dto);
    assertThat(resultingDto).isNotNull();
    assertThat(resultingDto.ownerId).isEqualTo(revocation.getOwnerId());
    assertThat(resultingDto.autoPolicyWaiverId).isEqualTo(revocation.getAutoPolicyWaiverId());
    assertThat(resultingDto.hash).isEqualTo(revocation.getHash());
    assertThat(resultingDto.scanId).isEqualTo(revocation.getScanId());
    assertThat(resultingDto.associatedPackageUrl).isEqualTo(revocation.getAssociatedPackageUrl());
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_AlreadyExists() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorName",
        "fakeCreatorId",
        new Date(),
        waiver.getId(),
        "asfliausydfoasfh",
        "associatedPackageUrl",
        "scanId"
    );
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    ApiAutoPolicyWaiverRevocationDTO resultingDto = apiAutoPolicyWaiverRevocationService
        .addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto);
    assertThat(resultingDto).isNotNull();
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("revocation already exists for this component");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_InvalidOwnerType() {
    ApiAutoPolicyWaiverRevocationDTO dto = new ApiAutoPolicyWaiverRevocationDTO();
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.REPOSITORY, "ownerId", dto)
    ).isInstanceOf(IllegalStateException.class).hasMessage("Unknown owner type: repository");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_InvalidOwnerId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    dto.ownerId = "invalid";
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("combination of ownerId and autoPolicyWaiverId is invalid");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_InvalidAutoPolicyWaiverId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    dto.autoPolicyWaiverId = "invalid";
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("combination of ownerId and autoPolicyWaiverId is invalid");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_WrongWaiverOwner() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    dto.ownerId = app.getOrganizationId();
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class)
        .hasMessage("combination of ownerId and autoPolicyWaiverId is invalid");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_InvalidHash() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    dto.hash = null;
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("hash is required");
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_InvalidScanId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    dto.scanId = null;
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("scanId is required");
  }
  
  @Test
  public void testAddAutoPolicyWaiverRevocation_MissingOwnerId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    ApiAutoPolicyWaiverRevocationDTO dto = ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation);
    dto.ownerId = null;
    assertThatThrownBy(() ->
            apiAutoPolicyWaiverRevocationService.addAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(), dto)
    ).isInstanceOf(BadRequestException.class).hasMessage("ownerId is required");
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_Application() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(),
        revocation.getId());
    assertThat(autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverId(app.getId(), waiver.getId())).isEmpty();
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_Organization() {
    Organization org = tempEntity.newOrganization();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(org.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(org.getId(), waiver.getId());
    apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(OwnerType.ORGANIZATION, org.getId(),
        revocation.getId());
    assertThat(autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverId(org.getId(), waiver.getId())).isEmpty();
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_InvalidOwnerType() {
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(OwnerType.REPOSITORY, "ownerId",
            "revocationId")
    ).isInstanceOf(IllegalStateException.class).hasMessage("Unknown owner type: repository");
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_InvalidOwnerId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(OwnerType.APPLICATION, "invalid",
            revocation.getId())
    ).isInstanceOf(NotFoundException.class).hasMessage(
        "Cannot find an auto policy waiver revocation with ID " +
            revocation.getId() + " for application with ID invalid");
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_InvalidAutoPolicyWaiverRevocationId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(OwnerType.APPLICATION, app.getId(),
            "invalid")
    ).isInstanceOf(NotFoundException.class).hasMessage(
        "AutoPolicyWaiverRevocation with ID invalid does not exist."
    );
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_OwnerIdMismatch() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    assertThatThrownBy(() ->
        apiAutoPolicyWaiverRevocationService.deleteAutoPolicyWaiverRevocation(OwnerType.APPLICATION,
            app.getOrganizationId(),
            revocation.getId())
    ).isInstanceOf(NotFoundException.class).hasMessage(
        "Cannot find an auto policy waiver revocation with ID " +
            revocation.getId() + " for application with ID " + app.getOrganizationId()
    );
  }
}
