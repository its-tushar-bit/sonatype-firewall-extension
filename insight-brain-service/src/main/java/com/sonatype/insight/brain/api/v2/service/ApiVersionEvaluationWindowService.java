/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.ApiVersionEvaluationWindowDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiVersionEvaluationWindowsDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.VersionEvaluationWindowDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.VersionEvaluationWindow;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named
@Singleton
public class ApiVersionEvaluationWindowService
{
  private final VersionEvaluationWindowDAO versionEvaluationWindowDAO;

  @Inject
  public ApiVersionEvaluationWindowService(VersionEvaluationWindowDAO versionEvaluationWindowDAO) {
    this.versionEvaluationWindowDAO = versionEvaluationWindowDAO;
  }

  @Authorize(permission = Permission.READ)
  public ApiVersionEvaluationWindowsDTO getVersionEvaluationWindows(@AuthzContext(Key.OWNER) final Owner owner) {
    return new ApiVersionEvaluationWindowsDTO(versionEvaluationWindowDAO.getByOwnerId(owner.getId()).stream()
        .map(this::toDTO)
        .toList());
  }

  @Authorize(permission = Permission.WRITE)
  public void setVersionEvaluationWindow(
      @AuthzContext(Key.OWNER) final Owner owner,
      final ApiVersionEvaluationWindowDTO dto)
  {
    AuditData.get().setVersionEvaluationWindow(dto.contextId(), dto.maxVersions(), dto.maxAgeInDays());
    try (TransactionContext tx = versionEvaluationWindowDAO.createTransactionContext()) {
      tx.begin();
      VersionEvaluationWindow window =
          versionEvaluationWindowDAO.getByOwnerIdAndContextId(owner.getId(), dto.contextId());

      if (window == null) {
        window = new VersionEvaluationWindow();
        window.setOwnerId(owner.getId());
        window.setContextId(dto.contextId());
      }

      window.setMaxVersions(dto.maxVersions());
      window.setMaxAgeInDays(dto.maxAgeInDays());

      if (window.getId() == null) {
        versionEvaluationWindowDAO.insert(tx, window);
      }
      else {
        versionEvaluationWindowDAO.update(tx, window);
      }

      tx.commit();
    }
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteVersionEvaluationWindows(@AuthzContext(Key.OWNER) final Owner owner, final String contextId) {
    AuditData.get().setVersionEvaluationWindow(contextId, null, null);
    if (contextId == null) {
      versionEvaluationWindowDAO.deleteByOwnerId(owner.getId());
    }
    else {
      versionEvaluationWindowDAO.deleteByOwnerIdAndContextId(owner.getId(), contextId);
    }
  }

  private ApiVersionEvaluationWindowDTO toDTO(final VersionEvaluationWindow window) {
    return new ApiVersionEvaluationWindowDTO(
        window.getContextId(),
        window.getMaxVersions(),
        window.getMaxAgeInDays()
    );
  }
}
