/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupVulnerabilityDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroup;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroupVulnerability;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiVulnerabiltyGroupServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiVulnerabiltyGroupService service;

  @Test
  public void testSaveVulnerabilityGroup_nullApiVulnerabilityDTO() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> service.saveVulnerabilityGroup(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, null))
        .withMessage("No Owner ID provided in the request");
  }

  @Test
  public void testSaveVulnerabilityGroup_nullOwnerId() {
    ApiVulnerabilityGroupDTO vulnGroupDTO =
        new ApiVulnerabilityGroupDTO("group1", new ArrayList<>(Arrays.asList("CVE-123")), null);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.saveVulnerabilityGroup(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, vulnGroupDTO))
        .withMessage("No Owner ID provided in the request");
  }

  @Test
  public void testSaveVulnerabilityGroup_noVulnerabilities() {
    ApiVulnerabilityGroupDTO vulnGroupDTO = new ApiVulnerabilityGroupDTO("group1", Collections.emptyList(), "OWNER_ID");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.saveVulnerabilityGroup(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, vulnGroupDTO))
        .withMessage("No vulnerabilities provided in the request");
  }

  @Test
  public void testSaveVulnerabilityGroup_ownerIDNotFound() {
    ApiVulnerabilityGroupDTO vulnGroupDTO =
        new ApiVulnerabilityGroupDTO("group1", new ArrayList<>(Arrays.asList("CVE-123")), "OWNER_ID");
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.saveVulnerabilityGroup(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, vulnGroupDTO))
        .withMessage("Cannot find Owner for id OWNER_ID");
  }

  @Test
  public void testSaveVulnerabilityGroup() {
    VulnerabilityGroupDAO vGroupDao = new VulnerabilityGroupDAO();
    VulnerabilityGroupVulnerabilityDAO vGroupVulnDao = new VulnerabilityGroupVulnerabilityDAO();
    ApiVulnerabilityGroupDTO vulnGroupDTO = new ApiVulnerabilityGroupDTO("group1",
        new ArrayList<>(Arrays.asList("CVE-123")), Organization.ROOT_ORGANIZATION_ID);
    String groupId =
        service.saveVulnerabilityGroup(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, vulnGroupDTO);
    try (TransactionContext tx = vGroupDao.createTransactionContext()) {
      VulnerabilityGroup vulngroup = vGroupDao.getById(groupId);
      tempEntity.register(vulngroup);
      List<VulnerabilityGroupVulnerability> vGroupVuln =
          vGroupVulnDao.getByGroupId(tx, groupId);
      assertThat(vulnGroupDTO.getGroupName()).isEqualTo(vulngroup.getVulnerabilityGroupName());
      assertThat(vulnGroupDTO.getOwnerId()).isEqualTo(vulngroup.getOwnerId());
      assertThat(vulnGroupDTO.getVulnerabilityGroupId()).isEqualTo(vulnGroupDTO.getVulnerabilityGroupId());
      assertThat(vulnGroupDTO.getVulnIds()).isEqualTo(
          vGroupVuln.stream().map(VulnerabilityGroupVulnerability::getVulnerabilityRefId).collect(Collectors.toList()));
    }
  }

  @Test
  public void testSaveVulnerabilityGroup_update() {
    VulnerabilityGroup vulnGroup = tempEntity.newVulnerabilityGroup("group1", Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newVulnerabilityGroupVulnerability(vulnGroup.getId(), "CVE-1234");
    // new DTO with same Name and Group ID to update
    ApiVulnerabilityGroupDTO vulnGroupDTO = new ApiVulnerabilityGroupDTO(vulnGroup.getId(), "group1",
        new ArrayList<>(Arrays.asList("CVE-5678", "CVE-9101")), Organization.ROOT_ORGANIZATION_ID);
    String groupId =
        service.saveVulnerabilityGroup(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, vulnGroupDTO);
    ApiVulnerabilityGroupDTO actualVulnGroupDTO =
        service.getVulnerabilityGroupById(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, groupId);
    assertApiVulnerabilityGroupDTO(actualVulnGroupDTO, vulnGroupDTO);
  }

  @Test
  public void testGetVulnerabilityGroupsByOwner_OwnerNotExists() throws Exception {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getVulnerabilityGroupByOwner(OwnerType.APPLICATION, "OwnerId"))
        .withMessage("Could not find an application with ID OwnerId.");
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getVulnerabilityGroupByOwner(OwnerType.ORGANIZATION, "OwnerId"))
        .withMessage("Cannot find organization with ID OwnerId.");
  }

  @Test
  public void testGetVulnerabilityGroupsByOwner() throws Exception {
    VulnerabilityGroup vulnGroup = tempEntity.newVulnerabilityGroup("group1", Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newVulnerabilityGroupVulnerability(vulnGroup.getId(), "CVE-1234");
    ApiVulnerabilityGroupDTO actualVulnGroupDTO =
        new ApiVulnerabilityGroupDTO(vulnGroup.getId(), "group1", new ArrayList<>(Arrays.asList("CVE-1234")));
    List<ApiVulnerabilityGroupDTO> vulnGroupDTOList =
        service.getVulnerabilityGroupByOwner(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
    assertThat(vulnGroupDTOList).hasSize(1);
    assertApiVulnerabilityGroupDTO(vulnGroupDTOList.get(0), actualVulnGroupDTO);
  }

  @Test
  public void testGetVulnerabilityGroupByGroupName_groupNameNotExists() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.getVulnerabilityGroupByName(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, "group1"))
        .withMessage("Cannot find vulnerability group for name group1");
  }

  @Test
  public void testGetVulnerabilityGroupByGroupName() throws Exception {
    VulnerabilityGroup vulnGroup = tempEntity.newVulnerabilityGroup("group1", Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newVulnerabilityGroupVulnerability(vulnGroup.getId(), "CVE-1234");
    ApiVulnerabilityGroupDTO actualVulnGroupDTO = new ApiVulnerabilityGroupDTO(vulnGroup.getId(), "group1",
        new ArrayList<>(Arrays.asList("CVE-1234")), Organization.ROOT_ORGANIZATION_ID);
    ApiVulnerabilityGroupDTO vulnGroupDTO =
        service.getVulnerabilityGroupByName(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, "group1");
    assertApiVulnerabilityGroupDTO(vulnGroupDTO, actualVulnGroupDTO);
  }

  @Test
  public void testGetVulnerabilityGroupByVulnerabilityGroupId_GroupIdNotExists() throws Exception {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> service.getVulnerabilityGroupById(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, "id1"))
        .withMessage("Cannot find vulnerability group for id id1");
  }

  @Test
  public void testGetVulnerabilityGroupByVulnerabilityGroupId() throws Exception {
    VulnerabilityGroup vulnGroup = tempEntity.newVulnerabilityGroup("group1", Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newVulnerabilityGroupVulnerability(vulnGroup.getId(), "CVE-1234");
    ApiVulnerabilityGroupDTO actualVulnGroupDTO = new ApiVulnerabilityGroupDTO(vulnGroup.getId(), "group1",
        new ArrayList<>(Arrays.asList("CVE-1234")), Organization.ROOT_ORGANIZATION_ID);
    ApiVulnerabilityGroupDTO vulnGroupDTO =
        service.getVulnerabilityGroupById(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, vulnGroup.getId());
    assertApiVulnerabilityGroupDTO(vulnGroupDTO, actualVulnGroupDTO);
  }

  @Test
  public void testDeleteVulnerabilityGroupByVulnerabilityGroupId_GroupIdNotExists() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.deleteVulnerabilityGroupById(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, "id1"))
        .withMessage("Cannot find vulnerability group for id id1");
  }

  @Test
  public void testDeleteVulnerabilityGroupByVulnerabilityGroupId() throws Exception {
    VulnerabilityGroupDAO vGroupDao = new VulnerabilityGroupDAO();
    VulnerabilityGroupVulnerabilityDAO vGroupVulnDao = new VulnerabilityGroupVulnerabilityDAO();
    VulnerabilityGroup vulnGroup = tempEntity.newVulnerabilityGroup("group1", Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newVulnerabilityGroupVulnerability(vulnGroup.getId(), "CVE-1234");
    service.deleteVulnerabilityGroupById(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, vulnGroup.getId());
    try (TransactionContext tx = vGroupDao.createTransactionContext()) {
      VulnerabilityGroup vulngroup2 = vGroupDao.getById(vulnGroup.getId());
      List<VulnerabilityGroupVulnerability> vGroupVuln = vGroupVulnDao.getByGroupId(tx, vulnGroup.getId());
      assertThat(vulngroup2).isNull();
      assertThat(vGroupVuln).isEmpty();
    }
  }

  private void assertApiVulnerabilityGroupDTO(ApiVulnerabilityGroupDTO actual, ApiVulnerabilityGroupDTO expected) {
    assertThat(actual).isNotNull();
    assertThat(actual.getGroupName()).isEqualTo(expected.getGroupName());
    assertThat(actual.getVulnerabilityGroupId()).isEqualTo(expected.getVulnerabilityGroupId());
    assertThat(actual.getOwnerId()).isEqualTo(expected.getOwnerId());
    assertThat(actual.getVulnIds()).isEqualTo(expected.getVulnIds());
  }
}
