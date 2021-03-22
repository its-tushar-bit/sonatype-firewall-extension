/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField.FirewallFilterableField;

public class FirewallRepositoryComponentFilter
{
  public int page;

  public int pageSize;

  public FirewallSortableField sortableField;

  public boolean asc;

  public List<FirewallFilterField> filterFields;

  public FirewallComponentFilterState firewallComponentFilterState;

  public FirewallRepositoryComponentFilter(
      final int page,
      final int pageSize,
      FirewallComponentFilterState firewallComponentFilterState,
      final FirewallSortableField sortableField,
      final boolean asc,
      final List<FirewallFilterField> filterFields)
  {
    this.page = page;
    this.pageSize = pageSize;
    this.firewallComponentFilterState = firewallComponentFilterState;
    this.sortableField = sortableField;
    this.asc = asc;
    this.filterFields = filterFields;
  }

  public Map<FirewallFilterableField, String> getFilterFieldsMap() {
    Map<FirewallFilterableField, String> map = new EnumMap<>(FirewallFilterableField.class);

    for (FirewallFilterField filterField : this.filterFields) {
      map.put(filterField.getField(), filterField.getValue());
    }

    return map;
  }

  public enum FirewallComponentFilterState
  {
    AUDIT,
    QUARANTINE,
    UNQUARANTINE_ALL,
    UNQUARANTINE_MANUAL,
    UNQUARANTINE_AUTO,
    ALL
  }
}
