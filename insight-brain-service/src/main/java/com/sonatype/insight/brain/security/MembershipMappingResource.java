/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages role membership mappings for authorization.
 * 
 * @since 1.7
 */
@Named
@Path(MembershipMappingResource.SERVICE_PATH)
public class MembershipMappingResource
{
  public static final String SERVICE_PATH = "rest/membershipMapping/{ownerType: application|organization}/{ownerId}";

  public static final String ROLE_PATH = "role/{roleId}";

  private static final Logger log = LoggerFactory.getLogger(MembershipMappingResource.class);

  private ApplicationDAO appDAO = new ApplicationDAO();

  private OrganizationDAO orgDAO = new OrganizationDAO();

  private RoleDAO roleDAO = new RoleDAO();

  private MembershipMappingDAO memberMapDAO = new MembershipMappingDAO();

  /**
   * Gets the applicable membership mappings for a given application/organization, that is including mappings inherited
   * from parent organizations.
   */
  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  public ApplicableMembershipMappings getApplicableMembershipMappings(@PathParam("ownerType") String ownerType,
      @PathParam("ownerId") String ownerId)
  {
    log.debug("Getting all applicable membership mappings for {} id {}", ownerType, ownerId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);

    Map<String, MembersByRole> membersByRoleByRoleId = new LinkedHashMap<String, MembersByRole>();

    // Initialize membersByRoleByRoleId with container for all roles to associate members to (MembersByRole) 
    for (Role role : roleDAO.getApplicationRoles()) {
      MembersByRole byRole = new MembersByRole();
      byRole.roleId = role.getId();
      byRole.roleName = role.getName();
      byRole.roleDescription = role.getDescription();
      membersByRoleByRoleId.put(byRole.roleId, byRole);
    }
    DisplayNames displayNames = new DisplayNames();

    String organizationId;
    // Add app members
    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      Application app = appDAO.getByIdNotNull(internalOwnerId);
      for (Map.Entry<String, MembersByOwner> entry : loadMembers(app.getId(), app.getName(), IdUtils.TYPE_APPLICATION,
          displayNames).entrySet()) {
        entry.getValue().ownerId = app.getPublicId();
        membersByRoleByRoleId.get(entry.getKey()).membersByOwner.add(entry.getValue());
      }
      organizationId = app.getOrganizationId();
    }
    else {
      organizationId = internalOwnerId;
    }
    // Add org members
    if (organizationId != null) {
      Organization org = orgDAO.getByIdNotNull(organizationId);
      for (Map.Entry<String, MembersByOwner> entry : loadMembers(org.getId(), org.getName(), IdUtils.TYPE_ORGANIZATION,
          displayNames).entrySet()) {
        membersByRoleByRoleId.get(entry.getKey()).membersByOwner.add(entry.getValue());
      }
    }

