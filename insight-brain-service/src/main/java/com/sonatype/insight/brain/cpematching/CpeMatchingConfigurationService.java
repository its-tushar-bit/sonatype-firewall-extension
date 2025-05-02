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
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.license.model.ProductLicenseDetails.PRODUCT_SBOM_MANAGER;
import static java.lang.String.format;

@Singleton
@Named
public class CpeMatchingConfigurationService
{
  private final OwnerDAO ownerDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO;

  private final ProductLicense productLicense;

  private static final Logger log = LoggerFactory.getLogger(CpeMatchingConfigurationService.class);

  @Inject
  public CpeMatchingConfigurationService(
      final OwnerDAO ownerDAO,
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO,
      final ProductLicense productLicense)
  {
    this.ownerDAO = ownerDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.cpeMatchingConfigurationDAO = cpeMatchingConfigurationDAO;
    this.productLicense = productLicense;
  }

  @Authorize(permission = Permission.READ)
  public CpeMatchingConfigurationDTO getCpeMatchingConfiguration(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId)
  {
    return getCpeMatchingConfigurationNoAuthz(ownerType, ownerId);
  }

  public CpeMatchingConfigurationDTO getCpeMatchingConfigurationNoAuthz(OwnerType ownerType, String ownerId) {
    Owner owner = ownerDAO.getByIdNotNull(ownerId);
    CpeMatchingConfiguration ownerConfig = cpeMatchingConfigurationDAO.getByOwnerId(ownerId);
    if (ownerConfig != null) {
      return CpeMatchingConfigurationDTO.forSelf(ownerConfig.isCpeEnabled(),
          !OwnerType.APPLICATION.equals(ownerType) && ownerConfig.isAllowOverride());
    }

    for (Owner parent : ownerDAO.walkHierarchy(owner)) {
      CpeMatchingConfiguration parentConfig = cpeMatchingConfigurationDAO.getByOwnerId(parent.getId());
      if (parentConfig != null) {
        return CpeMatchingConfigurationDTO.fromParent(parentConfig.isCpeEnabled(), parent.getName(),
            parentConfig.isAllowOverride(), OwnerType.APPLICATION.equals(ownerType));
      }
    }
    // In the case no configuration was found for the owner or via any of its ancestors,
    // we return an empty configuration
    return new CpeMatchingConfigurationDTO();
  }

  @Authorize(permission = Permission.WRITE)
  public CpeMatchingConfigurationDTO updateCpeMatchingConfiguration(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      CpeMatchingConfigurationRequest configRequest)
  {
    CpeMatchingConfiguration cpeConfigUpdated;
    validateConfigurationDTO(configRequest);
    CpeMatchingConfigurationDTO existingConfig = getCpeMatchingConfigurationNoAuthz(ownerType, internalOwnerId);

    if (existingConfig.inheritedFromOrganizationAllowOverride != null &&
        !existingConfig.inheritedFromOrganizationAllowOverride) {
      throw new UnauthorizedException(format("Updating cpe matching configuration for ownerId %s is " +
          "disabled by parent organization %s", internalOwnerId, existingConfig.inheritedFromOrganizationName));
    }

    // Reaching this point means that no inherited cpe configuration coming down from ancestors was found. The
    // requested configRequest object can be written into the db for the requested internalOwnerId
    switch (ownerType) {
      case APPLICATION:
        cpeConfigUpdated = updateConfigurationForOwner(new CpeMatchingConfiguration(internalOwnerId,
            configRequest.enabled, false));
        break;
      case ORGANIZATION:
        if (!configRequest.allowOverride) {
          // Request object is asking to disable overriding at current level, therefore, any children
          // overriding-records need to be disabled/purged if found
          disableCpeMatchingConfiguration(ownerType, internalOwnerId, false);
        }
        cpeConfigUpdated = updateConfigurationForOwner(new CpeMatchingConfiguration(internalOwnerId,
            configRequest.enabled, configRequest.allowOverride));
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

  /*
   * Returns true if license has CPE matching feature AND either:
   * 1. this is an SBOM Manager only product
   * OR
   * 2. this is a multi product license and the CPE matching configuration
   *    is enabled for the application or an organization in the hierarchy above
   * */
  public boolean isCpeDataMatchingEnabled(String applicationId) {
    return productLicense.hasFeature(LicensedFeature.CPE_MATCHING) &&
        ((!productLicense.getProducts().isEmpty() &&
            productLicense.getProducts().stream().allMatch(p -> p.startsWith(PRODUCT_SBOM_MANAGER))) ||
            BooleanUtils.isTrue(getCpeMatchingConfigurationNoAuthz(OwnerType.APPLICATION, applicationId).enabled));
  }
}
