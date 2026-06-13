/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Flex, Heading, Link, Table, Text } from '@radix-ui/themes';
import { useTile } from 'MainRoot/nosc/dashboard/useTile';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { getApplicationsUrl } from 'MainRoot/util/CLMLocation';
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';
import { useRouter } from '@uirouter/react';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';

import '@radix-ui/themes/styles.css';

/**
 * Preview Applications list page (CLM-39709).
 *
 * Read-only list: real data from `GET /rest/application`, rendered as a Radix
 * Table inside the Preview shell. Each row links to the native Preview
 * Application Detail page (`nexusOneApplicationsDetail`); a secondary "Open in
 * Classic" link is kept as a one-click escape hatch.
 *
 * The endpoint returns a top-level JSON array (no envelope).
 *
 * TODO(CLM-39709): filter sidebar, sort, and pagination are follow-ups.
 */
interface ApplicationSummary {
  id: string;
  publicId: string;
  name: string;
  organizationId?: string;
  organizationName?: string;
}

export default function ApplicationsList() {
  const { status, data, retry } = useTile<ApplicationSummary[]>(getApplicationsUrl());
  const offsets = usePreviewShellOffsets();
  const { stateService } = useRouter();

  // Native Preview app-detail route via the UI-Router state registry; the
  // Classic escape hatch goes through bundleIndexUrl (context-path / MTIQ aware).
  const previewAppDetailUrl = (publicId: string): string =>
    stateService.href('nexusOneApplicationsDetail', { publicId });
  const classicAppUrl = (publicId: string): string =>
    bundleIndexUrl('classic', `/management/view/application/${encodeURIComponent(publicId)}`);

  const apps = Array.isArray(data) ? data : [];

  return (
    // Radix Theme is provided once by NexusOneShellLayout; render content into a
    // fixed, scrollable <main> region below the shell chrome.
    <Box
      asChild
      p="6"
      style={{
        position: 'fixed',
        ...offsets,
        right: 0,
        bottom: 0,
        overflowY: 'auto',
        backgroundColor: 'var(--gray-1)',
      }}
    >
      <main data-testid="preview-applications-page">
        <Flex direction="column" gap="2" mb="5">
          <Flex align="center" justify="between">
            <Flex align="center" gap="3">
              <DomainIcons.Applications size={28} color="var(--accent-9)" />
              <Heading size="6">Applications</Heading>
            </Flex>
            {status === 'ready' && (
              <Text size="2" color="gray">
                {apps.length} {apps.length === 1 ? 'application' : 'applications'} in scope
              </Text>
            )}
          </Flex>
          <Text size="2" color="gray">
            All applications visible to your account, scanned by IQ Server.
          </Text>
        </Flex>

        {status === 'loading' && (
          <LoadingSkeleton height={240} data-testid="applications-list-loading" />
        )}

        {status === 'error' && (
          <Flex
            direction="column"
            gap="3"
            align="start"
            p="4"
            data-testid="applications-list-error"
            style={{
              backgroundColor: 'var(--red-3)',
              borderRadius: 'var(--radius-3)',
            }}
          >
            <Text size="2" color="red">
              Failed to load applications.
            </Text>
            <Link size="2" href="#" onClick={(e) => { e.preventDefault(); retry(); }}>
              Retry
            </Link>
          </Flex>
        )}

        {status === 'ready' && apps.length === 0 && (
          <Flex
            direction="column"
            align="center"
            gap="2"
            py="8"
            data-testid="applications-list-empty"
          >
            <DomainIcons.Applications size={32} color="var(--gray-9)" />
            <Text size="3" color="gray">
              No applications in scope yet.
            </Text>
            <Text size="2" color="gray">
              Run an IQ scan against an application to see it here.
            </Text>
          </Flex>
        )}

        {status === 'ready' && apps.length > 0 && (
          <Table.Root variant="surface" size="2" data-testid="applications-list-table">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>Name</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Public ID</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Organization</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell justify="end">Actions</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {apps.map((app) => (
                <Table.Row key={app.id} data-testid="applications-list-row">
                  <Table.RowHeaderCell>
                    <Link
                      href={previewAppDetailUrl(app.publicId)}
                      weight="medium"
                      data-testid="applications-list-row-name-link"
                    >
                      {app.name}
                    </Link>
                  </Table.RowHeaderCell>
                  <Table.Cell>
                    <Text size="2" color="gray" style={{ fontFamily: 'var(--code-font-family)' }}>
                      {app.publicId}
                    </Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2">{app.organizationName ?? '—'}</Text>
                  </Table.Cell>
                  <Table.Cell justify="end">
                    <Flex align="center" gap="3" justify="end">
                      <Link
                        size="2"
                        href={previewAppDetailUrl(app.publicId)}
                        data-testid="applications-list-row-detail-link"
                      >
                        View Details →
                      </Link>
                      <Link
                        size="2"
                        color="gray"
                        href={classicAppUrl(app.publicId)}
                        data-testid="applications-list-row-classic-link"
                      >
                        Classic
                      </Link>
                    </Flex>
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        )}
      </main>
    </Box>
  );
}
