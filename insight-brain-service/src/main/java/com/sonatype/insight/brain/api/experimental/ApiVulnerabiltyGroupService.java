/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupVulnerabilityDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroup;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroupVulnerability;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.collections4.CollectionUtils;

@Named
public class ApiVulnerabiltyGroupService
{
  private final VulnerabilityGroupDAO vulnerabilityGroupDAO;

  private final VulnerabilityGroupVulnerabilityDAO vulnerabilityGroupVulnerabilityDAO;

  private final OwnerDAO ownerDAO;

  @Inject
  public ApiVulnerabiltyGroupService(
      VulnerabilityGroupDAO vulnerabilityGroupDAO,
      VulnerabilityGroupVulnerabilityDAO vulnerabilityGroupVulnerabilityDAO,
      OwnerDAO ownerDAO)
  {
    this.vulnerabilityGroupDAO = vulnerabilityGroupDAO;
    this.vulnerabilityGroupVulnerabilityDAO = vulnerabilityGroupVulnerabilityDAO;
    this.ownerDAO = ownerDAO;
  }

  @Authorize(permission = Permission.WRITE)
  public String saveVulnerabilityGroup(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      ApiVulnerabilityGroupDTO apiVulnerabilityGroupDTO)
  {
    if (apiVulnerabilityGroupDTO == null || apiVulnerabilityGroupDTO.getOwnerId() == null) {
      throw new BadRequestException("No Owner ID provided in the request");
    }
    if (CollectionUtils.isEmpty(apiVulnerabilityGroupDTO.getVulnIds())) {
      throw new BadRequestException("No vulnerabilities provided in the request");
    }
    try (final TransactionContext tx = vulnerabilityGroupDAO.createTransactionContext()) {
      tx.begin();
      Owner groupOwner = ownerDAO.getById(tx, apiVulnerabilityGroupDTO.getOwnerId());
      if (groupOwner != null) {
        VulnerabilityGroup currentGroup =
            vulnerabilityGroupDAO.getByOwnerIdAndGroupName(tx, groupOwner.getId(),
                apiVulnerabilityGroupDTO.getGroupName());
        if (currentGroup != null) {
          deleteVulnerabilitiesInGroupByGroupId(tx, currentGroup.getId());
          saveVulnerabilitiesInGroup(tx, apiVulnerabilityGroupDTO.getVulnIds(), currentGroup.getId());
          tx.commit();
          return currentGroup.getId();
        }
        else {
          VulnerabilityGroup vulnGroupObj =
              new VulnerabilityGroup( apiVulnerabilityGroupDTO.getGroupName(),groupOwner.getId());
          vulnerabilityGroupDAO.insert(tx, vulnGroupObj);
          saveVulnerabilitiesInGroup(tx, apiVulnerabilityGroupDTO.getVulnIds(), vulnGroupObj.getId());
          tx.commit();
          return vulnGroupObj.getId();
        }
      }
      else {
        throw new NotFoundException("Cannot find Owner for id " + apiVulnerabilityGroupDTO.getOwnerId());
      }
    }
  }

  private void saveVulnerabilitiesInGroup(TransactionContext tx, List<String> vulnIds, String vulnerabilityGroupId) {
    for (String vulnid : vulnIds) {
      VulnerabilityGroupVulnerability vulnGroupVuln = new VulnerabilityGroupVulnerability(vulnerabilityGroupId, vulnid);
      vulnerabilityGroupVulnerabilityDAO.insert(tx, vulnGroupVuln);
    }
  }

