/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Flex, Heading, Text } from '@radix-ui/themes';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import ApplicationsFilterRail from 'MainRoot/nosc/applications/ApplicationsFilterRail';
import ApplicationsToolbar from 'MainRoot/nosc/applications/ApplicationsToolbar';
import EvaluationCardGrid from 'MainRoot/nosc/applications/EvaluationCardGrid';
import {
  ApplicationRiskScore,
  ApplicationsFilterFacetCounts,
} from 'MainRoot/nosc/applications/applicationListTypes';

import './applicationsPageLayout.css';

export interface ApplicationsPageProps {
  readonly applications: ReadonlyArray<ApplicationRiskScore>;
  readonly facets: ApplicationsFilterFacetCounts;
  readonly loading?: boolean;
  readonly error?: string | null;
  readonly onRetry?: () => void;
}

/**
 * Martha V1 Applications page shell (CLM-42223): filter rail + toolbar + card grid.
 *
 * Data wiring (POST /rest/dashboard/applications/list) lands in CLM-42228; filter
 * interactions in CLM-42225; card polish + stage navigation in CLM-42224.
 */
export default function ApplicationsPage({
  applications,
  facets,
  loading = false,
  error = null,
  onRetry,
}: ApplicationsPageProps): JSX.Element {
  const offsets = usePreviewShellOffsets();

  return (
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
          <Flex align="center" gap="3">
            <DomainIcons.Applications size={28} color="var(--accent-9)" />
            <Heading size="6">Applications</Heading>
          </Flex>
          <Text size="2" color="gray">
            Evaluation history and policy risk across every application visible to your account.
          </Text>
        </Flex>

        <Flex gap="4" align="start" wrap="wrap" data-testid="applications-page-layout">
          <ApplicationsFilterRail facets={facets} />
          <Box className="applications-page__content" data-testid="applications-page-content">
            <Flex direction="column" gap="4">
              <ApplicationsToolbar totalCount={applications.length} />

              <AsyncPageState
                loading={loading}
                error={error}
                onRetry={onRetry}
                loadingTestId="applications-list-loading"
                errorTestId="applications-list-error"
                errorTitle="Failed to load applications"
                errorVariant="banner"
              >
                {applications.length === 0 ? (
                  <Flex
                    direction="column"
                    align="center"
                    gap="2"
                    py="8"
                    data-testid="applications-list-empty"
                  >
                    <DomainIcons.Applications size={32} color="var(--gray-9)" />
                    <Text size="3" color="gray">
                      No applications to display.
                    </Text>
                    <Text size="2" color="gray">
                      Applications visible to your account will appear here once data is loaded.
                    </Text>
                  </Flex>
                ) : (
                  <EvaluationCardGrid applications={applications} />
                )}
              </AsyncPageState>
            </Flex>
          </Box>
        </Flex>
      </main>
    </Box>
  );
}
