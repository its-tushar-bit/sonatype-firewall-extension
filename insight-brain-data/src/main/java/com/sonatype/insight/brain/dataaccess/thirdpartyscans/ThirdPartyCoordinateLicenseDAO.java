/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Record;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.CoordinateLicense.COORDINATE_LICENSE;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE;

@Named
@Singleton
public class ThirdPartyCoordinateLicenseDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyCoordinateLicense>
{
  @Inject
  public ThirdPartyCoordinateLicenseDAO(final ThirdPartyScansDataStore thirdPartyScansDataStore) {
    super(thirdPartyScansDataStore);
  }

  public List<ThirdPartyCoordinateLicense> getByFileCoordinateId(TransactionContext tx, String coordinateFileId) {
    return tx.dsl()
        .selectFrom(COORDINATE_LICENSE)
        .where(COORDINATE_LICENSE.FILE_COORDINATE_ID.eq(coordinateFileId))
        .fetchInto(ThirdPartyCoordinateLicense.class);
  }

  public int deleteByFileCoordinateId(TransactionContext tx, String fileCoordinateId) {
    return tx.dsl()
        .deleteFrom(COORDINATE_LICENSE)
        .where(COORDINATE_LICENSE.FILE_COORDINATE_ID.eq(fileCoordinateId))
        .execute();
  }

  public List<ThirdPartyCoordinateLicense> getByFileCoordinateId(final String fileCoordinateId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByFileCoordinateId(tx, fileCoordinateId);
    }
  }

  public List<ThirdPartyCoordinateLicense> getByFileCoordinateIds(final Set<String> fileCoordinateIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(COORDINATE_LICENSE)
          .where(COORDINATE_LICENSE.FILE_COORDINATE_ID.in(fileCoordinateIds))
          .fetchInto(ThirdPartyCoordinateLicense.class);
    }
  }

  public ThirdPartyCoordinateLicense getByFileCoordinateIdAndLicenseId(
      final String fileCoordinateId,
      final String licenseId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByFileCoordinateIdAndLicenseId(tx, fileCoordinateId, licenseId);
    }
  }

  public ThirdPartyCoordinateLicense getByFileCoordinateIdAndLicenseId(
      final TransactionContext tx,
      final String fileCoordinateId,
      final String licenseId)
  {
    return tx.dsl()
        .selectFrom(COORDINATE_LICENSE)
        .where(COORDINATE_LICENSE.FILE_COORDINATE_ID.eq(fileCoordinateId)
            .and(DSL.upper(COORDINATE_LICENSE.LICENSE_ID).eq(licenseId.toUpperCase())))
        .fetchOneInto(ThirdPartyCoordinateLicense.class);
  }

  public List<ThirdPartyCoordinateLicense> getByComponentHash(String componentHash) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(COORDINATE_LICENSE.fields())
          .from(COORDINATE_LICENSE)
          .join(FILE_COORDINATE)
          .on(COORDINATE_LICENSE.FILE_COORDINATE_ID.eq(FILE_COORDINATE.FILE_COORDINATE_ID))
          .where(FILE_COORDINATE.HASH.eq(componentHash))
          .fetchInto(ThirdPartyCoordinateLicense.class);
    }
  }

  public List<ThirdPartyCoordinateLicense> getByComponentHashAndThirdPartyFileId(
      String componentHash,
      String thirdPartyFileId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(COORDINATE_LICENSE.fields())
          .from(COORDINATE_LICENSE)
          .join(FILE_COORDINATE)
          .on(COORDINATE_LICENSE.FILE_COORDINATE_ID.eq(FILE_COORDINATE.FILE_COORDINATE_ID))
          .where(FILE_COORDINATE.HASH.eq(componentHash))
          .and(FILE_COORDINATE.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId))
          .fetchInto(ThirdPartyCoordinateLicense.class);
    }
  }

  public Map<String, List<ThirdPartyCoordinateLicense>> getByComponentHashes(Set<String> componentHashes) {
    List<Record> records = getListWithSqlInClause(componentHashes, partition -> {
      try (TransactionContext tx = createTransactionContext()) {
        return tx.dsl()
            .select(COORDINATE_LICENSE.fields())
            .select(FILE_COORDINATE.HASH)
            .from(COORDINATE_LICENSE)
            .join(FILE_COORDINATE)
            .on(COORDINATE_LICENSE.FILE_COORDINATE_ID.eq(FILE_COORDINATE.FILE_COORDINATE_ID))
            .where(FILE_COORDINATE.HASH.in(partition))
            .fetch();
      }
    });
    return records.stream()
        .collect(Collectors.groupingBy(
            r -> r.get(FILE_COORDINATE.HASH),
            Collectors.mapping(r -> r.into(ThirdPartyCoordinateLicense.class), Collectors.toList())));
  }

  public Map<String, List<ThirdPartyCoordinateLicense>> getByComponentHashesAndThirdPartyFileIds(
      Set<String> componentHashes,
      Set<String> thirdPartyFileIds)
  {
    List<Record> records = getListWithSqlInClause(componentHashes,
        hashPartition -> getListWithSqlInClause(thirdPartyFileIds, fileIdPartition -> {
          try (TransactionContext tx = createTransactionContext()) {
            return tx.dsl()
                .select(COORDINATE_LICENSE.fields())
                .select(FILE_COORDINATE.HASH)
                .from(COORDINATE_LICENSE)
                .join(FILE_COORDINATE)
                .on(COORDINATE_LICENSE.FILE_COORDINATE_ID.eq(FILE_COORDINATE.FILE_COORDINATE_ID))
                .where(FILE_COORDINATE.HASH.in(hashPartition))
                .and(FILE_COORDINATE.THIRD_PARTY_FILE_ID.in(fileIdPartition))
                .fetch();
          }
        }));
    return records.stream()
        .collect(Collectors.groupingBy(
            r -> r.get(FILE_COORDINATE.HASH),
            Collectors.mapping(r -> r.into(ThirdPartyCoordinateLicense.class), Collectors.toList())));
  }

  public boolean insertSafely(final TransactionContext tx, final ThirdPartyCoordinateLicense entity) {
    if (getByFileCoordinateIdAndLicenseId(tx, entity.getFileCoordinateId(), entity.getLicenseId()) != null) {
      return false;
    }
    insert(tx, entity);
    return true;
  }

  @Override
  public org.jooq.Table<?> getJooqTable() {
    return COORDINATE_LICENSE;
  }

  @Override
  public Class<ThirdPartyCoordinateLicense> getEntityClass() {
    return ThirdPartyCoordinateLicense.class;
  }
}
