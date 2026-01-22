/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.PasswordHandler;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SourceControlDataService
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlDataService.class);

  private final SourceControlDAO sourceControlDAO;

  private final PasswordHandler passwordHandler;

  @Inject
  public SourceControlDataService(
      SourceControlDAO sourceControlDAO,
      PasswordHandler passwordHandler)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.passwordHandler = passwordHandler;
  }

  public SourceControl getCompositeSourceControlByOwnerDecrypted(final String ownerId) {
    SourceControl sourceControl = sourceControlDAO.buildCompositeSourceControlInApplication(ownerId);
    if (sourceControl != null && StringUtils.isNotEmpty(sourceControl.getToken())) {
      fillWithDecryptedToken(sourceControl);
    }
    return sourceControl;
  }

  public SourceControl getCompositeSourceControlByApplicationId(final String applicationId) {
    return sourceControlDAO.buildCompositeSourceControlForApplicationId(applicationId);
  }

  public void fillWithDecryptedToken(final SourceControl sourceControl) {
    String token = decryptToken(sourceControl.getToken());
    sourceControl.setToken(token);
  }

  public String decryptToken(final String encryptedToken) {
    try {
      return passwordHandler.decryptPassword(encryptedToken);
    }
    catch (IllegalStateException e) {
      log.error("Unable to decrypt SourceControl token", e);
      throw e;
    }
  }
}
