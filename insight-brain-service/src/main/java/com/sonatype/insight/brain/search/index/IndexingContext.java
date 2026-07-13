/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.search.ConversionHelper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.document.Document;

public abstract class IndexingContext
{
  private final OwnerDAO ownerDAO;

  private final Map<String, Owner> ownersById = new ConcurrentHashMap<>();

  private final Map<String, String> vulnDescByVulnId = new ConcurrentHashMap<>();

  private final Map<String, String> licenseNameById = new ConcurrentHashMap<>();

  /**
   * Memoized {@code org.getId()} -> its full ancestor-org id chain (incl. self), so the
   * {@code walkHierarchy} DB walk runs at most once per org per indexing run rather than per
   * document (the Label/Policy/Tag paths reindex many docs sharing an org).
   */
  private final Map<String, List<String>> ancestorOrgIdsByOrgId = new ConcurrentHashMap<>();

  /**
   * Per-run dedupe for the orphan-application WARN (see DocumentBuilderHelper). Scoped to this
   * context so it resets each reindex run — a recurring orphan re-WARNs on the next run rather
   * than being suppressed for the JVM lifetime, and it cannot grow unbounded across runs.
   */
  private final Set<String> orphanAppWarnedIds = ConcurrentHashMap.newKeySet();

  /** @return true the first time {@code applicationId} is seen this run (caller should WARN), false thereafter. */
  public boolean shouldWarnOrphanApp(final String applicationId) {
    return orphanAppWarnedIds.add(applicationId);
  }

  private final ConversionHelper conversionHelper;

  public IndexingContext(final OwnerDAO ownerDAO, final ConversionHelper conversionHelper) {
    this.ownerDAO = ownerDAO;
    this.conversionHelper = conversionHelper;
  }

  public Map<String, String> getVulnDescByVulnId() {
    return vulnDescByVulnId;
  }

  public Map<String, String> getLicenseNameById() {
    return licenseNameById;
  }

  public void addOwners(final Collection<? extends Owner> owners) {
    owners.forEach(owner -> ownersById.put(owner.getId(), owner));
  }

  public Owner getOwner(final String id) {
    return ownersById.computeIfAbsent(id, ownerDAO::getById);
  }

  /**
   * The org's ancestor-org id chain ({@code org, parent, ..., root}), computed via
   * {@link OwnerDAO#walkHierarchy(Owner)} once per org and cached for the run. Callers apply their
   * own sentinel filtering; this returns the raw ids.
   */
  public List<String> getAncestorOrgIds(final Organization org) {
    if (org == null) {
      return List.of();
    }
    return ancestorOrgIdsByOrgId.computeIfAbsent(org.getId(), id -> {
      List<String> ids = new java.util.ArrayList<>();
      ownerDAO.walkHierarchy(org).forEach(o -> ids.add(o.getId()));
      return ids;
    });
  }

  public String newQuery(final FieldIdentifier fieldIdentifier, final String fieldValue) {
    return fieldIdentifier.label + ":" + fieldValue;
  }

  public abstract void deleteDocuments(final String query) throws IOException;

  public abstract void addDocuments(final List<Document> documents) throws IOException;

  public void addNonNullDocuments(final List<Document> documents) throws IOException {
    if (CollectionUtils.isEmpty(documents)) {
      return;
    }
    addDocuments(documents.stream().filter(Objects::nonNull).toList());
  }

  public ConversionHelper getConversionHelper() {
    return conversionHelper;
  }
}
