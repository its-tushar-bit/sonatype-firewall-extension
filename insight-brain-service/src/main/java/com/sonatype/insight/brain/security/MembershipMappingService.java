/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiMemberMappingAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryContainerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.UserDirectory.QueryResult;
import com.sonatype.insight.brain.webhook.EventAction;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.11.0
 */
@Named
public class MembershipMappingService
{
  private static final Logger log = LoggerFactory.getLogger(MembershipMappingService.class);

  private final ApplicationDAO appDAO;

  private final OrganizationDAO orgDAO;

  private final RepositoryContainerDAO repositoryContainerDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final RoleDAO roleDAO;

  private final RolePermissionDAO rolePermissionDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  private final OwnerDAO ownerDAO;

  private final UserDirectory userDirectory;

  private final ManagementEventService managementEventService;

  private final ApiMemberMappingAdapter apiMemberMappingAdapter;

  @Inject
  public MembershipMappingService(
      final ApplicationDAO appDAO,
      final OrganizationDAO orgDAO,
      final RepositoryContainerDAO repositoryContainerDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryManagerDAO repositoryManagerDAO,
      final RoleDAO roleDAO,
      final RolePermissionDAO rolePermissionDAO,
      final MembershipMappingDAO membershipMappingDAO,
      final OwnerDAO ownerDAO,
      final UserDirectory userDirectory,
      final ManagementEventService managementEventService,
      final ApiMemberMappingAdapter apiMemberMappingAdapter)
  {
    this.appDAO = appDAO;
    this.orgDAO = orgDAO;
    this.repositoryContainerDAO = repositoryContainerDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.roleDAO = roleDAO;
    this.rolePermissionDAO = rolePermissionDAO;
    this.membershipMappingDAO = membershipMappingDAO;
    this.ownerDAO = ownerDAO;
    this.userDirectory = userDirectory;
    this.managementEventService = managementEventService;
    this.apiMemberMappingAdapter = apiMemberMappingAdapter;
  }

  // Authorization is checked in loadMembersByRoleForGlobalContext and loadMembersByRoleForNonGlobalContext
  public ApplicableMembershipMappings getApplicableMembershipMappings(
      final OwnerType ownerType,
      final String internalOwnerId)
  {
    log.debug("Getting all applicable membership mappings for {} id {}", ownerType, internalOwnerId);

    final Map<String, MembersByRole> membersByRoleByRoleId = new LinkedHashMap<>();

    // Initialize membersByRoleByRoleId with container for all roles to associate members to (MembersByRole)
    final List<Role> roles;
    if (OwnerType.GLOBAL.equals(ownerType)) {
      roles = roleDAO.getGlobalRoles();
    }
    else {
      roles = roleDAO.getApplicationRoles();
    }
    for (final Role role : roles) {
      final MembersByRole membersByRole = new MembersByRole();
      membersByRole.roleId = role.getId();
      membersByRole.roleName = role.getName();
      membersByRole.roleDescription = role.getDescription();
      membersByRoleByRoleId.put(membersByRole.roleId, membersByRole);
    }
    if (OwnerType.GLOBAL.equals(ownerType)) {
      loadMembersByRoleForGlobalContext(roles, membersByRoleByRoleId);
    }
    else {
      loadMembersByRoleForNonGlobalContext(ownerType, internalOwnerId, roles, membersByRoleByRoleId);
    }
    loadMemberDetails(membersByRoleByRoleId);

    if (!SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.isEnabled()) {
      membersByRoleByRoleId.remove(Role.USAGE_VIEWER_ROLE_ID);
    }

    final ApplicableMembershipMappings result = new ApplicableMembershipMappings();
    result.membersByRole.addAll(membersByRoleByRoleId.values());
    result.groupSearchEnabled = !userDirectory.isGroupSearchDisabled();
    return result;
  }

