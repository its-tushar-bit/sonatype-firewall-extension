/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useEffect, useMemo, useState } from 'react';
import {
  Badge,
  Box,
  Button,
  Card,
  Checkbox,
  Flex,
  Grid,
  Heading,
  IconButton,
  Inset,
  Link as RadixLink,
  Table,
  Text,
  TextField,
  Tooltip,
} from '@radix-ui/themes';
import { SectionHeading } from '@sonatype/nexus-one-components';
import { ActionIcons, DomainIcons, StatusIcons } from 'MainRoot/nosc/icons';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { Pagination } from 'MainRoot/nosc/components/Pagination';
import { violationDetailHref } from 'MainRoot/nosc/violations/violationDetailHref';
import { FlatViolation, ThreatLabel } from './applicationDetailTypes';
import { LargeScanBanner } from './LargeScanBanner';
import { THREAT_GROUPS, VIOLATION_PAGE_SIZE } from './applicationDetailUtils';
import './PolicyFailuresTab.scss';

interface PolicyFailuresTabProps {
  readonly violations: ReadonlyArray<FlatViolation>;
  readonly loading: boolean;
  readonly errored: boolean;
  readonly onRetry: () => void;
  /** True when reports loaded but the application has no scan yet. */
  readonly showNoScanYet?: boolean;
}

