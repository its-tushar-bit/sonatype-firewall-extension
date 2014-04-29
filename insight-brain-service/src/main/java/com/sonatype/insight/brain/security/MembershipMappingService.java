/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.AbstractDAO;
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

  private final LdapManager ldapManager;

  private final OwnerMapper publicMapper = new PublicIdOwnerMapper();

  private final OwnerMapper internalMapper = new InternalIdOwnerMapper();


  @Inject
  public MembershipMappingService(final ApplicationDAO appDAO, OrganizationDAO orgDAO, final RoleDAO roleDAO,
      final MembershipMappingDAO memberMapDAO, final LdapManager ldapManager)
  {
    this.appDAO = appDAO;
    this.orgDAO = orgDAO;
    this.roleDAO = roleDAO;
    this.memberMapDAO = memberMapDAO;
    this.ldapManager = ldapManager;
  }

  @Authorize(permission = Permission.READ)
  public ApplicableMembershipMappings getApplicableMembershipMappingsByInternalId(
      @AuthzContext(AuthzContext.Key.TYPE) final String ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    return getApplicableMembershipMappings(ownerType, ownerId, internalMapper);
  }

  @Authorize(permission = Permission.READ)
  public ApplicableMembershipMappings getApplicableMembershipMappingsByPublicId(
      @AuthzContext(AuthzContext.Key.TYPE) final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId)
  {
    return getApplicableMembershipMappings(ownerType, ownerId, publicMapper);
  }

  private ApplicableMembershipMappings getApplicableMembershipMappings(final String ownerType, final String ownerId,
                                                                       final OwnerMapper ownerMapper)
  {
    log.debug("Getting all applicable membership mappings for {} id {}", ownerType, ownerId);

    final String internalOwnerId = ownerMapper.getInternalOwnerId(ownerType, ownerId);

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
    final MemberAttributeResolver memberAttributeResolver = new MemberAttributeResolver(ldapManager);

    String organizationId = null;
    // Add app members
    switch (ownerType) {
      case IdUtils.TYPE_APPLICATION:
        Application app = appDAO.getByIdNotNull(internalOwnerId);
        for (Map.Entry<String, MembersByOwner> entry : loadMembers(app.getId(), app.getName(), IdUtils.TYPE_APPLICATION,
            memberAttributeResolver, roles).entrySet()) {
          entry.getValue().ownerId = ownerMapper.getExternalId(app);
          membersByRoleByRoleId.get(entry.getKey()).membersByOwner.add(entry.getValue());
        }
        organizationId = app.getOrganizationId();
        break;
      case IdUtils.TYPE_GLOBAL:
        for (Map.Entry<String, MembersByOwner> entry : loadMembers(MembershipMapping.GLOBAL_CONTEXT_ID,
            MembershipMapping.GLOBAL_CONTEXT_NAME, IdUtils.TYPE_GLOBAL, memberAttributeResolver, roles).entrySet()) {
          membersByRoleByRoleId.get(entry.getKey()).membersByOwner.add(entry.getValue());
        }
        break;
      default:
        organizationId = internalOwnerId;
        break;
    }
    // Add org members
    if (organizationId != null) {
      final Organization org = orgDAO.getByIdNotNull(organizationId);
      for (Map.Entry<String, MembersByOwner> entry : loadMembers(org.getId(), org.getName(), IdUtils.TYPE_ORGANIZATION,
          memberAttributeResolver, roles).entrySet()) {
        membersByRoleByRoleId.get(entry.getKey()).membersByOwner.add(entry.getValue());
      }
    }

    final ApplicableMembershipMappings result = new ApplicableMembershipMappings();
    result.membersByRole.addAll(membersByRoleByRoleId.values());
    return result;
  }

  @Authorize(permission = Permission.WRITE)
  public void setMembershipMappingForRolesByInternalId(
      @AuthzContext(AuthzContext.Key.TYPE) final String ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final Map<String, List<Member>> roleToMembers)
  {
    EntityManager em = memberMapDAO.createEntityManager();
    try {
      em.getTransaction().begin();

      for (Entry<String, List<Member>> entry : roleToMembers.entrySet()) {
        String roleId = entry.getKey();
        List<Member> members = entry.getValue();
        setMembershipMappingForRole(em, ownerType, ownerId, roleId, members, internalMapper);
      }

      em.getTransaction().commit();
    } finally {
      AbstractDAO.close(em);
    }
  }

  @Authorize(permission = Permission.WRITE)
  public void setMembershipMappingForRoleByPublicId(
      @AuthzContext(AuthzContext.Key.TYPE) final String ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final String roleId, final List<Member> members)
  {
    EntityManager em = memberMapDAO.createEntityManager();
    try {
      em.getTransaction().begin();

      setMembershipMappingForRole(em, ownerType, ownerId, roleId, members, publicMapper);

      em.getTransaction().commit();
    } finally {
      AbstractDAO.close(em);
    }
  }


  private void setMembershipMappingForRole(final EntityManager entityManager, final String ownerType,
      final String ownerId, final String roleId, final List<Member> members, final OwnerMapper ownerMapper)
  {
    log.debug("Setting membership mappings for {} id {} and role id {}", ownerType, ownerId, roleId);

    final Role role = validateRole(ownerType, roleId);

    if (members.isEmpty() && isAdminRole(role)) {
      throw new BadRequestException("There must be at least one user in the administrator role.");
    }

    final String internalOwnerId = ownerMapper.getInternalOwnerId(ownerType, ownerId);
    validateContextId(ownerType, internalOwnerId);

    final List<MembershipMapping> memberMaps = new ArrayList<>();
    for (final Member member : members) {
      validateMember(member);
      final MembershipMapping memberMap = new MembershipMapping(member.getInternalName(), member.getType());
      memberMaps.add(memberMap);
    }
    memberMapDAO.setMembershipMappingsForContextAndRole(entityManager, internalOwnerId, roleId, memberMaps);
  }

  private Role validateRole(final String ownerType, final String roleId) {
    final Role role = roleDAO.getByIdNotNull(roleId);
    if (!IdUtils.TYPE_GLOBAL.equals(ownerType) && role.isGlobal()) {
      throw new BadRequestException("Cannot map members to global role in context of " + ownerType);
    }
    if (IdUtils.TYPE_GLOBAL.equals(ownerType) && !role.isGlobal()) {
      throw new BadRequestException("Cannot map members to application role in global context");
    }
    return role;
  }

  private boolean isAdminRole(final Role role) {
    return role.isGlobal() && "Administrator".equals(role.getName());
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

  private Map<String, MembersByOwner> loadMembers(final String ownerId, final String ownerName, final String ownerType,
                                                  final MemberAttributeResolver memberAttributeResolver,
                                                  final List<Role> roles)
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

    //go through and make sure each role contains the owner, even if its empty list
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
