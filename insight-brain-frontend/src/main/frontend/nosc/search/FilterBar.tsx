/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useRef } from 'react';
import { Box, Button, DropdownMenu, Flex, Separator, Text } from '@radix-ui/themes';
import { FILTER_TREE, FilterLeaf, FilterNode } from 'MainRoot/nosc/search/searchFilterTree';
import { quoteEnumValue } from 'MainRoot/nosc/search/searchFilterInsert';

/**
 * The "add search terms" filter builder: a row of category buttons (Type,
 * Application, Component, License, Organization, Policy, Violation,
 * Vulnerability), each opening a Radix DropdownMenu of leaves. A leaf inserts its
 * query syntax into the search input; enum-backed leaves open one more submenu
 * level (a value flyout) so the value is picked rather than typed.
 *
 * Toggled visible by the omnibar's "Show filters" button. Rendered as a
 * `toolbar` (independent commands, not a selectable set). Radix supplies the ARIA
 * roles + keyboard handling for the menus (Enter/Space to open, arrows within,
 * Esc to close). The omnibar's global arrow-nav handler bails when focus is
 * inside a Radix popper so ↓ navigates the open menu, not the results listbox.
 */

/**
 * A resolved insertion request. `syntax` is the literal string to append (enum
 * value already appended when applicable). Whether the syntax is a committable
 * predicate is derived by the caller from `computeFilterInsert`, which reports it
 * as `complete`.
 */
export interface FilterInsertRequest {
  readonly syntax: string;
  readonly label: string;
}

export interface FilterBarProps {
  /** Called with the resolved syntax when a leaf is chosen. */
  readonly onInsert: (request: FilterInsertRequest) => void;
  /** Filter tree to render. Defaults to the shared FILTER_TREE. */
  readonly nodes?: readonly FilterNode[];
  /**
   * `compact` strips the toolbar padding and the trailing separator for callers
   * that place the bar inside already-spaced page chrome (the results page).
   */
  readonly compact?: boolean;
  /**
   * `hideTriggerIcons` drops the chevron from each category pill for callers whose
   * bar sits in a narrow container (the omnibar panel), where the extra width
   * pushes the categories onto a second row.
   */
  readonly hideTriggerIcons?: boolean;
  /** DOM id so the toggle button can reference it via aria-controls. */
  readonly id?: string;
  /**
   * Called when a category menu closes (Esc or outside click) without a leaf
   * being chosen. The omnibar uses it to return focus to the search input, which
   * would otherwise fall to document.body (onCloseAutoFocus is prevented so Radix
   * doesn't yank focus back to the trigger and race the panel).
   */
  readonly onMenuClose?: () => void;
}

function isEnumLeaf(leaf: FilterLeaf): boolean {
  return !!leaf.values && leaf.values.length > 0;
}

export function FilterBar({
  onInsert,
  nodes = FILTER_TREE,
  compact = false,
  hideTriggerIcons = false,
  id,
  onMenuClose,
}: FilterBarProps): JSX.Element {
  return (
    <Box id={id}>
      <Flex
        role="toolbar"
        aria-label="Search filters"
        gap="2"
        px={compact ? '0' : '4'}
        py={compact ? '0' : '2'}
        wrap="wrap"
        // In the omnibar the bar sits below a narrow (~560px) input; without a
        // content-sized floor the eight category pills wrap onto a second row.
        // The prototype lets the bar break out to its natural single-row width
        // (matching its wide results card), so pin the row to its content width.
        // Compact callers already sit in wide page chrome and keep the default.
        style={compact ? undefined : { minWidth: 'max-content' }}
      >
        {nodes.map((node) => (
          <FilterCategory
            key={node.label}
            node={node}
            onInsert={onInsert}
            onMenuClose={onMenuClose}
            hideTriggerIcons={hideTriggerIcons}
          />
        ))}
      </Flex>
      {!compact && <Separator size="4" />}
    </Box>
  );
}

