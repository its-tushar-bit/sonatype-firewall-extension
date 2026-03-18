/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.integration.Goal;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource.FilePathRegex;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource.ProprietaryConfigByOwner;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource.ProprietaryConfigHierarchy;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.scan.archive.CompoundSelector;
import com.sonatype.insight.scan.archive.PathSelector;
import com.sonatype.insight.scan.archive.RegexSelector;
import com.sonatype.insight.scan.archive.Selector;
import com.sonatype.insight.scan.archive.Selector.Selection;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.22.0
 */
@Named
public class ProprietaryConfigService
{
  private final ProprietaryConfigDAO proprietaryConfigDAO;

  private final OwnerDAO ownerDAO;

  private final IdUtils idUtils;

  @Inject
  public ProprietaryConfigService(
      final ProprietaryConfigDAO proprietaryConfigDAO,
      final OwnerDAO ownerDAO,
      final IdUtils idUtils)
  {

    this.proprietaryConfigDAO = proprietaryConfigDAO;
    this.ownerDAO = ownerDAO;
    this.idUtils = idUtils;
  }

  @Authorize(permission = Permission.READ)
  public ProprietaryConfigHierarchy getProprietaryConfigHierarchy(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    ProprietaryConfigHierarchy proprietaryConfigHierarchy = new ProprietaryConfigHierarchy();

    for (Owner owner : ownerDAO.walkHierarchy(internalOwnerId)) {
      ProprietaryConfig proprietaryConfig = proprietaryConfigDAO.getByOwnerId(owner.getId());

      if (proprietaryConfig == null) {
        proprietaryConfig = new ProprietaryConfig(owner.getId(), null, null);
      }

      ProprietaryConfigByOwner proprietaryConfigByOwner = new ProprietaryConfigByOwner(owner.getId(), owner.getName(),
          owner.getType(), proprietaryConfig);
      proprietaryConfigHierarchy.proprietaryConfigByOwners.add(proprietaryConfigByOwner);
    }
    return proprietaryConfigHierarchy;
  }

  @Authorize(permission = Permission.MANAGE_PROPRIETARY)
  public ProprietaryConfig upsertProprietaryConfig(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final ProprietaryConfig proprietaryConfig)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    ProprietaryConfig existingConfigByOwner = proprietaryConfigDAO.getByOwnerId(internalOwnerId);

    proprietaryConfig.setOwnerId(internalOwnerId);

    if (existingConfigByOwner == null) {
      proprietaryConfigDAO.insert(proprietaryConfig);
    }
    else {
      proprietaryConfig.setId(existingConfigByOwner.getId());
      proprietaryConfigDAO.update(proprietaryConfig);
    }

