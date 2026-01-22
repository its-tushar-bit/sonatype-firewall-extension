/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.util.Collection;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.Lists;

import static java.util.stream.Collectors.toList;

@Named
@Singleton
public class HashComponentIdentifierDAO
    extends AbstractOperationalSqlDAO<HashComponentIdentifier>
{
  public static final String NOT_FOUND_MESSAGE = "There is no claimed component with hash ";

  @Inject
  public HashComponentIdentifierDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public HashComponentIdentifier getByHashNotNull(String hash) {
    HashComponentIdentifier hashComponentIdentifier = getByHash(hash);
    if (hashComponentIdentifier == null) {
      throw new NotFoundException(NOT_FOUND_MESSAGE + hash + ".");
    }
    return hashComponentIdentifier;
  }

  public HashComponentIdentifier getByHash(String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByHash(tx, hash);
    }
  }

  private HashComponentIdentifier getByHash(TransactionContext tx, String hash) {
    // Note that our truncated representation of the hash is what is stored, hence the transformation on the input
    // hash.
    String sQuery = "SELECT entity FROM HashComponentIdentifier entity" + //
        " WHERE entity.hash=?1";
    return get(tx, sQuery, HashHelper.truncateHash(hash));
  }

  private HashComponentIdentifier getByComponentIdentifier(TransactionContext tx,
                                                           ComponentIdentifier componentIdentifier)
  {
    String sQuery = "SELECT entity FROM HashComponentIdentifier entity" + //
        " WHERE entity.componentIdFormat=?1 and entity.componentIdCoordinatesJson=?2";
    return get(tx, sQuery, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
  }

  @Override
  public List<HashComponentIdentifier> getAll() {
    String sQuery = "SELECT entity FROM HashComponentIdentifier entity ORDER BY entity.hash";
    return getList(sQuery);
  }

  @Override
  public void insert(TransactionContext tx, HashComponentIdentifier entity) {
    HashComponentIdentifier other = getByHash(tx, entity.getHash());
    if (other != null) {
      throw new BadRequestException("This component is already mapped to '"
          + ComponentDisplayNameUtil.fromIdentifier(other.getComponentIdentifier()) + "'.");
    }
    other = getByComponentIdentifier(tx, entity.getComponentIdentifier());
    if (other != null) {
      throw new BadRequestException("Another component is already mapped to '"
          + ComponentDisplayNameUtil.fromIdentifier(other.getComponentIdentifier()) + "'.");
    }
    super.insert(tx, entity);
  }

  public HashComponentIdentifier getByComponentIdentifier(ComponentIdentifier componentIdentifier) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByComponentIdentifier(tx, componentIdentifier);
    }
  }

  public List<HashComponentIdentifier> getByHashes(List<String> hashes) {
    // Note that our truncated representation of the hash is what is stored, hence the transformation on the input
    // hash.
    List<String> truncatedHashes = hashes.stream().map(HashHelper::truncateHash).collect(toList());

    String sQuery = "SELECT entity FROM HashComponentIdentifier entity" + //
        " WHERE entity.hash IN (?1)";

    List<List<String>> partitions = Lists.partition(truncatedHashes, getInOperatorThreshold());

    return partitions.stream()
        .map(partition -> getList(sQuery, partition))
        .flatMap(Collection::stream)
        .collect(toList());
  }
}
