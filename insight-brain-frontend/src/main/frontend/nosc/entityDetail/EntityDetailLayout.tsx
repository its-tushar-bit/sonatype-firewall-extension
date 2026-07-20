/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import type { ReactElement, ReactNode } from 'react';
import { Box, Flex, Tabs, Text } from '@radix-ui/themes';
import { ErrorBoundary } from 'react-error-boundary';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { EntityDetailContextRail } from './EntityDetailContextRail';
import type { EntityDetailContextChain } from './entityDetailTypes';

export interface EntityDetailLayoutTab {
  readonly value: string;
  readonly label: ReactNode;
  readonly testId: string;
}

export interface EntityDetailLayoutProps {
  readonly breadcrumb: ReactNode;
  readonly header: ReactNode;
  readonly context: EntityDetailContextChain | null;
  readonly tabs: readonly EntityDetailLayoutTab[];
  /**
   * Controlled tab value. Callers own keeping this in {@link tabs}; when it does
   * not match any tab, the shell renders {@code tabs[0]} for display only and does
   * not call {@link onTabChange}. Router/URL correction belongs to the consumer.
   */
  readonly activeTab: string;
  readonly onTabChange: (value: string) => void;
  readonly children: ReactNode;
  readonly mainTestId: string;
  /**
   * Prefix for shared shell test ids (`-tabs`, `-tab-content-*`, context rail).
   * Defaults to {@code nosc-entity-detail}; pass a per-entity value when multiple
   * detail pages may be mounted or queried in the same test.
   */
  readonly testIdPrefix?: string;
}

function resolveActiveTab(tabs: readonly EntityDetailLayoutTab[], activeTab: string): string {
  if (tabs.some((tab) => tab.value === activeTab)) {
    return activeTab;
  }
  return tabs[0]?.value ?? activeTab;
}

function TabErrorFallback({ testIdPrefix }: { readonly testIdPrefix: string }): ReactElement {
  return (
    <Flex direction="column" gap="2" p="4" mt="4" data-testid={`${testIdPrefix}-tab-error`}>
      <Text size="3" color="red" weight="medium">
        This tab failed to load.
      </Text>
      <Text size="2" color="gray">
        Try another tab, or reload the page.
      </Text>
    </Flex>
  );
}

export function EntityDetailLayout({
  breadcrumb,
  header,
  context,
  tabs,
  activeTab,
  onTabChange,
  children,
  mainTestId,
  testIdPrefix = 'nosc-entity-detail',
}: EntityDetailLayoutProps): ReactElement {
  const offsets = usePreviewShellOffsets();
  const resolvedActiveTab = resolveActiveTab(tabs, activeTab);

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
      <main data-testid={mainTestId}>
        <Box mb="3">{breadcrumb}</Box>
        <Box mb="5">{header}</Box>

        {context && (
          <EntityDetailContextRail context={context} testId={`${testIdPrefix}-context-rail`} />
        )}

        <Tabs.Root
          value={resolvedActiveTab}
          onValueChange={onTabChange}
          data-testid={`${testIdPrefix}-tabs`}
        >
          <Tabs.List size="2">
            {tabs.map((tab) => (
              <Tabs.Trigger key={tab.value} value={tab.value} data-testid={tab.testId}>
                {tab.label}
              </Tabs.Trigger>
            ))}
          </Tabs.List>

          {tabs.map((tab) => (
            <Tabs.Content
              key={tab.value}
              value={tab.value}
              data-testid={`${testIdPrefix}-tab-content-${tab.value}`}
            >
              {/* Only the active tab mounts an ErrorBoundary; switching tabs remounts a fresh boundary. */}
              {tab.value === resolvedActiveTab ? (
                <ErrorBoundary
                  onError={(error) => {
                    // eslint-disable-next-line no-console
                    console.error('Entity detail tab failed to load', error);
                  }}
                  fallbackRender={() => <TabErrorFallback testIdPrefix={testIdPrefix} />}
                >
                  {children}
                </ErrorBoundary>
              ) : null}
            </Tabs.Content>
          ))}
        </Tabs.Root>
      </main>
    </Box>
  );
}
