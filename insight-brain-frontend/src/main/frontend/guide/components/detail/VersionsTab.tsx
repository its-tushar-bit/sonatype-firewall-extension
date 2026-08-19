/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useState } from 'react';
import { useParams, useOutletContext } from 'react-router';
import { Box, Card, Flex, Skeleton, Tabs } from '@radix-ui/themes';
import {
  MobileFilterWrapper,
  VersionsTable,
  Pagination,
  useAdapterSearchParams,
} from '@guide/ui-core';
import {
  buildVersionsFilters,
  COMPONENT_FILTER_ORDER,
  componentFilterDefinitions,
  getOffsetFromParams,
  getLimitFromParams,
  getSortFromParams,
  getStringParam,
  tokens,
} from '@guide/ui-core/utils';
import { getComponentVersions } from 'GuideRoot/api/componentsBackend';
import { buildArtifactFormAction, toParamsRecord } from 'GuideRoot/utils/searchParams';
import type { ComponentVersionsResponse } from 'GuideRoot/api/componentsBackend';
import type { SortOption } from '@guide/ui-core/types';
import type { ArtifactOutletContext } from './ComponentDetailPage';

const LIMIT = 25;

const versionsSortOptions: SortOption[] = [
  { value: 'version:desc', label: 'Version (Newest first)', field: 'version', order: 'desc' },
];

function VersionsTabSkeleton() {
  const cols = ['25%', '20%', '25%', '15%', '15%'];
  return (
    <Tabs.Content value="versions">
      <Flex gap={tokens.space.section} data-testid="tab-skeleton">
        <Box width="250px" display={{ initial: 'none', md: 'block' }}>
          <Skeleton width="100%" height="150px" />
        </Box>
        <Box flexBasis="1" minWidth="0" width="100%">
          <Flex direction="column" gap={tokens.space.section}>
            <Flex gap={tokens.space.inline}>
              <Skeleton width="200px" height={tokens.skeleton.height.input} />
            </Flex>
            <Card size={tokens.card.small}>
              <Flex direction="column" gap={tokens.space.inline}>
                <Flex gap={tokens.space.item} pb={tokens.space.inline}>
                  {cols.map((w, i) => <Skeleton key={i} width={w} height="20px" />)}
                </Flex>
                {[...Array(8)].map((_, i) => (
                  <Flex key={i} gap={tokens.space.item} py={tokens.space.inline}>
                    {cols.map((w, j) => <Skeleton key={j} width={w} height="32px" />)}
                  </Flex>
                ))}
              </Flex>
            </Card>
          </Flex>
        </Box>
      </Flex>
    </Tabs.Content>
  );
}

export function VersionsTab() {
  const { ecosystem = '', pkg = '', version = '' } = useParams<{
    ecosystem: string; pkg: string; version: string;
  }>();
  const { extension, classifier } = useOutletContext<ArtifactOutletContext>();
  const searchParams = useAdapterSearchParams();
  const [response, setResponse] = useState<ComponentVersionsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const params = toParamsRecord(searchParams);
    const query = getStringParam(params, 'query') || undefined;
    const filters = buildVersionsFilters(params);
    const { sortField = 'version', sortOrder = 'desc' } = getSortFromParams(params);
    const offset = getOffsetFromParams(params);
    const limit = getLimitFromParams(params, LIMIT);

    setLoading(true);
    setError(null);

    getComponentVersions(ecosystem, pkg, version, query, filters, { offset, limit, sortField, sortOrder }, { extension, classifier })
      .then((data) => { if (!cancelled) setResponse(data); })
      .catch((e) => { if (!cancelled) setError(e instanceof Error ? e.message : String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [ecosystem, pkg, version, searchParams, extension, classifier]);

  if (loading && response === null) return <VersionsTabSkeleton />;

  if (error) {
    return (
      <Tabs.Content value="versions">
        <p style={{ color: 'var(--red-11)' }}>Error loading versions: {error}</p>
      </Tabs.Content>
    );
  }

  const hits = response?.hits ?? [];
  const total = response?.total ?? 0;
  const aggregations = response?.aggregations ?? {};
  const offset = response?.offset ?? 0;
  const limit = response?.limit ?? LIMIT;
  const basePath = `/component/${encodeURIComponent(ecosystem)}/${encodeURIComponent(pkg)}/${encodeURIComponent(version)}/versions`;
  const formAction = buildArtifactFormAction(basePath, { extension, classifier });
  const paramsRecord = toParamsRecord(searchParams);

  return (
    <Tabs.Content value="versions">
      <Flex gap={tokens.space.section}>
      <MobileFilterWrapper
        filterSidebarProps={{
          aggregations,
          formAction,
          searchParams: paramsRecord,
          customFilterConfigs: { ...COMPONENT_FILTER_ORDER, ...componentFilterDefinitions },
          showPreReleaseFilter: true,
        }}
        filterToolbarProps={{
          formAction,
          searchParams: paramsRecord,
          searchPlaceholder: 'Filter by version number',
          sortOptions: versionsSortOptions,
          defaultSortValue: 'version:desc',
          showSort: false,
          totalResults: total,
          clearRemovesQuery: true,
        }}
      >
        <VersionsTable
          currentVersion={version}
          versions={hits}
          formAction={formAction}
          searchParams={paramsRecord}
          artifactFilter={{ extension, classifier }}
        />
        {total > limit && (
          <Pagination
            formAction={formAction}
            searchParams={paramsRecord}
            total={total}
            limit={limit}
            offset={offset}
          />
        )}
      </MobileFilterWrapper>
      </Flex>
    </Tabs.Content>
  );
}
