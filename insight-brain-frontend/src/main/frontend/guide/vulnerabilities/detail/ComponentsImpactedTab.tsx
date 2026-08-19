/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router';
import { Flex, Skeleton, Box } from '@radix-ui/themes';
import { tokens } from '@guide/ui-core/utils';
import { AffectedComponentsTable, BodyText } from '@guide/ui-core';
import { getVulnerabilityAffectedComponents, AFFECTED_COMPONENTS_SORT_FIELDS } from 'GuideRoot/api/vulnerabilitiesBackend';
import type { AffectedComponentVersion } from '@guide/ui-core/types';

const DEFAULT_PAGE_SIZE = 25;
const MAX_PAGE_SIZE = 25;

interface ComponentsResponse {
  hits: AffectedComponentVersion[];
  total: number;
  offset: number;
  limit: number;
}

function parsePaginationParam(value: string | null, defaultValue: number, min: number, max: number): number {
  if (!value) return defaultValue;
  const parsed = parseInt(value, 10);
  if (isNaN(parsed)) return defaultValue;
  return Math.max(min, Math.min(max, parsed));
}

export function ComponentsImpactedTab() {
  const { vulnId } = useParams<{ vulnId: string }>();
  const [searchParams] = useSearchParams();
  const [response, setResponse] = useState<ComponentsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  useEffect(() => {
    if (!vulnId) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setIsError(false);
    let cancelled = false;
    const query = searchParams.get('query') ?? undefined;
    const offset = parsePaginationParam(searchParams.get('offset'), 0, 0, Number.MAX_SAFE_INTEGER);
    const limit = parsePaginationParam(searchParams.get('limit'), DEFAULT_PAGE_SIZE, 1, MAX_PAGE_SIZE);
    const rawSortField = searchParams.get('sortField') ?? 'packageName';
    const sortField = AFFECTED_COMPONENTS_SORT_FIELDS.has(rawSortField) ? rawSortField : 'packageName';
    const rawSortOrder = searchParams.get('sortOrder');
    const sortOrder: 'asc' | 'desc' = rawSortOrder === 'desc' ? 'desc' : 'asc';

    getVulnerabilityAffectedComponents(vulnId, { query, offset, limit, sortField, sortOrder })
      .then((data) => {
        if (!cancelled) {
          setResponse(data ?? null);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setIsError(true);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [vulnId, searchParams]);

  // Component is only rendered under /vulnerability/:vulnId — this guard prevents a broken formAction
  if (!vulnId) return null;

  // Convert searchParams to Record for table
  const paramsRecord: Record<string, string | string[]> = {};
  searchParams.forEach((value, key) => {
    paramsRecord[key] = value;
  });

  if (isLoading && response === null) {
    return (
      <Box mt={tokens.space.section} aria-busy="true">
        {[...Array(5)].map((_, i) => (
          <Flex key={i} gap={tokens.space.section} py={tokens.space.inline}>
            <Skeleton width="35%" height="16px" />
            <Skeleton width="15%" height="16px" />
            <Skeleton width="20%" height="16px" />
            <Skeleton width="20%" height="16px" />
          </Flex>
        ))}
      </Box>
    );
  }

  if (isError) {
    return (
      <Box mt={tokens.space.section}>
        <BodyText align="center">Failed to load affected components. Please try again.</BodyText>
      </Box>
    );
  }

  return (
    <Flex direction="column" gap={tokens.space.section}>
      <AffectedComponentsTable
        affectedComponentVersions={response?.hits ?? []}
        total={response?.total ?? 0}
        offset={response?.offset ?? 0}
        limit={response?.limit ?? DEFAULT_PAGE_SIZE}
        formAction={`/vulnerability/${vulnId}/components-impacted`}
        searchParams={paramsRecord}
      />
    </Flex>
  );
}
