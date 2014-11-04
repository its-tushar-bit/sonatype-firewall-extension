/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.IOException;
import java.util.LinkedHashMap;
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
import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.error.exception.BadRequestException;

import org.codehaus.plexus.util.StringUtils;

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

  @Inject
  public HashComponentIdentifierResource(SaasClient saasClient, HashComponentIdentifierDAO hashComponentIdentifierDAO) {
    this.client = saasClient;
    this.hashComponentIdentifierDAO = hashComponentIdentifierDAO;
  }

  /**
   * @since 1.4.1
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HashComponentIdentifier set(HashComponentIdentifier hashComponentIdentifier) throws IOException {
    ensureUnknownComponent(hashComponentIdentifier);

    hashComponentIdentifier.setId(null);
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);

    ReportService.flushReportChanges();

    return hashComponentIdentifier;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HashComponentIdentifier update(HashComponentIdentifier hashComponentIdentifier) throws IOException {
    ensureUnknownComponent(hashComponentIdentifier);

    HashComponentIdentifier existingHashComponentIdentifier = hashComponentIdentifierDAO
        .getByHash(hashComponentIdentifier.getHash());
    hashComponentIdentifier.setId(existingHashComponentIdentifier.getId());
    hashComponentIdentifierDAO.update(hashComponentIdentifier);

    ReportService.flushReportChanges();

    return hashComponentIdentifier;
  }

  @DELETE
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
      throw new BadRequestException("The '" + hashComponentIdentifier.getComponentIdentifier()
          + "' coordinates are already in use.");
    }
  }

  private ComponentSummary getComponentSummary(ComponentIdentifier componentIdentifier) throws IOException {
    Map<String, String> queryParams = new LinkedHashMap<String, String>();
    switch (componentIdentifier.format) {
      case (ComponentIdentifier.FORMAT_MAVEN):
        queryParams.put("groupId", componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID));
        queryParams.put("artifactId", componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID));
        queryParams.put("version", componentIdentifier.get(ComponentIdentifier.VERSION));
        // optional fields
        if (StringUtils.isNotBlank(componentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION))) {
          queryParams.put("extension", componentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION));
        }
        if (StringUtils.isNotBlank(componentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER))) {
          queryParams.put("classifier", componentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER));
        }
        break;
      case (ComponentIdentifier.FORMAT_NUGET):
        queryParams.put("artifactId", componentIdentifier.get(ComponentIdentifier.NUGET_PACKAGE_ID));
        queryParams.put("version", componentIdentifier.get(ComponentIdentifier.VERSION));
        break;
      default:
        throw new BadRequestException("Unknow component identifier format: " + componentIdentifier.format);
    }

    return client.get(ComponentSummary.class, "rest/ide/component", queryParams);
  }
}
