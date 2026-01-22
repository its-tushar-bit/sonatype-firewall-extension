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

@Named
@Singleton
public class ProductLicenseDAO
    extends AbstractOperationalSqlDAO<ProductLicense>
{
  public static final String SINGLETON_ENTITY_ID = "product-license";

  @Inject
  public ProductLicenseDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public ProductLicense get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  @Override
  public void insert(TransactionContext tx, ProductLicense productLicense) {
    productLicense.setId(SINGLETON_ENTITY_ID);
    super.insert(tx, productLicense);
  }

  @Override
  public void update(TransactionContext tx, ProductLicense productLicense) {
    productLicense.setId(SINGLETON_ENTITY_ID);
    super.update(tx, productLicense);
  }

  public void delete() {
    ProductLicense productLicense = get();
    if (productLicense != null) {
      delete(productLicense);
    }
  }
}
