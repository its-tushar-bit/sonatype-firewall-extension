/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
// @ts-expect-error — CLMLocation is legacy JavaScript without type declarations
import { getDashboardFilters } from 'MainRoot/util/CLMLocation';
import type { DashboardMetricsScope, UseDashboardActiveFilterResult } from './dashboardMetricsTypes';

interface ActiveFilterWire {
  readonly organizationFilters?: readonly string[] | null;
  readonly applicationFilters?: readonly string[] | null;
  readonly stageTypeFilters?: readonly string[] | null;
  readonly tagFilters?: readonly string[] | null;
}

interface ActiveFilterResponse {
  readonly needsAcknowledgement?: boolean;
  readonly filter?: ActiveFilterWire | null;
}

function nonEmpty(values: readonly string[] | null | undefined): readonly string[] | undefined {
  return values && values.length > 0 ? values : undefined;
}

export function activeFilterToMetricsScope(filter: ActiveFilterWire | null | undefined): DashboardMetricsScope {
  if (!filter) return {};
  const organizationIds = nonEmpty(filter.organizationFilters);
  const applicationIds = nonEmpty(filter.applicationFilters);
  const stageIds = nonEmpty(filter.stageTypeFilters);
  const tagIds = nonEmpty(filter.tagFilters);
  return {
    ...(organizationIds && { organizationIds }),
    ...(applicationIds && { applicationIds }),
    ...(stageIds && { stageIds }),
    ...(tagIds && { tagIds }),
  };
}

export function useDashboardActiveFilter(): UseDashboardActiveFilterResult {
  const [attempt, setAttempt] = useState(0);
  const [loading, setLoading] = useState(true);
  const [scope, setScope] = useState<DashboardMetricsScope>({});
  const [needsAcknowledgement, setNeedsAcknowledgement] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    // Clear stale acknowledgement for the in-flight window so metrics stay gated only by loading.
    setNeedsAcknowledgement(false);

    axios
      .get<ActiveFilterResponse>(getDashboardFilters(), { signal: controller.signal })
      .then(({ data }) => {
        if (cancelled) return;
        setScope(activeFilterToMetricsScope(data.filter));
        setNeedsAcknowledgement(data.needsAcknowledgement ?? false);
        setLoading(false);
      })
      .catch((err) => {
        if (cancelled || axios.isCancel?.(err)) return;
        setError(err instanceof Error ? err : new Error(String(err)));
        setLoading(false);
      });

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [attempt]);

  const retry = useCallback(() => {
    setAttempt((value) => value + 1);
  }, []);

  return { loading, scope, needsAcknowledgement, error, retry };
}
