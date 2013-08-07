/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.model.component.MavenCoordinates;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.error.exception.BadRequestException;

import org.codehaus.plexus.util.StringUtils;

/**
 * Associates component hash to a Maven GAV.
 * 
 * @since 1.4.1
 */
@Named
@Path(HashGAVResource.SERVICE_PATH)
public class HashGAVResource
{
  public static final String SERVICE_PATH = "rest/component/identified";

  private SaasClient client;

  @Inject
  public HashGAVResource(SaasClient saasClient) {
    this.client = saasClient;
  }

  /**
   * @since 1.4.1
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HashGAV setHashGAV(HashGAV hashGAV) throws IOException {
    ComponentSummary componentSummary = getComponentSummary(hashGAV.getCoordinates());

    if (componentSummary.isKnown()) {
      throw new BadRequestException("The '" + hashGAV.getGAVECString() + "' coordinates are already in use");
    }

    hashGAV.setId(null);
    new HashGAVDAO().insert(hashGAV);

    ReportResource.flushReportChanges();

    return hashGAV;
  }

  private ComponentSummary getComponentSummary(MavenCoordinates coordinates) throws IOException {
    Map<String, String> queryParams = new LinkedHashMap<String, String>();
    queryParams.put("groupId", coordinates.getGroupId());
    queryParams.put("artifactId", coordinates.getArtifactId());
    queryParams.put("version", coordinates.getVersion());

    // optional fields
    if (StringUtils.isNotBlank(coordinates.getExtension())) {
      queryParams.put("extension", coordinates.getExtension());
    }

    if (StringUtils.isNotBlank(coordinates.getClassifier())) {
      queryParams.put("classifier", coordinates.getClassifier());
    }

    return client.get(ComponentSummary.class, "rest/ide/component", queryParams);
  }
}
