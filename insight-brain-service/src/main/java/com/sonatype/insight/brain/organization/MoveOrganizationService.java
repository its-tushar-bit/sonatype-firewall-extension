/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO.ValidationError;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO.ValidationError.MoveOrganizationValidationErrorType;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO.ValidationWarning;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO.ValidationWarning.MoveOrganizationValidationWarningType;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseThreatGroupValueType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.collections4.IterableUtils;

import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.stream.Collectors.toList;

/**
 * @since 1.158
 */
@Named
public class MoveOrganizationService
{
  private final OrganizationDAO organizationDAO;

  private final OwnerDAO ownerDAO;

  private final TagDAO tagDAO;

  private final PolicyDAO policyDAO;

  private final LabelDAO labelDAO;

  private final LicenseThreatGroupDAO ltgDAO;

  private final ApplicationDAO applicationDAO;

  private final ApplicationTagDAO applicationTagDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final ManagementEventService managementEventService;

  private final OrganizationService organizationService;

  @Inject
  public MoveOrganizationService(
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final TagDAO tagDAO,
      final PolicyDAO policyDAO,
      final LabelDAO labelDAO,
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final ApplicationDAO applicationDAO,
      final ApplicationTagDAO applicationTagDAO,
      final LicenseOverrideDAO licenseOverrideDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final PolicyMonitoringDAO policyMonitoringDAO,
      final ManagementEventService managementEventService,
      final OrganizationService organizationService)
  {
    this.organizationDAO = organizationDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.labelDAO = labelDAO;
    this.tagDAO = tagDAO;
    this.ltgDAO = licenseThreatGroupDAO;
    this.applicationDAO = applicationDAO;
    this.applicationTagDAO = applicationTagDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.managementEventService = managementEventService;
    this.organizationService = organizationService;
  }

