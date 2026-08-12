/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useOutletContext } from 'react-router';
import { Box, Card, DataList, Flex, Grid } from '@radix-ui/themes';
import {
  SectionHeading,
  MarkdownContent,
  SeverityBadge,
  ThreatTypeBadge,
  TagGroup,
  SecurityEventBlogLink,
  LinedDataList,
  BodyText,
} from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import type { SecurityEventDetailDocument } from '@guide/ui-core/types';

function knownExploitedText(isKnownExploited?: boolean): string {
  if (isKnownExploited === undefined) return 'Not available.';
  return isKnownExploited
    ? 'Known to be exploited in the wild'
    : 'Not in KEV Catalog: No known exploits';
}

export function SecurityEventOverviewTab() {
  const event = useOutletContext<SecurityEventDetailDocument>();

  return (
    <Grid columns={{ initial: '1', md: '1fr 1fr' }} gap={tokens.space.section}>
      <Card size={tokens.card.large}>
        <Flex direction="column" gap={tokens.space.section}>
          <Box>
            <SectionHeading mb={tokens.space.inline}>Overview</SectionHeading>
            {event.detail?.trim() ? (
              <MarkdownContent content={event.detail} size="sm" />
            ) : (
              <BodyText tone="subtle">Not available.</BodyText>
            )}
          </Box>

          <Box>
            <SectionHeading mb={tokens.space.inline}>Details</SectionHeading>
            <LinedDataList>
              <DataList.Item>
                <DataList.Label>Severity</DataList.Label>
                <DataList.Value>
                  <SeverityBadge severity={event.eventSeverityCategory} />
                </DataList.Value>
              </DataList.Item>

              <DataList.Item>
                <DataList.Label>Threat Type</DataList.Label>
                <DataList.Value>
                  <ThreatTypeBadge threatType={event.eventThreatType} />
                </DataList.Value>
              </DataList.Item>

              <DataList.Item>
                <DataList.Label>Known Exploited</DataList.Label>
                <DataList.Value>{knownExploitedText(event.isKnownExploited)}</DataList.Value>
              </DataList.Item>

              <TagGroup label="Ecosystems" values={event.affectedEcosystems ?? []} />
              <TagGroup label="Malware Threat Types" values={event.malwareThreatTypes ?? []} />
              <TagGroup label="Attack Vectors" values={event.malwareAttackVectors ?? []} />
              <TagGroup label="CWEs" values={event.cwes ?? []} />
              <TagGroup label="Advisory References" values={event.advisoryReferenceIds ?? []} />
            </LinedDataList>
          </Box>
        </Flex>
      </Card>

      <Card size={tokens.card.large}>
        <Flex direction="column" gap={tokens.space.section}>
          <Box>
            <SectionHeading mb={tokens.space.inline}>Guidance</SectionHeading>
            {event.guidance?.trim() ? (
              <MarkdownContent content={event.guidance} size="sm" />
            ) : (
              <BodyText tone="subtle">Not available.</BodyText>
            )}
          </Box>

          <SecurityEventBlogLink url={event.sonatypeBlogUrl} />
        </Flex>
      </Card>
    </Grid>
  );
}
