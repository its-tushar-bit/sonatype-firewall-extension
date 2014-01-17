/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.ArrayList;
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
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Path(LabelResource.SERVICE_PATH)
public class LabelResource
{
  public static final String SERVICE_BASEPATH = "rest/label/";

  public static final String SERVICE_PATH = SERVICE_BASEPATH + "{ownerType: application|organization}/{ownerId}";

  private static final Logger log = LoggerFactory.getLogger(LabelResource.class);

  private final InsightWork work;

  private LabelDAO labelDAO = new LabelDAO();

  @Inject
  public LabelResource(InsightWork work) {
    this.work = work;
  }

  /**
   * @param inherit boolean if {@code true} the returned list will include labels inherited from organization
   *          hierarchy, default is {@code false}
   * @since 1.6
   */
  @GET
  @Produces({ MediaType.APPLICATION_JSON })
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
  @Produces({ MediaType.APPLICATION_JSON })
  @Path("applicable")
  @Authorize(permission = Permission.READ)
  public ApplicableLabels getApplicableLabels(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId)
  {
    log.debug("Received request to get all applicable labels for {} id {}", ownerType, ownerId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    ApplicableLabels result = new ApplicableLabels();

    result.labelsByOwner = new ArrayList<LabelsByOwner>();
    String organizationId;
    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      Application application = new ApplicationDAO().getByIdNotNull(internalOwnerId);
      LabelsByOwner labelsByOwner = new LabelsByOwner();
      labelsByOwner.ownerId = application.getId();
      labelsByOwner.ownerName = application.getName();
      labelsByOwner.ownerType = IdUtils.TYPE_APPLICATION;
      labelsByOwner.labels = labelDAO.getByOwnerId(application.getId());
      result.labelsByOwner.add(labelsByOwner);
      organizationId = application.getOrganizationId();
    }
    else {
      organizationId = internalOwnerId;
    }
    if (organizationId != null) {
      Organization organization = new OrganizationDAO().getByIdNotNull(organizationId);
      LabelsByOwner labelsByOwner = new LabelsByOwner();
      labelsByOwner.ownerId = organization.getId();
      labelsByOwner.ownerName = organization.getName();
      labelsByOwner.ownerType = IdUtils.TYPE_ORGANIZATION;
      labelsByOwner.labels = labelDAO.getByOwnerId(organization.getId());
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
  @Produces({ MediaType.APPLICATION_JSON })
  @Path("applicable/context/{labelId}")
  @Authorize(permission = Permission.READ)
  public ApplicableContext getApplicableContexts(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, @PathParam("labelId") String labelId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    Label label = labelDAO.getByIdNotNull(labelId);
    if (!internalOwnerId.equals(label.getOwnerId())) {
      throw new NotFoundException("Cannot find a label with id " + labelId + " for " + ownerType + " id " + ownerId);
    }

    ApplicableContext context;

    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      Application app = new ApplicationDAO().getByIdNotNull(label.getOwnerId());
      context = new ApplicableContext(app.getPublicId(), app.getName(), IdUtils.TYPE_APPLICATION);
    }
    else {
      Organization org = new OrganizationDAO().getByIdNotNull(label.getOwnerId());
      context = new ApplicableContext(org.getId(), org.getName(), IdUtils.TYPE_ORGANIZATION);
      context.setChildren(new ArrayList<ApplicableContext>());
      for (Application app : new ApplicationDAO().getByOrganizationId(org.getId())) {
        context.getChildren().add(new ApplicableContext(app.getPublicId(), app.getName(), IdUtils.TYPE_APPLICATION));
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
      throw new NotFoundException("Cannot find a label with id " + labelId + " for " + ownerType + " id " + ownerId);
    }

    // Verify that the label is not used in a policy condition
    PolicyDAO policyDAO = new PolicyDAO(work.getWorkDir());

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

    public String ownerType;

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
