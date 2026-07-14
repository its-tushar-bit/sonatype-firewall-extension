/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { Button, Flex, Text, TextField, VisuallyHidden } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';

export interface ViolationsToolbarProps {
  readonly totalCount: number;
  /** Current committed search term (controlled from the container). */
  readonly searchValue: string;
  /** Called with the trimmed term when the user submits the search (Enter). */
  readonly onSearchSubmit: (term: string) => void;
}

/**
 * Toolbar row for Martha V1 Violations (CLM-42257). Search submits on Enter and drives the server
 * query via the list API's {@code search} field. CSV export + interactive sort land with CLM-42260.
 */
export default function ViolationsToolbar({
  totalCount,
  searchValue,
  onSearchSubmit,
}: ViolationsToolbarProps): JSX.Element {
  const [draft, setDraft] = useState(searchValue);

  // Keep the local draft in sync when the committed value changes elsewhere (e.g. reset).
  useEffect(() => {
    setDraft(searchValue);
  }, [searchValue]);

  return (
    <Flex align="center" justify="between" gap="3" wrap="wrap" data-testid="violations-toolbar">
      <Flex align="center" gap="3" flexGrow="1" minWidth="240px">
        {/* The sort indicator is informational, so it lives outside role="search" — a screen reader
            should not announce it as part of the search form's accessible name/description. Interactive
            sort controls land in CLM-42260. */}
        <form
          role="search"
          onSubmit={(event) => {
            event.preventDefault();
            onSearchSubmit(draft.trim());
          }}
          // Span the full content width (no maxWidth cap) so the search bar fills the row per the
          // Lifecycle V1 prototype; flex:1 lets it grow next to the sort text.
          style={{ flex: 1 }}
        >
          <TextField.Root
            placeholder="Search component, application, organization, policy..."
            aria-label="Search violations"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            data-testid="violations-toolbar-search"
            style={{ width: '100%' }}
          >
            <TextField.Slot>
              <ActionIcons.Search size={16} />
            </TextField.Slot>
          </TextField.Root>
        </form>
        <Text size="2" color="gray" data-testid="violations-toolbar-sort">
          Sort: Threat (highest first)
        </Text>
      </Flex>

      <Flex align="center" gap="3">
        {/* Disabled placeholder until CSV export ships. title + aria-describedby give assistive tech a
            reason for the disabled state rather than an unexplained dimmed control. */}
        <Button
          variant="outline"
          color="gray"
          size="2"
          disabled
          title="CSV export — coming soon"
          aria-describedby="violations-toolbar-csv-hint"
          data-testid="violations-toolbar-csv"
        >
          <ActionIcons.Download size={14} />
          CSV
        </Button>
        <VisuallyHidden id="violations-toolbar-csv-hint">CSV export is coming soon.</VisuallyHidden>
        <Text size="2" color="gray" data-testid="violations-toolbar-count">
          {totalCount} {totalCount === 1 ? 'violation' : 'violations'}
        </Text>
      </Flex>
    </Flex>
  );
}
