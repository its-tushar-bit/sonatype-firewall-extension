/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cpematching;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CpeMatchingConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public class CpeMatchingConfigurationService
{
  private final OwnerDAO ownerDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO;

  private static final Logger log = LoggerFactory.getLogger(CpeMatchingConfigurationService.class);

  @Inject
  public CpeMatchingConfigurationService(
      final OwnerDAO ownerDAO,
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO)
  {
    this.ownerDAO = ownerDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.cpeMatchingConfigurationDAO = cpeMatchingConfigurationDAO;
  }

  @Authorize(permission = Permission.READ)
  public CpeMatchingConfigurationDTO getCpeMatchingConfiguration(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId)
  {
    CpeMatchingConfigurationDTO cpeMatchingConfigurationDTO = new CpeMatchingConfigurationDTO();
    ownerDAO.getByIdNotNull(ownerId); // will trigger a 404 if the owner does not exist

    String parentOrgId;
    switch (ownerType) {
      case APPLICATION:
        Application app = applicationDAO.getById(ownerId);
        CpeMatchingConfiguration appCpeMatchingConfiguration = cpeMatchingConfigurationDAO.getByOwnerId(ownerId);
        parentOrgId = app.getOrganizationId();
        if (appCpeMatchingConfiguration != null) {
          cpeMatchingConfigurationDTO.enabled = appCpeMatchingConfiguration.isCpeEnabled();
        }
        break;
      case ORGANIZATION:
        Organization org = organizationDAO.getByIdNotNull(ownerId);
        CpeMatchingConfiguration orgCpeMatchingConfiguration = cpeMatchingConfigurationDAO.getByOwnerId(ownerId);
        parentOrgId = org.getParentOrganizationId();
        if (orgCpeMatchingConfiguration != null) {
          cpeMatchingConfigurationDTO.enabled = orgCpeMatchingConfiguration.isCpeEnabled();
          cpeMatchingConfigurationDTO.allowOverride = orgCpeMatchingConfiguration.isAllowOverride();
        }
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }

    while (parentOrgId != null) {
      Organization org = organizationDAO.getByIdNotNull(parentOrgId);
      CpeMatchingConfiguration parentCpeMatchingConfiguration = cpeMatchingConfigurationDAO.getByOwnerId(parentOrgId);

      if (parentCpeMatchingConfiguration != null) {
        if (cpeMatchingConfigurationDTO.enabledInParent == null) {
          cpeMatchingConfigurationDTO.enabledInParent = parentCpeMatchingConfiguration.isCpeEnabled();
        }

        if (!parentCpeMatchingConfiguration.isAllowOverride()) {
          cpeMatchingConfigurationDTO.enabled = parentCpeMatchingConfiguration.isCpeEnabled();
          cpeMatchingConfigurationDTO.inheritedFromOrganizationName = org.getName();
          cpeMatchingConfigurationDTO.allowOverride = false;
        }
        else if (cpeMatchingConfigurationDTO.enabled == null) {
          cpeMatchingConfigurationDTO.enabled = parentCpeMatchingConfiguration.isCpeEnabled();
          cpeMatchingConfigurationDTO.inheritedFromOrganizationName = org.getName();
        }
      }

      parentOrgId = org.getParentOrganizationId();
    }

    // Set default values for nullables boolean objects after the algorithm logic has finished
    if (cpeMatchingConfigurationDTO.enabled == null) {
      cpeMatchingConfigurationDTO.enabled = false;
    }

    if (cpeMatchingConfigurationDTO.enabledInParent == null) {
      cpeMatchingConfigurationDTO.enabledInParent = false;
    }

    return cpeMatchingConfigurationDTO;
  }

  @Authorize(permission = Permission.WRITE)
  public CpeMatchingConfigurationDTO updateCpeMatchingConfiguration(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      CpeMatchingConfigurationRequest configRequest)
  {
    CpeMatchingConfiguration cpeConfigUpdated;
    validateConfigurationDTO(configRequest);
    ownerDAO.getByIdNotNull(internalOwnerId); // will trigger a 404 if the owner does not exist
    switch (ownerType) {
      case APPLICATION:
        cpeConfigUpdated = updateConfigurationForOwner(new CpeMatchingConfiguration(internalOwnerId,
            configRequest.enabled, false));
        break;
      case ORGANIZATION:
        cpeConfigUpdated = updateConfigurationForOwner(
            new CpeMatchingConfiguration(internalOwnerId, configRequest.enabled, configRequest.allowOverride));
        if (!configRequest.allowOverride) {
          disableCpeMatchingConfiguration(ownerType, internalOwnerId, false);
        }
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
    CpeMatchingConfigurationDTO cpeConfigUpdatedDTO = new CpeMatchingConfigurationDTO();
    cpeConfigUpdatedDTO.enabled = cpeConfigUpdated.isCpeEnabled();
    cpeConfigUpdatedDTO.allowOverride = cpeConfigUpdated.isAllowOverride();
    return cpeConfigUpdatedDTO;
  }

  private void validateConfigurationDTO(final CpeMatchingConfigurationRequest request) {
    if (request == null) {
      throw new BadRequestException("CPE matching configuration cannot be null");
    }
    if (request.enabled == null) {
      throw new BadRequestException("CPE matching configuration enabled cannot be null");
    }
  }

  @Authorize(permission = Permission.WRITE)
  public void disableCpeMatchingConfiguration(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      final Boolean allowOverride)
  {
    ownerDAO.getByIdNotNull(internalOwnerId); // will trigger a 404 if the owner does not exist
    switch (ownerType) {
      case APPLICATION:
        disableForApplication(internalOwnerId);
        break;
      case ORGANIZATION:
        disableForOrganization(internalOwnerId, allowOverride);
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
  }

  private void disableForOrganization(final String orgId, final Boolean allowOverride) {
    log.info("Disable CPE matching configuration for organization with ID: {}.", orgId);
    CpeMatchingConfiguration orgConfig = cpeMatchingConfigurationDAO.getByOwnerId(orgId);
    if (orgConfig == null && allowOverride == null) {
      return;
    }

    try (TransactionContext tx = cpeMatchingConfigurationDAO.createTransactionContext()) {
      tx.begin();
      if (orgConfig == null) {
        orgConfig = new CpeMatchingConfiguration(orgId, false, allowOverride);
        cpeMatchingConfigurationDAO.insert(tx, orgConfig);
      }
      if (allowOverride != null && orgConfig.isAllowOverride() != allowOverride) {
        orgConfig.setAllowOverride(allowOverride);
        cpeMatchingConfigurationDAO.update(tx, orgConfig);
      }
      if (!orgConfig.isAllowOverride()) {
        deleteChildrenConfigInHierarchy(tx, orgId);
      }
      tx.commit();
    }
  }

  private void deleteChildrenConfigInHierarchy(final TransactionContext tx, final String orgId) {
    final List<String> childApplicationIds = new ArrayList<>(getChildApplicationIdsForOrg(orgId, tx));
    organizationDAO.getAllChildOrganizations(tx, orgId).stream()
        .filter(org -> !orgId.equals(org.getId())) //remove itself from children
        .forEach(childOrg -> {
          childApplicationIds.addAll(getChildApplicationIdsForOrg(childOrg.getId(), tx));
          cpeMatchingConfigurationDAO.delete(tx, childOrg.getId());
        });
    childApplicationIds.forEach(appId -> cpeMatchingConfigurationDAO.delete(tx, appId));
  }

  private List<String> getChildApplicationIdsForOrg(final String orgId, final TransactionContext tx) {
    return applicationDAO.getByOrganizationId(tx, orgId).stream().map(Application::getId).toList();
  }

  private void disableForApplication(final String applicationId) {
    log.info("Disable CPE matching configuration for application with ID: {}.", applicationId);
    cpeMatchingConfigurationDAO.delete(applicationId);
  }

  private CpeMatchingConfiguration updateConfigurationForOwner(CpeMatchingConfiguration cpeMatchingConfig) {
    CpeMatchingConfiguration existingConfig = cpeMatchingConfigurationDAO.getByOwnerId(cpeMatchingConfig.getOwnerId());
    if (existingConfig == null) {
      cpeMatchingConfigurationDAO.insert(cpeMatchingConfig);
      return cpeMatchingConfig;
    }
    else {
      existingConfig.setCpeEnabled(cpeMatchingConfig.isCpeEnabled());
      existingConfig.setAllowOverride(cpeMatchingConfig.isAllowOverride());
      cpeMatchingConfigurationDAO.update(existingConfig);
      return existingConfig;
    }
  }
}
