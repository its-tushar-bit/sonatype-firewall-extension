/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useState } from 'react';
import type {
  ViolationFilterSetGroup,
  ViolationsFilterState,
  ViolationThreatRange,
  ViolationWaiverType,
} from 'MainRoot/nosc/violations/violationListTypes';
import { createDefaultViolationsFilterState } from 'MainRoot/nosc/violations/violationsListApi';

export interface UseViolationFiltersResult {
  readonly filters: ViolationsFilterState;
  readonly toggle: (group: ViolationFilterSetGroup, id: string) => void;
  readonly setThreatRange: (range: ViolationThreatRange) => void;
  readonly setWaiverType: (waiverType: ViolationWaiverType) => void;
  readonly reset: () => void;
}

/**
 * Local violation filter state + handlers for the search results Violations tab (CLM-42562). The
 * Violations list page owns its own filter state in its container (with URL persistence); this hook is
 * a lightweight equivalent for the client-side-filtered search tab.
 */
export function useViolationFilters(): UseViolationFiltersResult {
  const [filters, setFilters] = useState<ViolationsFilterState>(createDefaultViolationsFilterState);

  const toggle = useCallback((group: ViolationFilterSetGroup, id: string) => {
    setFilters((prev) => {
      const next = new Set(prev[group]);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return { ...prev, [group]: next };
    });
  }, []);

  const setThreatRange = useCallback(
    (range: ViolationThreatRange) => setFilters((prev) => ({ ...prev, threatRange: range })),
    [],
  );

  const setWaiverType = useCallback(
    (waiverType: ViolationWaiverType) => setFilters((prev) => ({ ...prev, waiverType })),
    [],
  );

  const reset = useCallback(() => setFilters(createDefaultViolationsFilterState()), []);

  return { filters, toggle, setThreatRange, setWaiverType, reset };
}
