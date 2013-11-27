/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzErrorMsg;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.client.utils.AuditUtils;
import com.sonatype.insight.error.ErrorResponseGenerator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.utils.IdUtils.TYPE_ORGANIZATION;

@Named
@Path(PolicyResource.SERVICE_PATH)
public class PolicyResource
{
  public static final String SERVICE_PATH = "rest/policy/{ownerType: application|organization}/{ownerId}";

  private static final Logger log = LoggerFactory.getLogger(PolicyResource.class);

  private static final String BAD_FORMAT_FILE_UPLOAD = "The file you selected failed to upload correctly, are you certain" +
      " it is a properly formatted policy import json file?";

  private final InsightWork work;

  private final PolicyImporter policyImporter;

  private ErrorResponseGenerator errorResponseGenerator = new ErrorResponseGenerator(false);

  @Inject
  public PolicyResource(InsightWork work, PolicyImporter policyImporter) {
    this.work = work;
    this.policyImporter = policyImporter;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public List<Policy> getPolicies(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId)
  {
    log.debug("Received request to get all policies for {} id {}", ownerType, ownerId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    return policyDAO().getByOwnerId(internalOwnerId);
  }

  /**
   * @since 1.6
   */
  @GET
  @Path("applicable")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public ApplicablePolicies getApplicablePolicies(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId)
  {
    log.debug("Received request to get all applicable policies for {} id {}", ownerType, ownerId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    ApplicablePolicies result = new ApplicablePolicies();

    result.policiesByOwner = new ArrayList<>();
    String organizationId;
    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      Application application = new ApplicationDAO().getByIdNotNull(internalOwnerId);
      PoliciesByOwner policiesByOwner = new PoliciesByOwner();
      policiesByOwner.ownerId = application.getId();
      policiesByOwner.ownerName = application.getName();
      policiesByOwner.ownerType = IdUtils.TYPE_APPLICATION;
      policiesByOwner.policies = policyDAO().getByOwnerId(application.getId());
      result.policiesByOwner.add(policiesByOwner);
      organizationId = application.getOrganizationId();
    }
    else {
      organizationId = internalOwnerId;
    }
    if (organizationId != null) {
      Organization organization = new OrganizationDAO().getByIdNotNull(organizationId);
      PoliciesByOwner policiesByOwner = new PoliciesByOwner();
      policiesByOwner.ownerId = organization.getId();
      policiesByOwner.ownerName = organization.getName();
      policiesByOwner.ownerType = IdUtils.TYPE_ORGANIZATION;
      policiesByOwner.policies = policyDAO().getByOwnerId(organization.getId());
      result.policiesByOwner.add(policiesByOwner);
    }

    return result;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public Policy addPolicy(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId,
      final Policy policy,
      @QueryParam("user") final String user,
      @QueryParam("where") final String where,
      @Context final HttpServletRequest request)
  {
    log.debug("Received request to add {} policy for ownerId {}", ownerType, ownerId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    return policyDAO().session(user, AuditUtils.findIP(request), where).insert(internalOwnerId, policy);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public Policy updatePolicy(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId,
      final Policy policy,
      @QueryParam("user") final String user,
      @QueryParam("where") final String where,
      @Context final HttpServletRequest request)
  {
    log.debug("Received request to update {} policy for ownerId {}, policyId {}", ownerType, ownerId, policy.getId());

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    return policyDAO().session(user, AuditUtils.findIP(request), where).update(internalOwnerId, policy);
  }

  @DELETE
  @Path("{policyId}")
  @Authorize(permission = Permission.WRITE)
  public void deletePolicy(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") final String ownerId,
      @PathParam("policyId") final String policyId,
      @QueryParam("user") final String user,
      @QueryParam("where") final String where,
      @Context final HttpServletRequest request)
  {
    log.debug("Received request to delete {} policy for ownerId {}, policyId {}", ownerType, ownerId, policyId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    policyDAO().session(user, AuditUtils.findIP(request), where).delete(internalOwnerId, policyId);
  }

  @GET
  @Path("export")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public PolicyExportResult exportPolicies(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    PolicyExportResult exportDTO = new PolicyExportResult();
    exportDTO.policies = policyDAO().getByOwnerId(internalOwnerId);
    exportDTO.labels = new LabelDAO().getByOwnerId(internalOwnerId);
    exportDTO.licenseThreatGroups = new LicenseThreatGroupDAO().getByOwnerId(internalOwnerId);
    exportDTO.licenseThreatGroupLicenses = new LicenseThreatGroupLicenseDAO().getByOwnerId(internalOwnerId);

    return exportDTO;
  }

  @PUT
  @Path("import")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public PolicyImportResult importPolicies(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @Context HttpServletRequest servletRequest) throws IOException
  {
    return importPolicies(ownerType, ownerId, servletRequest.getInputStream());
  }

  @POST
  @Path("import/ie")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Authorize(permission = Permission.WRITE)
  @AuthzErrorMsg
  public String importPolicies(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail)
  {

    String errorMessage = "";
    try {
      importPolicies(ownerType, ownerId, uploadedInputStream);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      errorMessage = errorResponseGenerator.mapException(e).getMessageBody();
    }

    return errorMessage;
  }

  private PolicyImportResult importPolicies(String ownerType, String ownerId, InputStream in) throws IOException {
    PolicyExportResult exportDTO = readPolicyExportResult(in);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    if (TYPE_ORGANIZATION.equals(ownerType)) {
      return policyImporter.importOrganization(new OrganizationDAO().getByIdNotNull(internalOwnerId), exportDTO);
    }
    return policyImporter.importApplication(new ApplicationDAO().getByIdNotNull(internalOwnerId), exportDTO);

  }

  private PolicyExportResult readPolicyExportResult(InputStream stream) throws IOException {
    byte[] importBytes;
    try {
      importBytes = IOUtil.toByteArray(stream);
    }
    finally {
      IOUtil.close(stream);
    }
    PolicyExportResult parse;
    try {
      parse = JsonUtils.parse(importBytes, PolicyExportResult.class);
    }
    catch (IOException e) {
      log.error("Policy file import failure, unable to marshal to json", e);
      throw new BadRequestException(BAD_FORMAT_FILE_UPLOAD);
    }
    // Any random json file can be uploaded and result in an empty PolicyImportResult; ensure that we can parse
    // relevant data in expected format, for which minimally these should be empty collections.
    if(parse.policies == null || parse.labels == null || parse.licenseThreatGroupLicenses == null ||
        parse.licenseThreatGroups == null){
      throw new BadRequestException(BAD_FORMAT_FILE_UPLOAD);
    }
    return parse;
  }

  private PolicyDAO policyDAO() {
    return new PolicyDAO(work.getWorkDir());
  }

  public static class ApplicablePolicies
  {
    public List<PoliciesByOwner> policiesByOwner;
  }

  public static class PoliciesByOwner
  {
    public String ownerId;

    public String ownerName;

    public String ownerType;

    public List<Policy> policies;
  }
}