  private void deleteVulnerabilitiesInGroupByGroupId(TransactionContext tx, String groupId) {
    List<VulnerabilityGroupVulnerability> deleteList = vulnerabilityGroupVulnerabilityDAO.getByGroupId(tx, groupId);
    deleteList.forEach(vulnerabilityGroupVulnerabilityDAO::delete);
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteVulnerabilityGroupById(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      String vulnerabilityGroupId)
  {
    try (final TransactionContext tx = vulnerabilityGroupDAO.createTransactionContext()) {
      tx.begin();
      VulnerabilityGroup currentGroup = vulnerabilityGroupDAO.getById(vulnerabilityGroupId);
      if (currentGroup != null) {
        vulnerabilityGroupDAO.delete(currentGroup);
        tx.commit();
      }
      else {
        throw new NotFoundException("Cannot find vulnerability group for id " + vulnerabilityGroupId);
      }
    }
  }

  @Authorize(permission = Permission.READ)
  public ApiVulnerabilityGroupDTO getVulnerabilityGroupById(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      String vulnerabilityGroupId)
  {
    ApiVulnerabilityGroupDTO vulnGroupDTO = new ApiVulnerabilityGroupDTO();
    try (final TransactionContext tx = vulnerabilityGroupDAO.createTransactionContext()) {
      VulnerabilityGroup vulnGroup = vulnerabilityGroupDAO.getById(vulnerabilityGroupId);
      if (vulnGroup != null) {
        List<VulnerabilityGroupVulnerability> vulnList =
            vulnerabilityGroupVulnerabilityDAO.getByGroupId(tx, vulnGroup.getId());
        vulnGroupDTO = new ApiVulnerabilityGroupDTO(vulnGroup.getId(), vulnGroup.getVulnerabilityGroupName(),
            vulnList.stream().map(VulnerabilityGroupVulnerability::getVulnerabilityRefId).collect(Collectors.toList()),
            vulnGroup.getOwnerId());
        return vulnGroupDTO;
      }
      else {
        throw new NotFoundException("Cannot find vulnerability group for id " + vulnerabilityGroupId);
      }
    }
  }

  @Authorize(permission = Permission.READ)
  public ApiVulnerabilityGroupDTO getVulnerabilityGroupByName(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      String vulnerabilityGroupName)
  {
    ApiVulnerabilityGroupDTO vulnGroupDTO = new ApiVulnerabilityGroupDTO();
    try (final TransactionContext tx = vulnerabilityGroupDAO.createTransactionContext()) {
      VulnerabilityGroup vulnGroup = vulnerabilityGroupDAO.getByGroupName(tx, vulnerabilityGroupName);
      if (vulnGroup != null) {
        List<VulnerabilityGroupVulnerability> vulnList =
            vulnerabilityGroupVulnerabilityDAO.getByGroupId(tx, vulnGroup.getId());
        vulnGroupDTO = new ApiVulnerabilityGroupDTO(vulnGroup.getId(), vulnGroup.getVulnerabilityGroupName(),
            vulnList.stream().map(VulnerabilityGroupVulnerability::getVulnerabilityRefId).collect(Collectors.toList()),
            vulnGroup.getOwnerId());
        return vulnGroupDTO;
      }
      else {
        throw new NotFoundException("Cannot find vulnerability group for name " + vulnerabilityGroupName);
      }
    }
  }

  @Authorize(permission = Permission.READ)
  public List<ApiVulnerabilityGroupDTO> getVulnerabilityGroupByOwner(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    List<ApiVulnerabilityGroupDTO> vulnGroupDTOList = new ArrayList<>();
    try (final TransactionContext tx = vulnerabilityGroupDAO.createTransactionContext()) {
      List<VulnerabilityGroup> vulnGroupList = vulnerabilityGroupDAO.getByOwnerId(tx, owner.getId());
      for (VulnerabilityGroup vulnerabilityGroup : vulnGroupList) {
        List<VulnerabilityGroupVulnerability> vulnList =
            vulnerabilityGroupVulnerabilityDAO.getByGroupId(tx, vulnerabilityGroup.getId());
        ApiVulnerabilityGroupDTO vulnGroupDTO = new ApiVulnerabilityGroupDTO(vulnerabilityGroup.getId(),
            vulnerabilityGroup.getVulnerabilityGroupName(),
            vulnList.stream().map(VulnerabilityGroupVulnerability::getVulnerabilityRefId).collect(Collectors.toList()));
        vulnGroupDTOList.add(vulnGroupDTO);
      }
    }
    return vulnGroupDTOList;
  }
}
