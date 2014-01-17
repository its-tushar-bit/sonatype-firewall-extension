/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
@Path(ComponentLabelResource.SERVICE_PATH)
public class ComponentLabelResource
{
  public static final String SERVICE_BASEPATH = "rest/label/component/";

  public static final String SERVICE_PATH = SERVICE_BASEPATH + "{ownerType: application|organization}/{ownerId}/{hash}";

  private LabelDAO labelDAO = new LabelDAO();

  private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  /**
   * Enables visualization of applied component labels. Most notably, the returned DTO holds the names of relevant
   * entities and public IDs as opposed to internal IDs to facilitate follow-up REST requests like deletion.
   * 
   * @since 1.6
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public AppliedLabels getComponentLabels(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, @PathParam("hash") String hash)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    AppliedLabels result = new AppliedLabels();

    String organizationId;
    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      Application app = new ApplicationDAO().getByIdNotNull(ownerId);
      result.add(app.getPublicId(), app.getName(), IdUtils.TYPE_APPLICATION, labelDAO.getByOwnerIdAndHash(app.getId(), hash));
      organizationId = app.getOrganizationId();
    }
    else {
      organizationId = ownerId;
    }
    if (organizationId != null) {
      Organization org = new OrganizationDAO().getByIdNotNull(organizationId);
      result
          .add(org.getId(), org.getName(), IdUtils.TYPE_ORGANIZATION, labelDAO.getByOwnerIdAndHash(org.getId(), hash));
    }

    return result;
  }

  /**
   * Assigns an existing label to a component identified by hash in a given context (org/app).
   * 
   * @since 1.6
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public void setComponentLabel(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, @PathParam("hash") String hash,
      Label label)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    ComponentLabel componentLabel = new ComponentLabel(internalOwnerId, label.getId(), hash);
    componentLabelDAO.insert(componentLabel);
  }

  /**
   * Removes the component label given by the owning context and label id.
   * 
   * @since 1.6
   */
  @DELETE
  @Path("{labelId}")
  @Authorize(permission = Permission.WRITE)
  public void removeComponentLabel(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, @PathParam("hash") String hash,
      @PathParam("labelId") String labelId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    ComponentLabel label = componentLabelDAO.getByOwnerIdAndHashAndLabelId(internalOwnerId, hash, labelId);
    if (label == null) {
      throw new NotFoundException("Cannot find the label with id " + labelId + " for " + ownerType + " id " + ownerId
          + " on the component " + hash);
    }
    componentLabelDAO.delete(label);
  }

  /**
   * Enumerates the labels applied to a given component in a way that allows to clients to identify at which point in
   * the organizational hierarchy the label has been applied.
   * 
   * @since 1.6
   */
  static class AppliedLabels
  {
    public List<LabelsByOwner> labelsByOwner = new ArrayList<LabelsByOwner>();

    void add(String ownerId, String ownerName, String ownerType, List<Label> labels) {
      if (labels == null || labels.isEmpty()) {
        return;
      }
      for (Label label : labels) {
        label.setOwnerId(ownerId);
      }
      LabelsByOwner lbo = new LabelsByOwner();
      lbo.ownerId = ownerId;
      lbo.ownerName = ownerName;
      lbo.ownerType = ownerType;
      lbo.labels = labels;
      labelsByOwner.add(lbo);
    }
  }

  /**
   * Enumerates the component labels contributed from a given context (app/org) along with basic identifying info about
   * the context itself, suitable for future REST requests to manage the component labels.
   * 
   * @since 1.6
   */
  static class LabelsByOwner
  {
    public String ownerId;

    public String ownerName;

    public String ownerType;

    public List<Label> labels;
  }
}
