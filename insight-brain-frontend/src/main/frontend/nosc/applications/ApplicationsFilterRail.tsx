/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Button, Checkbox, Flex, Text } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { ApplicationsFilterFacetCounts } from 'MainRoot/nosc/applications/applicationListTypes';
import { ApplicationsListFilterState } from 'MainRoot/nosc/applications/applicationsListFilters';
import './ApplicationsFilterRail.scss';

export interface ApplicationsFilterRailProps {
  readonly facets: ApplicationsFilterFacetCounts;
  readonly filters: ApplicationsListFilterState;
  readonly hasActiveFilters: boolean;
  readonly onToggleFilter: (
    field: keyof ApplicationsListFilterState,
    id: string,
  ) => void;
  readonly onResetFilters: () => void;
}

/**
 * Filter sidebar for the Martha V1 Applications page.
 *
 * Selection state is local to the list page and refreshes via POST
 * /rest/dashboard/applications/list with stage/org/app/threat filters.
 */
export default function ApplicationsFilterRail({
  facets,
  filters,
  hasActiveFilters,
  onToggleFilter,
  onResetFilters,
}: ApplicationsFilterRailProps): JSX.Element {
  return (
    <Box asChild className="nosc-applications-filter-rail" data-testid="applications-filter-rail">
      <aside aria-label="Application filters">
      <Flex align="center" justify="start" mb="4">
        <Button
          variant="outline"
          color="gray"
          size="2"
          disabled={!hasActiveFilters}
          onClick={onResetFilters}
          data-testid="applications-filter-reset"
        >
          <ActionIcons.Refresh size={12} />
          Reset filters
        </Button>
      </Flex>

      <Flex direction="column" gap="4">
        {facets.threatLevels.length > 0 && (
        <fieldset className="nosc-applications-filter-group" data-testid="applications-filter-threat-level">
          <legend className="nosc-applications-filter-legend">Policy Threat Level</legend>
          <Flex direction="column" gap="1">
            {facets.threatLevels.map(({ id, label, count }) => (
              <Text key={id} as="label" size="2" color="gray">
                <Flex align="center" gap="2">
                  <Checkbox
                    checked={filters.threatLevelIds.has(id)}
                    onCheckedChange={() => onToggleFilter('threatLevelIds', id)}
                  />
                  {label}
                  {count > 0 && (
                    <Badge size="1" color="gray" variant="soft" radius="full">
                      {count}
                    </Badge>
                  )}
                </Flex>
              </Text>
            ))}
          </Flex>
        </fieldset>
        )}

        <fieldset className="nosc-applications-filter-group" data-testid="applications-filter-stages">
          <legend className="nosc-applications-filter-legend">Stages</legend>
          <Flex direction="column" gap="1">
            {facets.stages.map(({ id, label, count }) => (
              <Text key={id} as="label" size="2" color="gray">
                <Flex align="center" gap="2">
                  <Checkbox
                    checked={filters.stageIds.has(id)}
                    onCheckedChange={() => onToggleFilter('stageIds', id)}
                  />
                  {label}
                  {count > 0 && (
                    <Badge size="1" color="gray" variant="soft" radius="full">
                      {count}
                    </Badge>
                  )}
                </Flex>
              </Text>
            ))}
          </Flex>
        </fieldset>

        <fieldset className="nosc-applications-filter-group" data-testid="applications-filter-organizations">
          <legend className="nosc-applications-filter-legend">Organizations</legend>
          <Flex direction="column" gap="1">
            {facets.organizations.map(({ id, label, count }) => (
              <Text key={id} as="label" size="2" color="gray">
                <Flex align="center" gap="2">
                  <Checkbox
                    checked={filters.organizationIds.has(id)}
                    onCheckedChange={() => onToggleFilter('organizationIds', id)}
                  />
                  {label}
                  {count > 0 && (
                    <Badge size="1" color="gray" variant="soft" radius="full">
                      {count}
                    </Badge>
                  )}
                </Flex>
              </Text>
            ))}
          </Flex>
        </fieldset>

        <fieldset className="nosc-applications-filter-group" data-testid="applications-filter-applications">
          <legend className="nosc-applications-filter-legend">Applications</legend>
          <Flex direction="column" gap="1">
            {facets.applications.map(({ id, label, count }) => (
              <Text key={id} as="label" size="2" color="gray">
                <Flex align="center" gap="2">
                  <Checkbox
                    checked={filters.applicationIds.has(id)}
                    onCheckedChange={() => onToggleFilter('applicationIds', id)}
                  />
                  {label}
                  {count > 0 && (
                    <Badge size="1" color="gray" variant="soft" radius="full">
                      {count}
                    </Badge>
                  )}
                </Flex>
              </Text>
            ))}
          </Flex>
        </fieldset>
      </Flex>
      </aside>
    </Box>
  );
}
