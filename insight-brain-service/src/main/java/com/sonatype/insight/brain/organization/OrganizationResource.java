/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.dto.WaivedComponentUpgradeNotificationDTO;
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

  public static final String WAIVED_COMPONENT_UPGRADE_NOTIFICATION =
      "/waivedComponentUpgradeNotification";

  private final OrganizationService organizationService;

  private final InsightWork work;

  @Inject
  public OrganizationResource(final InsightWork work,
                              final RobotImageService robotImageService,
                              final BaseUrl baseUrl,
                              final OrganizationService organizationService,
                              final NgUploadResponseGenerator ngUploadResponseGenerator)
  {
    super(baseUrl, ngUploadResponseGenerator, robotImageService);
    this.work = work;
    this.organizationService = organizationService;
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
    return Organization.ROOT_ORGANIZATION_ID.equals(ownerId) ?
        "defaulticon_root_org.png" : "defaulticon_organization.png";
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
  public Response setIcon(@FormDataParam(AntiCsrfFilter.CSRF_HEADER_NAME) String csrfToken,
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
   * @since 1.159
   */
  @PUT
  @Path(WAIVED_COMPONENT_UPGRADE_NOTIFICATION)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_ORGANIZATION)
  public Organization updateWaivedComponentUpgradeNotification(
      WaivedComponentUpgradeNotificationDTO waivedComponentUpgradeNotificationDTO)
  {
    return organizationService.updateWaivedComponentUpgradeNotification(
        waivedComponentUpgradeNotificationDTO);
  }

  /**
   * @since 1.159
   */
  @GET
  @Path(WAIVED_COMPONENT_UPGRADE_NOTIFICATION)
  @Produces(MediaType.APPLICATION_JSON)
  public WaivedComponentUpgradeNotificationDTO getWaivedComponentUpgradeNotification() {
    return organizationService.getWaivedComponentUpgradeNotification();
  }
}
