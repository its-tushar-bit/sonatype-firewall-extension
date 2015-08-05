/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.component.ComponentIdentifierValidator;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dto.audit.BomAudit;
import com.sonatype.insight.brain.dto.audit.LicenseOverrideAudit;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.jaxrs.JsonEncodedComponentIdentifier;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

/**
 * @since 1.17.0
 */
@Named
public class LicenseOverrideService
{
  private final InsightWork work;

  private final CurrentUser currentUser;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final OwnerDAO ownerDAO;

  @Inject
  public LicenseOverrideService(final InsightWork work, final CurrentUser currentUser,
      final LicenseOverrideDAO licenseOverrideDAO, final OwnerDAO ownerDAO)
  {
    this.work = work;
    this.currentUser = currentUser;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.ownerDAO = ownerDAO;
  }

  @Authorize(permission = Permission.WRITE)
  public LicenseOverride addLicenseOverride(@AuthzContext(AuthzContext.Key.TYPE) final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId, final LicenseOverride licenseOverride,
      final String where, final HttpServletRequest request)
      throws IOException
  {
    ComponentIdentifierValidator.validate(licenseOverride.getComponentIdentifier());

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    licenseOverride.setOwnerId(internalOwnerId);

    LicenseOverride existingLicenseOverride = licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(internalOwnerId,
        licenseOverride.getComponentIdentifier());
    if (existingLicenseOverride != null) {
      licenseOverride.setId(existingLicenseOverride.getId());
      licenseOverrideDAO.update(licenseOverride);
    }
    else {
      licenseOverride.setId(null);
      licenseOverrideDAO.insert(licenseOverride);
    }

    String user = currentUser.getUsername();
    String ipAddress = currentUser.getIP(request);
    auditLicenseOverride(internalOwnerId, licenseOverride, user, where, ipAddress, false /* isDelete */);

    return licenseOverride;
  }

  private void auditLicenseOverride(String ownerId, LicenseOverride licenseOverride, String user, String where,
      String ipAddress, boolean isDelete) throws IOException
  {
    JsonStore store = JsonUtils.fileStore(work.getAuditDir(ownerId));

    LicenseOverrideAudit licenseOverrideAudit = new LicenseOverrideAudit(licenseOverride);
    if (isDelete) {
      licenseOverrideAudit.setStatus("Deleted");
      licenseOverrideAudit.setComment(null);
    }
    store.commit("licenses.json", JsonUtils.stamp(user, ipAddress, where, JsonUtils.asTree(licenseOverrideAudit)));
    BomAudit bomAudit = new BomAudit(licenseOverride.getComponentIdentifier(), !isDelete /* modified */);
    store.commit("bom.json", JsonUtils.stamp(user, ipAddress, where, JsonUtils.asTree(bomAudit)));
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteLicenseOverride(@AuthzContext(AuthzContext.Key.TYPE) final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId, final String licenseOverrideId, final String where,
      final HttpServletRequest request) throws IOException
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    LicenseOverride licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverrideId);
    if (!internalOwnerId.equals(licenseOverride.getOwnerId())) {
      throw new NotFoundException("Cannot find a license override with ID " + licenseOverrideId + " for " + ownerType
          + " ID " + ownerId);
    }

    String user = currentUser.getUsername();
    String ipAddress = currentUser.getIP(request);
    auditLicenseOverride(internalOwnerId, licenseOverride, user, where, ipAddress, true /* isDelete */);

    licenseOverrideDAO.delete(licenseOverride);
  }

  @Authorize(permission = Permission.READ)
  public AppliedLicenseOverrides getAppliedLicenseOverrides(@AuthzContext(AuthzContext.Key.TYPE) final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId, final JsonEncodedComponentIdentifier componentIdentifier)
  {
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    AppliedLicenseOverrides result = new AppliedLicenseOverrides();
    result.licenseOverridesByOwner = new ArrayList<>();

    for (Owner owner : ownerDAO.walkHierarchy(internalOwnerId)) {
      LicenseOverrideByOwner licenseOverrideByOwner = new LicenseOverrideByOwner();
      licenseOverrideByOwner.ownerId = owner.getPublicId();
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
  }

  public static class LicenseOverrideByOwner
  {
    public String ownerId;

    public String ownerName;

    public OwnerType ownerType;

    public LicenseOverride licenseOverride;
  }
}