  private void loadMemberDetails(Map<String, MembersByRole> membersByRoleByRoleId) {
    List<Member> members = new ArrayList<>();
    for (MembersByRole membersByRole : membersByRoleByRoleId.values()) {
      for (MembersByOwner membersByOwner : membersByRole.membersByOwner) {
        members.addAll(membersByOwner.members);
      }
    }

    MemberAttributeResolver memberAttributeResolver = new MemberAttributeResolver(userDirectory);
    memberAttributeResolver.resolve(members);
  }

  public ApiRoleMemberMappingListDTO getRoleMembershipsOmitEmpty(OwnerType ownerType, String internalOwnerId) {
    if (internalOwnerId == null) {
      internalOwnerId = getIdGlobalOrRepositoryContainer(ownerType);
    }
    ApplicableMembershipMappings applicableMembershipMappings =
        getApplicableMembershipMappings(ownerType, internalOwnerId);
    ApiRoleMemberMappingListDTO roleMemberMappingList = apiMemberMappingAdapter.convert(applicableMembershipMappings);
    roleMemberMappingList.memberMappings = roleMemberMappingList.memberMappings.stream()
        .filter(dto -> !dto.members.isEmpty())
        .collect(Collectors.toList());
    return roleMemberMappingList;
  }

  public void grantRoleMembershipsForNonGlobalContextNoAuthz(
      final OwnerType ownerType,
      final String internalOwnerId,
      final String roleId,
      final MemberType memberType,
      final Set<String> memberNames) throws SQLException
  {
    validateContextId(ownerType, internalOwnerId);

    Map<String, List<Member>> roleToMembers = new HashMap<>();
    List<Member> members = new ArrayList<>();
    List<MembershipMapping> membershipMappings = new ArrayList<>();

    for (String memberName : memberNames) {
      Member member = new Member();
      member.setInternalName(memberName);
      member.setType(memberType);
      members.add(member);

      membershipMappings.add(new MembershipMapping(internalOwnerId, roleId, memberName, memberType));
    }

    membershipMappingDAO.insertAll(membershipMappings);

    Role role = validateRole(ownerType, roleId);

    AuditData auditData = AuditData.get();
    auditRoleMemberData(auditData, role, members);

    roleToMembers.put(roleId, members);
    managementEventService.postEvent(EventAction.CREATED, roleToMembers, internalOwnerId);
  }

  public void grantRoleMembership(
      OwnerType ownerType,
      String internalOwnerId,
      String roleId,
      MemberType memberType,
      String memberName,
      boolean validateMember)
  {
    if (validateMember) {
      // perform opt-in validation
      Member userMember = new Member(MemberType.USER, memberName, memberName);
      Member groupMember = new Member(MemberType.GROUP, memberName, memberName);
      QueryResult userOrGroup = userDirectory.getMembersByNames(Set.of(userMember, groupMember));

      // Object equality to the passed in members means we didn't find anything and just echoed the args
      List<Member> result = new ArrayList<>(userOrGroup.get());
      result.removeIf(member -> member == userMember || member == groupMember);

      if (result.isEmpty()) {
        throw new NotFoundException("Could not find user or group with name " + memberName);
      }
    }
    grantRoleMembership(ownerType, internalOwnerId, roleId, memberType, memberName);
  }

  /**
   * @since 1.70
   */
  // Authorization is checked in grantRoleMembershipForGlobalContext and grantRoleMembershipForNonGlobalContext
  public void grantRoleMembership(
      OwnerType ownerType,
      String internalOwnerId,
      String roleId,
      MemberType memberType,
      String memberName)
  {
    if (internalOwnerId == null) {
      internalOwnerId = getIdGlobalOrRepositoryContainer(ownerType);
    }
    validateOwner(ownerType, internalOwnerId);

    grantRoleMembershipInternal(ownerType, internalOwnerId, roleId, memberType, memberName);

    // Grant role membership for related organization if applicable
    String relatedOrganizationId = getRelatedOrganizationId(ownerType, internalOwnerId);
    if (relatedOrganizationId != null) {
      grantRoleMembershipInternal(OwnerType.ORGANIZATION, relatedOrganizationId, roleId, memberType, memberName);
    }
  }

