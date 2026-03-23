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
import org.jooq.Table;

import static java.util.stream.Collectors.toList;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.HashComponentIdentifier.HASH_COMPONENT_IDENTIFIER;

@Named
@Singleton
public class HashComponentIdentifierDAO
    extends AbstractOperationalSqlDAO<HashComponentIdentifier>
{
  public static final String NOT_FOUND_MESSAGE = "There is no claimed component with hash ";

  @Inject
  public HashComponentIdentifierDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public void insert(final TransactionContext tx, final HashComponentIdentifier entity) {
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

  public HashComponentIdentifier getByHashNotNull(final String hash) {
    HashComponentIdentifier hashComponentIdentifier = getByHash(hash);
    if (hashComponentIdentifier == null) {
      throw new NotFoundException(NOT_FOUND_MESSAGE + hash + ".");
    }
    return hashComponentIdentifier;
  }

  public HashComponentIdentifier getByHash(final String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByHash(tx, hash);
    }
  }

  private HashComponentIdentifier getByHash(final TransactionContext tx, final String hash) {
    // Note that our truncated representation of the hash is what is stored, hence the transformation on the input
    // hash.
    return toEntity(tx.dsl()
        .selectFrom(HASH_COMPONENT_IDENTIFIER)
        .where(HASH_COMPONENT_IDENTIFIER.HASH.eq(HashHelper.truncateHash(hash)))
        .fetchOne());
  }

  private HashComponentIdentifier getByComponentIdentifier(
      final TransactionContext tx,
      final ComponentIdentifier componentIdentifier)
  {
    return toEntity(tx.dsl()
        .selectFrom(HASH_COMPONENT_IDENTIFIER)
        .where(HASH_COMPONENT_IDENTIFIER.COMPONENT_ID_FORMAT.eq(componentIdentifier.getFormat()))
        .and(HASH_COMPONENT_IDENTIFIER.COMPONENT_ID_COORDINATES_JSON.eq(
            ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates())))
        .fetchOne());
  }

  public HashComponentIdentifier getByComponentIdentifier(final ComponentIdentifier componentIdentifier) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByComponentIdentifier(tx, componentIdentifier);
    }
  }

  @Override
  public List<HashComponentIdentifier> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(HASH_COMPONENT_IDENTIFIER)
          .orderBy(HASH_COMPONENT_IDENTIFIER.HASH)
          .fetch()
          .stream()
          .map(this::toEntity)
          .collect(toList());
    }
  }

  public List<HashComponentIdentifier> getByHashes(final List<String> hashes) {
    // Note that our truncated representation of the hash is what is stored, hence the transformation on the input
    // hash.
    List<String> truncatedHashes = hashes.stream().map(HashHelper::truncateHash).collect(toList());

    List<List<String>> partitions = Lists.partition(truncatedHashes, getInOperatorThreshold());

    try (TransactionContext tx = createTransactionContext()) {
      return partitions.stream()
          .map(partition -> tx.dsl()
              .selectFrom(HASH_COMPONENT_IDENTIFIER)
              .where(HASH_COMPONENT_IDENTIFIER.HASH.in(partition))
              .fetch()
              .stream()
              .map(this::toEntity)
              .collect(toList()))
          .flatMap(Collection::stream)
          .collect(toList());
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return HASH_COMPONENT_IDENTIFIER;
  }

  @Override
  public Class<HashComponentIdentifier> getEntityClass() {
    return HashComponentIdentifier.class;
  }
}
