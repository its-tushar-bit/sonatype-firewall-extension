/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO.ValidationError;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.Csv;
import com.sonatype.insight.brain.utils.NgUploadResponseGenerator;

import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Named
@Timed
@Path(OrganizationResource.RESOURCE_PATH)
public class OrganizationResource
    extends AbstractResourceWithIcon
{
  public static final String RESOURCE_PATH = "rest/organization";

  public static final String GET_ORGANIZATION_PATH = "{organizationId}";

  public static final String ORGANIZATION_ICON_PATH = ICON_PATH + "/{organizationId}";

  public static final String DELETE_ORGANIZATION_PATH = "{organizationId}";

  public static final String MOVE_ORGANIZATION_ERRORS_EXPORT_PATH =
      "{organizationId}/move/destination/{destinationId}/export";

  private final OrganizationService organizationService;

  private final MoveOrganizationService moveOrganizationService;

  private final InsightWork work;

  @Inject
  public OrganizationResource(
      final InsightWork work,
      final RobotImageService robotImageService,
      final BaseUrl baseUrl,
      final OrganizationService organizationService,
      final NgUploadResponseGenerator ngUploadResponseGenerator,
      final MoveOrganizationService moveOrganizationService)
  {
    super(baseUrl, ngUploadResponseGenerator, robotImageService);
    this.work = work;
    this.organizationService = organizationService;
    this.moveOrganizationService = moveOrganizationService;
  }

  /**
   * @since 1.6
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Organization> getAll() {
    return organizationService.getAll();
  }

  @GET
  @Path(GET_ORGANIZATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Organization getOrganization(@PathParam("organizationId") final String organizationId) {
    return organizationService.getOrganization(organizationId);
  }

  /**
   * @since 1.6
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_ORGANIZATION)
  public Organization addOrganization(Organization organization) {
    return organizationService.addOrganization(organization);
  }

  /**
   * @since 1.6
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_ORGANIZATION)
  public Organization updateOrganization(Organization organization) {
    return organizationService.updateOrganization(organization);
  }

  @Override
  protected String getDefaultIconFilename(String ownerId) {
    return Organization.ROOT_ORGANIZATION_ID.equals(ownerId)
        ? "defaulticon_root_org.png"
        : "defaulticon_organization.png";
  }

  /**
   * @since 1.6
   */
  @Override
  @GET
  @Path(GENERATE_ICON_PATH)
  @Produces("image/png")
  public Response generateIcon(@PathParam("hashcode") final String hashcode) {
    return super.generateIcon(hashcode);
  }

  /**
   * @since 1.6
   */
  @GET
  @Path(ORGANIZATION_ICON_PATH)
  @Produces("image/png")
  @Authorize(permission = Permission.READ)
  public Response getIcon(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) @PathParam("organizationId") String orgId) throws IOException
  {
    return super.getIcon(orgId, work.getOrganizationIconDir());
  }

  /**
   * This is one of two service methods used for editing and adding icons. This method is used for AJAX calls since
   * its return type is a JSON object.
   *
   * @since 1.6
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
  @Path(ORGANIZATION_ICON_PATH)
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.CONFIGURE_ORGANIZATION_ICON)
  public Response setIcon(
      @FormDataParam(AntiCsrfFilter.CSRF_HEADER_NAME) String csrfToken,
      @Context HttpHeaders headers,
      @AuthzContext(Key.ORGANIZATION_ID) @PathParam("organizationId") String organizationId,
      @FormDataParam("hasRobotSource") boolean hasRobotSource,
      @FormDataParam("hashcode") String hashcode,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @QueryParam("noFormData") boolean noFormData) throws Exception
  {
    return super.setIcon(organizationId, work.getOrganizationIconDir(), hasRobotSource, hashcode, uploadedInputStream,
        fileDetail, csrfToken, headers, noFormData);
  }

  /**
   * Deletes an organization and associated policies, license threat groups, labels and waivers. Also deletes all
   * applications under the organization.
   *
   * @since 1.6
   */
  @DELETE
  @Path(DELETE_ORGANIZATION_PATH)
  @Audited(AuditEvent.DELETE_ORGANIZATION)
  public void deleteOrganization(@PathParam("organizationId") final String organizationId) throws IOException {
    organizationService.deleteOrganization(organizationId);
  }

  /**
   * Move an organization under a new parent.
   *
   * @since 1.159
   */

  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces("text/csv")
  @Path(MOVE_ORGANIZATION_ERRORS_EXPORT_PATH)
  @Audited(AuditEvent.EXPORT_MOVE_ORGANIZATION_ERRORS_LIST)
  public Response moveOrganizationErrorsExport(
      @PathParam("organizationId") final String orgId,
      @PathParam("destinationId") final String newParentOrgId)
  {
    List<ValidationError> validationErrors =
        moveOrganizationService.getMoveOrganizationErrors(orgId, newParentOrgId);

    final String fileName = "move_organization_errors";
    return Csv.generate(Response.ok(), fileName, MoveOrganizationResponseDTO.ValidationError.getCsvHeader(),
        validationErrors)
        .build();
  }
}
