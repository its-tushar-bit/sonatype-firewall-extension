/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.JsonFileStore;
import com.sonatype.insight.brain.utils.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Reads a merged audit-log feed for an {@link Owner} and every ancestor in its hierarchy. Used by
 * both {@link ReportResource#auditLog} (Application owner) and
 * {@link HostedRepositoryComponentReportResource#auditLog} (HostedRepositoryComponent owner);
 * ancestor traversal is polymorphic via {@link OwnerDAO#walkHierarchy}.
 */
@Named
@Singleton
public class AuditLogReader
{
  private final InsightWork work;

  private final OwnerDAO ownerDAO;

  private final ClusterLockManager clusterLockManager;

  @Inject
  public AuditLogReader(
      final InsightWork work,
      final OwnerDAO ownerDAO,
      final ClusterLockManager clusterLockManager)
  {
    this.work = work;
    this.ownerDAO = ownerDAO;
    this.clusterLockManager = clusterLockManager;
  }

  @Authorize(permission = Permission.READ)
  public Response readAuditLog(
      @AuthzContext(AuthzContext.Key.OWNER) final Owner owner,
      final String path,
      final String encodedKey) throws IOException
  {
    final ContainerNode<?> key = decodeKey(encodedKey);
    final String[] paths = path.split("[+]+");

    ContainerNode<?> mergedFeed = null;
    for (Owner ancestor : ownerDAO.walkHierarchy(owner)) {
      final String ancestorId = ancestor.getId();
      final JsonStore store = new JsonFileStore(work.getAuditDir(ancestorId), ancestorId, clusterLockManager);
      mergedFeed = mergeFeeds(mergedFeed, store.history(key, paths));
    }

    if (mergedFeed != null) {
      return Response.ok(JsonUtils.generate(mergedFeed)).build();
    }
    return Response.ok().build();
  }

  private ContainerNode<?> mergeFeeds(final ContainerNode<?> base, final ContainerNode<?> additional) {
    if (additional == null) {
      return base;
    }
    if (base == null) {
      return additional;
    }
    JsonNode baseNode = base.get("aaData");
    JsonNode additionalNode = additional.get("aaData");
    if (baseNode instanceof ArrayNode baseEntries && additionalNode instanceof ArrayNode additionalEntries) {
      baseEntries.addAll(additionalEntries);
    }
    return base;
  }

  private ContainerNode<?> decodeKey(final String encodedKey) throws IOException {
    if (encodedKey == null) {
      return null;
    }
    ContainerNode<?> decodedKey = JsonUtils.parse(encodedKey.getBytes(StandardCharsets.UTF_8));
    ComponentIdentifierAdapter.replaceGavWithComponentIdentifier((ObjectNode) decodedKey);
    return decodedKey;
  }
}
