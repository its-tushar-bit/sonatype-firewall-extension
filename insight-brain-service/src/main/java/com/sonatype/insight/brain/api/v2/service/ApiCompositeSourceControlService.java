/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeValueDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.license.model.LicensedFeature;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.service.ApiSourceControlService.ENC;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;

@Named
@Singleton
public class ApiCompositeSourceControlService
{
  private static final Logger log = LoggerFactory.getLogger(ApiCompositeSourceControlService.class);

  private final SourceControlDAO sourceControlDAO;

  private final ApplicationDAO applicationDAO;

  private final ProductLicense productLicense;

  private final OrganizationDAO organizationDAO;

  private final PlexusCipher plexusCipher;

  @Inject
  public ApiCompositeSourceControlService(
      final SourceControlDAO sourceControlDAO,
      final ApplicationDAO applicationDAO,
      final ProductLicense productLicense,
      final OrganizationDAO organizationDAO,
      final PlexusCipher plexusCipher)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.applicationDAO = applicationDAO;
    this.productLicense = productLicense;
    this.organizationDAO = organizationDAO;
    this.plexusCipher = plexusCipher;
  }

  @Authorize(permission = Permission.READ)
  public ApiCompositeSourceControlDTO getCompositeSourceControlByOwner(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    return getCompositeSourceControlByOwner(ownerType, ownerId, true);
  }

  /**
   * IQ-internal ONLY helper function to return the composite SC DTO with the
   * token fully decrypted
   */
  public ApiCompositeSourceControlDTO getCompositeSourceControlByOwnerDecrypted(
      final OwnerType ownerType,
      final String ownerId)
  {
    ApiCompositeSourceControlDTO dto = getCompositeSourceControlByOwner(ownerType, ownerId, false);
    return dto;
  }

  /**
   * Retrieves the composite source control, given an owner (org or app)
   * @param ownerType type of owner (org or app)
   * @param ownerId internal ID of the owner
   * @param obscureToken true if the token should be obscured, ie: if it is returned to the frontend
   *                     or anywhere outside of IQ
   * @return the populated DTO
   */
  @Authorize(permission = Permission.READ)
  private ApiCompositeSourceControlDTO getCompositeSourceControlByOwner(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final boolean obscureToken)
  {
    checkLicense();

    String parentId;
    String grandParentId;
    if (ownerType.equals(OwnerType.APPLICATION)) {
      Application application = applicationDAO.getByIdNotNull(ownerId);
      Organization organization = organizationDAO.getByIdNotNull(application.getOrganizationId());
      parentId = organization.getId();
      grandParentId = organization.getParentOrganizationId();
    }
    else {
      Organization organization = organizationDAO.getByIdNotNull(ownerId);
      parentId = organization.getParentOrganizationId();
      grandParentId = null;
    }
    return getCompositeSourceControlFromHierarchyIds(ownerId, parentId, grandParentId, obscureToken);
  }

  private ApiCompositeSourceControlDTO getCompositeSourceControlFromHierarchyIds(
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final String parentId,
      final String grandParentId,
      final boolean obscureToken)
  {
    ApiCompositeSourceControlDTO dto = new ApiCompositeSourceControlDTO();
    dto.ownerId = ownerId;

    Optional<SourceControl> sourceControl = Optional.ofNullable(sourceControlDAO.getByOwnerId(ownerId));
    sourceControl.ifPresent(sc -> {
      dto.id = sc.getId();
      setTokenValueForReturn(sc, obscureToken);
    });

    Optional<SourceControl> parentSourceControl = Optional.empty();
    String parentName = null;
    if (parentId != null) {
      parentSourceControl = Optional.ofNullable(sourceControlDAO.getByOwnerId(parentId));
      parentSourceControl.ifPresent(sc -> setTokenValueForReturn(sc, obscureToken));
      parentName = organizationDAO.getByIdNotNull(parentId).getName();
    }

    Optional<SourceControl> grandParentSourceControl = Optional.empty();
    String grandParentName = null;
    if (grandParentId != null) {
      grandParentSourceControl = Optional.ofNullable(sourceControlDAO.getByOwnerId(grandParentId));
      grandParentSourceControl.ifPresent(sc -> setTokenValueForReturn(sc, obscureToken));
      grandParentName = organizationDAO.getByIdNotNull(grandParentId).getName();
    }

    SourceControl defaultSourceControl = new SourceControl.Builder().build();
    collateCompositeSourceControl(dto, sourceControl.orElse(defaultSourceControl), parentName,
        parentSourceControl.orElse(defaultSourceControl), grandParentName,
        grandParentSourceControl.orElse(defaultSourceControl));
    return dto;
  }

  private void collateCompositeSourceControl(
      final ApiCompositeSourceControlDTO dto,
      final SourceControl sourceControl,
      final String parentName,
      final SourceControl parentSourceControl,
      final String grandParentName,
      final SourceControl grandParentSourceControl)
  {

    //If parent and grandParent are set, this means it is an application.
    if (parentName != null && grandParentName != null) {
      dto.repositoryUrl = sourceControl.getRepositoryUrl();
    }

    dto.provider = collateCompositeDTO(
        sourceControl.getProvider() == null ? null : sourceControl.getProvider().toString(),
        parentName,
        parentSourceControl.getProvider() == null ? null : parentSourceControl.getProvider().toString(),
        grandParentName,
        grandParentSourceControl.getProvider() == null ? null : grandParentSourceControl.getProvider().toString()
    );

    dto.username = collateCompositeDTO(
        sourceControl.getUsername(),
        parentName,
        parentSourceControl.getUsername(),
        grandParentName,
        grandParentSourceControl.getUsername()
    );

    dto.token = collateCompositeDTO(
        sourceControl.getToken(),
        parentName,
        parentSourceControl.getToken(),
        grandParentName,
        grandParentSourceControl.getToken()
    );

    dto.baseBranch = collateCompositeDTO(
        sourceControl.getBaseBranch(),
        parentName,
        parentSourceControl.getBaseBranch(),
        grandParentName,
        grandParentSourceControl.getBaseBranch()
    );

    dto.enablePullRequests = collateCompositeDTO(
        sourceControl.getEnablePullRequests(),
        parentName,
        parentSourceControl.getEnablePullRequests(),
        grandParentName,
        grandParentSourceControl.getEnablePullRequests()
    );

    dto.enableStatusChecks = collateCompositeDTO(
        sourceControl.getEnableStatusChecks(),
        parentName,
        parentSourceControl.getEnableStatusChecks(),
        grandParentName,
        grandParentSourceControl.getEnableStatusChecks()
    );
  }

  private <T> ApiCompositeValueDTO<T> collateCompositeDTO(
      T value,
      String parentName,
      T parentValue,
      String grandparentName,
      T grandparentValue)
  {
    ApiCompositeValueDTO<T> dto = new ApiCompositeValueDTO<>();
    dto.value = value;
    if (null != parentValue) {
      dto.parentName = parentName;
      dto.parentValue = parentValue;
    }
    else if (null != grandparentValue) {
      dto.parentName = grandparentName;
      dto.parentValue = grandparentValue;
    }
    return dto;
  }

  private void setTokenValueForReturn(final SourceControl sourceControl, boolean obscureToken) {
    sourceControl.setToken(Strings.isNullOrEmpty(sourceControl.getToken()) ?
        null :
        obscureToken ? FAKE_SECRET_KEY : decrypt(sourceControl.getToken()));
  }

  private String decrypt(String value) {
    if (value == null) {
      return null;
    }
    synchronized (plexusCipher) {
      try {
        return plexusCipher.decrypt(value, ENC);
      }
      catch (PlexusCipherException e) {
        log.error("Unable to decrypt SourceControl token", e);
        throw new IllegalStateException(e);
      }
    }
  }

  private void checkLicense() {
    if (!(productLicense.hasFeature(LicensedFeature.NOTIFICATIONS) ||
        productLicense.hasFeature(LicensedFeature.AUTOMATION))) {
      log.debug("License does not support SourceControl notification features");
      throw new InvalidLicenseException();
    }
  }
}
