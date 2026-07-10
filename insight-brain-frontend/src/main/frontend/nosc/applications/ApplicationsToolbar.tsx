/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Button, Flex, Text, TextField } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';

export interface ApplicationsToolbarProps {
  readonly totalCount: number;
}

/**
 * Toolbar row for the Martha V1 Applications page (CLM-42223 / CLM-42226).
 *
 * Search, CSV export, and interactive sort are placeholders until CLM-42226
 * wires URL state and export.
 */
export default function ApplicationsToolbar({ totalCount }: ApplicationsToolbarProps): JSX.Element {
  return (
    <Flex
      align="center"
      justify="between"
      gap="3"
      wrap="wrap"
      data-testid="applications-toolbar"
    >
      <Flex align="center" gap="3" flexGrow="1" minWidth="240px">
        <TextField.Root
          placeholder="Search applications..."
          aria-label="Search applications"
          disabled
          data-testid="applications-toolbar-search"
          style={{ flex: 1, maxWidth: '360px' }}
        >
          <TextField.Slot>
            <ActionIcons.Search size={16} />
          </TextField.Slot>
        </TextField.Root>
        <Text size="2" color="gray" data-testid="applications-toolbar-sort">
          Sort: Latest Evaluation
        </Text>
      </Flex>

      <Flex align="center" gap="3">
        <Button variant="outline" color="gray" size="2" disabled data-testid="applications-toolbar-csv">
          <ActionIcons.Download size={14} />
          CSV
        </Button>
        <Text size="2" color="gray" data-testid="applications-toolbar-count">
          {totalCount} {totalCount === 1 ? 'application' : 'applications'}
        </Text>
      </Flex>
    </Flex>
  );
}
