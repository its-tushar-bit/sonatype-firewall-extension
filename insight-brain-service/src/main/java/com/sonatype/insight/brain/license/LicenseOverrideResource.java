/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dto.audit.BomAudit;
import com.sonatype.insight.brain.dto.audit.LicenseOverrideAudit;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuditUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

/**
 * @since 1.6
 */
@Named
@Path(LicenseOverrideResource.SERVICE_PATH)
public class LicenseOverrideResource
{
  public static final String SERVICE_BASEPATH = "rest/licenseOverride/";

  public static final String SERVICE_PATH = SERVICE_BASEPATH + "{ownerType: application|organization}/{ownerId}";

  private final InsightWork work;

  @Inject
  public LicenseOverrideResource(InsightWork work) {
    this.work = work;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public LicenseOverride addLicenseOverride(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, LicenseOverride licenseOverride,
      @QueryParam("where") String where, @Context final HttpServletRequest request)
      throws IOException
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    licenseOverride.setOwnerId(internalOwnerId);

    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    LicenseOverride existingLicenseOverride = licenseOverrideDAO.getByOwnerIdAndGAV(internalOwnerId,
        licenseOverride.getGroupId(), licenseOverride.getArtifactId(), licenseOverride.getVersion());
    if (existingLicenseOverride != null) {
      licenseOverride.setId(existingLicenseOverride.getId());
      licenseOverrideDAO.update(licenseOverride);
    }
    else {
      licenseOverride.setId(null);
      licenseOverrideDAO.insert(licenseOverride);
    }

    String user = AuditUtils.findUser();
    String ipAddress = AuditUtils.findIP(request);
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

    BomAudit bomAudit = new BomAudit(licenseOverride.getGroupId(), licenseOverride.getArtifactId(),
        licenseOverride.getVersion(), !isDelete /* modified */);
    store.commit("bom.json", JsonUtils.stamp(user, ipAddress, where, JsonUtils.asTree(bomAudit)));
  }

  @DELETE
  @Path("{licenseOverrideId}")
  @Authorize(permission = Permission.WRITE)
  public void deleteLicenseOverride(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @PathParam("licenseOverrideId") String licenseOverrideId, @QueryParam("where") String where,
      @Context final HttpServletRequest request) throws IOException
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    LicenseOverride licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverrideId);
    if (!internalOwnerId.equals(licenseOverride.getOwnerId())) {
      throw new NotFoundException("Cannot find a license override with id " + licenseOverrideId + " for " + ownerType
          + " id " + ownerId);
    }

    String user = AuditUtils.findUser();
    String ipAddress = AuditUtils.findIP(request);
    auditLicenseOverride(internalOwnerId, licenseOverride, user, where, ipAddress, true /* isDelete */);

    licenseOverrideDAO.delete(licenseOverride);
  }

  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  @Path("applied/{groupId}/{artifactId}/{version}")
  @Authorize(permission = Permission.READ)
  public AppliedLicenseOverrides getAppliedLicenseOverrides(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, @PathParam("groupId") String groupId,
      @PathParam("artifactId") String artifactId, @PathParam("version") String version)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    AppliedLicenseOverrides result = new AppliedLicenseOverrides();

    result.licenseOverridesByOwner = new ArrayList<LicenseOverrideByOwner>();
    String organizationId;
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      Application application = new ApplicationDAO().getByIdNotNull(internalOwnerId);
      LicenseOverrideByOwner licenseOverrideByOwner = new LicenseOverrideByOwner();
      licenseOverrideByOwner.ownerId = application.getPublicId();
      licenseOverrideByOwner.ownerName = application.getName();
      licenseOverrideByOwner.ownerType = IdUtils.TYPE_APPLICATION;
      licenseOverrideByOwner.licenseOverride = licenseOverrideDAO.getByOwnerIdAndGAV(application.getId(), groupId,
          artifactId, version);
      result.licenseOverridesByOwner.add(licenseOverrideByOwner);
      organizationId = application.getOrganizationId();
    }
    else {
      organizationId = internalOwnerId;
    }

    Organization organization = new OrganizationDAO().getByIdNotNull(organizationId);
    LicenseOverrideByOwner licenseOverrideByOwner = new LicenseOverrideByOwner();
    licenseOverrideByOwner.ownerId = organization.getId();
    licenseOverrideByOwner.ownerName = organization.getName();
    licenseOverrideByOwner.ownerType = IdUtils.TYPE_ORGANIZATION;
    licenseOverrideByOwner.licenseOverride = licenseOverrideDAO.getByOwnerIdAndGAV(organization.getId(), groupId,
        artifactId, version);
    result.licenseOverridesByOwner.add(licenseOverrideByOwner);

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

    public String ownerType;

    public LicenseOverride licenseOverride;
  }
}
