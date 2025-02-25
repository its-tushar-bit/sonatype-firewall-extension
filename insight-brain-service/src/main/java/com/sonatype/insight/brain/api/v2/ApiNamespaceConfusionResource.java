/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.integration.repository.RepositoryService;

import com.codahale.metrics.annotation.Timed;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
@Timed
@Path(ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_PATH)
public class ApiNamespaceConfusionResource
{
  public static final String NAMESPACE_CONFUSION_ROOT = "namespace_confusion";

  public static final String NAMESPACE_CONFUSION_PATH = PublicApiPaths.FIREWALL_RESOURCE_PATH + "/"
      + NAMESPACE_CONFUSION_ROOT + "/{format}";

  protected static final String NAMESPACE_CONFUSION_PREFIX = "nsc_";

  private final RepositoryService repositoryService;

  @Inject
  public ApiNamespaceConfusionResource(final RepositoryService repositoryService) {
    this.repositoryService = checkNotNull(repositoryService);
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.ADD_PROPRIETARY_COMPONENT_NAMES)
  @Timed
  public void addProprietaryComponentNames(
      @PathParam("format") String format,
      List<String> namespaces)
  {
    repositoryService.addProprietaryNamespaceNames(NAMESPACE_CONFUSION_ROOT,
        NAMESPACE_CONFUSION_PREFIX + format, format, namespaces);
  }

  @DELETE
  @Audited(AuditEvent.REMOVE_PROPRIETARY_COMPONENT_NAMES)
  @Timed
  public void removeProprietaryComponentNames(
      @PathParam("format") String format)
  {
    repositoryService.removeProprietaryNamespaceNames(NAMESPACE_CONFUSION_ROOT,
        NAMESPACE_CONFUSION_PREFIX + format);
  }
}
