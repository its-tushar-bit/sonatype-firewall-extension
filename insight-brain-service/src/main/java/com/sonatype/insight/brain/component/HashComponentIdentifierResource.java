/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * Associates component hash to a component identifier.
 * 
 * @since 1.4.1
 */
@Named
@Path(HashComponentIdentifierResource.SERVICE_PATH)
public class HashComponentIdentifierResource
{
  public static final String SERVICE_PATH = "rest/component/identified";

  private final SaasClient client;

  private final HashComponentIdentifierDAO hashComponentIdentifierDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  @Inject
  public HashComponentIdentifierResource(final SaasClient saasClient,
      final HashComponentIdentifierDAO hashComponentIdentifierDAO, final LicenseOverrideDAO licenseOverrideDAO)
  {
    this.client = saasClient;
    this.hashComponentIdentifierDAO = hashComponentIdentifierDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
  }

  /**
   * @since 1.4.1
   */
  @POST
  @Authorize(permission = Permission.CLAIM_COMPONENT)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HashComponentIdentifierDTO set(HashComponentIdentifier hashComponentIdentifier) throws IOException {
    ComponentIdentifierValidator.validate(hashComponentIdentifier.getComponentIdentifier());

    ensureUnknownComponent(hashComponentIdentifier);

    hashComponentIdentifier.setId(null);
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);

    ReportService.flushReportChanges();

    return new HashComponentIdentifierDTO(hashComponentIdentifier,
        ComponentDisplayNameUtil.fromIdentifier(hashComponentIdentifier.getComponentIdentifier()));
  }

  @PUT
  @Authorize(permission = Permission.CLAIM_COMPONENT)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HashComponentIdentifierDTO update(HashComponentIdentifier hashComponentIdentifier) throws IOException {
    ComponentIdentifierValidator.validate(hashComponentIdentifier.getComponentIdentifier());

    ensureUnknownComponent(hashComponentIdentifier);

    HashComponentIdentifier existingHashComponentIdentifier = hashComponentIdentifierDAO
        .getByHashNotNull(hashComponentIdentifier.getHash());

    try (TransactionContext tx = hashComponentIdentifierDAO.createTransactionContext()) {
      tx.begin();
      // update the component identifier
      hashComponentIdentifier.setId(existingHashComponentIdentifier.getId());
      hashComponentIdentifierDAO.update(tx, hashComponentIdentifier);

      // Now get the existing license overrides and update them
      List<LicenseOverride> licenseOverrideList =
          licenseOverrideDAO.getByComponentIdentifier(tx, existingHashComponentIdentifier.getComponentIdentifier());
      for (LicenseOverride licenseOverride : licenseOverrideList) {
        licenseOverride.setComponentIdentifier(hashComponentIdentifier.getComponentIdentifier());
        licenseOverrideDAO.update(tx, licenseOverride);
      }
      tx.commit();
    }

    ReportService.flushReportChanges();

    return new HashComponentIdentifierDTO(hashComponentIdentifier,
        ComponentDisplayNameUtil.fromIdentifier(hashComponentIdentifier.getComponentIdentifier()));
  }

  @DELETE
  @Authorize(permission = Permission.CLAIM_COMPONENT)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{hash}")
  public void delete(@PathParam("hash") String hash) {
    HashComponentIdentifier toDelete = hashComponentIdentifierDAO.getByHash(hash);
    if (toDelete == null) {
      throw new BadRequestException("Unable to find a claimed component with hash: " + hash + ".");
    }

    hashComponentIdentifierDAO.delete(toDelete);
    ReportService.flushReportChanges();
  }

  private void ensureUnknownComponent(final HashComponentIdentifier hashComponentIdentifier) throws IOException {
    ComponentSummary componentSummary = getComponentSummary(hashComponentIdentifier.getComponentIdentifier());

    if (componentSummary.isKnown()) {
      throw new BadRequestException("The '"
          + ComponentDisplayNameUtil.fromIdentifier(hashComponentIdentifier.getComponentIdentifier())
          + "' coordinates are already in use.");
    }
  }

  private ComponentSummary getComponentSummary(ComponentIdentifier componentIdentifier) throws IOException {
    Map<String, String> queryParams = Collections.singletonMap("componentIdentifier",
        ComponentIdentifierAdapter.toJson(componentIdentifier));
    return client.get(ComponentSummary.class, "rest/component/summary", queryParams);
  }
}