    ApplicableMembershipMappings result = new ApplicableMembershipMappings();
    result.membersByRole.addAll(membersByRoleByRoleId.values());
    return result;
  }

  private Map<String, MembersByOwner> loadMembers(String ownerId, String ownerName, String ownerType,
      DisplayNames displayNames)
  {
    Map<String, MembersByOwner> byRole = new LinkedHashMap<String, MembersByOwner>();
    for (MembershipMapping memberMap : memberMapDAO.getByContextId(ownerId)) {
      MembersByOwner byOwner = byRole.get(memberMap.getRoleId());
      if (byOwner == null) {
        byOwner = new MembersByOwner(ownerId, ownerName, ownerType);
        byRole.put(memberMap.getRoleId(), byOwner);
      }
      Member member = new Member(memberMap.getMemberType(), memberMap.getMemberName(), memberMap.getMemberName());
      if (MemberType.USER.equals(member.type)) {
        member.displayName = displayNames.get(member.internalName);
      }
      byOwner.members.add(member);
    }
    
    //go through and make sure each role contains the owner, even if its empty list
    for ( Role role : roleDAO.getApplicationRoles() ) {
      MembersByOwner byOwner = byRole.get(role.getId());
      if (byOwner == null) {
        byOwner = new MembersByOwner(ownerId, ownerName, ownerType);
        byRole.put(role.getId(), byOwner);
      }
    }
    
    return byRole;
  }

  /**
   * Updates the membership mapping for a given application/organization and role to the given members.
   */
  @PUT
  @Path(ROLE_PATH)
  @Consumes({ MediaType.APPLICATION_JSON })
  public void setMembershipMappingForRole(@PathParam("ownerType") String ownerType,
      @PathParam("ownerId") String ownerId, @PathParam("roleId") String roleId, List<Member> members)
  {
    log.debug("Setting membership mappings for {} id {} and role id {}", ownerType, ownerId, roleId);

    validateRole(ownerType, roleId);

    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    validateContextId(ownerType, internalOwnerId);

    List<MembershipMapping> memberMaps = new ArrayList<MembershipMapping>();
    for (Member member : members) {
      validateMember(member);
      MembershipMapping memberMap = new MembershipMapping(member.internalName, member.type);
      memberMaps.add(memberMap);
    }
    memberMapDAO.setMembershipMappingsForContextAndRole(internalOwnerId, roleId, memberMaps);
  }

  public static class ApplicableMembershipMappings
  {
    public List<MembersByRole> membersByRole = new ArrayList<MembersByRole>();
  }

  public static class MembersByRole
  {
    public String roleId;

    public String roleName;

    public String roleDescription;

    public List<MembersByOwner> membersByOwner = new ArrayList<MembersByOwner>();

    public MembersByRole() {
    }

    public MembersByRole(String roleId, String roleName, String roleDescription) {
      this.roleId = roleId;
      this.roleName = roleName;
      this.roleDescription = roleDescription;
    }
  }

  public static class MembersByOwner
  {
    public String ownerId;

    public String ownerName;

    public String ownerType;

    public List<Member> members = new ArrayList<Member>();

    public MembersByOwner() {
    }

    public MembersByOwner(String ownerId, String ownerName, String ownerType) {
      this.ownerId = ownerId;
      this.ownerName = ownerName;
      this.ownerType = ownerType;
    }
  }

  public static class Member
  {
    public MemberType type;

    public String internalName;

    public String displayName;

    public Member() {
    }

    public Member(MemberType type, String internalName, String displayName) {
      this.type = type;
      this.internalName = internalName;
      this.displayName = displayName;
    }
  }

  private static class DisplayNames
  {
    private final Map<String, String> resolvedNames = new HashMap<String, String>();

    private final UserDAO userDAO = new UserDAO();

    public String get(String internalName) {
      String displayName = resolvedNames.get(internalName);
      if (displayName == null) {
        User user = userDAO.getByUsernameLowercase(internalName.toLowerCase(Locale.ENGLISH));
        if (user != null) {
          displayName = user.getFirstName() + " " + user.getLastName();
        }
        else {
          displayName = internalName;
        }
        resolvedNames.put(internalName, displayName);
      }
      return displayName;
    }
  }

  private void validateRole(String ownerType, String roleId) {
    Role role = roleDAO.getByIdNotNull(roleId);
    if (role.isGlobal()) {
      throw new BadRequestException("Cannot map members to global role in context of " + ownerType);
    }
  }

  /**
   * The membership mapping table can't have foreign key constraints so validate the context id is valid.
   */
  private void validateContextId(String ownerType, String internalOwnerId) {
    if (IdUtils.TYPE_APPLICATION.equals(ownerType)) {
      appDAO.getByIdNotNull(internalOwnerId);
    }
    else {
      orgDAO.getByIdNotNull(internalOwnerId);
    }
  }

  private void validateMember(Member member) {
    if (member.internalName == null || member.internalName.isEmpty()) {
      throw new BadRequestException("Internal name of role member has not been specified");
    }
    if (member.type == null) {
      throw new BadRequestException("Type of role member has not been specified");
    }
  }
}
