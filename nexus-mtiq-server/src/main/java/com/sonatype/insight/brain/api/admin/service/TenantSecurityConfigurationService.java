/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.admin.dto.SecurityConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class TenantSecurityConfigurationService
{
  private static final Logger log = LoggerFactory.getLogger(TenantSecurityConfigurationService.class);

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  private ApiSamlConfigurationService apiSamlConfigurationService;

  private MembershipMappingService membershipMappingService;

  private RoleDAO roleDAO;

  @Inject
  public TenantSecurityConfigurationService(
      TenantUtil tenantUtil,
      TenantValidator tenantValidator,
      ApiSamlConfigurationService apiSamlConfigurationService,
      MembershipMappingService membershipMappingService,
      RoleDAO roleDAO)
  {
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.apiSamlConfigurationService = apiSamlConfigurationService;
    this.membershipMappingService = membershipMappingService;
    this.roleDAO = roleDAO;
  }

  /**
   * Insert/Updates the SAML configuration for a tenant and grants Admin permissions to given admins emails
   *
   * @param securityConfiguration the security configuration to apply
   */
  public void updateSamlConfigurationAndGrantAdminPermissions(
      final SecurityConfigurationDTO securityConfiguration,
      final String tenantSlug)
  {
    updateSamlConfiguration(securityConfiguration, tenantSlug);

    grantAdminPermissionForAdmins(securityConfiguration.getAdminEmails(), tenantSlug);
  }

  /**
   * Insert/Updates the SAML configuration for a tenant
   *
   * @param securityConfiguration the security configuration to apply
   */
  public void updateSamlConfiguration(
      final SecurityConfigurationDTO securityConfiguration,
      final String tenantSlug)
  {
    validateCurrentTenant(tenantSlug);

    String decodedIdentityProviderXml = decodeIdentityProviderXml(securityConfiguration);

    apiSamlConfigurationService.insertOrUpdateSamlConfigurationNoAuthz(decodedIdentityProviderXml,
        securityConfiguration.getSamlConfiguration());
  }

  /**
   * Grants Admin permissions to given admins emails for a tenant
   *
   * @param admins list of emails we will give Admin access
   */
  public void grantAdminPermissionForAdmins(final List<String> admins, final String tenantSlug) {
    validateCurrentTenant(tenantSlug);

    List<Role> roles = roleDAO.getGlobalRoles();

    List<Member> adminMembers = admins.stream()
        .map(admin -> new Member(MemberType.USER, admin, admin))
        .collect(
            Collectors.toList());

    Map<String, List<Member>> roleToMembers = new HashMap<>();
    for (Role role : roles) {
      roleToMembers.put(role.getId(), adminMembers);
    }

    membershipMappingService.grantMembershipMappingsForGlobalContextNoAuthz(roleToMembers);
  }

  /**
   * Returns the distinct admin emails (global-role {@code USER} members) for a tenant.
   *
   * @param tenantSlug the tenant slug
   * @return the tenant's admin emails (possibly empty)
   */
  public List<String> getAdminEmails(final String tenantSlug) {
    validateCurrentTenant(tenantSlug);

    return membershipMappingService.getAdminEmailsForGlobalContextNoAuthz();
  }

  private void validateCurrentTenant(final String tenantSlug) {
    /*
     * Proper validations for the tenant name were executed as part of the AdminTenantFilter.
     * Here we are just checking we are not using the global tenant
     */
    if (tenantUtil.isGlobalTenant()) {
      throw new BadRequestException("Invalid tenant");
    }

    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.debug("Tenant {} doesn't exist", tenantSlug.replaceAll("[\n\r]", "_"));
      throw new NotFoundException("Tenant doesn't exist");
    }
  }

  private static String decodeIdentityProviderXml(final SecurityConfigurationDTO samlConfiguration) {
    byte[] decodedIdentityProviderXml =
        Base64.getDecoder()
            .decode(samlConfiguration.getBase64IdentityProviderXml()
                .getBytes(
                    StandardCharsets.UTF_8));
    return new String(decodedIdentityProviderXml);
  }
}
