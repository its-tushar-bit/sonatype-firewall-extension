/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.dto.audit.BomAudit;
import com.sonatype.insight.brain.dto.audit.LicenseOverrideAudit;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.client.utils.AuditUtils;
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
  public LicenseOverride addLicenseOverride(@PathParam("ownerType") String ownerType,
      @PathParam("ownerId") String ownerId, LicenseOverride licenseOverride, @QueryParam("user") String user,
      @QueryParam("where") String where, @Context final HttpServletRequest request) throws IOException
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    licenseOverride.setId(null);
    licenseOverride.setOwnerId(internalOwnerId);
    new LicenseOverrideDAO().insert(licenseOverride);

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
  public void deleteLicenseOverride(@PathParam("ownerType") String ownerType, @PathParam("ownerId") String ownerId,
      @PathParam("licenseOverrideId") String licenseOverrideId, @QueryParam("user") String user,
      @QueryParam("where") String where, @Context final HttpServletRequest request) throws IOException
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    LicenseOverride licenseOverride = licenseOverrideDAO.getByIdNotNull(licenseOverrideId);
    if (!internalOwnerId.equals(licenseOverride.getOwnerId())) {
      throw new NotFoundException("Cannot find a license override with id " + licenseOverrideId + " for " + ownerType
          + " id " + ownerId);
    }

    String ipAddress = AuditUtils.findIP(request);
    auditLicenseOverride(internalOwnerId, licenseOverride, user, where, ipAddress, true /* isDelete */);

    licenseOverrideDAO.delete(licenseOverride);
  }

  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  @Path("applicable/{groupId}/{artifactId}/{version}")
  public ApplicableLicenseOverrides getApplicableLicenseOverrides(@PathParam("ownerType") String ownerType,
      @PathParam("ownerId") String ownerId, @PathParam("groupId") String groupId,
      @PathParam("artifactId") String artifactId, @PathParam("version") String version)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    ApplicableLicenseOverrides result = new ApplicableLicenseOverrides();

    result.licenseOverridesByOwner = new ArrayList<LicenseOverridesByOwner>();
    String organizationId;
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      Application application = new ApplicationDAO().getByIdNotNull(internalOwnerId);
      LicenseOverridesByOwner licenseOverridesByOwner = new LicenseOverridesByOwner();
      licenseOverridesByOwner.ownerId = application.getId();
      licenseOverridesByOwner.ownerName = application.getName();
      licenseOverridesByOwner.ownerType = IdUtils.TYPE_APPLICATION;
      LicenseOverride licenseOverride = licenseOverrideDAO.getByOwnerIdAndGAV(application.getId(), groupId, artifactId,
          version);
      if (licenseOverride != null) {
        licenseOverridesByOwner.licenseOverrides.add(licenseOverride);
      }
      result.licenseOverridesByOwner.add(licenseOverridesByOwner);
      organizationId = application.getOrganizationId();
    }
    else {
      organizationId = internalOwnerId;
    }
    if (organizationId != null) {
      Organization organization = new OrganizationDAO().getByIdNotNull(organizationId);
      LicenseOverridesByOwner licenseOverridesByOwner = new LicenseOverridesByOwner();
      licenseOverridesByOwner.ownerId = organization.getId();
      licenseOverridesByOwner.ownerName = organization.getName();
      licenseOverridesByOwner.ownerType = IdUtils.TYPE_ORGANIZATION;
      LicenseOverride licenseOverride = licenseOverrideDAO.getByOwnerIdAndGAV(organization.getId(), groupId,
          artifactId, version);
      if (licenseOverride != null) {
        licenseOverridesByOwner.licenseOverrides.add(licenseOverride);
      }
      result.licenseOverridesByOwner.add(licenseOverridesByOwner);
    }

    return result;
  }

  @GET
  @Path("applicable/context")
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicableContext getApplicableContexts(@PathParam("ownerType") String ownerType,
      @PathParam("ownerId") String ownerId)
  {
    ApplicationDAO applicationDAO = new ApplicationDAO();
    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      Application application = applicationDAO.getByPublicIdNotNull(ownerId);
      return new ApplicableContext(application.getId(), application.getName(), IdUtils.TYPE_APPLICATION);
    }

    Organization organization = new OrganizationDAO().getByIdNotNull(ownerId);
    ApplicableContext result = new ApplicableContext(organization.getId(), organization.getName(),
        IdUtils.TYPE_ORGANIZATION);
    result.children = new ArrayList<ApplicableContext>();
    for (Application application : applicationDAO.getByOrganizationId(organization.getId())) {
      result.children.add(new ApplicableContext(application.getId(), application.getName(), IdUtils.TYPE_APPLICATION));
    }
    return result;
  }

  public static class ApplicableLicenseOverrides
  {
    public List<LicenseOverridesByOwner> licenseOverridesByOwner;
  }

  public static class LicenseOverridesByOwner
  {
    public String ownerId;

    public String ownerName;

    public String ownerType;

    public List<LicenseOverride> licenseOverrides = new ArrayList<LicenseOverride>();
  }
}
