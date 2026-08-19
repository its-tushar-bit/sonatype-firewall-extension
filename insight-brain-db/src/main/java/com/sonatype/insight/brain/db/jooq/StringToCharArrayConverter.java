/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.jooq;

import org.jooq.Converter;

public class StringToCharArrayConverter
    implements Converter<String, char[]>
{
  @Override
  public char[] from(final String databaseObject) {
    if (databaseObject == null) {
      return null;
    }
    return databaseObject.toCharArray();
  }

  @Override
  public String to(final char[] userObject) {
    if (userObject == null) {
      return null;
    }
    return new String(userObject);
  }

  @Override
  public Class<String> fromType() {
    return String.class;
  }

  @Override
  public Class<char[]> toType() {
    return char[].class;
  }
}