  private void grantRoleMembershipInternal(
      OwnerType ownerType,
      String internalOwnerId,
      String roleId,
      MemberType memberType,
      String memberName)
  {
    // Validate member fields first to provide appropriate BadRequestException for invalid input
    Member member = new Member();
    member.setInternalName(memberName);
    member.setType(memberType);
    validateMember(member);

    MembershipMapping existing = membershipMappingDAO.getByContextIdAndRoleIdAndMemberNameAndMemberType(internalOwnerId,
        roleId, memberName, memberType);

    if (existing != null) {
      return; // Already granted
    }

    validateContextId(ownerType, internalOwnerId);
    Role role = validateRole(ownerType, roleId);

    MembershipMapping membershipMapping = new MembershipMapping(internalOwnerId, roleId, memberName, memberType);

    AuditData auditData = AuditData.get();
    auditRoleMemberData(auditData, role, member);

    if (OwnerType.GLOBAL.equals(ownerType)) {
      grantRoleMembershipForGlobalContext(membershipMapping);
    }
    else {
      grantRoleMembershipForNonGlobalContext(ownerType, membershipMapping.getContextId(),
          membershipMapping);
    }

    Map<String, List<Member>> roleToMembers = new HashMap<>();
    roleToMembers.put(membershipMapping.getRoleId(), Arrays.asList(member));
    managementEventService.postEvent(EventAction.CREATED, roleToMembers, membershipMapping.getContextId());
  }

  /**
   * @since 1.70
   */
  // Authorization is checked in revokeRoleMembershipForGlobalContext and revokeRoleMembershipForNonGlobalContext
  public void revokeRoleMembership(
      OwnerType ownerType,
      String internalOwnerId,
      String roleId,
      MemberType memberType,
      String memberName)
  {
    if (internalOwnerId == null) {
      internalOwnerId = getIdGlobalOrRepositoryContainer(ownerType);
    }
    validateOwner(ownerType, internalOwnerId);

    revokeRoleMembershipInternal(ownerType, internalOwnerId, roleId, memberType, memberName);

    // Revoke role membership for related organization if applicable
    String relatedOrganizationId = getRelatedOrganizationId(ownerType, internalOwnerId);
    if (relatedOrganizationId != null) {
      revokeRoleMembershipInternal(OwnerType.ORGANIZATION, relatedOrganizationId, roleId, memberType, memberName);
    }
  }

  private void revokeRoleMembershipInternal(
      OwnerType ownerType,
      String internalOwnerId,
      String roleId,
      MemberType memberType,
      String memberName)
  {
    // validateContextId is not called here: if the membership mapping does not exist, a 404 is returned regardless of
    // owner existence.
    MembershipMapping membershipMapping = membershipMappingDAO
        .getByContextIdAndRoleIdAndMemberNameAndMemberType(internalOwnerId, roleId, memberName, memberType);

    Member member = new Member();
    member.setInternalName(memberName);
    member.setType(memberType);

    AuditData auditData = AuditData.get();
    Role role = validateRole(ownerType, roleId);
    auditRoleMemberData(auditData, role, member);

    if (OwnerType.GLOBAL.equals(ownerType)) {
      revokeRoleMembershipForGlobalContext(membershipMapping);
    }
    else {
      revokeRoleMembershipForNonGlobalContext(ownerType, internalOwnerId, membershipMapping);
    }

    Map<String, List<Member>> roleToMembers = new HashMap<>();
    roleToMembers.put(roleId, Arrays.asList(member));
    managementEventService.postEvent(EventAction.DELETED, roleToMembers, internalOwnerId);
  }

