/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.ProductLicense;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ProductLicense.PRODUCT_LICENSE;

@Named
@Singleton
public class ProductLicenseDAO
    extends AbstractOperationalSqlDAO<ProductLicense>
{
  public static final String SINGLETON_ENTITY_ID = "product-license";

  @Inject
  public ProductLicenseDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public ProductLicense get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  @Override
  public int insert(final TransactionContext tx, final ProductLicense productLicense) {
    productLicense.setId(SINGLETON_ENTITY_ID);
    return super.insert(tx, productLicense);
  }

  @Override
  public void update(final TransactionContext tx, final ProductLicense productLicense) {
    productLicense.setId(SINGLETON_ENTITY_ID);
    // Use upsert semantics to match the old JPA merge() behavior
    // DatabasePreferences.putSpi() calls update() expecting it to create the row if it doesn't exist
    if (getById(tx, SINGLETON_ENTITY_ID) == null) {
      insert(tx, productLicense);
    }
    else {
      super.update(tx, productLicense);
    }
  }

  public void delete() {
    ProductLicense productLicense = get();
    if (productLicense != null) {
      delete(productLicense);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return PRODUCT_LICENSE;
  }

  @Override
  public Class<ProductLicense> getEntityClass() {
    return ProductLicense.class;
  }
}
