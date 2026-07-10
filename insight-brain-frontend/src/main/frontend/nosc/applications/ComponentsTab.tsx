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
  Flex,
  Link as RadixLink,
  Table,
  Text,
  TextField,
} from '@radix-ui/themes';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { Pagination } from 'MainRoot/nosc/components/Pagination';
import { RawReportComponent } from './applicationDetailTypes';
import { LargeScanBanner } from './LargeScanBanner';
import {
  COMPONENTS_PAGE_SIZE,
  classicReportHrefForComponent,
  deriveComponentName,
  deriveComponentThreat,
  deriveLicense,
  matchStateColor,
  matchStateLabel,
  threatColorFor,
} from './applicationDetailUtils';

/**
 * Components tab inside the Application Detail page (CLM-39709 / P1-F7c).
 *
 * Reads from /api/v2/applications/{publicId}/reports/{scanId}/raw which
 * returns the full component catalog for the scan (NOT just components
 * with violations — that's the policythreats.json subset). Per-component
 * active-violation counts come from policythreats.json which is already
 * fetched for the Violations tab, so no extra round-trip.
 *
 * Columns (CLM-39709 review):
 *   1. Threat — derived: max(securityIssue.severity, licenseThreat).
 *   2. Component — displayName + format glyph.
 *   3. Match — Exact / Similar / Unknown badge from matchState.
 *   4. Violations — active count from policythreats.json.
 *   5. License — effectiveLicenses joined.
 *   6. Direct — direct vs transitive dependency.
 *   7. Actions — link to Classic component report (no Nexus One
 *      detail page — deferred until Guide ships per user note).
 *
 * Plus search filter + 20-row pagination, mirroring the Violations
 * tab UX.
 */
interface ComponentsTabProps {
  components: ReadonlyArray<RawReportComponent>;
  status: 'idle' | 'loading' | 'ready' | 'error';
  publicId: string;
  scanId: string | null;
  /** hash → active violation count, computed from policythreats.json. */
  violationCountByHash: Record<string, number>;
  onRetry: () => void;
}

