/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Fragment } from 'react';
import type { ReactNode } from 'react';
import { Flex, Spinner } from '@radix-ui/themes';
import { usePolicyContext } from 'GuideRoot/components/navigation/context-picker/PolicyContext';

/**
 * Boundary around the routed Guide content that ties every view to the policy-context selection:
 *
 * - While the persisted selection is still resolving on load ({@code !hydrated}), it shows a
 *   spinner in the content area (nav chrome stays mounted above it) so the first Guide data
 *   request waits for the resolved owner instead of firing at root and re-fetching.
 * - Once hydrated, it keys the subtree on the active owner id, so switching the picker remounts
 *   the routed content and every view re-fetches with the new scope. URL-driven filters/sort/
 *   pagination survive the remount because they live in the URL, not component state.
 */
export function PolicyScopeBoundary({ children }: { children: ReactNode }) {
  const { hydrated, activeOwner } = usePolicyContext();

  if (!hydrated) {
    return (
      <Flex align="center" justify="center" style={{ minHeight: '60dvh' }}>
        <Spinner size="3" />
      </Flex>
    );
  }

  return <Fragment key={activeOwner?.id ?? 'root'}>{children}</Fragment>;
}