    auditProprietaryConfigUpdates(proprietaryConfig);
    return proprietaryConfig;
  }

  @Authorize(permission = Permission.MANAGE_PROPRIETARY)
  public ProprietaryConfig addFilePathRegexToProprietaryConfig(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final FilePathRegex filePathRegex)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    ProprietaryConfig proprietaryConfig = proprietaryConfigDAO.getByOwnerId(internalOwnerId);
    boolean shouldInsertAsNewConfig = false;

    if (proprietaryConfig == null) {
      proprietaryConfig = new ProprietaryConfig(internalOwnerId, null, null);
      shouldInsertAsNewConfig = true;
    }

    List<String> regexes = proprietaryConfig.getRegexes();

    if (filePathRegex.paths != null) {
      for (String path : filePathRegex.paths) {
        // Need to escape the file paths as to not interpret it as regex operators
        String escapedPath = Pattern.quote(path);
        addIfStringUnique(regexes, escapedPath);
      }
    }

    if (filePathRegex.regex != null) {
      addIfStringUnique(regexes, filePathRegex.regex);
    }

    proprietaryConfig.setRegexes(regexes);

    if (shouldInsertAsNewConfig) {
      proprietaryConfigDAO.insert(proprietaryConfig);
    }
    else {
      proprietaryConfigDAO.update(proprietaryConfig);
    }

    auditProprietaryConfigUpdates(proprietaryConfig);
    return proprietaryConfig;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  com.sonatype.clm.dto.model.ProprietaryConfig getProprietaryConfigForApplicationEvaluator(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    return getProprietaryConfig(OwnerType.APPLICATION, applicationPublicId);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  com.sonatype.clm.dto.model.ProprietaryConfig getProprietaryConfigForComponentEvaluator(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    return getProprietaryConfig(OwnerType.APPLICATION, applicationPublicId);
  }

  /**
   * NOTE: Permissions are NOT checked for this call
   */
  public com.sonatype.clm.dto.model.ProprietaryConfig getProprietaryConfig(OwnerType ownerType, String ownerId) {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    return getProprietaryConfig(internalOwnerId, ownerDAO, proprietaryConfigDAO);
  }

  public com.sonatype.clm.dto.model.ProprietaryConfig getProprietaryConfig(
      String internalOwnerId,
      OwnerDAO ownerDAO,
      ProprietaryConfigDAO proprietaryConfigDAO)
  {
    com.sonatype.clm.dto.model.ProprietaryConfig result = new com.sonatype.clm.dto.model.ProprietaryConfig();
    result.setPackages(new ArrayList<>());
    result.setRegexes(new ArrayList<>());

    for (Owner owner : ownerDAO.walkHierarchy(internalOwnerId)) {
      ProprietaryConfig ownerConfig = proprietaryConfigDAO.getByOwnerId(owner.getId());
      if (ownerConfig != null) {
        result.getPackages().addAll(ownerConfig.getPackages());
        result.getRegexes().addAll(ownerConfig.getRegexes());
      }
    }

    return result;
  }

  public com.sonatype.clm.dto.model.ProprietaryConfig getProprietaryConfig(Goal goal, String applicationPublicId) {
    if (goal == null || StringUtils.isBlank(applicationPublicId)) {
      // to support pre-1.22 clients, should be removed along w/ anonymous access
      // Last versions that use this path:
      // - insight-brain 1.21.0
      // - insight-ci 2.16.0 (maybe later versions too)
      // - clm-bamboo-plugin 1.2.0 (maybe later versions too)
      // - insight-ide 2.10.1.20160404-1434 (maybe later versions too)
      // - clm-maven-plugin 2.5.0
      return getProprietaryConfig(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
    }

    switch (goal) {
      case EVALUATE_APPLICATION:
        return getProprietaryConfigForApplicationEvaluator(applicationPublicId);
      case EVALUATE_COMPONENT:
        return getProprietaryConfigForComponentEvaluator(applicationPublicId);
      default:
        throw new BadRequestException("Proprietary Configuration requested for invalid goal: " + goal);
    }
  }

  private void addIfStringUnique(List<String> list, String s) {
    if (!list.contains(s)) {
      list.add(s);
    }
  }

  private void auditProprietaryConfigUpdates(final ProprietaryConfig proprietaryConfig) {
    AuditData.get()
        .setData("packageMatchers", proprietaryConfig.getPackages())
        .setData("regexMatchers", proprietaryConfig.getRegexes());
  }

  public Predicate<String> createIsProprietary(String internalOwnerId) {
    com.sonatype.clm.dto.model.ProprietaryConfig proprietaryConfig =
        getProprietaryConfig(internalOwnerId, ownerDAO, proprietaryConfigDAO);
    List<Selector> selectors = new ArrayList<>();
    if (!proprietaryConfig.getPackages().isEmpty()) {
      selectors.add(PathSelector.forProprietaryPackages(
          StringUtils.join(proprietaryConfig.getPackages().iterator(),
              com.sonatype.clm.dto.model.ProprietaryConfig.PACKAGE_DELIM)));
    }
    if (!proprietaryConfig.getRegexes().isEmpty()) {
      selectors.add(RegexSelector.forProprietaryRegexes(
          StringUtils.join(proprietaryConfig.getRegexes().iterator(),
              com.sonatype.clm.dto.model.ProprietaryConfig.REGEX_DELIM)));
    }
    if (selectors.isEmpty()) {
      return s -> false;
    }
    Selector compoundSelector = new CompoundSelector(PathSelector.PROPERTY_NAME, selectors.toArray(new Selector[0]));
    return s -> compoundSelector.isSelected(s) == Selection.EXCLUDED;
  }
}