  @Authorize(permission = Permission.READ)
  protected void loadMembersByRoleForNonGlobalContext(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      List<Role> roles,
      Map<String, MembersByRole> membersByRoleByRoleId)
  {
    if (OwnerType.GLOBAL.equals(ownerType)) {
      throw new BadRequestException("The '" + ownerType + "' context is not allowed.");
    }

    for (Owner owner : ownerDAO.walkHierarchy(internalOwnerId)) {
      Map<String, MembersByOwner> membersByOwnerByRoleId =
          loadMembers(owner.getId(), owner.getName(), owner.getType(), roles);
      for (Map.Entry<String, MembersByOwner> entry : membersByOwnerByRoleId.entrySet()) {
        if (OwnerType.APPLICATION.equals(owner.getType())) {
          entry.getValue().ownerId = owner.getPublicId();
        }
        else {
          entry.getValue().ownerId = owner.getId();
        }
        MembersByRole membersByRole = membersByRoleByRoleId.get(entry.getKey());
        if (membersByRole == null) {
          // Custom role not in the initial list - look it up and add to results
          Role role = roleDAO.getById(entry.getKey());
          if (role != null) {
            membersByRole = new MembersByRole();
            membersByRole.roleId = role.getId();
            membersByRole.roleName = role.getName();
            membersByRole.roleDescription = role.getDescription();
            membersByRoleByRoleId.put(membersByRole.roleId, membersByRole);
          }
          else {
            log.debug("Skipping membership mapping for unknown role id: {}", entry.getKey());
          }
        }
        if (membersByRole != null) {
          membersByRole.membersByOwner.add(entry.getValue());
        }
      }
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  protected void loadMembersByRoleForGlobalContext(List<Role> roles, Map<String, MembersByRole> membersByRoleByRoleId) {
    Map<String, MembersByOwner> membersByOwnerByRoleId = loadMembers(MembershipMapping.GLOBAL_CONTEXT_ID,
        MembershipMapping.GLOBAL_CONTEXT_NAME, OwnerType.GLOBAL, roles);
    for (Map.Entry<String, MembersByOwner> entry : membersByOwnerByRoleId.entrySet()) {
      MembersByRole membersByRole = membersByRoleByRoleId.get(entry.getKey());
      if (membersByRole == null) {
        // Custom role not in the initial list - look it up and add to results
        Role role = roleDAO.getById(entry.getKey());
        if (role != null) {
          membersByRole = new MembersByRole();
          membersByRole.roleId = role.getId();
          membersByRole.roleName = role.getName();
          membersByRole.roleDescription = role.getDescription();
          membersByRoleByRoleId.put(membersByRole.roleId, membersByRole);
        }
        else {
          log.debug("Skipping membership mapping for unknown role id: {}", entry.getKey());
        }
      }
      if (membersByRole != null) {
        membersByRole.membersByOwner.add(entry.getValue());
      }
    }
  }

  // Authorization is checked in setMembershipMappingsForGlobalContext and setMembershipMappingsForNonGlobalContext
  public void setMembershipMappings(
      final OwnerType ownerType,
      final String internalOwnerId,
      final Map<String, List<Member>> roleToMembers)
  {
    if (OwnerType.GLOBAL.equals(ownerType)) {
      setMembershipMappingsForGlobalContext(roleToMembers);
    }
    else {
      validateOwner(ownerType, internalOwnerId);
      setMembershipMappingsForNonGlobalContext(ownerType, internalOwnerId, roleToMembers);
      String relatedOrgId = getRelatedOrganizationId(ownerType, internalOwnerId);
      if (relatedOrgId != null) {
        setMembershipMappingsForNonGlobalContext(OwnerType.ORGANIZATION, relatedOrgId, roleToMembers);
      }
    }
  }

  public Set<String> getPermissionsForUserPrincipal(String username, Set<String> userMembership) {
    return membershipMappingDAO.getByUserCaseInsensitiveAndGroups(username, userMembership)
        .stream()
        .map(it -> rolePermissionDAO.getPermissionsForRole(it.getRoleId()))
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .filter(Permission::isVisible)
        .map(Permission::getDisplayName)
        .collect(Collectors.toSet());
  }

  @Authorize(permission = Permission.EDIT_ACCESS_CONTROL)
  protected void setMembershipMappingsForNonGlobalContext(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      Map<String, List<Member>> roleToMembers)
  {
    if (OwnerType.GLOBAL.equals(ownerType)) {
      throw new BadRequestException("The '" + ownerType + "' context is not allowed.");
    }

    setMembershipMappingsForRoles(ownerType, internalOwnerId, roleToMembers);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  protected void setMembershipMappingsForGlobalContext(Map<String, List<Member>> roleToMembers) {
    setMembershipMappingsForRoles(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roleToMembers);
  }

  @Authorize(permission = Permission.EDIT_ACCESS_CONTROL)
  void grantRoleMembershipForNonGlobalContext(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      MembershipMapping membershipMapping)
  {
    membershipMappingDAO.insert(membershipMapping);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  void grantRoleMembershipForGlobalContext(MembershipMapping membershipMapping) {
    membershipMappingDAO.insert(membershipMapping);
  }

  @Authorize(permission = Permission.EDIT_ACCESS_CONTROL)
  void revokeRoleMembershipForNonGlobalContext(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
      MembershipMapping membershipMapping)
  {
    revokeRoleMembership(membershipMapping);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  void revokeRoleMembershipForGlobalContext(MembershipMapping membershipMapping) {
    revokeRoleMembership(membershipMapping);
  }

  private void revokeRoleMembership(MembershipMapping membershipMapping) {
    if (membershipMapping == null) {
      throw new NotFoundException("Role membership not found.");
    }
    membershipMappingDAO.delete(membershipMapping);
  }

  String getIdGlobalOrRepositoryContainer(OwnerType ownerType) {
    if (ownerType == OwnerType.GLOBAL) {
      return MembershipMapping.GLOBAL_CONTEXT_ID;
    }
    else if (ownerType == OwnerType.REPOSITORY_CONTAINER) {
      return RepositoryContainer.REPOSITORY_CONTAINER_ID;
    }
    else {
      throw new UnsupportedOperationException(
          "Only for " + OwnerType.GLOBAL + " and " + OwnerType.REPOSITORY_CONTAINER);
    }
  }

  private void setMembershipMappingsForRoles(
      OwnerType ownerType,
      String internalOwnerId,
      Map<String, List<Member>> roleToMembers)
  {
    try (TransactionContext tx = membershipMappingDAO.createTransactionContext()) {
      tx.begin();

      for (Entry<String, List<Member>> entry : roleToMembers.entrySet()) {
        String roleId = entry.getKey();
        List<Member> members = entry.getValue();
        setMembershipMappingsForRole(tx, ownerType, internalOwnerId, roleId, members);
      }

      tx.commit();
      AuditData.get().commitSubEvents();
      // After successfully committing the subevents, the parent event is not needed so cancel it
      AuditData.get().setEvent(null);
    }

    managementEventService.postEvent(EventAction.UPDATED, roleToMembers, internalOwnerId);
  }

  private void setMembershipMappingsForRole(
      final TransactionContext tx,
      final OwnerType ownerType,
      final String internalOwnerId,
      final String roleId,
      final List<Member> members)
  {
    log.debug("Setting membership mappings for {} id {} and role id {}", ownerType, internalOwnerId, roleId);

    final Role role = validateRole(ownerType, roleId);

    if (members.isEmpty() && isSystemAdminRole(role)) {
      throw new BadRequestException("There must be at least one user in the System Administrator role.");
    }

    validateContextId(ownerType, internalOwnerId);

    final List<MembershipMapping> membershipMappings = new ArrayList<>();
    for (final Member member : members) {
      validateMember(member);
      final MembershipMapping membershipMapping = new MembershipMapping(member.getInternalName(), member.getType());
      membershipMappings.add(membershipMapping);
    }

    auditConfigureRoleMembership(role, members);

    membershipMappingDAO.setMembershipMappingsForContextAndRole(tx, internalOwnerId, roleId, membershipMappings);
  }

  private void auditConfigureRoleMembership(Role role, Collection<Member> members) {
    try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, false)) {
      AuditData.get()
          .setData("roleId", role.getId())
          .setData("roleName", role.getName())
          .setData("roleMembers", MemberDTO.transcribe(members));
    }
  }

  private Role validateRole(final OwnerType ownerType, final String roleId) {
    final Role role = roleDAO.getByIdNotNull(roleId);
    if (!OwnerType.GLOBAL.equals(ownerType) && role.isGlobal()) {
      throw new BadRequestException("Cannot map members to global role in context of " + ownerType + ".");
    }
    if (OwnerType.GLOBAL.equals(ownerType) && !role.isGlobal()) {
      throw new BadRequestException("Cannot map members to application role in global context.");
    }
    return role;
  }

  private boolean isSystemAdminRole(final Role role) {
    return role.isGlobal() && Role.SYSTEM_ADMIN_ROLE_ID.equals(role.getId());
  }

  /**
   * The membership mapping table can't have foreign key constraints so validate the context id is valid.
   */
  private void validateContextId(final OwnerType ownerType, final String internalOwnerId) {
    if (OwnerType.APPLICATION.equals(ownerType)) {
      appDAO.getByIdNotNull(internalOwnerId);
    }
    else if (OwnerType.ORGANIZATION.equals(ownerType)) {
      orgDAO.getByIdNotNull(internalOwnerId);
    }
    else if (OwnerType.REPOSITORY.equals(ownerType)) {
      repositoryDAO.getByIdNotNull(internalOwnerId);
    }
    else if (OwnerType.REPOSITORY_MANAGER.equals(ownerType)) {
      repositoryManagerDAO.getByIdNotNull(internalOwnerId);
    }
  }

  private void validateMember(final Member member) {
    if (member.getInternalName() == null || member.getInternalName().isEmpty()) {
      throw new BadRequestException("Internal name of role member has not been specified");
    }
    if (member.getType() == null) {
      throw new BadRequestException("Type of role member has not been specified");
    }
  }

  private Map<String, MembersByOwner> loadMembers(
      final String ownerId,
      final String ownerName,
      final OwnerType ownerType,
      final List<Role> roles)
  {
    final Map<String, MembersByOwner> membersByOwnerByRoleId = new LinkedHashMap<>();
    for (final MembershipMapping membershipMapping : membershipMappingDAO.getByContextId(ownerId)) {
      MembersByOwner membersByOwner = membersByOwnerByRoleId.get(membershipMapping.getRoleId());
      if (membersByOwner == null) {
        membersByOwner = new MembersByOwner(ownerId, ownerName, ownerType);
        membersByOwnerByRoleId.put(membershipMapping.getRoleId(), membersByOwner);
      }
      final Member member = new Member(membershipMapping.getMemberType(), membershipMapping.getMemberName(),
          membershipMapping.getMemberName());
      membersByOwner.members.add(member);
    }

    // go through and make sure each role contains the owner, even if its empty list
    for (final Role role : roles) {
      MembersByOwner membersByOwner = membersByOwnerByRoleId.get(role.getId());
      if (membersByOwner == null) {
        membersByOwner = new MembersByOwner(ownerId, ownerName, ownerType);
        membersByOwnerByRoleId.put(role.getId(), membersByOwner);
      }
    }

    return membersByOwnerByRoleId;
  }

  private void auditRoleMemberData(AuditData auditData, Role role, Member member) {
    auditData.setData("roleId", role.getId());
    auditData.setData("roleName", role.getName());
    auditData.setData("roleMember", MemberDTO.transcribe(member));
  }

  private void auditRoleMemberData(AuditData auditData, Role role, List<Member> members) {
    auditData.setData("roleId", role.getId());
    auditData.setData("roleName", role.getName());
    auditData.setData("roleMembers", MemberDTO.transcribe(members));
  }

  public void grantMembershipMappingsForGlobalContextNoAuthz(Map<String, List<Member>> roleToMembers) {
    grantMembershipMappingsForRoles(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roleToMembers);
  }

  private void grantMembershipMappingsForRoles(
      OwnerType ownerType,
      String internalOwnerId,
      Map<String, List<Member>> roleToMembers)
  {
    try (TransactionContext tx = membershipMappingDAO.createTransactionContext()) {
      tx.begin();

      for (Entry<String, List<Member>> entry : roleToMembers.entrySet()) {
        String roleId = entry.getKey();
        List<Member> members = entry.getValue();
        grantMembershipMappingsForRole(tx, ownerType, internalOwnerId, roleId, members);
      }

      tx.commit();
      AuditData.get().commitSubEvents();
    }

    managementEventService.postEvent(EventAction.UPDATED, roleToMembers, internalOwnerId);
  }

  private void grantMembershipMappingsForRole(
      final TransactionContext tx,
      final OwnerType ownerType,
      final String internalOwnerId,
      final String roleId,
      final List<Member> members)
  {
    log.debug("Granting membership mappings for {} id {} and role id {}", ownerType, internalOwnerId, roleId);

    final Role role = validateRole(ownerType, roleId);

    validateContextId(ownerType, internalOwnerId);

    for (final Member member : members) {
      validateMember(member);

      final MembershipMapping membershipMapping =
          new MembershipMapping(internalOwnerId, roleId, member.getInternalName(), member.getType());

      grantMembershipMappingsForUser(membershipMapping, tx);
    }

    auditConfigureRoleMembership(role, members);
  }

  private void grantMembershipMappingsForUser(MembershipMapping membershipMapping, TransactionContext tx) {
    MembershipMapping existing =
        membershipMappingDAO.getByContextIdAndRoleIdAndMemberNameAndMemberType(membershipMapping.getContextId(),
            membershipMapping.getRoleId(), membershipMapping.getMemberName(), membershipMapping.getMemberType());

    if (existing != null) {
      return; // Already granted
    }

    membershipMappingDAO.insert(tx, membershipMapping);
  }

  private String getRelatedOrganizationId(OwnerType ownerType, String internalOwnerId) {
    switch (ownerType) {
      case REPOSITORY_CONTAINER:
        return repositoryContainerDAO.getRelatedOrganizationId();
      case REPOSITORY_MANAGER:
        List<Organization> relatedOrgsByManager = orgDAO.getByRelatedRepositoryManagerId(internalOwnerId);
        if (!relatedOrgsByManager.isEmpty()) {
          return relatedOrgsByManager.get(0).getId();
        }
        break;
      case REPOSITORY:
        List<Organization> relatedOrgsByRepo = orgDAO.getByRelatedRepositoryId(internalOwnerId);
        if (!relatedOrgsByRepo.isEmpty()) {
          return relatedOrgsByRepo.get(0).getId();
        }
        break;
      default:
        return null;
    }
    return null;
  }

  private void validateOwner(OwnerType ownerType, String internalOwnerId) {
    if (ownerType == OwnerType.ORGANIZATION) {
      validateRelatedOrganization(orgDAO.getById(internalOwnerId), false);
    }
    else if (ownerType == OwnerType.APPLICATION) {
      Application application = appDAO.getById(internalOwnerId);
      if (application != null &&
          application.getOrganizationId() != null)
      {
        validateRelatedOrganization(orgDAO.getById(application.getOrganizationId()), true);
      }
    }
  }

  private void validateRelatedOrganization(Organization organization, boolean fromApplication) {
    if (organization == null) {
      return;
    }
    if (organization.getId().equals(repositoryContainerDAO.getRelatedOrganizationId())) {
      throw new BadRequestException(
          "Access control is not permitted for an organization related to a repository container.");
    }
    else if (organization.getRelatedRepositoryManagerId() != null) {
      throw new BadRequestException(
          "Access control is not permitted for an organization related to a repository manager.");
    }
    else if (organization.getRelatedRepositoryId() != null) {
      throw new BadRequestException(
          fromApplication
              ? "Access control is not permitted for an application whose parent organization is related to a repository."
              : "Access control is not permitted for an organization related to a repository.");
    }
  }
}
