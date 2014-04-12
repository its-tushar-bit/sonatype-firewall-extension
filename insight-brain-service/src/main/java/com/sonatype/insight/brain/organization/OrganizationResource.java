/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.io.InputStream;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzErrorMsg;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Path(OrganizationResource.SERVICE_PATH)
public class OrganizationResource
    extends AbstractResourceWithIcon
{
  public static final String SERVICE_PATH = "rest/organization";

  public static final String GET_ICON_PATH = ICON_PATH + "/{organizationId}";

  public static final String DELETE_ORGANIZATION_PATH = "{organizationId}";

  private static final Logger log = LoggerFactory.getLogger(OrganizationResource.class);

  private final OrganizationService organizationService;

  private final InsightWork work;

  @Inject
  public OrganizationResource(final InsightWork work, final SaasClient client, final BaseUrl baseUrl,
                              final OrganizationService organizationService)
  {
    super(client, baseUrl);
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

  /**
   * @since 1.6
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Organization addOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION_OWNER) Organization organization) {
    return organizationService.addOrganization(organization);
  }

  /**
   * @since 1.6
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Organization updateOrganization(@AuthzContext(AuthzContext.Key.ORGANIZATION) Organization organization) {
    return organizationService.updateOrganization(organization);
  }

  @Override
  protected String getDefaultIconFilename() {
    return "defaulticon_organization.png";
  }

  /**
   * @since 1.6
   */
  @Override
  @GET
  @Path(GENERATE_ICON_PATH)
  @Produces("image/png")
  public Response generateIcon(@PathParam("hashcode") final String hashcode, @Context final HttpServletRequest req)
      throws IOException
  {
    return super.generateIcon(hashcode, req);
  }

  /**
   * @since 1.6
   */
  @GET
  @Path(GET_ICON_PATH)
  @Produces("image/png")
  @Authorize(permission = Permission.READ)
  public Response getIcon(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) @PathParam("organizationId") String organizationId)
      throws IOException
  {
    return super.getIcon(organizationId, work.getOrganizationIconDir());
  }

  /**
   * This is one of two service methods used for editing and adding icons. This method is used for AJAX calls since
   * its return type is a JSON object.
   * 
   * @since 1.6
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path(ICON_PATH)
  @Authorize(permission = Permission.WRITE)
  public void setIcon(
      @FormDataParam("organizationId") @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId,
      @FormDataParam("hasRobotSource") boolean hasRobotSource, @FormDataParam("robotHash") String robotHash,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException
  {
    super.setIcon(organizationId, work.getOrganizationIconDir(), hasRobotSource, robotHash, uploadedInputStream,
        fileDetail);
  }

  /**
   * This is one of two service methods used for editing and adding icons. This method is used by angular ng-upload
   * and returns an empty string for success and the error message otherwise
   * 
   * @since 1.6
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path(ICON_PATH_SYNC)
  @Authorize(permission = Permission.WRITE)
  @AuthzErrorMsg
  public String setIconSync(
      @FormDataParam("organizationId") @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId,
      @FormDataParam("hasRobotSource") boolean hasRobotSource, @FormDataParam("robotHash") String robotHash,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail)
  {
    return super.setIconSync(organizationId, work.getOrganizationIconDir(), hasRobotSource, robotHash,
        uploadedInputStream, fileDetail);
  }

  /**
   * Deletes an organization and associated policies, license threat groups, labels and waivers. Also deletes all
   * applications under the organization.
   * 
   * @since 1.6
   */
  @DELETE
  @Path(DELETE_ORGANIZATION_PATH)
  public void deleteOrganization(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) @PathParam("organizationId") final String organizationId)
      throws IOException
  {
    organizationService.deleteOrganization(organizationId);
  }
}
