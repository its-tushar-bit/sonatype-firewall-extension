/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiMoveApplicationResponseDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.label.ComponentLabel;
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
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.ClearRolePermissionCache;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationMoveService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationMoveService.class);

  static final String TAG_MISSING_MSG = "The application category '%s' from the organization '%s' has no counterpart"
      + " in the new parent organization.";

  public static final String POLICY_MISSING_MSG = "The policy '%s' from the organization '%s' has no counterpart"
      + " in the new parent organization.";

  static final String TAG_MISMATCH_MSG = "The policy '%s' from the organization '%s' inherits to different"
      + " application categories than its counterpart in the new parent organization.";

  static final String TAG_MISMATCH_2_MSG = "The policy '%s' from the application conflicts with a policy from"
      + " the new organization whose application categories however do not match the application.";

  static final String LABEL_MISSING_MSG = "The label '%s' from the organization '%s' has no counterpart"
      + " in the new parent organization.";

  static final String LTG_MISSING_MSG = "The license threat group '%s' from the organization '%s' has no counterpart"
      + " in the new parent organization.";

  static final String POLICY_MONITORING_MISSING_MSG = "The new parent organization does not use continuous"
      + " policy monitoring.";

  static final String POLICY_MONITORING_DIFFERENT_MSG = "The new parent organization uses a different stage"
      + " for continuous policy monitoring.";

  static final String POLICY_WAIVERS_LOST_MSG = "Some policy waivers (%d in total) that were previously inherited"
      + " no longer apply in the new parent organization.";

  static final String COMPONENT_LABELS_LOST_MSG = "Some component labels (%d in total) that were previously inherited"
      + " no longer exist in the new parent organization.";

  static final String LICENSE_OVERRIDES_LOST_MSG = "Some license overrides (%d in total) that were previously inherited"
      + " no longer exist in the new parent organization.";

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final OwnerDAO ownerDAO;

  private final ApplicationTagDAO applicationTagDAO;

  private final TagDAO tagDAO;

  private final PolicyDAO policyDAO;

  private final PolicyTagDAO policyTagDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final LabelDAO labelDAO;

  private final ComponentLabelDAO componentLabelDAO;

  private final LicenseThreatGroupDAO ltgDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  private final CurrentUser currentUser;

  private final ClearRolePermissionCache authorizationCacheInvalidator;

  @Inject
  public ApplicationMoveService(
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      OwnerDAO ownerDAO,
      ApplicationTagDAO applicationTagDAO,
      TagDAO tagDAO,
      PolicyDAO policyDAO,
      PolicyTagDAO policyTagDAO,
      PolicyViolationDAO policyViolationDAO,
      PolicyWaiverDAO policyWaiverDAO,
      PolicyMonitoringDAO policyMonitoringDAO,
      LabelDAO labelDAO,
      ComponentLabelDAO componentLabelDAO,
      LicenseThreatGroupDAO ltgDAO,
      LicenseOverrideDAO licenseOverrideDAO,
      MembershipMappingDAO membershipMappingDAO,
      CurrentUser currentUser,
      ClearRolePermissionCache authorizationCacheInvalidator)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.ownerDAO = ownerDAO;
    this.applicationTagDAO = applicationTagDAO;
    this.tagDAO = tagDAO;
    this.policyDAO = policyDAO;
    this.policyTagDAO = policyTagDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.labelDAO = labelDAO;
    this.componentLabelDAO = componentLabelDAO;
    this.ltgDAO = ltgDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.membershipMappingDAO = membershipMappingDAO;
    this.currentUser = currentUser;
    this.authorizationCacheInvalidator = authorizationCacheInvalidator;
  }

  @Authorize(permission = Permission.WRITE)
  public List<Organization> getDestinationOrganizations(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId)
  {
    Application application = applicationDAO.getByIdNotNull(applicationId);
    List<Organization> organizations = new ArrayList<>();
    for (Organization organization : getPermittedDestinationOrganizations()) {
      // moving to the current parent organization is pointless so exclude it
      if (!organization.getId().equals(application.getOrganizationId())) {
        organizations.add(organization);
      }
    }
    return organizations;
  }

  @AuthzFilter(permission = Permission.ADD_APPLICATION, context = AuthzFilter.Context.ORGANIZATION)
  List<Organization> getPermittedDestinationOrganizations() {
    return organizationDAO.getAll()
        .stream()
        .filter(organization -> !organization.getId().equals(Organization.ROOT_ORGANIZATION_ID))
        .collect(Collectors.toList());
  }

  /**
   * @return Warning messages about differences in policy configuration after move.
   */
  @Authorize(permission = Permission.WRITE)
  public ApiMoveApplicationResponseDTOV2 moveApplication(
      @AuthzContext(Key.APPLICATION_ID) String applicationId,
      String organizationId)
  {
    AuditData.get().setParentOrganization(organizationDAO.getById(organizationId));
    Application application = applicationDAO.getByIdNotNull(applicationId);
    AuditData.get().setApplicationWithDetails(application);
    if (application.getOrganizationId().equals(organizationId)) {
      throw new BadRequestException("The destination organization must be different from the current organization");
    }
    ApiMoveApplicationResponseDTOV2 response = new ApiMoveApplicationResponseDTOV2();
    response.warnings = moveApplication(application, organizationId);
    return response;
  }

  @Authorize(permission = Permission.ADD_APPLICATION)
  List<String> moveApplication(
      Application application,
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId)
  {
    if (Organization.ROOT_ORGANIZATION_ID.equals(organizationId)) {
      throw new BadRequestException("Applications cannot be moved to the root organization.");
    }
    long start = System.currentTimeMillis();
    List<String> warnings = new MoveOperation(application, organizationId).run();
    log.info("Moved application {} ({}) to organization {} in {} ms", application.getId(), application.getName(),
        organizationId, System.currentTimeMillis() - start);
    return warnings;
  }

  private class MoveOperation
  {
    private TransactionContext tx;

    private final Application application;

    private final Map<String, Owner> oldOwnersById = new LinkedHashMap<>();

    private List<Tag> oldTags;

    private List<Policy> oldPolicies;

    private List<LicenseThreatGroup> oldLtgs;

    private List<Label> oldLabels;

    private final String newParentId;

    private final Set<String> newAncestorIds = new LinkedHashSet<>();

    private final Map<String, Tag> newTagsByOldIds = new HashMap<>();

    private final Map<String, Policy> newPoliciesByOldIds = new HashMap<>();

    private final List<Policy> retainedOldAppPolicies = new ArrayList<>();

    private final Map<String, Label> newLabelsByOldIds = new HashMap<>();

    private final Map<String, LicenseThreatGroup> newLtgsByOldIds = new HashMap<>();

    private final List<String> errors = new ArrayList<>();

    private final Set<Policy> updatedPolicies = new HashSet<>();

    private final List<String> warnings = new ArrayList<>();

    private int lostPolicyWaivers;

    private int lostLicenseOverrides;

    private int lostComponentLabels;

    public MoveOperation(Application application, String newParentId) {
      this.application = application;
      this.newParentId = newParentId;
    }

    public List<String> run() {
      try (TransactionContext tx = this.tx = applicationDAO.createTransactionContext()) {
        tx.begin();

        log.debug("Mapping policy elements between current parent {} and new parent {}",
            application.getOrganizationId(), newParentId);
        for (Owner owner : ownerDAO.walkHierarchy(tx, newParentId)) {
          newAncestorIds.add(owner.getId());
        }

        Owner newParent = ownerDAO.getById(newParentId);
        for (Owner owner : ownerDAO.walkHierarchy(tx, application.getOrganizationId())) {
          if (owner.getId().equals(newParent.getParentOwnerId()) || owner.getId().equals(newParent.getId())) {
            break;
          }
          removePolicyOverrides(tx, owner.getId(), application.getId());
        }

        loadOldPolicyConfiguration();
        mapTags();
        mapPolicies();
        mapLabels();
        mapLicenseThreatGroups();
        failIfErrorsPresent();

        log.debug("Moving application to new parent organization");
        application.setOrganizationId(newParentId);
        applicationDAO.update(tx, application, true);
        updateApplicationTags();
        checkPolicyMonitoring();
        updatePolicyReferences();
        updateLabelReferences();
        updateLicenseThreatGroupReferences();
        checkLicenseOverrides();
        for (Policy policy : updatedPolicies) {
          policyDAO.update(tx, policy);
        }

        grantOwnerRoleIfNeeded();

        log.debug("Committing updated data");
        tx.afterCommit(authorizationCacheInvalidator::invalidateAuthorizationCachesForAllNodes);
        tx.commit();
      }

      if (lostPolicyWaivers > 0) {
        warnings.add(String.format(POLICY_WAIVERS_LOST_MSG, lostPolicyWaivers));
      }
      if (lostComponentLabels > 0) {
        warnings.add(String.format(COMPONENT_LABELS_LOST_MSG, lostComponentLabels));
      }
      if (lostLicenseOverrides > 0) {
        warnings.add(String.format(LICENSE_OVERRIDES_LOST_MSG, lostLicenseOverrides));
      }
      return warnings;
    }

    private void removePolicyOverrides(TransactionContext tx, String internalOwnerId, String applicationId) {
      policyDAO.getByOwnerId(tx, internalOwnerId)
          .forEach(policy -> {
            boolean updated = false;
            if (policy.getPolicyActionsOverrides() != null &&
                policy.getPolicyActionsOverrides().containsKey(applicationId))
          {
              policy.getPolicyActionsOverrides().remove(applicationId);
              updated = true;
            }
            if (policy.getPolicyNotificationsOverrides() != null &&
                policy.getPolicyNotificationsOverrides().containsKey(applicationId))
          {
              policy.getPolicyNotificationsOverrides().remove(applicationId);
              updated = true;
            }
            if (updated) {
              policyDAO.update(tx, policy);
            }
          });
    }

    private void loadOldPolicyConfiguration() {
      oldOwnersById.put(application.getId(), application);
      oldLtgs = new ArrayList<>(ltgDAO.getByOwnerIdWithHierarchy(tx, application.getId()));
      for (Owner owner : ownerDAO.walkHierarchy(tx, application.getOrganizationId())) {
        oldOwnersById.put(owner.getId(), owner);
      }
      oldPolicies = policyDAO.getApplicableByOwnerIdWithHierarchy(tx, application.getId());
      oldLabels = labelDAO.getByOwnerIdWithHierarchy(tx, application.getId());
      oldTags = tagDAO.getByApplicationId(tx, application.getId());
    }

    private void mapTags() {
      for (Tag oldTag : oldTags) {
        Tag newTag = null;
        for (String organizationId : newAncestorIds) {
          newTag = tagDAO.getByOrganizationIdAndName(tx, organizationId, oldTag.getName());
          if (newTag != null) {
            break;
          }
        }
        if (newTag != null) {
          newTagsByOldIds.put(oldTag.getId(), newTag);
        }
        else {
          addError(TAG_MISSING_MSG, oldTag.getName(), oldTag.getOrganizationId());
        }
      }
    }

    private void mapPolicies() {
      Set<String> newTagIds = new HashSet<>();
      for (Tag newTag : newTagsByOldIds.values()) {
        newTagIds.add(newTag.getId());
      }

      for (Policy oldPolicy : oldPolicies) {
        Policy newPolicy = null;
        for (String ownerId : newAncestorIds) {
          newPolicy = policyDAO.getByOwnerIdAndName(tx, ownerId, oldPolicy.getName());
          if (newPolicy != null) {
            break;
          }
        }
        if (newPolicy != null) {
          if (policyTagDAO.isPolicyApplicable(tx, newPolicy.getId(), newTagIds)) {
            newPoliciesByOldIds.put(oldPolicy.getId(), newPolicy);
          }
          else if (oldPolicy.getOwnerId().equals(application.getId())) {
            addError(TAG_MISMATCH_2_MSG, oldPolicy.getName(), oldPolicy.getOwnerId());
          }
          else {
            addError(TAG_MISMATCH_MSG, oldPolicy.getName(), oldPolicy.getOwnerId());
          }
        }
        else if (oldPolicy.getOwnerId().equals(application.getId())) {
          newPoliciesByOldIds.put(oldPolicy.getId(), oldPolicy);
          retainedOldAppPolicies.add(oldPolicy);
        }
        else {
          addError(POLICY_MISSING_MSG, oldPolicy.getName(), oldPolicy.getOwnerId());
        }
      }
    }

    private void mapLabels() {
      for (Label oldLabel : oldLabels) {
        Label newLabel = null;
        for (String ownerId : newAncestorIds) {
          newLabel = labelDAO.getByOwnerIdAndLabel(tx, ownerId, oldLabel.getLabel());
          if (newLabel != null) {
            break;
          }
        }
        if (newLabel != null) {
          newLabelsByOldIds.put(oldLabel.getId(), newLabel);
        }
        else if (oldLabel.getOwnerId().equals(application.getId())) {
          newLabelsByOldIds.put(oldLabel.getId(), oldLabel);
        }
        else if (isLabelUsed(oldLabel)) {
          addError(LABEL_MISSING_MSG, oldLabel.getLabel(), oldLabel.getOwnerId());
        }
      }
    }

    private boolean isLabelUsed(Label label) {
      if (getReferencedLabels(retainedOldAppPolicies).contains(label.getId())) {
        return true;
      }
      if (!componentLabelDAO.getByLabelIdAndOwnerIds(tx, label.getId(), oldOwnersById.keySet()).isEmpty()) {
        return true;
      }
      return false;
    }

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

    private Set<String> getReferencedLabels(List<Policy> policies) {
      Set<String> labelIds = new HashSet<>();
      for (Policy policy : policies) {
        for (Condition condition : getLabelReferencingConditions(policy)) {
          labelIds.add(condition.getValue());
        }
      }
      return labelIds;
    }

    private void mapLicenseThreatGroups() {
      for (LicenseThreatGroup oldLtg : oldLtgs) {
        LicenseThreatGroup newLtg = null;
        for (String ownerId : newAncestorIds) {
          newLtg = ltgDAO.getByOwnerIdAndName(tx, ownerId, oldLtg.getName());
          if (newLtg != null) {
            break;
          }
        }
        if (newLtg != null) {
          newLtgsByOldIds.put(oldLtg.getId(), newLtg);
        }
        else if (oldLtg.getOwnerId().equals(application.getId())) {
          newLtgsByOldIds.put(oldLtg.getId(), oldLtg);
        }
        else if (isLicenseThreatGroupUsed(oldLtg)) {
          addError(LTG_MISSING_MSG, oldLtg.getName(), oldLtg.getOwnerId());
        }
      }
    }

    private boolean isLicenseThreatGroupUsed(LicenseThreatGroup ltg) {
      if (getReferencedLicenseThreatGroups(retainedOldAppPolicies).contains(ltg.getId())) {
        return true;
      }
      return false;
    }

    private List<Condition> getLicenseThreatGroupReferencingConditions(Policy policy) {
      List<Condition> conditions = new ArrayList<>();
      for (Constraint constraint : policy.getConstraints()) {
        for (Condition condition : constraint.getConditions()) {
          if (LicenseThreatGroupConditionType.ID.equals(condition.getConditionTypeId())
              && !LicenseThreatGroupValueType.UNASSIGNED_LICENSE_THREAT_GROUP_ID.equals(condition.getValue()))
          {
            conditions.add(condition);
          }
        }
      }
      return conditions;
    }

    private Set<String> getReferencedLicenseThreatGroups(List<Policy> policies) {
      Set<String> ltgIds = new HashSet<>();
      for (Policy policy : policies) {
        for (Condition condition : getLicenseThreatGroupReferencingConditions(policy)) {
          ltgIds.add(condition.getValue());
        }
      }
      return ltgIds;
    }

    private void addError(String format, String entityName, String entityOwnerId) {
      errors.add(String.format(format, entityName, oldOwnersById.get(entityOwnerId).getName()));
    }

    private void failIfErrorsPresent() {
      if (!errors.isEmpty()) {
        ApiMoveApplicationResponseDTOV2 apiMoveApplicationResponseDTOV2 = new ApiMoveApplicationResponseDTOV2();
        apiMoveApplicationResponseDTOV2.errors = errors;
        throw new ApplicationMoveException(apiMoveApplicationResponseDTOV2);
      }
    }

    private void updateApplicationTags() {
      List<ApplicationTag> applicationTags = applicationTagDAO.getByApplicationId(tx, application.getId());
      log.debug("Updating {} application tags", applicationTags.size());
      for (ApplicationTag applicationTag : applicationTags) {
        Tag newTag = newTagsByOldIds.get(applicationTag.getTagId());
        if (!applicationTag.getTagId().equals(newTag.getId())) {
          log.debug("  Updating application tag from {} to {} ({})", applicationTag.getId(), newTag.getId(),
              newTag.getName());
          applicationTag.setTagId(newTag.getId());
          applicationTagDAO.update(tx, applicationTag);
        }
      }
    }

    private void checkPolicyMonitoring() {
      log.debug("Checking policy monitoring");
      List<PolicyMonitoring> oldMonitorings = getPolicyMonitoring(oldOwnersById.keySet());
      for (PolicyMonitoring oldMonitoring : oldMonitorings) {
        if (oldMonitoring == null || oldMonitoring.getOwnerId().equals(application.getId())) {
          return;
        }
      }

      List<PolicyMonitoring> newMonitorings = getPolicyMonitoring(newAncestorIds);
      if (!oldMonitorings.isEmpty() && newMonitorings.isEmpty()) {
        warnings.add(POLICY_MONITORING_MISSING_MSG);
      }
      for (PolicyMonitoring newMonitoring : newMonitorings) {
        for (PolicyMonitoring oldMonitoring : oldMonitorings) {
          if (!newMonitoring.getStageTypeId().equals(oldMonitoring.getStageTypeId())) {
            warnings.add(POLICY_MONITORING_DIFFERENT_MSG);
          }
        }
      }
    }

    private List<PolicyMonitoring> getPolicyMonitoring(Set<String> ownerIds) {
      List<PolicyMonitoring> monitorings = new ArrayList<>();
      for (String ownerId : ownerIds) {
        monitorings.addAll(policyMonitoringDAO.getByOwnerId(tx, ownerId));
      }
      return monitorings;
    }

    private void updatePolicyReferences() {
      log.debug("Updating references for {} policies", oldPolicies.size());
      for (Policy oldPolicy : oldPolicies) {
        Policy newPolicy = newPoliciesByOldIds.get(oldPolicy.getId());
        if (!oldPolicy.getId().equals(newPolicy.getId())) {
          log.debug("  Updating policy references from {} ({}) to {} ({})", oldPolicy.getId(), oldPolicy.getName(),
              newPolicy.getId(), newPolicy.getName());
          int updatedViolations = policyViolationDAO.replacePolicyId(tx, application.getId(), oldPolicy.getId(),
              newPolicy.getId());
          log.debug("    Updated {} policy violations", updatedViolations);
          updatePolicyWaivers(oldPolicy, newPolicy);
          if (oldPolicy.getOwnerId().equals(application.getId())) {
            policyDAO.delete(tx, oldPolicy);
            log.debug("    Deleted superseded policy {} ({})", oldPolicy.getId(), oldPolicy.getName());
          }
        }
      }
    }

    private void updatePolicyWaivers(Policy oldPolicy, Policy newPolicy) {
      List<PolicyWaiver> oldWaivers = policyWaiverDAO.getByPolicyIdAndOwnerIds(tx, oldPolicy.getId(),
          oldOwnersById.keySet());
      log.debug("    Updating {} policy waivers", oldWaivers.size());
      for (PolicyWaiver oldWaiver : oldWaivers) {
        if (oldWaiver.getOwnerId().equals(application.getId())) {
          oldWaiver.setPolicyId(newPolicy.getId());
          policyWaiverDAO.update(tx, oldWaiver);
        }
        else if (!isPolicyWaiverInheritedByNewParent(newPolicy.getId(), oldWaiver)) {
          lostPolicyWaivers++;
          log.info("Policy waiver for policy {} and component {} no longer applies after move of application",
              oldPolicy.getName(), oldWaiver.getHash());
        }
      }
    }

    private boolean isPolicyWaiverInheritedByNewParent(String policyId, PolicyWaiver oldWaiver) {
      if (newAncestorIds.contains(oldWaiver.getOwnerId())) {
        return true;
      }
      List<PolicyWaiver> waivers = policyWaiverDAO.getByPolicyIdAndOwnerIds(tx, policyId, newAncestorIds);
      for (PolicyWaiver waiver : waivers) {
        if (waiver.getHash() == null || waiver.getHash().equals(oldWaiver.getHash())) {
          return true;
        }
      }
      return false;
    }

    private void updateLabelReferences() {
      log.debug("Updating references for {} labels", oldLabels.size());
      for (Label oldLabel : oldLabels) {
        Label newLabel = newLabelsByOldIds.get(oldLabel.getId());
        if (newLabel != null && !oldLabel.getId().equals(newLabel.getId())) {
          log.debug("  Updating label references from {} ({}) to {} ({})", oldLabel.getId(), oldLabel.getLabel(),
              newLabel.getId(), newLabel.getLabel());
          updateComponentLabels(oldLabel, newLabel);
          for (Policy policy : retainedOldAppPolicies) {
            for (Condition condition : getLabelReferencingConditions(policy)) {
              if (oldLabel.getId().equals(condition.getValue())) {
                condition.setValue(newLabel.getId());
                updatedPolicies.add(policy);
              }
            }
          }
          if (oldLabel.getOwnerId().equals(application.getId())) {
            labelDAO.delete(tx, oldLabel);
            log.debug("    Deleted superseded label {} ({})", oldLabel.getId(), oldLabel.getLabel());
          }
        }
      }
    }

    private void updateComponentLabels(Label oldLabel, Label newLabel) {
      List<ComponentLabel> oldComponentLabels = componentLabelDAO.getByLabelIdAndOwnerIds(tx, oldLabel.getId(),
          oldOwnersById.keySet());
      log.debug("    Updating {} component labels", oldComponentLabels.size());
      for (ComponentLabel oldComponentLabel : oldComponentLabels) {
        if (oldComponentLabel.getOwnerId().equals(application.getId())) {
          oldComponentLabel.setLabelId(newLabel.getId());
          componentLabelDAO.update(tx, oldComponentLabel);
        }
        else if (!isComponentLabelInheritedFromNewParent(newLabel.getId(), oldComponentLabel)) {
          lostComponentLabels++;
          log.info("Component label {} no longer applies to component {} after move of application",
              oldLabel.getLabel(), oldComponentLabel.getHash());
        }
      }
    }

    private boolean isComponentLabelInheritedFromNewParent(String labelId, ComponentLabel oldComponentLabel) {
      if (newAncestorIds.contains(oldComponentLabel.getOwnerId())) {
        return true;
      }
      List<ComponentLabel> componentLabels = componentLabelDAO.getByLabelIdAndOwnerIds(tx, labelId, newAncestorIds);
      for (ComponentLabel componentLabel : componentLabels) {
        if (oldComponentLabel.getHash().equals(componentLabel.getHash())) {
          return true;
        }
      }
      return false;
    }

    private void updateLicenseThreatGroupReferences() {
      log.debug("Updating references for {} license threat groups", oldLtgs.size());
      for (LicenseThreatGroup oldLtg : oldLtgs) {
        LicenseThreatGroup newLtg = newLtgsByOldIds.get(oldLtg.getId());
        if (newLtg != null && !oldLtg.getId().equals(newLtg.getId())) {
          log.debug("  Updating license threat group references from {} ({}) to {} ({})", oldLtg.getId(),
              oldLtg.getName(), newLtg.getId(), newLtg.getName());
          for (Policy policy : retainedOldAppPolicies) {
            for (Condition condition : getLicenseThreatGroupReferencingConditions(policy)) {
              if (oldLtg.getId().equals(condition.getValue())) {
                condition.setValue(newLtg.getId());
                updatedPolicies.add(policy);
              }
            }
          }
          if (oldLtg.getOwnerId().equals(application.getId())) {
            ltgDAO.delete(tx, oldLtg);
            log.debug("    Deleted superseded license threat group {} ({})", oldLtg.getId(), oldLtg.getName());
          }
        }
      }
    }

    private void checkLicenseOverrides() {
      log.debug("Checking license overrides");
      Set<String> oldUncommonAncestorIds = new LinkedHashSet<>(oldOwnersById.keySet());
      oldUncommonAncestorIds.remove(application.getId());
      oldUncommonAncestorIds.removeAll(newAncestorIds);

      List<LicenseOverride> appOverrides = licenseOverrideDAO.getByOwnerId(tx, application.getId());
      List<LicenseOverride> oldInheritedOverrides = new ArrayList<>();
      for (String ownerId : oldUncommonAncestorIds) {
        List<LicenseOverride> inheritedOverrides = licenseOverrideDAO.getByOwnerId(tx, ownerId);
        for (LicenseOverride inheritedOverride : inheritedOverrides) {
          if (isLicenseOverrideApplicable(appOverrides, inheritedOverride)
              && isLicenseOverrideApplicable(oldInheritedOverrides, inheritedOverride))
          {
            oldInheritedOverrides.add(inheritedOverride);
          }
        }
      }

      for (LicenseOverride oldInheritedOverride : oldInheritedOverrides) {
        LicenseOverride newInheritedOverride = null;
        for (String ownerId : newAncestorIds) {
          newInheritedOverride = licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(tx, ownerId,
              oldInheritedOverride.getComponentIdentifier());
          if (newInheritedOverride != null) {
            break;
          }
        }
        if (newInheritedOverride == null || !newInheritedOverride.getStatus().equals(oldInheritedOverride.getStatus())
            || !newInheritedOverride.getLicenseIds().equals(oldInheritedOverride.getLicenseIds()))
        {
          lostLicenseOverrides++;
          log.info("License override with status {} and licenses {} no longer applies to component {}"
              + " after move of application", oldInheritedOverride.getStatus(), oldInheritedOverride.getLicenseIds(),
              oldInheritedOverride.getComponentIdentifier());
        }
      }
    }

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

    private void grantOwnerRoleIfNeeded() {
      UserPrincipal userPrincipal = currentUser.getUserPrincipal();
      Set<String> newOwnerIds = new HashSet<>();
      newOwnerIds.add(application.getId());
      newOwnerIds.addAll(newAncestorIds);
      for (String ownerId : newOwnerIds) {
        for (MembershipMapping membershipMapping : membershipMappingDAO.getByContextIdAndRoleId(tx, ownerId,
            Role.OWNER_ROLE_ID))
        {
          if (membershipMapping.includes(userPrincipal)) {
            return;
          }
        }
      }
      log.debug("Assigning current user to 'Owner' role for moved application");
      membershipMappingDAO.insert(tx, new MembershipMapping(application.getId(), Role.OWNER_ROLE_ID,
          userPrincipal.getUsername(), MemberType.USER));
    }
  }
}
