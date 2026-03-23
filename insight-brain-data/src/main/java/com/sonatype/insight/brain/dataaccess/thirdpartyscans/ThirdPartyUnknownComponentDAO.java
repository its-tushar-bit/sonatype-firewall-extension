/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyUnknownComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.ThirdPartyUnknownComponent.THIRD_PARTY_UNKNOWN_COMPONENT;

@Named
@Singleton
public class ThirdPartyUnknownComponentDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyUnknownComponent>
{
  @Inject
  public ThirdPartyUnknownComponentDAO(ThirdPartyScansDataStore thirdPartyScansDataStore) {
    super(thirdPartyScansDataStore);
  }

  public List<ThirdPartyUnknownComponent> getByThirdPartyFileId(String thirdPartyFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(THIRD_PARTY_UNKNOWN_COMPONENT)
          .where(THIRD_PARTY_UNKNOWN_COMPONENT.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId))
          .fetchInto(ThirdPartyUnknownComponent.class);
    }
  }

  public int deleteByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    return tx.dsl()
        .deleteFrom(THIRD_PARTY_UNKNOWN_COMPONENT)
        .where(THIRD_PARTY_UNKNOWN_COMPONENT.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId))
        .execute();
  }

  @Override
  public org.jooq.Table<?> getJooqTable() {
    return THIRD_PARTY_UNKNOWN_COMPONENT;
  }

  @Override
  public Class<ThirdPartyUnknownComponent> getEntityClass() {
    return ThirdPartyUnknownComponent.class;
  }
}