function FilterCategory({
  node,
  onInsert,
  onMenuClose,
  hideTriggerIcons,
}: {
  node: FilterNode;
  onInsert: (request: FilterInsertRequest) => void;
  onMenuClose?: () => void;
  hideTriggerIcons?: boolean;
}): JSX.Element {
  // A leaf selection already returns focus to the input (with a precise caret) via
  // the omnibar's insert handler; only a dismissal (Esc / outside click) needs the
  // fallback focus-return, so suppress it when the menu closed off a selection to
  // avoid two focus calls racing over the caret position.
  const selectedRef = useRef(false);
  const handleInsert = (request: FilterInsertRequest): void => {
    selectedRef.current = true;
    onInsert(request);
  };
  return (
    // modal={false} keeps the body's pointer-events intact so an outside click
    // focuses its target normally instead of routing through the menu's
    // dismissable layer — otherwise the focus race with the omnibar's
    // outside-click handler can close the whole search overlay.
    <DropdownMenu.Root
      modal={false}
      onOpenChange={(open) => {
        if (open) {
          selectedRef.current = false;
          return;
        }
        if (!selectedRef.current) onMenuClose?.();
      }}
    >
      <DropdownMenu.Trigger>
        <Button
          size="2"
          variant="outline"
          color="gray"
          data-testid={`nosc-search-filter-category-${node.label.toLowerCase()}`}
        >
          {node.label}
          {!hideTriggerIcons && <DropdownMenu.TriggerIcon />}
        </Button>
      </DropdownMenu.Trigger>
      <DropdownMenu.Content
        size="2"
        align="start"
        // Radix returns focus to the trigger on close; we prevent that so the
        // shared focusInputWithCaret helper (called by the omnibar) can return
        // focus to the search input instead.
        onCloseAutoFocus={(e) => e.preventDefault()}
      >
        {node.leaves?.map((leaf) => (
          <LeafItem key={leaf.label + leaf.syntax} leaf={leaf} onInsert={handleInsert} />
        ))}
        {node.groups?.map((group) => (
          <DropdownMenu.Sub key={group.label}>
            <DropdownMenu.SubTrigger>{group.label}</DropdownMenu.SubTrigger>
            <DropdownMenu.SubContent>
              {group.leaves.map((leaf) => (
                <LeafItem key={leaf.label + leaf.syntax} leaf={leaf} onInsert={handleInsert} />
              ))}
            </DropdownMenu.SubContent>
          </DropdownMenu.Sub>
        ))}
      </DropdownMenu.Content>
    </DropdownMenu.Root>
  );
}

function LeafItem({
  leaf,
  onInsert,
}: {
  leaf: FilterLeaf;
  onInsert: (request: FilterInsertRequest) => void;
}): JSX.Element {
  const testId = `nosc-search-filter-leaf-${leaf.syntax}`;

  if (isEnumLeaf(leaf)) {
    return (
      <DropdownMenu.Sub>
        <DropdownMenu.SubTrigger data-testid={testId}>
          <LeafContent label={leaf.label} hint={leaf.syntax} />
        </DropdownMenu.SubTrigger>
        <DropdownMenu.SubContent>
          {leaf.values!.map((value) => {
            const syntax = `${leaf.syntax}${quoteEnumValue(value)}`;
            return (
              <DropdownMenu.Item
                key={value}
                data-testid={`nosc-search-filter-value-${syntax}`}
                onSelect={() => onInsert({ syntax, label: `${leaf.label} = ${value}` })}
              >
                <LeafContent label={value} hint={syntax} />
              </DropdownMenu.Item>
            );
          })}
        </DropdownMenu.SubContent>
      </DropdownMenu.Sub>
    );
  }

  return (
    <DropdownMenu.Item
      data-testid={testId}
      onSelect={() => onInsert({ syntax: leaf.syntax, label: leaf.label })}
    >
      <LeafContent label={leaf.label} hint={leaf.syntax} />
    </DropdownMenu.Item>
  );
}

function LeafContent({ label, hint }: { label: string; hint: string }): JSX.Element {
  return (
    <Flex justify="between" align="center" gap="6" width="100%" minWidth="220px">
      <Text size="2">{label}</Text>
      <Text size="1" color="gray" style={{ fontFamily: 'var(--code-font-family)' }}>
        {hint}
      </Text>
    </Flex>
  );
}
