/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.db;

public class DatabaseException
    extends RuntimeException
{
  private static final long serialVersionUID = 7153075649179554947L;

  public DatabaseException(Exception cause) {
    super(cause);
  }

  public DatabaseException(String message) {
    super(message);
  }
}
