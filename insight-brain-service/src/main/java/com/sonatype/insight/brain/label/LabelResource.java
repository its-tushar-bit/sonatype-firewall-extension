/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Path(LabelResource.SERVICE_PATH)
public class LabelResource
{
  public static final String SERVICE_PATH = "rest/label/{ownerType: application|organization}/{ownerId}";

  private static final Logger log = LoggerFactory.getLogger(LabelResource.class);

  private LabelDAO labelDAO = new LabelDAO();

  private final OwnerDAO ownerDAO = new OwnerDAO();

  private PermissionService permissionService;


  @Inject
  public LabelResource(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  /**
   * @param inherit boolean if {@code true} the returned list will include labels inherited from organization
   *          hierarchy, default is {@code false}
   * @since 1.6
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public List<Label> getLabels(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @QueryParam("inherit") @DefaultValue("false") boolean inherit)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    return labelDAO.getByOwnerId(ownerId, inherit);
  }

  /**
   * Returns all the labels associated with an ownerId. The labels are grouped by ownerId and the owner name and type
   * are returned.
   * 
   * @since 1.6
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("applicable")
  @Authorize(permission = Permission.READ)
  public ApplicableLabels getApplicableLabels(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    log.debug("Received request to get all applicable labels for {} id {}", ownerType, ownerId);

    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    ApplicableLabels result = new ApplicableLabels();

    result.labelsByOwner = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      LabelsByOwner labelsByOwner = new LabelsByOwner();
      labelsByOwner.ownerId = owner.getId();
      labelsByOwner.ownerName = owner.getName();
      labelsByOwner.ownerType = owner.getType();
      labelsByOwner.labels = labelDAO.getByOwnerId(owner.getId());
      result.labelsByOwner.add(labelsByOwner);
    }

    return result;
  }

  /**
   * Enumerates the contexts (org/app) in which the given label could be applied.
   * 
   * @since 1.6
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("applicable/context/{labelId}")
  @Authorize(permission = Permission.WRITE)
  public ApplicableContext getApplicableContexts(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("ownerId") String ownerPublicId,
      @PathParam("labelId") String labelId)
  {
    Label label = labelDAO.getByIdNotNull(labelId);

    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application application = applicationDAO.getById(label.getOwnerId());
    if (application != null) {
      return new ApplicableContext(application.getPublicId(), application.getName(), OwnerType.APPLICATION);
    }

    application = applicationDAO.getByPublicIdNotNull(ownerPublicId);
    String ownerId = application.getId();

    ApplicableContext context = null;
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      if (!permissionService.hasPermissions(SecurityUtils.getSubject(), owner.getType(), owner.getId(),
          Collections.singleton(Permission.WRITE)).contains(Permission.WRITE)) {
        break;
      }

      ApplicableContext currentContext = new ApplicableContext(owner.getPublicId(), owner.getName(), owner.getType());
      if (context == null) {
        context = currentContext;
      }
      else {
        currentContext.setChildren(new ArrayList<ApplicableContext>());
        currentContext.getChildren().add(context);
        context = currentContext;
      }

      if (owner.getId().equals(label.getOwnerId())) {
        // only go as high as the owner of the label
        break;
      }
    }

    return context;
  }

  /**
   * @since 1.6
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public Label addLabel(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, Label label)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    label.setId(null);
    label.setOwnerId(ownerId);
    label.fixLabelLowercase();
    labelDAO.insert(label);

    return label;
  }

  /**
   * @since 1.6
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public Label updateLabel(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, Label label)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    label.setOwnerId(ownerId);
    label.fixLabelLowercase();
    labelDAO.update(label);

    return label;
  }

  /**
   * @since 1.6
   */
  @DELETE
  @Path("{labelId}")
  @Authorize(permission = Permission.WRITE)
  public void deleteLabel(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, @PathParam("labelId") String labelId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    Label label = labelDAO.getByIdNotNull(labelId);
    if (!internalOwnerId.equals(label.getOwnerId())) {
      throw new NotFoundException("Cannot find a label with ID " + labelId + " for " + ownerType + " ID " + ownerId);
    }

    // Verify that the label is not used in a policy condition
    PolicyDAO policyDAO = new PolicyDAO();

    String inUseError = "Cannot delete the label because it is used in a condition for the '%s' policy";

    for (Policy policy : policyDAO.getByOwnerId(internalOwnerId)) {
      if (isLabelUsedInPolicy(labelId, policy)) {
        throw new BadRequestException(String.format(inUseError, policy.getName()));
      }
    }

    if (IdUtils.TYPE_ORGANIZATION.equals(ownerType)) {
      inUseError = inUseError + " in application '%s'";

      for (Application app : new ApplicationDAO().getByOrganizationId(internalOwnerId)) {
        for (Policy policy : policyDAO.getByOwnerId(app.getId())) {
          if (isLabelUsedInPolicy(labelId, policy)) {
            throw new BadRequestException(String.format(inUseError, policy.getName(), app.getName()));
          }
        }
      }
    }

    labelDAO.delete(label);
  }

  public static class ApplicableLabels
  {
    public List<LabelsByOwner> labelsByOwner;
  }

  public static class LabelsByOwner
  {
    public String ownerId;

    public String ownerName;

    public OwnerType ownerType;

    public List<Label> labels;
  }

  /**
   * Returns {@code true} if the given labelId is used in the given policy; otherwise {@code false}.
   * 
   * @since 1.6
   */
  private static boolean isLabelUsedInPolicy(String labelId, Policy policy) {
    for (Constraint constraint : policy.getConstraints()) {
      for (Condition condition : constraint.getConditions()) {
        if (LabelConditionType.ID.equals(condition.getConditionTypeId()) && labelId.equals(condition.getValue())) {
          return true;
        }
      }
    }
    return false;
  }
}