export function ComponentsTab({
  components,
  status,
  publicId,
  scanId,
  violationCountByHash,
  onRetry,
}: ComponentsTabProps): JSX.Element {
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);

  // Reset page when the search filter or the underlying component set changes
  // (e.g. a retry resolves with a shorter report) so we never strand the user
  // on a now-out-of-range page.
  useEffect(() => {
    setPage(0);
  }, [searchTerm, components]);

  const filtered = useMemo(() => {
    const q = searchTerm.trim().toLowerCase();
    if (!q) return components;
    return components.filter((c) => {
      const name = deriveComponentName(c).toLowerCase();
      const purl = (c.packageUrl ?? '').toLowerCase();
      const license = deriveLicense(c).toLowerCase();
      return name.includes(q) || purl.includes(q) || license.includes(q);
    });
  }, [components, searchTerm]);

  const pageCount = Math.max(1, Math.ceil(filtered.length / COMPONENTS_PAGE_SIZE));
  const safePage = Math.min(page, pageCount - 1);
  const pageRows = filtered.slice(
    safePage * COMPONENTS_PAGE_SIZE,
    (safePage + 1) * COMPONENTS_PAGE_SIZE,
  );

  if (status === 'loading' || status === 'idle') {
    return (
      <Box pt="3" data-testid="nosc-app-detail-components-tab">
        <LoadingSkeleton height={240} data-testid="nosc-app-detail-components-loading" />
      </Box>
    );
  }

  if (status === 'error') {
    return (
      <Box pt="3" data-testid="nosc-app-detail-components-tab">
        <Card data-testid="nosc-app-detail-components-error">
          <Flex direction="column" gap="3" p="4" align="start">
            <Text size="3" color="red" weight="medium">
              Failed to load components
            </Text>
            <Button
              size="2"
              variant="soft"
              onClick={onRetry}
              data-testid="nosc-app-detail-components-retry"
            >
              Retry
            </Button>
          </Flex>
        </Card>
      </Box>
    );
  }

  if (components.length === 0) {
    return (
      <Box pt="3" data-testid="nosc-app-detail-components-tab">
        <Flex
          direction="column"
          align="center"
          gap="2"
          py="8"
          data-testid="nosc-app-detail-components-empty"
        >
          <DomainIcons.Component size={32} color="var(--gray-9)" />
          <Text size="3" color="gray">
            No components scanned yet
          </Text>
          <Text size="2" color="gray" align="center" style={{ maxWidth: 480 }}>
            Run an IQ scan against this application to populate the
            component inventory.
          </Text>
        </Flex>
      </Box>
    );
  }

  return (
    <Box pt="3" data-testid="nosc-app-detail-components-tab">
      <LargeScanBanner
        itemCount={components.length}
        itemLabel="components"
        guidance="Search and pagination run in your browser — use Classic for full export on very large inventories."
        testId="nosc-app-detail-components-large-scan"
      />
      <Flex justify="between" align="center" mb="3" gap="3" wrap="wrap">
        <Text size="2" color="gray">
          <strong>{components.length.toLocaleString()}</strong> components scanned in the latest report.
        </Text>
        <Flex align="center" gap="3">
          <TextField.Root
            placeholder="Search components or licenses…"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            size="2"
            style={{ minWidth: 280 }}
            data-testid="nosc-app-detail-components-search"
          />
          {scanId && (
            <RadixLink
              size="2"
              href={classicReportHrefForComponent(publicId, scanId)}
              data-testid="nosc-app-detail-components-classic-link"
            >
              View in Classic →
            </RadixLink>
          )}
        </Flex>
      </Flex>

      {filtered.length === 0 ? (
        <Flex
          direction="column"
          align="center"
          gap="2"
          py="6"
          data-testid="nosc-app-detail-components-no-matches"
        >
          <Text size="2" color="gray">
            No components match <strong>{searchTerm}</strong>.
          </Text>
        </Flex>
      ) : (
        <>
          <Table.Root
            variant="surface"
            size="2"
            data-testid="nosc-app-detail-components-table"
          >
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>Threat</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Match</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell justify="end">Violations</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>License</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Direct</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell justify="end">Actions</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {pageRows.map((c, idx) => {
                const threat = deriveComponentThreat(c);
                const violations = c.hash ? violationCountByHash[c.hash] ?? 0 : 0;
                const isDirect = c.dependencyData?.directDependency === true;
                const matchLabel = matchStateLabel(c.matchState);
                const rowKey = c.hash || `${idx}-${c.packageUrl ?? deriveComponentName(c)}`;
                return (
                  <Table.Row key={rowKey} data-testid="nosc-app-detail-components-row">
                    <Table.Cell>
                      <Badge color={threatColorFor(threat)} variant="solid">
                        {threat}
                      </Badge>
                    </Table.Cell>
                    <Table.Cell>
                      <Flex direction="column" gap="1">
                        <Text size="2" weight="medium">
                          {deriveComponentName(c)}
                        </Text>
                        {c.componentIdentifier?.format && (
                          <Text
                            size="1"
                            color="gray"
                            style={{ fontFamily: 'var(--code-font-family)' }}
                          >
                            {c.componentIdentifier.format}
                          </Text>
                        )}
                      </Flex>
                    </Table.Cell>
                    <Table.Cell>
                      <Badge color={matchStateColor(c.matchState)} variant="soft">
                        {matchLabel}
                      </Badge>
                    </Table.Cell>
                    <Table.Cell justify="end">
                      <Text size="2" color={violations > 0 ? 'orange' : 'gray'} weight={violations > 0 ? 'medium' : 'regular'}>
                        {violations}
                      </Text>
                    </Table.Cell>
                    <Table.Cell>
                      <Text size="2" color="gray">
                        {deriveLicense(c)}
                      </Text>
                    </Table.Cell>
                    <Table.Cell>
                      <Text size="2" color="gray">
                        {isDirect ? 'Direct' : 'Transitive'}
                      </Text>
                    </Table.Cell>
                    <Table.Cell justify="end">
                      {scanId ? (
                        <RadixLink
                          size="2"
                          href={classicReportHrefForComponent(publicId, scanId, c.hash)}
                          data-testid="nosc-app-detail-components-row-link"
                        >
                          View →
                        </RadixLink>
                      ) : (
                        <Text size="2" color="gray">
                          —
                        </Text>
                      )}
                    </Table.Cell>
                  </Table.Row>
                );
              })}
            </Table.Body>
          </Table.Root>

          {pageCount > 1 && (
            <Pagination
              page={safePage + 1}
              pageSize={COMPONENTS_PAGE_SIZE}
              totalItems={filtered.length}
              onPageChange={(next) => setPage(next - 1)}
              data-testid="nosc-app-detail-components-pagination"
            />
          )}
        </>
      )}
    </Box>
  );
}
