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
import java.util.concurrent.ConcurrentHashMap;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.search.ConversionHelper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.document.Document;

public abstract class IndexingContext
{
  private final OwnerDAO ownerDAO;

  private final Map<String, Owner> ownersById = new ConcurrentHashMap<>();

  private final Map<String, String> vulnDescByVulnId = new ConcurrentHashMap<>();

  private final ConversionHelper conversionHelper;

  public IndexingContext(final OwnerDAO ownerDAO, final ConversionHelper conversionHelper) {
    this.ownerDAO = ownerDAO;
    this.conversionHelper = conversionHelper;
  }

  public Map<String, String> getVulnDescByVulnId() {
    return vulnDescByVulnId;
  }

  public void addOwners(final Collection<? extends Owner> owners) {
    owners.forEach(owner -> ownersById.put(owner.getId(), owner));
  }

  public Owner getOwner(final String id) {
    return ownersById.computeIfAbsent(id, ownerDAO::getById);
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
