/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Button, Checkbox, Flex, Text, VisuallyHidden } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { ViolationsListFacets } from 'MainRoot/nosc/violations/violationListTypes';
import {
  stageLabel,
  threatCategoryLabel,
  violationStateLabel,
} from 'MainRoot/nosc/violations/violationsListApi';
import './ViolationsFilterRail.scss';

export interface ViolationsFilterRailProps {
  readonly facets?: ViolationsListFacets;
  /**
   * id→display-name maps for the org / app facets (facet maps are id-keyed only; org/app rows carry
   * both id and name). Stage facets are labeled by {@link stageLabel} instead — the row-side stage is
   * a display name, not the id the facet is keyed by.
   */
  readonly labels?: {
    readonly organizations: Readonly<Record<string, string>>;
    readonly applications: Readonly<Record<string, string>>;
  };
}

type FacetEntry = { readonly id: string; readonly label: string; readonly count: number };

function toEntries(
  counts: Readonly<Record<string, number>> | undefined,
  labelFor: (id: string) => string,
): ReadonlyArray<FacetEntry> {
  if (!counts) return [];
  return Object.entries(counts)
    .map(([id, count]) => ({ id, label: labelFor(id), count }))
    .sort((a, b) => a.label.localeCompare(b.label));
}

function FilterSection({
  title,
  testId,
  entries,
}: {
  readonly title: string;
  readonly testId: string;
  readonly entries: ReadonlyArray<FacetEntry>;
}): JSX.Element | null {
  if (entries.length === 0) return null;
  return (
    <fieldset className="nosc-violations-filter-group" data-testid={testId}>
      <legend className="nosc-violations-filter-legend">{title}</legend>
      <Flex direction="column" gap="1">
        {entries.map(({ id, label, count }) => (
          <Text key={id} as="label" size="2" color="gray">
            <Flex align="center" gap="2">
              <Checkbox checked={false} disabled />
              {label}
              <Badge size="1" color="gray" variant="soft" radius="full">
                {count}
              </Badge>
            </Flex>
          </Text>
        ))}
      </Flex>
    </fieldset>
  );
}

/**
 * Filter sidebar scaffold for Martha V1 Violations (CLM-42257). Renders violation-state, policy-type,
 * stage, organization, and application sections from the API facet maps. Checkboxes are disabled;
 * selection + server refresh land with the filter sidebar story (CLM-42258).
 */
export default function ViolationsFilterRail({ facets, labels }: ViolationsFilterRailProps): JSX.Element {
  const orgLabel = (id: string): string => labels?.organizations[id] ?? id;
  const appLabel = (id: string): string => labels?.applications[id] ?? id;

  return (
    <Box asChild className="nosc-violations-filter-rail" data-testid="violations-filter-rail">
      <aside aria-label="Violation filters">
        <Flex align="center" justify="start" mb="4">
          {/* Disabled placeholder until interactive filters ship. title + aria-describedby explain why. */}
          <Button
            variant="outline"
            color="gray"
            size="2"
            disabled
            title="Reset filters — coming soon"
            aria-describedby="violations-filter-reset-hint"
            data-testid="violations-filter-reset"
          >
            <ActionIcons.Refresh size={12} />
            Reset filters
          </Button>
          <VisuallyHidden id="violations-filter-reset-hint">Reset filters is coming soon.</VisuallyHidden>
        </Flex>

        <Flex direction="column" gap="4">
          <FilterSection
            title="Violation State"
            testId="violations-filter-state"
            entries={toEntries(facets?.states, violationStateLabel)}
          />
          <FilterSection
            title="Policy Type"
            testId="violations-filter-policy-type"
            entries={toEntries(facets?.threatCategories, threatCategoryLabel)}
          />
          <FilterSection
            title="Stages"
            testId="violations-filter-stages"
            entries={toEntries(facets?.stages, stageLabel)}
          />
          <FilterSection
            title="Organizations"
            testId="violations-filter-organizations"
            entries={toEntries(facets?.organizations, orgLabel)}
          />
          <FilterSection
            title="Applications"
            testId="violations-filter-applications"
            entries={toEntries(facets?.applications, appLabel)}
          />
        </Flex>
      </aside>
    </Box>
  );
}
