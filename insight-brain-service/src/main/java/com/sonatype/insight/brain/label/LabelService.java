/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiLabelDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
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
import com.sonatype.insight.brain.webhook.ManagementEventService;
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

  private final IdUtils idUtils;

  @Inject
  public LabelService(
      final PermissionService permissionService,
      final LabelDAO labelDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final ApplicationDAO applicationDAO,
      final ManagementEventService managementEventService,
      final IdUtils idUtils)
  {
    this.permissionService = permissionService;
    this.labelDAO = labelDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.applicationDAO = applicationDAO;
    this.managementEventService = managementEventService;
    this.idUtils = idUtils;
  }

  /**
   * @param inherit boolean if {@code true} the returned list will include labels inherited from organization
   *          hierarchy, default is {@code false}
   */
  public List<ApiLabelDTO> getLabels(
      OwnerType ownerType,
      String ownerId,
      boolean inherit)
  {
    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    return getLabelsWithAuthzCheck(ownerType, ownerId, inherit);
  }

  @Authorize(permission = Permission.READ)
  List<ApiLabelDTO> getLabelsWithAuthzCheck(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) String ownerId,
      boolean inherit)
  {
    List<Label> labels = inherit ? labelDAO.getByOwnerIdWithHierarchy(ownerId) : labelDAO.getByOwnerId(ownerId);
    List<ApiLabelDTO> labelDTOs = labels.stream().map(label -> toDTO(label, ownerType)).collect(Collectors.toList());

    if (inherit) {
      for (Owner owner : ownerDAO.getOwnersInHierarchy(ownerId, ownerType)) {
        labelDTOs.stream()
            .filter(dto -> dto.ownerId.equals(owner.getId()))
            .forEach(apiLabelDTO -> apiLabelDTO.ownerType = owner.getType().name());
      }
    }

    return labelDTOs;
  }

  /**
   * Returns all the labels associated with an ownerId. The labels are grouped by ownerId and the owner name and type
   * are returned.
   */
  public ApplicableLabels getApplicableLabels(OwnerType ownerType, String ownerId) {
    log.debug("Received request to get all applicable labels for {} id {}", ownerType, ownerId);
    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    return getApplicableLabelsWithAuthzCheck(ownerType, ownerId);
  }

  @Authorize(permission = Permission.READ)
  ApplicableLabels getApplicableLabelsWithAuthzCheck(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) String ownerId)
  {
    List<Owner> owners = ownerDAO.getOwnersInHierarchy(ownerId, ownerType);
    List<String> ownerIds = owners.stream().map(Owner::getId).collect(Collectors.toList());
    Map<String, List<Label>> labelsByOwnerId = labelDAO.getByOwnerIds(ownerIds)
        .stream()
        .collect(Collectors.groupingBy(Label::getOwnerId));

    ApplicableLabels result = new ApplicableLabels();
    result.labelsByOwner = new ArrayList<>();
    for (Owner owner : owners) {
      LabelsByOwner labelsByOwner = new LabelsByOwner();
      labelsByOwner.ownerId = owner.getId();
      labelsByOwner.ownerName = owner.getName();
      labelsByOwner.ownerType = owner.getType();
      labelsByOwner.labels = labelsByOwnerId.getOrDefault(owner.getId(), Collections.emptyList())
          .stream()
          .map(label -> toDTO(label, owner.getType()))
          .collect(Collectors.toList());
      result.labelsByOwner.add(labelsByOwner);
    }

    return result;
  }

  /**
   * Enumerates the contexts (org/app) in which the given label could be applied.
   */
  public ApplicableContext getApplicableContexts(OwnerType ownerType, String ownerId, String labelId) {
    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    return getApplicableContextsWithAuthzCheck(ownerType, ownerId, labelId);
  }

  @Authorize(permission = Permission.WRITE)
  ApplicableContext getApplicableContextsWithAuthzCheck(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) String ownerId,
      String labelId)
  {
    Label label = labelDAO.getByIdNotNull(labelId);

    if (OwnerType.APPLICATION.equals(ownerType)) {
      Application application = applicationDAO.getById(label.getOwnerId());
      if (application != null) {
        return new ApplicableContext(application.getPublicId(), application.getName(), OwnerType.APPLICATION);
      }
    }

    ApplicableContext context = null;
    // walkHierarchy stays lazy so permission checks / label-owner stop can short-circuit
    for (Owner owner : ownerDAO.walkHierarchy(ownerId, ownerType)) {
      if (!permissionService.validatePermission(SecurityUtils.getSubject(), owner.getType(), owner.getId(),
          Collections.singleton(Permission.WRITE)).contains(Permission.WRITE))
      {
        break;
      }

      ApplicableContext currentContext = new ApplicableContext(
          OwnerType.APPLICATION.equals(ownerType) ? owner.getPublicId() : owner.getId(), owner.getName(),
          owner.getType());
      if (context == null) {
        context = currentContext;
      }
      else {
        currentContext.setChildren(new ArrayList<>());
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

  public ApiLabelDTO addLabel(OwnerType ownerType, String ownerId, ApiLabelDTO apiLabelDTO) {
    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    return addLabelWithAuthzCheck(ownerType, ownerId, apiLabelDTO);
  }

  @Authorize(permission = Permission.WRITE)
  ApiLabelDTO addLabelWithAuthzCheck(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) String ownerId,
      ApiLabelDTO apiLabelDTO)
  {
    if (apiLabelDTO.id != null) {
      throw new BadRequestException("ID must be null when creating a Label.");
    }

    validateOwnerIdAndOwnerType(ownerType, ownerId, apiLabelDTO);

    Label label = fromDTO(ownerId, apiLabelDTO);
    labelDAO.insert(label);
    managementEventService.postEvent(CREATED, label);
    setAuditLogLabelData(label);
    return toDTO(label, ownerType);
  }

  public ApiLabelDTO updateLabel(OwnerType ownerType, String ownerId, ApiLabelDTO apiLabelDTO) {
    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    return updateLabelWithAuthzCheck(ownerType, ownerId, apiLabelDTO);
  }

  @Authorize(permission = Permission.WRITE)
  ApiLabelDTO updateLabelWithAuthzCheck(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) String ownerId,
      ApiLabelDTO apiLabelDTO)
  {
    validateOwnerIdAndOwnerType(ownerType, ownerId, apiLabelDTO);

    Label label = labelDAO.getByIdNotNull(apiLabelDTO.id);
    if (!ownerId.equals(label.getOwnerId())) {
      throw new NotFoundException("Cannot find a label with id " + label.getId() + " for owner id " + ownerId);
    }

    label = fromDTO(ownerId, apiLabelDTO);

    labelDAO.update(label);

    managementEventService.postEvent(UPDATED, label);
    setAuditLogLabelData(label);

    return toDTO(label, ownerType);
  }

  public void deleteLabel(OwnerType ownerType, String ownerId, String labelId) {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    deleteLabelWithAuthzCheck(ownerType, internalOwnerId, labelId);
  }

  @Authorize(permission = Permission.WRITE)
  void deleteLabelWithAuthzCheck(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) String ownerId,
      String labelId)
  {
    Label label = labelDAO.getByIdNotNull(labelId);
    if (!ownerId.equals(label.getOwnerId())) {
      throw new NotFoundException("Cannot find a label with ID " + labelId + " for " + ownerType + " ID " + ownerId);
    }

    validateLabelNotUsedInAnyPolicy(ownerDAO.getById(ownerId), label);

    labelDAO.delete(label);

    setAuditLogLabelData(label);

    managementEventService.postEvent(DELETED, label);
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

  private void validateOwnerIdAndOwnerType(OwnerType ownerType, String ownerId, ApiLabelDTO apiLabelDTO) {
    if (apiLabelDTO.ownerId != null && !apiLabelDTO.ownerId.equals(ownerId)) {
      throw new BadRequestException("Owner ID mismatch.");
    }
    if (apiLabelDTO.ownerType != null && !apiLabelDTO.ownerType.equals(ownerType.name())) {
      throw new BadRequestException("Owner Type mismatch.");
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

  private void setAuditLogLabelData(Label label) {
    AuditData.get()
        .setLabel(label)
        .setData("labelDescription", label.getDescription())
        .setEnum("labelColor", label.getColor());
  }

  public static ApiLabelDTO toDTO(Label label, OwnerType ownerType) {
    ApiLabelDTO dto = new ApiLabelDTO(label.getLabel(), label.getDescription(), label.getColor().toValue());
    dto.id = label.getId();
    dto.ownerType = ownerType.name();
    dto.ownerId = label.getOwnerId();
    return dto;
  }

  private Label fromDTO(String ownerId, ApiLabelDTO apiLabelDTO) {
    Label label = new Label();
    label.setId(apiLabelDTO.id);
    label.setLabel(apiLabelDTO.label);
    label.setDescription(apiLabelDTO.description);
    label.setColor(Color.convertColorStringToEnum(apiLabelDTO.color));
    label.setOwnerId(ownerId);
    return label;
  }
}
