/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

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

  private final MembershipMappingDAO memberMapDAO;

  private final OwnerDAO ownerDAO;

  private final UserDirectory userDirectory;

  @Inject
  public MembershipMappingService(final ApplicationDAO appDAO, OrganizationDAO orgDAO, final RoleDAO roleDAO,
      final MembershipMappingDAO memberMapDAO, final OwnerDAO ownerDAO, UserDirectory userDirectory)
  {
    this.appDAO = appDAO;
    this.orgDAO = orgDAO;
    this.roleDAO = roleDAO;
    this.memberMapDAO = memberMapDAO;
    this.ownerDAO = ownerDAO;
    this.userDirectory = userDirectory;
  }

  // Authorization is checked in loadMembersByRoleForGlobalContext and loadMembersByRoleForNonGlobalContext
  public ApplicableMembershipMappings getApplicableMembershipMappings(final String ownerType,
      final String internalOwnerId)
  {
    log.debug("Getting all applicable membership mappings for {} id {}", ownerType, internalOwnerId);

    final Map<String, MembersByRole> membersByRoleByRoleId = new LinkedHashMap<>();

    // Initialize membersByRoleByRoleId with container for all roles to associate members to (MembersByRole)
    final List<Role> roles;
    if (IdUtils.TYPE_GLOBAL.equals(ownerType)) {
      roles = roleDAO.getGlobalRoles();
    }
    else {
      roles = roleDAO.getApplicationRoles();
    }
    for (final Role role : roles) {
      final MembersByRole byRole = new MembersByRole();
      byRole.roleId = role.getId();
      byRole.roleName = role.getName();
      byRole.roleDescription = role.getDescription();
      membersByRoleByRoleId.put(byRole.roleId, byRole);
    }
    final MemberAttributeResolver memberAttributeResolver = new MemberAttributeResolver(userDirectory);
    if (IdUtils.TYPE_GLOBAL.equals(ownerType)) {
      loadMembersByRoleForGlobalContext(memberAttributeResolver, roles, membersByRoleByRoleId);
    }
    else {
      loadMembersByRoleForNonGlobalContext(ownerType, internalOwnerId, memberAttributeResolver, roles,
          membersByRoleByRoleId);
    }

    final ApplicableMembershipMappings result = new ApplicableMembershipMappings();
    result.membersByRole.addAll(membersByRoleByRoleId.values());
    result.groupSearchEnabled = !userDirectory.isDynamicGroupSearchDisabled();
    result.ldapRealm = userDirectory.getGroupRealm();
    return result;
  }

  @Authorize(permission = Permission.READ)
  protected void loadMembersByRoleForNonGlobalContext(@AuthzContext(AuthzContext.Key.TYPE) String ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId, MemberAttributeResolver memberAttributeResolver,
      List<Role> roles, Map<String, MembersByRole> membersByRoleByRoleId)
  {
    if (IdUtils.TYPE_GLOBAL.equals(ownerType)) {
      throw new BadRequestException("The '" + ownerType + "' context is not allowed.");
    }

    while (internalOwnerId != null) {
      Owner owner = ownerDAO.getById(internalOwnerId);
      for (Map.Entry<String, MembersByOwner> entry : loadMembers(owner.getId(), owner.getName(), owner.getType(),
          memberAttributeResolver, roles).entrySet()) {
        entry.getValue().ownerId = owner.getPublicId();
        membersByRoleByRoleId.get(entry.getKey()).membersByOwner.add(entry.getValue());
      }
      internalOwnerId = owner.getParentOrganizationId();
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  protected void loadMembersByRoleForGlobalContext(MemberAttributeResolver memberAttributeResolver, List<Role> roles,
      Map<String, MembersByRole> membersByRoleByRoleId)
  {
    for (Map.Entry<String, MembersByOwner> entry : loadMembers(MembershipMapping.GLOBAL_CONTEXT_ID,
        MembershipMapping.GLOBAL_CONTEXT_NAME, OwnerType.GLOBAL, memberAttributeResolver, roles).entrySet()) {
      membersByRoleByRoleId.get(entry.getKey()).membersByOwner.add(entry.getValue());
    }
  }

  // Authorization is checked in setMembershipMappingsForGlobalContext and setMembershipMappingsForNonGlobalContext
  public void setMembershipMappings(final String ownerType, final String internalOwnerId,
      final Map<String, List<Member>> roleToMembers)
  {
    if (IdUtils.TYPE_GLOBAL.equals(ownerType)) {
      setMembershipMappingsForGlobalContext(roleToMembers);
    }
    else {
      setMembershipMappingsForNonGlobalContext(ownerType, internalOwnerId, roleToMembers);
    }
  }

  @Authorize(permission = Permission.WRITE)
  protected void setMembershipMappingsForNonGlobalContext(@AuthzContext(AuthzContext.Key.TYPE) String ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId, Map<String, List<Member>> roleToMembers)
  {
    if (IdUtils.TYPE_GLOBAL.equals(ownerType)) {
      throw new BadRequestException("The '" + ownerType + "' context is not allowed.");
    }

    setMembershipMappingsForRoles(ownerType, internalOwnerId, roleToMembers);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  protected void setMembershipMappingsForGlobalContext(Map<String, List<Member>> roleToMembers) {
    setMembershipMappingsForRoles(IdUtils.TYPE_GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, roleToMembers);
  }

  private void setMembershipMappingsForRoles(String ownerType, String internalOwnerId,
      Map<String, List<Member>> roleToMembers)
  {
    try (TransactionContext tx = memberMapDAO.createTransactionContext()) {
      tx.begin();

      for (Entry<String, List<Member>> entry : roleToMembers.entrySet()) {
        String roleId = entry.getKey();
        List<Member> members = entry.getValue();
        setMembershipMappingsForRole(tx, ownerType, internalOwnerId, roleId, members);
      }

      tx.commit();
    }
  }

  private void setMembershipMappingsForRole(final TransactionContext tx, final String ownerType,
      final String internalOwnerId, final String roleId, final List<Member> members)
  {
    log.debug("Setting membership mappings for {} id {} and role id {}", ownerType, internalOwnerId, roleId);

    final Role role = validateRole(ownerType, roleId);

    if (members.isEmpty() && isSystemAdminRole(role)) {
      throw new BadRequestException("There must be at least one user in the System Administrator role.");
    }

    validateContextId(ownerType, internalOwnerId);

    final List<MembershipMapping> memberMaps = new ArrayList<>();
    for (final Member member : members) {
      validateMember(member);
      final MembershipMapping memberMap = new MembershipMapping(member.getInternalName(), member.getType());
      memberMaps.add(memberMap);
    }
    memberMapDAO.setMembershipMappingsForContextAndRole(tx, internalOwnerId, roleId, memberMaps);
  }

  private Role validateRole(final String ownerType, final String roleId) {
    final Role role = roleDAO.getByIdNotNull(roleId);
    if (!IdUtils.TYPE_GLOBAL.equals(ownerType) && role.isGlobal()) {
      throw new BadRequestException("Cannot map members to global role in context of " + ownerType + ".");
    }
    if (IdUtils.TYPE_GLOBAL.equals(ownerType) && !role.isGlobal()) {
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
  private void validateContextId(final String ownerType, final String internalOwnerId) {
    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      appDAO.getByIdNotNull(internalOwnerId);
    }
    else if (IdUtils.TYPE_ORGANIZATION.equals(ownerType)) {
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

  private Map<String, MembersByOwner> loadMembers(final String ownerId, final String ownerName,
      final OwnerType ownerType, final MemberAttributeResolver memberAttributeResolver, final List<Role> roles)
  {
    final Map<String, MembersByOwner> byRole = new LinkedHashMap<>();
    for (final MembershipMapping memberMap : memberMapDAO.getByContextId(ownerId)) {
      MembersByOwner byOwner = byRole.get(memberMap.getRoleId());
      if (byOwner == null) {
        byOwner = new MembersByOwner(ownerId, ownerName, ownerType);
        byRole.put(memberMap.getRoleId(), byOwner);
      }
      final Member member = new Member(memberMap.getMemberType(), memberMap.getMemberName(), memberMap.getMemberName());
      byOwner.members.add(member);
    }

    // go through and make sure each role contains the owner, even if its empty list
    for (final Role role : roles) {
      MembersByOwner byOwner = byRole.get(role.getId());
      if (byOwner == null) {
        byOwner = new MembersByOwner(ownerId, ownerName, ownerType);
        byRole.put(role.getId(), byOwner);
      }

      // Fill in display names queried from userDAO and ldap
      memberAttributeResolver.resolve(byOwner.members);
    }

    return byRole;
  }
}
