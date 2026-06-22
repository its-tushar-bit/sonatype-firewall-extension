/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Button, Checkbox, Flex, Text, Tooltip } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';

/**
 * P1-F13: filter sidebar for the /preview/search results page.
 *
 * Matches the visual idiom of
 * apps/nexusone-ux-prototype/src/components/applications/guide/
 * GuideApplicationFilters.tsx exactly:
 *
 *   - "Reset filters" outline button at top
 *   - Bold section labels (size 2, weight bold)
 *   - Per-section "Clear" link (chevron-left + blue bold "Clear") when
 *     there are active filters in that section
 *   - Checkbox + label + small soft gray rounded-full count Badge per option
 *
 * Phase 1 has one filter facet: Ecosystem. The result-set's ecosystems
 * are computed at the parent level (SearchResultsPage) and passed in.
 * Phase 1.5 adds Severity (vulns), Threat Level (policies), Last
 * Updated, Organization filters. Each follows the same pattern; just add
 * a new section here.
 */

const ECOSYSTEMS = [
  { value: 'maven', label: 'Maven' },
  { value: 'npm', label: 'npm' },
  { value: 'pypi', label: 'pypi' },
  { value: 'nuget', label: 'NuGet' },
  { value: 'docker', label: 'Docker' },
  { value: 'gem', label: 'RubyGems' },
  { value: 'go', label: 'Go' },
  { value: 'cargo', label: 'Cargo' },
];

interface SearchResultsFiltersProps {
  readonly ecosystems: readonly string[];
  readonly onEcosystemsChange: (next: readonly string[]) => void;
  readonly onClearAll: () => void;
}

export function SearchResultsFilters({
  ecosystems,
  onEcosystemsChange,
  onClearAll,
}: SearchResultsFiltersProps): JSX.Element {
  const toggleEcosystem = (eco: string): void => {
    const next = ecosystems.includes(eco)
      ? ecosystems.filter((e) => e !== eco)
      : [...ecosystems, eco];
    onEcosystemsChange(next);
  };

  const clearEcosystems = (): void => onEcosystemsChange([]);

  const hasAnyFilter = ecosystems.length > 0;

  return (
    <Box p="1" data-testid="nosc-search-filters">
      <Flex align="center" justify="start" mb="4">
        <Button
          variant="outline"
          color="gray"
          size="2"
          onClick={onClearAll}
          disabled={!hasAnyFilter}
          data-testid="nosc-search-filters-reset"
        >
          <ActionIcons.Refresh size={12} />
          Reset filters
        </Button>
      </Flex>

      <Flex direction="column" gap="4">
        <Box>
          <Flex align="center" justify="between" mb="3">
            <Text size="2" weight="bold">Ecosystem</Text>
          </Flex>
          <Flex direction="column" gap="1">
            {ecosystems.length > 0 && (
              <Flex
                align="center"
                gap="2"
                style={{ cursor: 'pointer' }}
                onClick={clearEcosystems}
                data-testid="nosc-search-filters-clear-ecosystem"
              >
                <ActionIcons.ChevronLeft size={14} color="var(--blue-11)" />
                <Text size="2" color="blue" style={{ fontWeight: 500 }}>Clear</Text>
              </Flex>
            )}
            {ECOSYSTEMS.map(({ value, label }) => (
              <Flex key={value} align="center" gap="2">
                <Checkbox
                  checked={ecosystems.includes(value)}
                  onCheckedChange={() => toggleEcosystem(value)}
                  data-testid={`nosc-search-filters-ecosystem-${value}`}
                />
                <Text
                  size="2"
                  style={{ cursor: 'pointer' }}
                  onClick={() => toggleEcosystem(value)}
                >
                  {label}
                </Text>
                <Tooltip content={`Filter results to ${label} components only`}>
                  <Badge size="1" color="gray" variant="soft" radius="full">
                    {/* Phase 1.5: per-ecosystem counts from a backend
                        aggregation. For now we just show a placeholder. */}
                    &mdash;
                  </Badge>
                </Tooltip>
              </Flex>
            ))}
          </Flex>
        </Box>
      </Flex>
    </Box>
  );
}
