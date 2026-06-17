/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router';
import { Box, Card, Flex, Skeleton, Tabs } from '@radix-ui/themes';
import {
  MobileFilterWrapper,
  VulnerabilityResultCard,
  Pagination,
  SectionHeading,
  BodyText,
  useAdapterSearchParams,
} from '@guide/ui-core';
import {
  buildVulnerabilityFilters,
  vulnerabilitySortOptions,
  VULNERABILITY_FILTER_ORDER,
  getOffsetFromParams,
  getLimitFromParams,
  getSortFromParams,
  getStringParam,
  tokens,
} from '@guide/ui-core/utils';
import { getComponentVulnerabilities } from 'GuideRoot/api/componentsBackend';
import { toParamsRecord } from 'GuideRoot/utils/searchParams';
import type { VulnerabilitySearchResponse } from '@guide/ui-core/types';

const LIMIT = 25;

function VulnerabilitiesTabSkeleton() {
  return (
    <Tabs.Content value="vulnerabilities">
      <Flex gap={tokens.space.section} data-testid="tab-skeleton">
        <Box width="250px" display={{ initial: 'none', md: 'block' }}>
          <Skeleton width="100%" height="200px" />
        </Box>
        <Box flexBasis="1" minWidth="0" width="100%">
          <Flex direction="column" gap={tokens.space.section}>
            <Flex gap={tokens.space.inline}>
              <Skeleton width="300px" height={tokens.skeleton.height.input} />
              <Skeleton width="200px" height={tokens.skeleton.height.input} />
            </Flex>
            <Flex direction="column" gap={tokens.space.item}>
              {[...Array(5)].map((_, i) => (
                <Card key={i} size={tokens.card.small}>
                  <Flex direction="column" gap={tokens.space.inline}>
                    <Flex justify="between" align="start">
                      <Skeleton width="40%" height="28px" />
                      <Skeleton width="60px" height="24px" />
                    </Flex>
                    <Skeleton width="100%" height="48px" />
                    <Flex gap={tokens.space.inline}>
                      <Skeleton width="100px" height="20px" />
                      <Skeleton width="80px" height="20px" />
                    </Flex>
                  </Flex>
                </Card>
              ))}
            </Flex>
          </Flex>
        </Box>
      </Flex>
    </Tabs.Content>
  );
}

export function VulnerabilitiesTab() {
  const { ecosystem = '', pkg = '', version = '' } = useParams<{
    ecosystem: string; pkg: string; version: string;
  }>();
  const searchParams = useAdapterSearchParams();
  const [response, setResponse] = useState<VulnerabilitySearchResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const params = toParamsRecord(searchParams);
    const query = getStringParam(params, 'query') || undefined;
    const filters = buildVulnerabilityFilters(params);
    const { sortField, sortOrder } = getSortFromParams(params);
    const offset = getOffsetFromParams(params);
    const limit = getLimitFromParams(params, LIMIT);

    setLoading(true);
    setError(null);

    getComponentVulnerabilities(ecosystem, pkg, version, query, filters, { offset, limit, sortField, sortOrder })
      .then((data) => { if (!cancelled) setResponse(data); })
      .catch((e) => { if (!cancelled) setError(e instanceof Error ? e.message : String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [ecosystem, pkg, version, searchParams]);

  if (loading && response === null) return <VulnerabilitiesTabSkeleton />;

  if (error) {
    return (
      <Tabs.Content value="vulnerabilities">
        <p style={{ color: 'var(--red-11)' }}>Error loading vulnerabilities: {error}</p>
      </Tabs.Content>
    );
  }

  const vulnerabilities = response?.hits ?? [];
  const total = response?.total ?? 0;
  const aggregations = response?.aggregations ?? {};
  const offset = response?.offset ?? 0;
  const limit = response?.limit ?? LIMIT;
  const formAction = `/component/${encodeURIComponent(ecosystem)}/${encodeURIComponent(pkg)}/${encodeURIComponent(version)}/vulnerabilities`;
  const paramsRecord = toParamsRecord(searchParams);

  return (
    <Tabs.Content value="vulnerabilities">
      <Flex gap={tokens.space.section}>
      <MobileFilterWrapper
        filterSidebarProps={{
          aggregations,
          formAction,
          searchParams: paramsRecord,
          customFilterConfigs: VULNERABILITY_FILTER_ORDER,
        }}
        filterToolbarProps={{
          formAction,
          searchParams: paramsRecord,
          searchPlaceholder: 'Filter by CVE ID or description',
          sortOptions: vulnerabilitySortOptions,
          defaultSortValue: 'publishedDate:desc',
          showFilter: false,
          totalResults: total,
        }}
      >
        {vulnerabilities.length === 0 && (
          <Card size={tokens.card.medium}>
            <Flex direction="column" align="center" justify="center" gap={tokens.space.item} py={tokens.space.section}>
              <SectionHeading align="center">No Known Vulnerabilities</SectionHeading>
              <BodyText tone="subtle" align="center">
                This version has no known security vulnerabilities.
              </BodyText>
            </Flex>
          </Card>
        )}
        {vulnerabilities.length > 0 && (
          <Flex direction="column" gap={tokens.space.item}>
            {vulnerabilities.map((vuln) => (
              <Link key={vuln.vulnId} to={`/vulnerability/${encodeURIComponent(vuln.vulnId)}`} className="unstyled-link">
                <VulnerabilityResultCard vulnerability={vuln} />
              </Link>
            ))}
          </Flex>
        )}
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
