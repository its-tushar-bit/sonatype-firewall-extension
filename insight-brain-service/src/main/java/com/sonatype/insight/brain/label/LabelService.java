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

import com.sonatype.insight.brain.webhook.ManagementEventService;
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

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;

/**
 * @since 1.17.0
 */
@Named
public class LabelService
{
  private static final Logger log = LoggerFactory.getLogger(LabelService.class);

  private final LabelDAO labelDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final ApplicationDAO applicationDAO;

  private final ManagementEventService managementEventService;

  private PermissionService permissionService;

  @Inject
  public LabelService(final PermissionService permissionService,
                      final LabelDAO labelDAO,
                      final OwnerDAO ownerDAO,
                      final PolicyDAO policyDAO,
                      final ApplicationDAO applicationDAO,
                      final ManagementEventService managementEventService)
  {
    this.permissionService = permissionService;
    this.labelDAO = labelDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.applicationDAO = applicationDAO;
    this.managementEventService = managementEventService;
  }

  /**
   * @param inherit boolean if {@code true} the returned list will include labels inherited from organization
   *          hierarchy, default is {@code false}
   */
  @Authorize(permission = Permission.READ)
  public List<Label> getLabels(@AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
                               @AuthzContext(AuthzContext.Key.ID) String ownerId,
                               final boolean inherit)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    return labelDAO.getByOwnerId(ownerId, inherit);
  }

  /**
   * Returns all the labels associated with an ownerId. The labels are grouped by ownerId and the owner name and type
   * are returned.
   */
  @Authorize(permission = Permission.READ)
  public ApplicableLabels getApplicableLabels(@AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
                                              @AuthzContext(AuthzContext.Key.ID) String ownerId)
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
   */
  @Authorize(permission = Permission.WRITE)
  public ApplicableContext getApplicableContexts(@AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
                                                 @AuthzContext(AuthzContext.Key.ID) final String ownerIdPrivateOrPublic,
                                                 final String labelId)
  {
    Label label = labelDAO.getByIdNotNull(labelId);

    final String ownerId = IdUtils.getInternalOwnerId(ownerType, ownerIdPrivateOrPublic);

    if (OwnerType.APPLICATION.equals(ownerType)) {
      Application application = applicationDAO.getById(label.getOwnerId());
      if (application != null) {
        return new ApplicableContext(application.getPublicId(), application.getName(), OwnerType.APPLICATION);
      }
    }

    ApplicableContext context = null;
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      if (!permissionService.hasPermissions(SecurityUtils.getSubject(), owner.getType(), owner.getId(),
          Collections.singleton(Permission.WRITE)).contains(Permission.WRITE)) {
        break;
      }

      ApplicableContext currentContext = new ApplicableContext(
          OwnerType.APPLICATION.equals(ownerType) ? owner.getPublicId() : owner.getId(), owner.getName(),
          owner.getType());
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

  @Authorize(permission = Permission.WRITE)
  public Label addLabel(@AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
                        @AuthzContext(AuthzContext.Key.ID) String ownerId,
                        final Label label)
  {
    ownerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    label.setId(null);
    label.setOwnerId(ownerId);
    label.fixLabelLowercase();
    labelDAO.insert(label);

    managementEventService.postEvent(CREATED, label);

    return label;
  }

  @Authorize(permission = Permission.WRITE)
  public Label updateLabel(@AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
                           @AuthzContext(AuthzContext.Key.ID) String ownerId,
                           final Label label)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    if (!internalOwnerId.equals(labelDAO.getByIdNotNull(label.getId()).getOwnerId())) {
      throw new NotFoundException("Cannot find a label with id " + label.getId() + " for owner id " + ownerId);
    }

    label.setOwnerId(internalOwnerId);
    label.fixLabelLowercase();
    labelDAO.update(label);

    managementEventService.postEvent(UPDATED, label);

    return label;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteLabel(@AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
                          @AuthzContext(AuthzContext.Key.ID) final String ownerId,
                          final String labelId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    Label label = labelDAO.getByIdNotNull(labelId);
    if (!internalOwnerId.equals(label.getOwnerId())) {
      throw new NotFoundException("Cannot find a label with ID " + labelId + " for " + ownerType + " ID " + ownerId);
    }

    validateLabelNotUsedInAnyPolicy(ownerDAO.getById(internalOwnerId), label);

    labelDAO.delete(label);

    managementEventService.postEvent(DELETED, label);
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

  private void validateLabelNotUsedInAnyPolicy(Owner owner, Label label) {
    for (Policy policy : policyDAO.getByOwnerId(owner.getId())) {
      if (isLabelUsedInPolicy(label.getId(), policy)) {
        String error = "Cannot delete the label because it is used in a condition for the '" + policy.getName()
            + "' policy";
        if (!label.getOwnerId().equals(owner.getId())) {
          error += " in " + owner.getType() + " '" + owner.getName() + "'";
        }
        throw new BadRequestException(error);
      }
    }
    for (Owner childOwner : ownerDAO.getChildOwners(owner)) {
      validateLabelNotUsedInAnyPolicy(childOwner, label);
    }
  }

  /**
   * Returns {@code true} if the given labelId is used in the given policy; otherwise {@code false}.
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
