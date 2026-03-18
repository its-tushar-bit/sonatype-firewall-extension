/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * Associates component hash to a component identifier.
 *
 * @since 1.15.0
 */
@Named
public class HashComponentIdentifierService
{
  private final HdsClient client;

  private final HashComponentIdentifierDAO hashComponentIdentifierDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final CurrentUser currentUser;

  @Inject
  public HashComponentIdentifierService(
      final HdsClient hdsClient,
      final HashComponentIdentifierDAO hashComponentIdentifierDAO,
      final LicenseOverrideDAO licenseOverrideDAO,
      final CurrentUser currentUser)
  {
    this.client = hdsClient;
    this.hashComponentIdentifierDAO = hashComponentIdentifierDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.currentUser = currentUser;
  }

  @Authorize(permission = Permission.CLAIM_COMPONENT)
  public HashComponentIdentifierDTO get(final String hash) {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(hash);

    if (hashComponentIdentifier != null) {
      return new HashComponentIdentifierDTO(hashComponentIdentifier,
          ComponentDisplayNameUtil.fromIdentifier(hashComponentIdentifier.getComponentIdentifier()));
    }
    else {
      throw new NotFoundException("Cannot find component claim for hash " + hash);
    }
  }

  @Authorize(permission = Permission.CLAIM_COMPONENT)
  public HashComponentIdentifierDTO set(final HashComponentIdentifier hashComponentIdentifier) {
    auditHashComponentIdentifier(hashComponentIdentifier);
    ComponentIdentifierValidator.validate(hashComponentIdentifier.getComponentIdentifier());

    ensureUnknownComponent(hashComponentIdentifier);

    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    hashComponentIdentifier.setClaimerId(userPrincipal.getUsername());
    hashComponentIdentifier.setClaimerName(userPrincipal.getDisplayName());

    hashComponentIdentifier.setId(null);
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);

    return new HashComponentIdentifierDTO(hashComponentIdentifier,
        ComponentDisplayNameUtil.fromIdentifier(hashComponentIdentifier.getComponentIdentifier()));
  }

  @Authorize(permission = Permission.CLAIM_COMPONENT)
  public HashComponentIdentifierDTO update(final HashComponentIdentifier hashComponentIdentifier) {
    auditHashComponentIdentifier(hashComponentIdentifier);
    ComponentIdentifierValidator.validate(hashComponentIdentifier.getComponentIdentifier());

    ensureUnknownComponent(hashComponentIdentifier);

    HashComponentIdentifier existingHashComponentIdentifier = hashComponentIdentifierDAO
        .getByHashNotNull(hashComponentIdentifier.getHash());

    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    hashComponentIdentifier.setClaimerId(userPrincipal.getUsername());
    hashComponentIdentifier.setClaimerName(userPrincipal.getDisplayName());

    try (TransactionContext tx = hashComponentIdentifierDAO.createTransactionContext()) {
      tx.begin();
      // update the component identifier
      hashComponentIdentifier.setId(existingHashComponentIdentifier.getId());
      hashComponentIdentifierDAO.update(tx, hashComponentIdentifier);

      // Now get the existing license overrides and update them
      List<LicenseOverride> licenseOverrideList = licenseOverrideDAO.getByComponentIdentifier(tx,
          existingHashComponentIdentifier.getComponentIdentifier());
      for (LicenseOverride licenseOverride : licenseOverrideList) {
        licenseOverride.setComponentIdentifier(hashComponentIdentifier.getComponentIdentifier());
        licenseOverrideDAO.update(tx, licenseOverride);
      }
      tx.commit();
    }

    return new HashComponentIdentifierDTO(hashComponentIdentifier,
        ComponentDisplayNameUtil.fromIdentifier(hashComponentIdentifier.getComponentIdentifier()));
  }

  @Authorize(permission = Permission.CLAIM_COMPONENT)
  public void delete(final String hash) {
    HashComponentIdentifier toDelete = hashComponentIdentifierDAO.getByHash(hash);
    if (toDelete == null) {
      throw new NotFoundException("Cannot find component claim for hash " + hash);
    }
    auditHashComponentIdentifier(toDelete);

    hashComponentIdentifierDAO.delete(toDelete);
  }

  private void ensureUnknownComponent(final HashComponentIdentifier hashComponentIdentifier) {
    ComponentSummary componentSummary = getComponentSummary(hashComponentIdentifier.getComponentIdentifier());

    if (componentSummary.isKnown()) {
      throw new BadRequestException("The '"
          + ComponentDisplayNameUtil.fromIdentifier(hashComponentIdentifier.getComponentIdentifier())
          + "' coordinates are already in use.");
    }
  }

  private ComponentSummary getComponentSummary(final ComponentIdentifier componentIdentifier) {
    Map<String, String> queryParams = Collections.singletonMap("componentIdentifier",
        ComponentIdentifierAdapter.toJson(componentIdentifier));
    return client.get(ComponentSummary.class, "rest/component/summary", queryParams);
  }

  private void auditHashComponentIdentifier(HashComponentIdentifier hashComponentIdentifier) {
    if (hashComponentIdentifier != null) {
      AuditData.get()
          .setComponentHash(hashComponentIdentifier.getHash())
          .setComponentIdentifier(hashComponentIdentifier.getComponentIdentifier())
          .setComment(hashComponentIdentifier.getComment());
    }
  }
}
