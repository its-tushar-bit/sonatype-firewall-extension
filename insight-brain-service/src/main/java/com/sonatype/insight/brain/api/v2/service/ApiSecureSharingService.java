/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.securesharing.ApiSecureSharingApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.securesharing.ApiSecureSharingApplicationListDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.util.CollectionUtils;

@Named
@Singleton
public class ApiSecureSharingService
{
  private static final BiMap<String, Permission> PERMISSION_BY_ALIAS = new ImmutableBiMap.Builder<String, Permission>()
      .put("export", Permission.EXPORT_SBOM)
      .put("import", Permission.IMPORT_SBOM)
      .build();

  private final PermissionService permissionService;

  private final CurrentUser currentUser;

  private final ApplicationDAO applicationDAO;

  @Inject
  public ApiSecureSharingService(
      final PermissionService permissionService,
      final CurrentUser currentUser,
      final ApplicationDAO applicationDAO)
  {
    this.permissionService = permissionService;
    this.currentUser = currentUser;
    this.applicationDAO = applicationDAO;
  }

  public ApiSecureSharingApplicationListDTO getApplicationsWithPermissions(
      final Set<Permission> permissions,
      final int page,
      final int pageSize)
  {
    checkAuthenticated();
    validate(page, pageSize);

    Set<Permission> permissionsWithGlobalOrRootOrgContexts = new HashSet<>();
    Map<Permission, Set<String>> contextIdsByPermission = new HashMap<>();
    Map<Permission, Set<String>> applicationIdsByPermission = new HashMap<>();
    for (Permission permission : PERMISSION_BY_ALIAS.values()) {
      Set<String> contextIds =
          permissionService.getContextIdsForUserWithPermission(currentUser.getUserPrincipal(), permission);
      if (contextIds.contains(MembershipMapping.GLOBAL_CONTEXT_ID) ||
          contextIds.contains(Organization.ROOT_ORGANIZATION_ID)) {
        permissionsWithGlobalOrRootOrgContexts.add(permission);
      }
      else {
        contextIdsByPermission.put(permission, contextIds);
        applicationIdsByPermission.put(permission, applicationDAO.getIdsByAncestorIds(contextIds));
      }
    }

    List<Application> applications;
    long total;
    if (permissions.stream().anyMatch(permissionsWithGlobalOrRootOrgContexts::contains)) {
      applications = applicationDAO.getAll(page, pageSize);
      total = applicationDAO.getCount();
    }
    else {
      Set<String> allContextIdsWithPermission = new HashSet<>();
      contextIdsByPermission.entrySet().stream()
          .filter(entry -> permissions.contains(entry.getKey()))
          .forEach(entry -> allContextIdsWithPermission.addAll(entry.getValue()));
      applications = applicationDAO.getByAncestorIds(allContextIdsWithPermission, page, pageSize);

      Set<String> allApplicationIdsWithPermission = new HashSet<>();
      applicationIdsByPermission.entrySet().stream()
          .filter(entry -> permissions.contains(entry.getKey()))
          .forEach(entry -> allApplicationIdsWithPermission.addAll(entry.getValue()));
      total = allApplicationIdsWithPermission.size();
    }
    return convertToDTO(
        applications,
        permissionsWithGlobalOrRootOrgContexts,
        applicationIdsByPermission,
        total
    );
  }

  private static void checkAuthenticated() {
    Object principal = SecurityUtils.getSubject().getPrincipal();
    if (principal == null) {
      throw new UnauthenticatedException("Anonymous access forbidden");
    }
  }

  public static Set<Permission> resolvePermissions(final Set<String> permissions) {
    if (CollectionUtils.isEmpty(permissions)) {
      return PERMISSION_BY_ALIAS.values();
    }
    Set<Permission> permissionValues = new HashSet<>();
    for (String permission : permissions) {
      Permission permissionValue = PERMISSION_BY_ALIAS.get(permission.toLowerCase(Locale.ROOT));
      if (permissionValue == null) {
        throw new BadRequestException("Unrecognized or unsupported permission '" + permission + "' expected one of '" +
            String.join("', '", PERMISSION_BY_ALIAS.keySet().toArray(new String[0])) + "'.");
      }
      permissionValues.add(permissionValue);
    }
    return permissionValues;
  }

  private static void validate(final int page, final int pageSize) {
    if (page < 1) {
      throw new BadRequestException("page must be at least 1.");
    }
    if (pageSize < 1) {
      throw new BadRequestException("pageSize must be at least 1.");
    }
  }

  private static ApiSecureSharingApplicationListDTO convertToDTO(
      final List<Application> applications,
      final Set<Permission> permissionsWithGlobalOrRootOrgContexts,
      final Map<Permission, Set<String>> applicationIdsByPermission,
      final long total)
  {
    ApiSecureSharingApplicationListDTO dto = new ApiSecureSharingApplicationListDTO();
    SortedSet<Permission> sortedPermissions = new TreeSet<>(PERMISSION_BY_ALIAS.values());
    dto.applications = applications.stream().map(application -> {
      ApiSecureSharingApplicationDTO apiSecureSharingApplicationDTO = new ApiSecureSharingApplicationDTO();
      apiSecureSharingApplicationDTO.id = application.getId();
      apiSecureSharingApplicationDTO.publicId = application.getPublicId();
      apiSecureSharingApplicationDTO.name = application.getName();
      apiSecureSharingApplicationDTO.permissions = new ArrayList<>();
      for (Permission permission : sortedPermissions) {
        if (permissionsWithGlobalOrRootOrgContexts.contains(permission) ||
            applicationIdsByPermission.get(permission).contains(application.getId())) {
          apiSecureSharingApplicationDTO.permissions.add(PERMISSION_BY_ALIAS.inverse().get(permission));
        }
      }
      return apiSecureSharingApplicationDTO;
    }).toList();
    dto.total = total;
    return dto;
  }
}
