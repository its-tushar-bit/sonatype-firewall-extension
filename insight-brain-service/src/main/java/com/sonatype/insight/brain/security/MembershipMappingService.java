/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiMemberMappingAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.AuthzContext.Key;
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

  private final RoleDAO roleDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  private final OwnerDAO ownerDAO;

  private final UserDirectory userDirectory;

  private final ManagementEventService managementEventService;

  private final ApiMemberMappingAdapter apiMemberMappingAdapter;

  @Inject
  public MembershipMappingService(
      final ApplicationDAO appDAO,
      OrganizationDAO orgDAO,
      final RoleDAO roleDAO,
      final MembershipMappingDAO membershipMappingDAO,
      final OwnerDAO ownerDAO,
      UserDirectory userDirectory,
      final ManagementEventService managementEventService,
      ApiMemberMappingAdapter apiMemberMappingAdapter)
  {
    this.appDAO = appDAO;
    this.orgDAO = orgDAO;
    this.roleDAO = roleDAO;
    this.membershipMappingDAO = membershipMappingDAO;
    this.ownerDAO = ownerDAO;
    this.userDirectory = userDirectory;
    this.managementEventService = managementEventService;
    this.apiMemberMappingAdapter = apiMemberMappingAdapter;
  }

  // Authorization is checked in loadMembersByRoleForGlobalContext and loadMembersByRoleForNonGlobalContext
  public ApplicableMembershipMappings getApplicableMembershipMappings(final OwnerType ownerType,
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
    final MemberAttributeResolver memberAttributeResolver = new MemberAttributeResolver(userDirectory);
    if (OwnerType.GLOBAL.equals(ownerType)) {
      loadMembersByRoleForGlobalContext(memberAttributeResolver, roles, membersByRoleByRoleId);
    }
    else {
      loadMembersByRoleForNonGlobalContext(ownerType, internalOwnerId, memberAttributeResolver, roles,
          membersByRoleByRoleId);
    }

    final ApplicableMembershipMappings result = new ApplicableMembershipMappings();
    result.membersByRole.addAll(membersByRoleByRoleId.values());
    result.groupSearchEnabled = !userDirectory.isDynamicGroupSearchDisabled();
    return result;
  }

  public ApiRoleMemberMappingListDTO getRoleMembershipsOmitEmpty(OwnerType ownerType, String internalOwnerId) {
    if (internalOwnerId == null) {
      internalOwnerId = getIdGlobalOrRepositoryContainer(ownerType);
    }
    ApplicableMembershipMappings applicableMembershipMappings =
        getApplicableMembershipMappings(ownerType, internalOwnerId);
    ApiRoleMemberMappingListDTO roleMemberMappingList = apiMemberMappingAdapter.convert(applicableMembershipMappings);
    roleMemberMappingList.memberMappings = roleMemberMappingList.memberMappings.stream()
        .filter(dto -> !dto.members.isEmpty()).collect(Collectors.toList());
    return roleMemberMappingList;
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
    MembershipMapping existing = membershipMappingDAO.getByContextIdAndRoleIdAndMemberNameAndMemberType(internalOwnerId,
        roleId, memberName, memberType);

    if (existing != null) {
      return;  // Already granted
    }

    Member member = new Member();
    member.setInternalName(memberName);
    member.setType(memberType);
    validateMember(member);

    validateContextId(ownerType, internalOwnerId);
    Role role = validateRole(ownerType, roleId);

    MembershipMapping membershipMapping = new MembershipMapping(internalOwnerId, roleId, memberName, memberType);

    AuditData auditData = AuditData.get();
    auditRoleMemberData(auditData, role, member);

    if (OwnerType.GLOBAL.equals(ownerType)) {
      grantRoleMembershipForGlobalContext(membershipMapping);
    }
    else {
      grantRoleMembershipForNonGlobalContext(ownerType, membershipMapping.getContextId(), membershipMapping);
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
  protected void loadMembersByRoleForNonGlobalContext(@AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
                                                      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId,
                                                      MemberAttributeResolver memberAttributeResolver,
                                                      List<Role> roles,
                                                      Map<String, MembersByRole> membersByRoleByRoleId)
  {
    if (OwnerType.GLOBAL.equals(ownerType)) {
      throw new BadRequestException("The '" + ownerType + "' context is not allowed.");
    }

    for (Owner owner : ownerDAO.walkHierarchy(internalOwnerId)) {
      for (Map.Entry<String, MembersByOwner> entry : loadMembers(owner.getId(), owner.getName(), owner.getType(),
          memberAttributeResolver, roles).entrySet()) {
        entry.getValue().ownerId = owner.getPublicId();
        membersByRoleByRoleId.get(entry.getKey()).membersByOwner.add(entry.getValue());
      }
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  protected void loadMembersByRoleForGlobalContext(MemberAttributeResolver memberAttributeResolver,
                                                   List<Role> roles,
                                                   Map<String, MembersByRole> membersByRoleByRoleId)
  {
    for (Map.Entry<String, MembersByOwner> entry : loadMembers(MembershipMapping.GLOBAL_CONTEXT_ID,
        MembershipMapping.GLOBAL_CONTEXT_NAME, OwnerType.GLOBAL, memberAttributeResolver, roles).entrySet()) {
      membersByRoleByRoleId.get(entry.getKey()).membersByOwner.add(entry.getValue());
    }
  }

  // Authorization is checked in setMembershipMappingsForGlobalContext and setMembershipMappingsForNonGlobalContext
  public void setMembershipMappings(final OwnerType ownerType,
                                    final String internalOwnerId,
                                    final Map<String, List<Member>> roleToMembers)
  {
    if (OwnerType.GLOBAL.equals(ownerType)) {
      setMembershipMappingsForGlobalContext(roleToMembers);
    }
    else {
      setMembershipMappingsForNonGlobalContext(ownerType, internalOwnerId, roleToMembers);
    }
  }

  @Authorize(permission = Permission.EDIT_ACCESS_CONTROL)
  protected void setMembershipMappingsForNonGlobalContext(@AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
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

  private void setMembershipMappingsForRoles(OwnerType ownerType,
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

  private void setMembershipMappingsForRole(final TransactionContext tx,
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
      AuditData.get().setData("roleId", role.getId()).setData("roleName", role.getName())
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
  }

  private void validateMember(final Member member) {
    if (member.getInternalName() == null || member.getInternalName().isEmpty()) {
      throw new BadRequestException("Internal name of role member has not been specified");
    }
    if (member.getType() == null) {
      throw new BadRequestException("Type of role member has not been specified");
    }
  }

  private Map<String, MembersByOwner> loadMembers(final String ownerId,
                                                  final String ownerName,
                                                  final OwnerType ownerType,
                                                  final MemberAttributeResolver memberAttributeResolver,
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

      // Fill in display names queried from userDAO and ldap
      memberAttributeResolver.resolve(membersByOwner.members);
    }

    return membersByOwnerByRoleId;
  }

  private void auditRoleMemberData(AuditData auditData, Role role, Member member) {
    auditData.setData("roleId", role.getId());
    auditData.setData("roleName", role.getName());
    auditData.setData("roleMember", MemberDTO.transcribe(member));
  }
}
