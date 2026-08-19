/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField.FirewallFilterableField;

public class FirewallRepositoryComponentFilter
{
  public int page;

  public int pageSize;

  public FirewallSortableField sortableField;

  public boolean asc;

  public List<FirewallFilterField> filterFields;

  public FirewallComponentFilterState firewallComponentFilterState;

  /**
   * Scoped repository IDs for Firewall Dashboard access control.
   *
   * <p>
   * Must always be set via {@code FirewallPermissionGate.resolvePermittedRepositoryIds()} before
   * the filter is passed to a DAO:
   * <ul>
   * <li>{@code null} — full access; no IN filter applied (returned by the gate for admin users
   * with container-level READ permission)
   * <li>non-empty {@code Set} — scoped; only components from these proxy repo IDs are returned
   * </ul>
   *
   * <p>
   * <strong>Do not leave this field unset.</strong> A {@code null} value is a deliberate
   * full-access sentinel, not an uninitialised default — future callers must explicitly assign
   * the result of {@code resolvePermittedRepositoryIds()} here.
   */
  public Set<String> permittedRepositoryIds;

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
    return filterFields.stream()
        .collect(Collectors.toMap(FirewallFilterField::getField, FirewallFilterField::getValue));
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
