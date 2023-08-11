/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.cache;

import org.apache.openjpa.datacache.ConcurrentQueryCache;
import org.apache.openjpa.datacache.QueryKey;
import org.apache.openjpa.datacache.QueryResult;
import org.apache.openjpa.persistence.DataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiTenantQueryCache
    extends ConcurrentQueryCache
{
  private static final Logger logger = LoggerFactory.getLogger(MultiTenantQueryCache.class);

  @Override
  public QueryResult put(final QueryKey queryKey, final QueryResult oids) {
    try {
      Class<?> aClass = Class.forName(queryKey.getCandidateTypeName());
      DataCache annotation = aClass.getAnnotation(DataCache.class);
      if (annotation != null && annotation.enabled()) {
        return super.put(queryKey, oids);
      }
      return null;
    }
    catch (ClassNotFoundException e) {
      logger.error("Unable to determine class", e);
      return null;
    }
  }
}