  /**
   * This method had to be divided into two to ensure that the Authorize annotation checks both organizations
   * write permissions
   *
   * @param movedOrgId Organization id to move
   * @param newParentId Destination parent organization id
   * @param failEarlyOnError
   * @return
   */
  @Authorize(permission = Permission.WRITE)
  public MoveOrganizationResponseDTO moveOrganization(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) final String movedOrgId,
      final String newParentId,
      boolean failEarlyOnError)
  {
    return moveOrganizationCheckDestinationWritePermissions(movedOrgId, newParentId, failEarlyOnError);
  }

  /* Visibility set to package to let the Authorize annotation properly work */
  @Authorize(permission = Permission.WRITE)
  MoveOrganizationResponseDTO moveOrganizationCheckDestinationWritePermissions(
      final String movedOrgId,
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) final String newParentId,
      boolean failEarlyOnError)
  {
    final boolean checkWarnings = true;

    MoveOrganizationValidator moveOrganizationValidator = new MoveOrganizationValidator();
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationValidator.validateMoveOrganizationOperation(movedOrgId, newParentId, failEarlyOnError,
            checkWarnings);

    if (moveOrganizationResponseDTO.errors.isEmpty()) {
      Organization movedOrganization = organizationDAO.getById(movedOrgId);
      movedOrganization.setParentOrganizationId(newParentId);
      organizationDAO.update(movedOrganization);

      AuditData.get()
          .setOrganization(movedOrganization)
          .setParentOrganization(organizationDAO.getById(newParentId));

      managementEventService.postEvent(UPDATED, movedOrganization);
    }

    return moveOrganizationResponseDTO;
  }

  @Authorize(permission = Permission.READ)
  public List<ValidationError> getMoveOrganizationErrors(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) final String movedOrgId,
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) final String newParentId)
  {
    // we want to report all the possible errors in csv
    final boolean failEarlyOnError = false;

    // do not process warning scenarios as csv should only contain errors
    final boolean checkWarnings = false;

    MoveOrganizationValidator moveOrganizationValidator = new MoveOrganizationValidator();
    return moveOrganizationValidator.validateMoveOrganizationOperation(movedOrgId, newParentId, failEarlyOnError,
        checkWarnings).errors;
  }

  @Authorize(permission = Permission.WRITE)
  public List<Organization> getDestinationOrganizations(
      @AuthzContext(Key.ORGANIZATION_ID) final String organizationId)
  {
    if (Organization.ROOT_ORGANIZATION_ID.equals(organizationId)) {
      return Collections.emptyList();
    }

    Organization organizationToMove = organizationDAO.getByIdNotNull(organizationId);

    Predicate<Organization> notSelf = organization -> !organizationToMove.getId().equals(organization.getId());
    Predicate<Organization> notCurrentParentOfMovedOrg =
        organization -> !organizationToMove.getParentOrganizationId().equals(organization.getId());
    Predicate<Organization> notChildrenOfMovedOrg = getNotChildrenOfMovedOrgPredicate(organizationToMove);

    return organizationService.getAllWithWritePermissions()
        .stream()
        .filter(notSelf)
        .filter(notCurrentParentOfMovedOrg)
        .filter(notChildrenOfMovedOrg)
        .collect(toList());
  }

  private Predicate<Organization> getNotChildrenOfMovedOrgPredicate(Organization organizationToMove) {
    final List<String> allChildOrganizationIds =
        getAllChildrenOrganization(organizationToMove).stream().map(Owner::getId).collect(toList());
    return organization -> !allChildOrganizationIds.contains(organization.getId());
  }

  private List<Owner> getAllChildrenOrganization(Organization organization) {
    List<Owner> childOrganizationAsOwners = new ArrayList<>();
    Deque<Owner> organizationDeque = new ArrayDeque<>(ownerDAO.getChildOwners(organization));
    while (!organizationDeque.isEmpty()) {
      Owner child = organizationDeque.removeFirst();
      if (OwnerType.ORGANIZATION.equals(child.getType())) {
        childOrganizationAsOwners.add(child);
        organizationDeque.addAll(ownerDAO.getChildOwners(child));
      }
    }
    return childOrganizationAsOwners;
  }

  class MoveOrganizationValidator
  {
    private final MoveOrganizationResponseDTO moveOrganizationResponseDTO = new MoveOrganizationResponseDTO();

    private final List<Tag> newInheritedTags = new ArrayList<>();

    private final List<Label> newInheritedLabels = new ArrayList<>();

    private final List<LicenseThreatGroup> newInheritedLtgs = new ArrayList<>();

    private final List<Policy> newInheritedPolicies = new ArrayList<>();

    private final List<Tag> ownTags = new ArrayList<>();

    private final List<Label> ownLabels = new ArrayList<>();

    private final List<LicenseThreatGroup> ownLtgs = new ArrayList<>();

    private final List<Policy> ownPolicies = new ArrayList<>();

    private final List<Tag> oldInheritedTags = new ArrayList<>();

    private final List<Label> oldInheritedLabels = new ArrayList<>();

    private final List<LicenseThreatGroup> oldInheritedLtgs = new ArrayList<>();

    private final List<Policy> oldInheritedPolicies = new ArrayList<>();

    private final List<Application> applications = new ArrayList<>();

    private final Map<String, List<String>> oldParentsPerApplication = new HashMap<>();

    private List<Owner> oldParents = new ArrayList<>();

    private List<Owner> newParents = new ArrayList<>();

    private List<String> oldParentIdsIncludingCommon = new ArrayList<>();

    private List<String> newParentIdsIncludingCommon = new ArrayList<>();

    private String newParentOrgName = null;

    private String oldParentOrgName = null;

    private boolean failEarlyOnError = false;

    MoveOrganizationResponseDTO validateMoveOrganizationOperation(
        final String movedOrgId,
        final String newParentId,
        final boolean failEarlyOnError,
        final boolean checkWarnings)
    {

      this.failEarlyOnError = failEarlyOnError;

      Organization movedOrg = organizationDAO.getByIdNotNull(movedOrgId);
      Organization newParentOrg = organizationDAO.getByIdNotNull(newParentId);

      // no need to process anything if new and old parents are same
      if (isNewParentSameAsOldParent(movedOrg, newParentOrg)) {
        return moveOrganizationResponseDTO;
      }

      // get all new and old parents
      oldParents = IterableUtils.toList(ownerDAO.walkHierarchy(movedOrg.getParentOrganizationId()));
      newParents = IterableUtils.toList(ownerDAO.walkHierarchy(newParentId));

      // remove duplicated parents. Same parents have the same configuration, so we can ignore them
      newParentIdsIncludingCommon = newParents.stream().map(HasStringId::getId).collect(toList());
      oldParentIdsIncludingCommon = oldParents.stream().map(HasStringId::getId).collect(toList());
      oldParents =
          oldParents.stream().filter(owner -> !newParentIdsIncludingCommon.contains(owner.getId())).collect(toList());
      newParents =
          newParents.stream().filter(owner -> !oldParentIdsIncludingCommon.contains(owner.getId())).collect(toList());

      if (isParentHierarchyValid(movedOrg, newParentOrg)) {
        return moveOrganizationResponseDTO;
      }

      oldParentOrgName = organizationDAO.getById(movedOrg.getParentOrganizationId()).getName();
      newParentOrgName = newParentOrg.getName();

      // store configuration for new parents hierarchy
      storeParentOrganizationsConfig(newParents, newInheritedTags, newInheritedLabels, newInheritedLtgs,
          newInheritedPolicies);

      // store configuration for old parents hierarchy
      storeParentOrganizationsConfig(oldParents, oldInheritedTags, oldInheritedLabels, oldInheritedLtgs,
          oldInheritedPolicies);

      // store configurations for all child organizations and applications
      prepareChildrenValidationDataByOwner(movedOrg);

      for (Application application : applications) {
        oldParentsPerApplication.put(application.getId(),
            oldParents.stream()
                .map(HasStringId::getId)
                .collect(toList()));
      }

      // check that we do not have Tags/Labels/LTGs in use in old organizations hierarchy
      validateTags();
      validateLabels();
      validateLicenseThreatGroups();
      validateInheritedPolicies();

      // check that config names are not duplicated between moved org children and new orgs hierarchy
      validateDuplicatedTagsBetweenMovedOrgChildrenAndNewParentOrgs();
      validateDuplicatedLabelsBetweenMovedOrgChildrenAndNewParentOrgs();
      validateDuplicatedLTGsBetweenMovedOrgChildrenAndNewParentOrgs();
      validateDuplicatedPoliciesBetweenMovedOrgChildrenAndNewParentOrgs();

      // check potential warnings that might come from the move and have to be informed
      if (checkWarnings) {
        validateLicenseOverrides();
        validatePolicyWaivers();
        validatePolicyMonitoring(movedOrg, newParentOrg);
      }

      return moveOrganizationResponseDTO;
    }

    private boolean isNewParentSameAsOldParent(Organization movedOrganization, Organization newParent) {
      if (movedOrganization.getParentOrganizationId().equals(newParent.getId())) {
        addValidationErrorToResponse(MoveOrganizationValidationErrorType.PARENT_HIERARCHY,
            String.format(ValidationError.SAME_PARENT_MSG, newParent.getName(), movedOrganization.getName()),
            Collections.emptySet());
        return true;
      }
      return false;
    }

    private boolean isParentHierarchyValid(
        Organization movedOrganization,
        Organization newParent)
    {
      boolean parentHierarchyViolated = newParents.stream()
          .anyMatch(parent -> parent.getId().equals(movedOrganization.getId()));

      if (parentHierarchyViolated) {
        addValidationErrorToResponse(MoveOrganizationValidationErrorType.PARENT_HIERARCHY,
            String.format(ValidationError.INVALID_PARENT_HIERARCHY_MSG, newParent.getName(),
                movedOrganization.getName()),
            Collections.emptySet());
      }
      return parentHierarchyViolated;
    }

    private void validateInheritedPolicies() {
      Set<String> missingPolicies = oldInheritedPolicies.stream().map(Policy::getName).collect(Collectors.toSet());
      if (!missingPolicies.isEmpty()) {
        addValidationErrorToResponse(MoveOrganizationValidationErrorType.POLICY,
            String.format(ValidationError.POLICY_MISSING_MSG, newParentOrgName),
            missingPolicies);
      }
    }

    private void validateLicenseOverrides() {
      List<String> parentIds = newParents.stream().map(HasStringId::getId).collect(toList());
      for (Application app : applications) {
        checkLicenseOverrides(app, oldParentsPerApplication.get(app.getId()), parentIds);
      }
    }

    private void checkLicenseOverrides(
        Application application,
        List<String> oldUncommonAncestorIds,
        final List<String> newAncestorIds)
    {
      List<LicenseOverride> appOverrides = licenseOverrideDAO.getByOwnerId(application.getId());

      // prepare the list of applicable inherited overrides
      // (from the old parents of the org being moved) for application
      List<LicenseOverride> oldInheritedOverrides = new ArrayList<>();
      for (String ownerId : oldUncommonAncestorIds) {
        List<LicenseOverride> inheritedOverrides = licenseOverrideDAO.getByOwnerId(ownerId);
        for (LicenseOverride inheritedOverride : inheritedOverrides) {
          if (isLicenseOverrideApplicable(appOverrides, inheritedOverride)
              && isLicenseOverrideApplicable(oldInheritedOverrides, inheritedOverride))
          {
            oldInheritedOverrides.add(inheritedOverride);
          }
        }
      }

      // check each inherited override with new parents of the org being moved.
      for (LicenseOverride oldInheritedOverride : oldInheritedOverrides) {
        LicenseOverride newInheritedOverride = null;
        for (String ownerId : newAncestorIds) {
          newInheritedOverride = licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(ownerId,
              oldInheritedOverride.getComponentIdentifier());
          if (newInheritedOverride != null) {
            break;
          }
        }
        if (newInheritedOverride == null || !newInheritedOverride.getStatus().equals(oldInheritedOverride.getStatus())
            || !newInheritedOverride.getLicenseIds().equals(oldInheritedOverride.getLicenseIds()))
        {
          addValidationWarningToResponse(MoveOrganizationValidationWarningType.LICENSE_OVERRIDE,
              String.format(ValidationWarning.LICENSE_OVERRIDES_LOST_MSG, newParentOrgName, oldParentOrgName));
          break;
          /*
           * TODO: performance improvement
           * We only need one entry for license override message. Once we find first violation,
           * we don't need to keep traversing for other override checks.
           */
        }
      }
    }

    // TODO: should move to some common class due to code duplication.
    private boolean isLicenseOverrideApplicable(
        List<LicenseOverride> applicableOverrides,
        LicenseOverride inheritedOverride)
    {
      for (LicenseOverride applicableOverride : applicableOverrides) {
        if (isLicenseOverrideForEqualComponent(applicableOverride, inheritedOverride)) {
          return false;
        }
      }
      return true;
    }

    // TODO: should move to some common class due to code duplication.
    private boolean isLicenseOverrideForEqualComponent(
        LicenseOverride effectiveOverride,
        LicenseOverride otherOverride)
    {
      ComponentIdentifier effective = effectiveOverride.getComponentIdentifier();
      ComponentIdentifier other = otherOverride.getComponentIdentifier();
      if (effective.equals(other)) {
        return true;
      }
      if (effective.isMaven() && other.isMaven() && effective.getCoordinates()
          .equals(ComponentIdentifierAdapter.toGavOnlyCoordinates(other.getCoordinates())))
      {
        return true;
      }
      return false;
    }

    private void validateLicenseThreatGroups() {
      /*
       * TODO: performance improvement
       * Do not need to re-check of the ltg is already reported once.
       */
      Set<String> missingLTGs = new HashSet<>();
      for (LicenseThreatGroup oldLtg : oldInheritedLtgs) {
        for (Policy policy : ownPolicies) {
          for (Condition condition : getLicenseThreatGroupReferencingConditions(policy)) {
            if (oldLtg.getId().equals(condition.getValue())) {
              missingLTGs.add(oldLtg.getName());
            }
          }
        }
      }
      if (!missingLTGs.isEmpty()) {
        addValidationErrorToResponse(MoveOrganizationValidationErrorType.LICENSE_THREAT_GROUP,
            String.format(ValidationError.LTG_MISSING_MSG, newParentOrgName),
            missingLTGs);
      }
    }

    private void validateLabels() {
      /*
       * TODO: perf improvement opp here.
       * since the message is onl going to contain label name,
       * we can put in the list to see if we really need to go through policies and conditions to check again and again.
       */
      Set<String> missingLabels = new HashSet<>();
      for (Label label : oldInheritedLabels) {
        for (Policy policy : ownPolicies) {
          for (Condition condition : getLabelReferencingConditions(policy)) {
            if (label.getId().equals(condition.getValue())) {
              missingLabels.add(label.getLabel());
            }
          }
        }
      }
      if (!missingLabels.isEmpty()) {
        addValidationErrorToResponse(MoveOrganizationValidationErrorType.LABEL,
            String.format(ValidationError.LABEL_MISSING_MSG, newParentOrgName),
            missingLabels);
      }
    }

    // TODO: move to the separate common class because of duplication in ApplicationMoveService.class
    private List<Condition> getLabelReferencingConditions(Policy policy) {
      List<Condition> conditions = new ArrayList<>();
      for (Constraint constraint : policy.getConstraints()) {
        for (Condition condition : constraint.getConditions()) {
          if (LabelConditionType.ID.equals(condition.getConditionTypeId())) {
            conditions.add(condition);
          }
        }
      }
      return conditions;
    }

    // TODO: move to the separate common class because of duplication in ApplicationMoveService.class
    private List<Condition> getLicenseThreatGroupReferencingConditions(Policy policy) {
      List<Condition> conditions = new ArrayList<>();
      for (Constraint constraint : policy.getConstraints()) {
        for (Condition condition : constraint.getConditions()) {
          if (LicenseThreatGroupConditionType.ID.equals(condition.getConditionTypeId()) &&
              !LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID.equals(condition.getValue()))
          {
            conditions.add(condition);
          }
        }
      }
      return conditions;
    }

    private void validateTags() {
      Set<String> missingTags = new HashSet<>();
      for (Application app : applications) {
        /*
         * //todo: performance improvement here.
         * // put the tag in a set here for that we don't keep hitting db again
         * as it does not matter if it is missing in one app or more.
         */
        for (Tag tag : oldInheritedTags) {
          ApplicationTag applicationTag =
              applicationTagDAO.getByApplicationIdAndTagId(app.getId(), tag.getId());
          if (applicationTag != null) {
            missingTags.add(tag.getName());
          }
        }
      }

      if (!missingTags.isEmpty()) {
        addValidationErrorToResponse(MoveOrganizationValidationErrorType.TAG,
            String.format(ValidationError.TAG_MISSING_MSG, newParentOrgName), missingTags);
      }
    }

    private Set<String> getDuplicatedValuesBetweenLists(List<String> newInheritedValues, List<String> childValues) {
      return newInheritedValues.stream()
          .filter(childValues::contains)
          .collect(Collectors.toSet());
    }

    private void validateDuplicatedTagsBetweenMovedOrgChildrenAndNewParentOrgs() {
      List<String> tagNames = newInheritedTags.stream().map(Tag::getName).collect(toList());
      List<String> childTagNames = ownTags.stream().map(Tag::getName).collect(toList());
      Set<String> duplicatedTags = getDuplicatedValuesBetweenLists(tagNames, childTagNames);

      if (!duplicatedTags.isEmpty()) {
        addValidationErrorToResponse(MoveOrganizationValidationErrorType.TAG,
            String.format(ValidationError.DUPLICATED_TAGS_MSG, newParentOrgName), duplicatedTags);
      }
    }

    private void validateDuplicatedLabelsBetweenMovedOrgChildrenAndNewParentOrgs() {
      List<String> labelNames = newInheritedLabels.stream().map(Label::getLabel).collect(toList());
      List<String> childLabelNames = ownLabels.stream().map(Label::getLabel).collect(toList());
      Set<String> duplicatedLabels = getDuplicatedValuesBetweenLists(labelNames, childLabelNames);

      if (!duplicatedLabels.isEmpty()) {
        addValidationErrorToResponse(MoveOrganizationValidationErrorType.LABEL,
            String.format(ValidationError.DUPLICATED_LABELS_MSG, newParentOrgName), duplicatedLabels);
      }
    }

    private void validateDuplicatedLTGsBetweenMovedOrgChildrenAndNewParentOrgs() {
      List<String> ltgNames = newInheritedLtgs.stream().map(LicenseThreatGroup::getName).collect(toList());
      List<String> childLtgNames = ownLtgs.stream().map(LicenseThreatGroup::getName).collect(toList());
      Set<String> duplicatedLTGs = getDuplicatedValuesBetweenLists(ltgNames, childLtgNames);

      if (!duplicatedLTGs.isEmpty()) {
        addValidationErrorToResponse(MoveOrganizationValidationErrorType.LICENSE_THREAT_GROUP,
            String.format(ValidationError.DUPLICATED_LTG_MSG, newParentOrgName), duplicatedLTGs);
      }
    }

    private void validateDuplicatedPoliciesBetweenMovedOrgChildrenAndNewParentOrgs() {
      List<String> policyNames = newInheritedPolicies.stream().map(Policy::getName).collect(toList());
      List<String> childPolicyNames = ownPolicies.stream().map(Policy::getName).collect(toList());
      Set<String> duplicatedPolicies = getDuplicatedValuesBetweenLists(policyNames, childPolicyNames);

      if (!duplicatedPolicies.isEmpty()) {
        addValidationErrorToResponse(MoveOrganizationValidationErrorType.POLICY,
            String.format(ValidationError.DUPLICATED_POLICIES_MSG, newParentOrgName), duplicatedPolicies);
      }
    }

    private void validatePolicyWaivers() {
      Predicate<PolicyWaiver> policyWaiverBelongsToChildrenOrSelfInsteadOfOldParents =
          policyWaiver -> !oldParentIdsIncludingCommon.contains(policyWaiver.getOwnerId());

      Optional<PolicyWaiver> foundWaiverForChildOrSelf = oldInheritedPolicies.stream()
          .map(Policy::getId)
          .map(policyWaiverDAO::getActiveByPolicyId)
          .flatMap(Collection::stream) // turn stream of lists into just stream of their waiver elements
          .filter(Objects::nonNull)
          .filter(policyWaiverBelongsToChildrenOrSelfInsteadOfOldParents)
          .findFirst();

      if (foundWaiverForChildOrSelf.isPresent()) {
        addValidationWarningToResponse(MoveOrganizationValidationWarningType.POLICY_WAIVER,
            ValidationWarning.POLICY_WAIVER_MSG);
      }
    }

    private void validatePolicyMonitoring(final Organization movedOrg, final Organization newParentOrg) {
      List<String> allOldOwnersToCheckForPolicyMonitoring = new ArrayList<>(oldParentIdsIncludingCommon);
      allOldOwnersToCheckForPolicyMonitoring.add(0, movedOrg.getId());
      Optional<PolicyMonitoring> configuredOrInheritedMonitoring =
          allOldOwnersToCheckForPolicyMonitoring.stream()
              .flatMap(ownerId -> policyMonitoringDAO.getByOwnerId(ownerId).stream())
              .filter(Objects::nonNull)
              .findFirst();

      if (configuredOrInheritedMonitoring.isPresent()) {
        PolicyMonitoring currentPolicyMonitoring = configuredOrInheritedMonitoring.get();
        if (currentPolicyMonitoring.getOwnerId().equals(movedOrg.getId())) {
          // Policy monitoring is configured at the moved org, so it will remain when moved to the new parent
          return;
        }

        List<String> allNewOwnersToCheckForPolicyMonitoring = new ArrayList<>(newParentIdsIncludingCommon);
        allNewOwnersToCheckForPolicyMonitoring.add(0, newParentOrg.getId());

        Optional<PolicyMonitoring> expectedNewPolicyMonitoring = allNewOwnersToCheckForPolicyMonitoring.stream()
            .flatMap(ownerId -> policyMonitoringDAO.getByOwnerId(ownerId).stream())
            .filter(Objects::nonNull)
            .findFirst();

        if (!expectedNewPolicyMonitoring.isPresent()) {
          addValidationWarningToResponse(MoveOrganizationValidationWarningType.POLICY_MONITORING,
              ValidationWarning.POLICY_MONITORING_MISSING_MSG);
        }
        else if (!expectedNewPolicyMonitoring.get().getStageTypeId().equals(currentPolicyMonitoring.getStageTypeId())) {
          addValidationWarningToResponse(MoveOrganizationValidationWarningType.POLICY_MONITORING,
              ValidationWarning.POLICY_MONITORING_DIFFERENT_MSG);
        }
      }
    }

    private void prepareChildrenValidationDataByOwner(Owner owner) {
      ownLabels.addAll(labelDAO.getByOwnerId(owner.getId()));
      ownLtgs.addAll(ltgDAO.getByOwnerId(owner.getId()));
      ownPolicies.addAll(policyDAO.getByOwnerId(owner.getId()));

      // prepare org specific data
      if (OwnerType.ORGANIZATION.equals(owner.getType())) {
        ownTags.addAll(tagDAO.getByOrganizationId(owner.getId()));
        applications.addAll(applicationDAO.getByOrganizationId(owner.getId()));
      }

      if (!owner.canHaveChildren()) {
        return;
      }

      // recursively prepare children validation data for each child
      ownerDAO.getChildOwners(owner)
          .forEach(this::prepareChildrenValidationDataByOwner);
    }

    private void storeParentOrganizationsConfig(
        final List<Owner> parentOrganizations,
        final List<Tag> tagSet,
        final List<Label> labelsSet,
        final List<LicenseThreatGroup> ltgSet,
        final List<Policy> policySet)
    {
      for (Owner organization : parentOrganizations) {
        tagSet.addAll(tagDAO.getByOrganizationId(organization.getId()));
        labelsSet.addAll(labelDAO.getByOwnerId(organization.getId()));
        ltgSet.addAll(ltgDAO.getByOwnerId(organization.getId()));
        policySet.addAll(policyDAO.getByOwnerId(organization.getId()));
      }
    }

    private void addValidationErrorToResponse(
        MoveOrganizationValidationErrorType errorType,
        String message,
        Set<String> messageDetails)
    {
      String completeErrorMessage = message + String.join(",", messageDetails);

      if (this.failEarlyOnError) {
        throw new ConflictException(completeErrorMessage);
      }

      moveOrganizationResponseDTO.errors.add(
          new ValidationError(errorType,
              completeErrorMessage));
    }

    private void addValidationWarningToResponse(
        MoveOrganizationValidationWarningType warningType,
        String message)
    {
      moveOrganizationResponseDTO.warnings.add(
          new ValidationWarning(warningType, message));
    }
  }
}
