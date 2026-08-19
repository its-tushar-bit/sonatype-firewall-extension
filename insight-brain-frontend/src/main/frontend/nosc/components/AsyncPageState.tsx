/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ReactNode } from 'react';
import { Button, Card, Flex, Heading, Text } from '@radix-ui/themes';
import { ActionIcons, StatusIcons } from 'MainRoot/nosc/icons';
import { LoadingSkeleton } from './LoadingSkeleton';

export type AsyncPageStateErrorVariant = 'card' | 'banner';
export type AsyncPageStateInfoVariant = 'card' | 'banner';

export interface AsyncPageStateInfoProps {
  readonly title: string;
  readonly message: string;
  readonly testId?: string;
}

export interface AsyncPageStateProps {
  readonly loading: boolean;
  readonly error: string | null;
  /** Informational full-page panel (e.g. index building / 409) with optional retry. */
  readonly info?: AsyncPageStateInfoProps | null;
  readonly children?: ReactNode;
  readonly onRetry?: () => void;
  readonly loadingHeight?: number;
  readonly loadingTestId?: string;
  readonly errorTestId?: string;
  readonly errorTitle?: string;
  readonly errorVariant?: AsyncPageStateErrorVariant;
  readonly infoVariant?: AsyncPageStateInfoVariant;
}

/**
 * Shared loading / error / ready gate for Nexus One pages and tables
 * (CLM-40901). Keeps skeleton + retry affordances consistent instead of
 * re-implementing the same branches on every async surface.
 */
export function AsyncPageState({
  loading,
  error,
  info = null,
  children = null,
  onRetry,
  loadingHeight = 240,
  loadingTestId,
  errorTestId,
  errorTitle = 'Failed to load',
  errorVariant = 'card',
  infoVariant = 'card',
}: AsyncPageStateProps): ReactNode {
  if (loading) {
    return <LoadingSkeleton height={loadingHeight} data-testid={loadingTestId} />;
  }

  if (info) {
    if (infoVariant === 'banner') {
      return (
        <Flex
          direction="column"
          gap="3"
          align="start"
          p="4"
          data-testid={info.testId}
          role="status"
          style={{ backgroundColor: 'var(--accent-3)', borderRadius: 'var(--radius-3)' }}
        >
          <Text size="2" weight="medium">
            {info.title}
          </Text>
          <Text size="2" color="gray">
            {info.message}
          </Text>
          {onRetry && <AsyncRetryButton onRetry={onRetry} />}
        </Flex>
      );
    }

    return (
      <Card data-testid={info.testId}>
        <Flex direction="column" gap="3" p="4" align="start" role="status">
          <Flex align="center" gap="2">
            <StatusIcons.Info size={18} color="var(--accent-9)" aria-hidden />
            <Heading size="3">{info.title}</Heading>
          </Flex>
          <Text size="2" color="gray">
            {info.message}
          </Text>
          {onRetry && <AsyncRetryButton onRetry={onRetry} />}
        </Flex>
      </Card>
    );
  }

  if (error) {
    if (errorVariant === 'banner') {
      return (
        <Flex
          direction="column"
          gap="3"
          align="start"
          p="4"
          data-testid={errorTestId}
          style={{ backgroundColor: 'var(--red-3)', borderRadius: 'var(--radius-3)' }}
        >
          <Text size="2" color="red">
            {errorTitle}: {error}
          </Text>
          {onRetry && <AsyncRetryButton onRetry={onRetry} />}
        </Flex>
      );
    }

    return (
      <Card data-testid={errorTestId}>
        <Flex direction="column" gap="3" p="4" align="start">
          <Text size="3" color="red" weight="medium">
            {errorTitle}
          </Text>
          <Text size="2" color="gray">
            {error}
          </Text>
          {onRetry && <AsyncRetryButton onRetry={onRetry} />}
        </Flex>
      </Card>
    );
  }

  return children;
}

function AsyncRetryButton({ onRetry }: { readonly onRetry: () => void }): JSX.Element {
  return (
    <Button type="button" variant="soft" size="2" onClick={onRetry}>
      <Flex as="span" align="center" gap="1">
        <ActionIcons.Refresh size={14} aria-hidden />
        Retry
      </Flex>
    </Button>
  );
}
