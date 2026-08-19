/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.api.admin.dto.SecurityConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantSecurityConfigurationServiceTest
    extends AbstractMultiTenantTest
{
  private static final String IDENTITY_PROVIDER_XML = "<xml>IdP Metadata<xml>";

  @Mock
  private TenantValidator tenantValidator;

  @Mock
  private ApiSamlConfigurationService apiSamlConfigurationService;

  @Mock
  private MembershipMappingService membershipMappingService;

  @Mock
  private RoleDAO roleDAO;

  @Captor
  ArgumentCaptor<Map<String, List<Member>>> roleToMembersCaptor;

  private SecurityConfigurationDTO securityConfiguration;

  private List<Role> globalRoles;

  private TenantUtil tenantUtil;

  private TenantSecurityConfigurationService underTest;

  @BeforeEach
  public void setup() {
    tenantUtil = new TenantUtil();
    underTest = new TenantSecurityConfigurationService(tenantUtil, tenantValidator, apiSamlConfigurationService,
        membershipMappingService, roleDAO);

    securityConfiguration = new SecurityConfigurationDTO();
    securityConfiguration.setBase64IdentityProviderXml(getEncodedIdPMetadata(IDENTITY_PROVIDER_XML));
    securityConfiguration.setSamlConfiguration(new ApiSamlConfigurationDTO());
    securityConfiguration.setAdminEmails(Arrays.asList("admin@local.com"));

    Role globalRole1 = new Role();
    globalRole1.setId("global-role-1");
    Role globalRole2 = new Role();
    globalRole2.setId("global-role-2");

    globalRoles = Arrays.asList(globalRole1, globalRole2);
  }

  @Test
  public void shouldUpdateSamlConfigurationAndGrantAdminPermissions() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(roleDAO.getGlobalRoles()).thenReturn(globalRoles);

      underTest.updateSamlConfigurationAndGrantAdminPermissions(securityConfiguration, tenant.tenantSlug);

      verify(apiSamlConfigurationService).insertOrUpdateSamlConfigurationNoAuthz(IDENTITY_PROVIDER_XML,
          securityConfiguration.getSamlConfiguration());
      verify(roleDAO).getGlobalRoles();
      verify(membershipMappingService).grantMembershipMappingsForGlobalContextNoAuthz(roleToMembersCaptor.capture());

      assertRolesToMembersMappingIsTheExpected();
    });
  }

  @Test
  public void updateSamlConfigurationAndGrantAdminPermissions_shouldThrowRuntimeException_whenTenantDoesntExist() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      assertThatThrownBy(
          () -> underTest.updateSamlConfigurationAndGrantAdminPermissions(securityConfiguration, tenant.tenantSlug))
              .withFailMessage("Tenant doesn't exist")
              .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void updateSamlConfigurationAndGrantAdminPermissions_shouldThrowRuntimeException_whenUsingGlobalTenant() {
    testAsGlobalTenant(tenant -> {
      assertThatThrownBy(
          () -> underTest.updateSamlConfigurationAndGrantAdminPermissions(securityConfiguration, tenant.tenantSlug))
              .withFailMessage("Invalid tenant")
              .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void shouldUpdateSamlConfiguration() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      underTest.updateSamlConfiguration(securityConfiguration, tenant.tenantSlug);

      verify(apiSamlConfigurationService).insertOrUpdateSamlConfigurationNoAuthz(IDENTITY_PROVIDER_XML,
          securityConfiguration.getSamlConfiguration());
      verify(roleDAO, never()).getGlobalRoles();
      verify(membershipMappingService, never()).grantMembershipMappingsForGlobalContextNoAuthz(
          roleToMembersCaptor.capture());
    });
  }

  @Test
  public void updateSamlConfiguration_shouldThrowRuntimeException_whenTenantDoesntExist() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      assertThatThrownBy(
          () -> underTest.updateSamlConfiguration(securityConfiguration, tenant.tenantSlug))
              .withFailMessage("Tenant doesn't exist")
              .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void updateSamlConfiguration_shouldThrowRuntimeException_whenUsingGlobalTenant() {
    testAsGlobalTenant(tenant -> {
      assertThatThrownBy(
          () -> underTest.updateSamlConfiguration(securityConfiguration, tenant.tenantSlug))
              .withFailMessage("Invalid tenant")
              .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void shouldGrantAdminPermissionsForAdmins() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(roleDAO.getGlobalRoles()).thenReturn(globalRoles);

      underTest.grantAdminPermissionForAdmins(securityConfiguration.getAdminEmails(), tenant.tenantSlug);

      verify(roleDAO).getGlobalRoles();
      verify(membershipMappingService).grantMembershipMappingsForGlobalContextNoAuthz(roleToMembersCaptor.capture());
      verify(apiSamlConfigurationService, never()).insertOrUpdateSamlConfigurationNoAuthz(IDENTITY_PROVIDER_XML,
          securityConfiguration.getSamlConfiguration());

      assertRolesToMembersMappingIsTheExpected();
    });
  }

  @Test
  public void grantAdminPermissionsForAdmins_shouldThrowRuntimeException_whenTenantDoesntExist() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      assertThatThrownBy(
          () -> underTest.grantAdminPermissionForAdmins(securityConfiguration.getAdminEmails(), tenant.tenantSlug))
              .withFailMessage("Tenant doesn't exist")
              .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void grantAdminPermissionsForAdmins_shouldThrowRuntimeException_whenUsingGlobalTenant() {
    testAsGlobalTenant(tenant -> {
      assertThatThrownBy(
          () -> underTest.grantAdminPermissionForAdmins(securityConfiguration.getAdminEmails(), tenant.tenantSlug))
              .withFailMessage("Invalid tenant")
              .isInstanceOf(BadRequestException.class);
    });
  }

  @Test
  public void shouldGetAdminEmails() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(membershipMappingService.getAdminEmailsForGlobalContextNoAuthz())
          .thenReturn(Arrays.asList("admin@local.com"));

      List<String> emails = underTest.getAdminEmails(tenant.tenantSlug);

      assertThat(emails).containsExactly("admin@local.com");
      verify(membershipMappingService).getAdminEmailsForGlobalContextNoAuthz();
    });
  }

  @Test
  public void getAdminEmails_shouldReturnEmptyList_whenNoAdminsConfigured() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      when(membershipMappingService.getAdminEmailsForGlobalContextNoAuthz()).thenReturn(List.of());

      List<String> emails = underTest.getAdminEmails(tenant.tenantSlug);

      assertThat(emails).isEmpty();
      verify(membershipMappingService).getAdminEmailsForGlobalContextNoAuthz();
    });
  }

  @Test
  public void getAdminEmails_shouldThrowNotFound_whenTenantDoesntExist() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      assertThatThrownBy(() -> underTest.getAdminEmails(tenant.tenantSlug))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("Tenant doesn't exist");

      verify(membershipMappingService, never()).getAdminEmailsForGlobalContextNoAuthz();
    });
  }

  @Test
  public void getAdminEmails_shouldThrowBadRequest_whenUsingGlobalTenant() {
    testAsGlobalTenant(tenant -> {
      assertThatThrownBy(() -> underTest.getAdminEmails(tenant.tenantSlug))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Invalid tenant");
    });
  }

  private String getEncodedIdPMetadata(String identityProviderXml) {
    return Base64.getEncoder().encodeToString(identityProviderXml.getBytes(StandardCharsets.UTF_8));
  }

  private void assertRolesToMembersMappingIsTheExpected() {
    Map<String, List<Member>> roleToMembers = roleToMembersCaptor.getValue();

    List<Member> members = roleToMembers.values().stream().flatMap(List::stream).collect(Collectors.toList());
    Set<String> keys = roleToMembers.keySet();

    assertThat(keys.stream()).contains("global-role-1", "global-role-2");
    assertThat(members.stream().map(Member::getInternalName)).containsExactly("admin@local.com", "admin@local.com");
  }
}
