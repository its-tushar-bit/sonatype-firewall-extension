/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Flex, Link, Separator, Text } from '@radix-ui/themes';

/**
 * Shared primitives for the global-search panel: the listbox option row, the
 * leading icon slot, and the panel footer.
 *
 * Every panel view (recent searches, placeholder, results) renders its rows
 * through ListboxOption so highlight painting, ARIA, and padding are defined
 * once. Rows are not Tab stops — they participate in the input's listbox
 * composite via aria-activedescendant, so the parent owns which row is active.
 */

export interface ListboxOptionProps {
  /** DOM id referenced by the input's aria-activedescendant. */
  readonly id: string;
  /** True when this row is the active descendant. */
  readonly active: boolean;
  /** Fired on hover so the parent can sync the highlight to the pointer. */
  readonly onHighlight: (id: string) => void;
  /** Activate the row (navigate / submit). */
  readonly onActivate?: () => void;
  readonly children: React.ReactNode;
  readonly testId?: string;
}

export function ListboxOption({
  id,
  active,
  onHighlight,
  onActivate,
  children,
  testId,
}: ListboxOptionProps): JSX.Element {
  return (
    <Box
      role="option"
      id={id}
      aria-selected={active}
      tabIndex={-1}
      data-active={active || undefined}
      data-testid={testId}
      className="nosc-search-option"
      px="4"
      py="2"
      onMouseEnter={() => onHighlight(id)}
      onMouseDown={(event: React.MouseEvent) => {
        if (!onActivate) return;
        // Keep focus on the input so the listbox composite survives the click,
        // for any button — a right-click must not blur the input either.
        event.preventDefault();
        // Only the primary button activates: right-click keeps the browser
        // context menu and middle-click stays a no-op.
        if (event.button !== 0) return;
        onActivate();
      }}
    >
      {children}
    </Box>
  );
}

/** Fixed 24px leading slot so icons and severity badges align across rows. */
export function IconSlot({ children }: { readonly children: React.ReactNode }): JSX.Element {
  return (
    <Flex align="center" justify="center" flexShrink="0" width="24px" height="24px" className="nosc-search-icon-slot">
      {children}
    </Flex>
  );
}

/** Trailing "Jump to" affordance shown on every activatable row. */
export function JumpToHint(): JSX.Element {
  return <span className="nosc-search-jump-to">Jump to</span>;
}

/**
 * Explains the reduced tab set while the Sonatype Catalog is the active source:
 * the catalog serves components and vulnerabilities only, so the IQ-local tabs
 * (Applications / Violations / Waivers) are absent rather than empty.
 */
export function CatalogScopeHint(): JSX.Element {
  return (
    <Box px="4" pb="2" data-testid="nosc-search-catalog-hint">
      <Text size="1" color="gray">
        Sonatype Catalog covers components and vulnerabilities.
      </Text>
    </Box>
  );
}

export interface PanelFooterProps {
  /** Search-syntax docs target, opened in a new tab. */
  readonly syntaxDocsUrl: string;
}

/** Bottom strip of the panel: the search-syntax docs link. */
export function PanelFooter({ syntaxDocsUrl }: PanelFooterProps): JSX.Element {
  return (
    <>
      <Separator size="4" />
      <Flex justify="between" align="center" px="4" py="3" flexShrink="0" data-testid="nosc-search-panel-footer">
        <Link
          size="1"
          href={syntaxDocsUrl}
          target="_blank"
          rel="noopener noreferrer"
          underline="hover"
          data-testid="nosc-search-syntax-link"
        >
          Search syntax
        </Link>
      </Flex>
    </>
  );
}
