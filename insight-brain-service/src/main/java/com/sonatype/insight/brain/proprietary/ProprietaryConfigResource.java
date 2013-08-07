/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.dataaccess.ProprietaryConfigDAO;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.client.utils.AuditUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Path(ProprietaryConfigResource.SERVICE_PATH)
public class ProprietaryConfigResource
{
  public static final String SERVICE_PATH = "rest/config/proprietary";

  private static final Logger log = LoggerFactory.getLogger(PolicyResource.class);

  @Context
  private InsightWork work;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ProprietaryConfig get() {
    return newDAO().get();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public void update(@QueryParam("user") final String user, @QueryParam("where") final String where,
      @Context final HttpServletRequest request, final ProprietaryConfig config)
  {

    log.debug("Received request to update proprietary component configuration");

    newDAO().session(user, AuditUtils.findIP(request), where).update(config);
  }

  private ProprietaryConfigDAO newDAO() {
    return new ProprietaryConfigDAO(work.getDataDir());
  }
}
