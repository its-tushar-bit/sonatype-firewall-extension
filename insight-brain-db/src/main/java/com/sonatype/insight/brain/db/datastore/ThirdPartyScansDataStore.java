/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

/**
 * Contract for the third party scans data store.
 */
public interface ThirdPartyScansDataStore
    extends DataStore
{
  String ID = "insight_brain_third_party_scans";

  @Override
  default String getID() {
    return ID;
  }
}
