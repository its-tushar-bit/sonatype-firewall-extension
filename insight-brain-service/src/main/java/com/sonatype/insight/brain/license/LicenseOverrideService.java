/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiAppliedLicenseOverridesDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentIdentifierValidator;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dto.audit.LicenseOverrideAudit;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.utils.JsonFileStore;
import com.sonatype.insight.brain.utils.JsonStore;
import com.sonatype.insight.brain.webhook.LicenseOverrideEventService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;

/**
 * @since 1.17.0
 */
@Named
public class LicenseOverrideService
{
  private static final Logger log = LoggerFactory.getLogger(LicenseOverrideService.class);

  private final InsightWork work;

  private final CurrentUser currentUser;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final OwnerDAO ownerDAO;

  private final LicenseDAO licenseDAO;

  private final LicenseOverrideEventService licenseOverrideEventService;

  private final ClusterLockManager clusterLockManager;

  private final IdUtils idUtils;

  @Inject
  public LicenseOverrideService(final InsightWork work,
                                final OwnerDAO ownerDAO,
                                final CurrentUser currentUser,
                                final LicenseOverrideDAO licenseOverrideDAO,
                                final LicenseDAO licenseDAO,
                                final LicenseOverrideEventService licenseOverrideEventService,
                                final ClusterLockManager clusterLockManager,
                                final IdUtils idUtils)
  {
    this.work = work;
    this.currentUser = currentUser;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.ownerDAO = ownerDAO;
    this.licenseDAO = licenseDAO;
    this.licenseOverrideEventService = licenseOverrideEventService;
    this.clusterLockManager = clusterLockManager;
    this.idUtils = idUtils;
  }

  @Authorize(permission = Permission.CHANGE_LICENSES)
  public LicenseOverride addLicenseOverride(@AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
                                            @AuthzContext(AuthzContext.Key.ID) final String ownerId,
                                            final LicenseOverride licenseOverride,
                                            final String where,
                                            final HttpServletRequest request) throws IOException
  {
    if (licenseOverride == null) {
      throw new BadRequestException("The license override cannot be null. Validate the body of " +
          "the request.");
    }

    ComponentIdentifierValidator.validate(licenseOverride.getComponentIdentifier());

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    licenseOverride.setOwnerId(internalOwnerId);

    LicenseOverride existingLicenseOverride = licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(internalOwnerId,
        licenseOverride.getComponentIdentifier());
    if (existingLicenseOverride != null) {
      licenseOverride.setId(existingLicenseOverride.getId());
      licenseOverrideDAO.update(licenseOverride);
      licenseOverrideEventService.postEvent(UPDATED, licenseOverride);
      log.debug("Updated license override with id {} for {} {}", existingLicenseOverride.getId(),
          ownerType, ownerId);
    }
    else {
      licenseOverride.setId(null);
      licenseOverrideDAO.insert(licenseOverride);
      licenseOverrideEventService.postEvent(CREATED, licenseOverride);
      log.debug("Created license override for {} {}", ownerType, ownerId);
    }
    auditLicenseOverride(licenseOverride, false);

    String user = currentUser.getUsername();
    String ipAddress = currentUser.getIP(request);
    auditLicenseOverride(internalOwnerId, licenseOverride, user, where, ipAddress, false /* isDelete */);

    return licenseOverride;
  }

  private void auditLicenseOverride(LicenseOverride licenseOverride, boolean isDelete) {
    if (isDelete) {
      AuditData.get().setData("status", "inherited");
    }
    else {
      AuditData.get().setEnum("status", licenseOverride.getStatus()).setComment(licenseOverride.getComment());

      List<String> selectedOverriddenLicenseNames = licenseOverride.getLicenseIds().stream().map(licenseDAO::getById)
          .map(License::getShortDisplayName).collect(Collectors.toList());
      if (!selectedOverriddenLicenseNames.isEmpty()) {
        AuditData.get().setData("licenseNames", selectedOverriddenLicenseNames);
      }
    }

    AuditData.get().setComponentIdentifier(licenseOverride.getComponentIdentifier());
  }