export function PolicyFailuresTab({
  violations,
  loading,
  errored,
  onRetry,
  showNoScanYet = false,
}: PolicyFailuresTabProps): JSX.Element {
  const [filters, setFilters] = useState<{
    search: string;
    policyTypes: string[];
    waived: string[];
    threat: string[];
  }>({ search: '', policyTypes: [], waived: [], threat: [] });
  // Destructure to the per-facet locals the readers below use.
  const { search, policyTypes: policyTypeFilter, waived: waivedFilter, threat: threatFilter } = filters;
  const toggleFilter = (key: 'policyTypes' | 'waived' | 'threat', value: string): void => {
    setFilters((f) => {
      const list = f[key];
      return {
        ...f,
        [key]: list.includes(value) ? list.filter((x) => x !== value) : [...list, value],
      };
    });
  };
  const [page, setPage] = useState(1);

  const policyTypeOptions = useMemo(() => {
    const set = new Set<string>();
    violations.forEach((v) => set.add(v.policyThreatCategory));
    return Array.from(set).sort();
  }, [violations]);

  // All three facet counts derive from the same `violations` array, so compute
  // them in a single pass / single memo (the `filtered` memo below stays
  // separate — it also depends on the filter state, so folding it in here would
  // recompute these counts on every keystroke).
  const { policyTypeCounts, waivedCounts, threatGroupCounts } = useMemo(() => {
    const policyTypes: Record<string, number> = {};
    const threatGroups: Record<ThreatLabel, number> = {
      Critical: 0,
      Severe: 0,
      Moderate: 0,
      Low: 0,
      None: 0,
    };
    let waivedYes = 0;
    violations.forEach((v) => {
      policyTypes[v.policyThreatCategory] = (policyTypes[v.policyThreatCategory] || 0) + 1;
      threatGroups[v.threatLabel] += 1;
      if (v.waived) waivedYes += 1;
    });
    return {
      policyTypeCounts: policyTypes,
      waivedCounts: { Yes: waivedYes, No: violations.length - waivedYes },
      threatGroupCounts: threatGroups,
    };
  }, [violations]);

  const filtered = useMemo(() => {
    let out = violations;
    if (search) {
      const q = search.toLowerCase();
      out = out.filter(
        (v) => v.policyName.toLowerCase().includes(q) || v.componentDisplay.toLowerCase().includes(q)
      );
    }
    if (policyTypeFilter.length > 0) {
      out = out.filter((v) => policyTypeFilter.includes(v.policyThreatCategory));
    }
    if (waivedFilter.length === 1) {
      const wantWaived = waivedFilter[0] === 'Yes';
      out = out.filter((v) => v.waived === wantWaived);
    }
    if (threatFilter.length > 0) {
      out = out.filter((v) => threatFilter.includes(v.threatLabel));
    }
    return [...out].sort((a, b) => b.policyThreatLevel - a.policyThreatLevel);
  }, [violations, search, policyTypeFilter, waivedFilter, threatFilter]);

  useEffect(() => {
    setPage(1);
  }, [search, policyTypeFilter, waivedFilter, threatFilter, violations]);

  const paged = filtered.slice((page - 1) * VIOLATION_PAGE_SIZE, page * VIOLATION_PAGE_SIZE);

  const clearFilters = (): void => {
    setFilters({ search: '', policyTypes: [], waived: [], threat: [] });
    setPage(1);
  };

  if (loading) {
    return (
      <Box mt="4">
        <LoadingSkeleton height={240} data-testid="nosc-app-detail-policy-failures-loading" />
      </Box>
    );
  }

  if (showNoScanYet) {
    return (
      <Card mt="4" data-testid="nosc-app-detail-policy-failures-no-scan">
        <Flex direction="column" align="center" gap="3" p="6">
          <StatusIcons.Info size={32} color="var(--gray-9)" />
          <SectionHeading>No scans yet</SectionHeading>
          <Text size="2" color="gray" align="center">
            Run an IQ scan against this application to see its policy failures here.
          </Text>
        </Flex>
      </Card>
    );
  }

  if (errored) {
    return (
      <Flex
        direction="column"
        gap="3"
        align="start"
        p="4"
        mt="4"
        data-testid="nosc-app-detail-policy-failures-error"
        style={{ backgroundColor: 'var(--red-3)', borderRadius: 'var(--radius-3)' }}
      >
        <Text size="2" color="red">
          Failed to load policy violations.
        </Text>
        <Button
          size="2"
          variant="soft"
          onClick={onRetry}
          data-testid="nosc-app-detail-policy-failures-retry"
        >
          Retry
        </Button>
      </Flex>
    );
  }

  if (violations.length === 0) {
    return (
      <Card mt="4" data-testid="nosc-app-detail-policy-failures-empty">
        <Flex direction="column" align="center" gap="3" p="6">
          <DomainIcons.Healthy size={40} color="var(--green-9)" />
          <Heading size="4">No policy failures</Heading>
          <Text size="2" color="gray" align="center">
            This application&apos;s most recent scan reported zero open policy violations.
          </Text>
        </Flex>
      </Card>
    );
  }

  return (
    <Box mt="4" data-testid="nosc-app-detail-policy-failures">
      <LargeScanBanner
        itemCount={violations.length}
        itemLabel="violations"
        guidance="Filters and pagination run in your browser — use Classic for full export and advanced reporting on very large inventories."
        testId="nosc-app-detail-policy-failures-large-scan"
      />
      <Grid columns={{ initial: '1', md: '220px 1fr' }} gap="4">
        <Box p="1" pt="0">
          <Flex align="center" justify="start" mb="4">
            <Button variant="outline" color="gray" size="2" onClick={clearFilters}>
              <ActionIcons.Refresh size={12} />
              Reset filters
            </Button>
          </Flex>
          <Flex direction="column" gap="4">
            <fieldset className="nosc-policy-failures-filter-group">
              <legend className="nosc-policy-failures-filter-legend">Policy Type</legend>
              <Flex direction="column" gap="1">
                {policyTypeOptions.map((type) => (
                  <Text key={type} as="label" size="2">
                    <Flex align="center" gap="2">
                      <Checkbox
                        checked={policyTypeFilter.includes(type)}
                        onCheckedChange={() => toggleFilter('policyTypes', type)}
                      />
                      {type}
                      <Badge size="1" color="gray" variant="soft" radius="full">
                        {policyTypeCounts[type] ?? 0}
                      </Badge>
                    </Flex>
                  </Text>
                ))}
              </Flex>
            </fieldset>
            <fieldset className="nosc-policy-failures-filter-group">
              <legend className="nosc-policy-failures-filter-legend">Waived</legend>
              <Flex direction="column" gap="1">
                {(['Yes', 'No'] as const).map((val) => (
                  <Text key={val} as="label" size="2">
                    <Flex align="center" gap="2">
                      <Checkbox
                        checked={waivedFilter.includes(val)}
                        onCheckedChange={() => toggleFilter('waived', val)}
                      />
                      {val}
                      <Badge size="1" color="gray" variant="soft" radius="full">
                        {waivedCounts[val]}
                      </Badge>
                    </Flex>
                  </Text>
                ))}
              </Flex>
            </fieldset>
            <fieldset className="nosc-policy-failures-filter-group">
              <legend className="nosc-policy-failures-filter-legend">Threat Level</legend>
              <Flex direction="column" gap="1">
                {THREAT_GROUPS.map(({ group, range }) => (
                  <Text key={group} as="label" size="2">
                    <Flex align="center" gap="2">
                      <Checkbox
                        checked={threatFilter.includes(group)}
                        onCheckedChange={() => toggleFilter('threat', group)}
                      />
                      {range} {group}
                      <Badge size="1" color="gray" variant="soft" radius="full">
                        {threatGroupCounts[group]}
                      </Badge>
                    </Flex>
                  </Text>
                ))}
              </Flex>
            </fieldset>
          </Flex>
        </Box>

        <Box>
          <Flex direction="column" gap="4">
            <TextField.Root
              placeholder="Search policies or components..."
              value={search}
              onChange={(e) => setFilters((f) => ({ ...f, search: e.target.value }))}
              data-testid="nosc-app-detail-policy-search"
            >
              <TextField.Slot>
                <ActionIcons.Search size={16} />
              </TextField.Slot>
              <TextField.Slot>
                {search && (
                  <IconButton
                    size="1"
                    variant="ghost"
                    onClick={() => setFilters((f) => ({ ...f, search: '' }))}
                    aria-label="Clear search"
                  >
                    <ActionIcons.Cancel size={14} />
                  </IconButton>
                )}
              </TextField.Slot>
            </TextField.Root>

            {paged.length > 0 ? (
              <Card>
                <Inset>
                  <Table.Root data-testid="nosc-app-detail-policy-failures-table">
                    <Table.Header>
                      <Table.Row>
                        <Table.ColumnHeaderCell style={{ textAlign: 'center', whiteSpace: 'nowrap' }}>
                          Threat
                        </Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Policy Name</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Policy Type</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell style={{ textAlign: 'center' }}>
                          Waived
                        </Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
                      </Table.Row>
                    </Table.Header>
                    <Table.Body>
                      {paged.map((v) => (
                        <Table.Row key={v.key} data-testid="nosc-app-detail-policy-failures-row">
                          <Table.Cell style={{ textAlign: 'center', whiteSpace: 'nowrap' }}>
                            <Tooltip content={`Threat Level: ${v.policyThreatLevel} ${v.threatLabel}`}>
                              <Badge size="1" variant="solid" color={v.threatColor} radius="full">
                                {v.policyThreatLevel}
                              </Badge>
                            </Tooltip>
                          </Table.Cell>
                          <Table.Cell style={{ whiteSpace: 'nowrap' }}>
                            {v.policyViolationId ? (
                              <RadixLink size="2" href={violationDetailHref(v.policyViolationId)}>
                                {v.policyName}
                              </RadixLink>
                            ) : (
                              <Text size="2">{v.policyName}</Text>
                            )}
                          </Table.Cell>
                          <Table.Cell style={{ whiteSpace: 'nowrap' }}>
                            <Text size="2">{v.policyThreatCategory}</Text>
                          </Table.Cell>
                          <Table.Cell style={{ textAlign: 'center', whiteSpace: 'nowrap' }}>
                            <Text size="2">{v.waived ? 'Yes' : 'No'}</Text>
                          </Table.Cell>
                          <Table.Cell style={{ wordBreak: 'break-all' }}>
                            <Text size="2" style={{ fontFamily: 'var(--code-font-family)' }}>
                              {v.componentDisplay}
                            </Text>
                          </Table.Cell>
                        </Table.Row>
                      ))}
                    </Table.Body>
                  </Table.Root>
                </Inset>
                <Pagination
                  page={page}
                  pageSize={VIOLATION_PAGE_SIZE}
                  totalItems={filtered.length}
                  onPageChange={setPage}
                  data-testid="nosc-app-detail-policy-failures-pagination"
                />
              </Card>
            ) : (
              <Card>
                <Flex direction="column" align="center" gap="3" p="6">
                  <DomainIcons.Policies size={40} color="var(--gray-9)" />
                  <Heading size="4">No matches found</Heading>
                  <Text size="2" color="gray" align="center">
                    Try adjusting your filters or search terms.
                  </Text>
                  <Button onClick={clearFilters} variant="soft">
                    <ActionIcons.Refresh size={14} />
                    Reset filters
                  </Button>
                </Flex>
              </Card>
            )}
          </Flex>
        </Box>
      </Grid>
    </Box>
  );
}
