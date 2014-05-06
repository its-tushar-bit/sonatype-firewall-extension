/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.filter;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.filter.DashboardFilter;

/**
 * @since 1.11.0
 */
public class DashboardFilterDAO
    extends AbstractOperationalSqlDAO<DashboardFilter>
{

  public DashboardFilter getByUsername(EntityManager em, String username) {
    String sQuery = "SELECT entity FROM DashboardFilter entity WHERE entity.username=?1";
    return get(em, sQuery, username);
  }

  public DashboardFilter getByUsername(String username) {
    EntityManager em = createEntityManager();
    try {
      return getByUsername(em, username);
    }
    finally {
      close(em);
    }
  }
}