  private void auditLicenseOverride(String ownerId,
                                    LicenseOverride licenseOverride,
                                    String user,
                                    String where,
                                    String ipAddress,
                                    boolean isDelete) throws IOException
  {
    JsonStore store = new JsonFileStore(work.getAuditDir(ownerId), ownerId, clusterLockManager);

    LicenseOverrideAudit licenseOverrideAudit = new LicenseOverrideAudit(licenseOverride, licenseDAO);
    if (isDelete) {
      licenseOverrideAudit.setStatus("Deleted");
      licenseOverrideAudit.setComment(null);
    }
    store.commit("licenses.json", JsonUtils.stamp(user, ipAddress, where, JsonUtils.asTree(licenseOverrideAudit)));
  }

  @Authorize(permission = Permission.CHANGE_LICENSES)
  public void deleteLicenseOverride(@AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
                                    @AuthzContext(AuthzContext.Key.ID) final String ownerId,
                                    final String licenseOverrideId,
                                    final String where,
                                    final HttpServletRequest request) throws IOException
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    LicenseOverride licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverrideId);
    if (!internalOwnerId.equals(licenseOverride.getOwnerId())) {
      throw new NotFoundException("Cannot find a license override with ID " + licenseOverrideId + " for " + ownerType
          + " ID " + ownerId);
    }

    String user = currentUser.getUsername();
    String ipAddress = currentUser.getIP(request);
    auditLicenseOverride(internalOwnerId, licenseOverride, user, where, ipAddress, true /* isDelete */);

    licenseOverrideDAO.delete(licenseOverride);

    auditLicenseOverride(licenseOverride, true);

    licenseOverrideEventService.postEvent(DELETED, licenseOverride);
    log.debug("Deleted license override {} for {} {}", licenseOverrideId, ownerType, ownerId);
  }

  @Authorize(permission = Permission.READ)
  public AppliedLicenseOverrides getAppliedLicenseOverridesForRead(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final ComponentIdentifier componentIdentifier)
  {
    return getAppliedLicenseOverridesNoAuth(ownerType, ownerId, componentIdentifier);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public AppliedLicenseOverrides getAppliedLicenseOverridesForLegalReviewer(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      final ComponentIdentifier componentIdentifier)
  {
    return getAppliedLicenseOverridesNoAuth(ownerType, ownerId, componentIdentifier);
  }

  @VisibleForTesting
  AppliedLicenseOverrides getAppliedLicenseOverridesNoAuth(
      OwnerType ownerType,
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    AppliedLicenseOverrides result = new AppliedLicenseOverrides();
    result.licenseOverridesByOwner = new ArrayList<>();

    for (Owner owner : ownerDAO.walkHierarchy(internalOwnerId)) {
      LicenseOverrideByOwner licenseOverrideByOwner = new LicenseOverrideByOwner();
      licenseOverrideByOwner.ownerId = OwnerType.REPOSITORY.equals(ownerType) ? owner.getId() : owner.getPublicId();
      licenseOverrideByOwner.ownerName = owner.getName();
      licenseOverrideByOwner.ownerType = owner.getType();
      licenseOverrideByOwner.licenseOverride = licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(owner.getId(),
          componentIdentifier);
      result.licenseOverridesByOwner.add(licenseOverrideByOwner);
    }

    return result;
  }

  public static class AppliedLicenseOverrides
  {
    public List<LicenseOverrideByOwner> licenseOverridesByOwner;

    public ApiAppliedLicenseOverridesDTO toDto() {
      ApiAppliedLicenseOverridesDTO dto = new ApiAppliedLicenseOverridesDTO();
      dto.licenseOverridesByOwner = licenseOverridesByOwner;
      return dto;
    }
  }

  public static class LicenseOverrideByOwner
  {
    public String ownerId;

    public String ownerName;

    public OwnerType ownerType;

    public LicenseOverride licenseOverride;
  }
}
