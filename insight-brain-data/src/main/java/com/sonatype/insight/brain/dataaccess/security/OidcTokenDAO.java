/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.OidcToken;

@Named
@Singleton
public class OidcTokenDAO
    extends AbstractOperationalSqlDAO<OidcToken>
{
  @Inject
  public OidcTokenDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public void cleanUpOidcTokens() {
    String sQuery = "DELETE FROM OidcToken entity WHERE entity.registrationTime < ?1";
    Instant fiveMinutesAgo = Instant.now().minus(Duration.ofMinutes(5));
    createQuery(sQuery, Date.from(fiveMinutesAgo)).executeUpdate();
  }

  public void deleteById(String id) {
    String sQuery = "DELETE FROM OidcToken entity WHERE entity.id=?1";
    createQuery(sQuery, id).executeUpdate();
  }
}
