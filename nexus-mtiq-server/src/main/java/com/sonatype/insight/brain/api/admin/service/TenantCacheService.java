/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.openjpa.datacache.CacheStatistics;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.StoreCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class TenantCacheService
{
  private static final Logger log = LoggerFactory.getLogger(TenantCacheService.class);

  private static final ObjectMapper mapper = new ObjectMapper();

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  private final OperationalDataStore operationalDataStore;

  @Inject
  public TenantCacheService(
      final TenantUtil tenantUtil,
      final TenantValidator tenantValidator,
      final OperationalDataStore operationalDataStore)
  {
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.operationalDataStore = operationalDataStore;
  }

  public String getCache(final String tenantSlug) {
    if (tenantUtil.isGlobalTenant()) {
      throw new BadRequestException("Invalid tenant");
    }

    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.debug("Tenant {} doesn't exist", tenantSlug.replaceAll("[\n\r]", "_"));
      throw new NotFoundException("Tenant doesn't exist");
    }

    OpenJPAEntityManagerFactory jpaEntityManagerFactory =
        (OpenJPAEntityManagerFactory) operationalDataStore.getJPAEntityManagerFactory();
    StoreCache storeCache = jpaEntityManagerFactory.getStoreCache();
    CacheStatistics statistics = storeCache.getStatistics();
    try {
      return mapper.writeValueAsString(statistics);
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException("Unable to parse CacheStatistics", e);
    }
  }
}
