/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SbomAction;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
public class ApiSbomService
{
  private final ThirdPartySbomMetadataDAO dao;

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final InsightWork insightWork;

  @Inject
  public ApiSbomService(
      final ThirdPartySbomMetadataDAO dao, final ThirdPartyFileDAO thirdPartyFileDAO, final InsightWork insightWork)
  {
    this.dao = dao;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.insightWork = insightWork;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteSbomVersion(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sbomVersion) throws IOException
  {
    final ThirdPartySbomMetadata thirdPartySbomMetadata =
        dao.getByApplicationIdAndSbomVersion(applicationId, sbomVersion);
    if (thirdPartySbomMetadata == null) {
      throw new NotFoundException(
          "Cannot find version " + sbomVersion + " for application with ID " + applicationId + ".");
    }

    AuditData.get().setSbomVersion(thirdPartySbomMetadata, SbomAction.DELETE);

    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      thirdPartyFileDAO.delete(tx, thirdPartySbomMetadata.getThirdPartyFileId());
      Files.delete(new File(insightWork.getSbomDir(applicationId), thirdPartySbomMetadata.getFilename()).toPath());
      tx.commit();
    }
  }
}
