/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.util.SortedMap;
import java.util.TreeMap;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.json.store.JsonUtils;

/**
 * @since 1.143
 */
public class ConfigurationInfo
{
  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  ConfigurationInfo(final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO) {
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  String getConfigurationInfo() {
    final SortedMap<String, Object> entries = new TreeMap<>();
    addEntry(entries, SystemConfigurationProperty.HDS_URL);
    addEntry(entries, SystemConfigurationProperty.CSRF_PROTECTION);
    addEntry(entries, SystemConfigurationProperty.CDN_URL);
    addEntry(entries, SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES);
    addEntry(entries, SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE);
    addEntry(entries, SystemConfigurationProperty.USER_AGENT_SUFFIX);
    addEntry(entries, SystemConfigurationProperty.CSP_ENABLED);
    addEntry(entries, SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH);
    addEntry(entries, SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH);
    addEntry(entries, SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH);
    addEntry(entries, SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE);
    addEntry(entries, SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT);
    addEntry(entries, SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD);
    addEntry(entries, SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT);
    addEntry(entries, SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER);
    addEntry(entries, SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS);
    addEntry(entries, SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS);
    addEntry(entries, SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS);
    addEntry(entries, SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER);
    addEntry(entries, SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING);
    addEntry(entries, SystemConfigurationProperty.POLICY_MONITORING_HOUR);
    addEntry(entries, SystemConfigurationProperty.DB_BACKUP_DIR);
    addEntry(entries, SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE);
    addEntry(entries, SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED);
    addEntry(entries, SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING);
    addEntry(entries, SystemConfigurationProperty.BASE_URL);
    addEntry(entries, SystemConfigurationProperty.FORCE_BASE_URL);
    addEntry(entries, SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST);
    return JsonUtils.format(entries);
  }

  private void addEntry(SortedMap<String, Object> entries, String propertyName) {
    SystemConfigurationProperty property = systemConfigurationPropertyDAO.getByName(propertyName);
    entries.put(propertyName, property == null ? null : property.getValue());
  }
}
