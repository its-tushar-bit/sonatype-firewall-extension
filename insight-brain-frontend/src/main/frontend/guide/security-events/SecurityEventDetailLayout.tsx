/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useState } from 'react';
import { useParams } from 'react-router';
import { Box, Card, DataList, Flex, Grid, Skeleton } from '@radix-ui/themes';
import {
  PageLayout,
  Breadcrumbs,
  SecurityEventHeader,
  SectionHeading,
  MarkdownContent,
  SeverityBadge,
  ThreatTypeBadge,
  TagGroup,
  SecurityEventBlogLink,
  LinedDataList,
  BodyText,
  PageHeading,
  Button,
} from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import type { SecurityEventDetailDocument } from '@guide/ui-core/types';
import { getSecurityEventDetails } from 'GuideRoot/api/securityEventsBackend';
import { reloadPage, clearErrorRetries } from 'GuideRoot/utils/navigation';
import { ErrorPage } from 'GuideRoot/layout/ErrorPage';

function knownExploitedText(isKnownExploited?: boolean): string {
  if (isKnownExploited === undefined) return 'Not available.';
  return isKnownExploited
    ? 'Known to be exploited in the wild'
    : 'Not in KEV Catalog: No known exploits';
}

export function SecurityEventDetailLayout() {
  const { eventId } = useParams<{ eventId: string }>();
  const [event, setEvent] = useState<SecurityEventDetailDocument | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  useEffect(() => {
    if (!eventId) {
      setIsLoading(false);
      return;
    }

    let cancelled = false;
    setIsLoading(true);
    setIsError(false);
    setEvent(null);

    getSecurityEventDetails(eventId)
      .then((data) => {
        if (!cancelled) {
          setEvent(data);
          clearErrorRetries();
        }
      })
      .catch(() => {
        if (!cancelled) {
          setIsError(true);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [eventId]);

  if (isLoading) {
    return (
      <PageLayout>
        <Flex
          direction="column"
          gap={tokens.space.section}
          role="status"
          aria-busy="true"
          aria-label="Loading security event details"
        >
          <Flex gap={tokens.space.inline}>
            <Skeleton width="120px" height="20px" />
            <Skeleton width="160px" height="20px" />
          </Flex>
          <Box>
            <Skeleton width="60%" height="32px" mb={tokens.space.inline} />
            <Skeleton width="100%" height="20px" />
          </Box>
          <Grid columns={{ initial: '1', md: '1fr 1fr' }} gap={tokens.space.section}>
            <Skeleton width="100%" height="240px" />
            <Skeleton width="100%" height="240px" />
          </Grid>
        </Flex>
      </PageLayout>
    );
  }

  if (isError) {
    return <ErrorPage onRetry={reloadPage} />;
  }

  if (!event) {
    return (
      <PageLayout>
        <Flex direction="column" align="center" gap={tokens.space.section}>
          <PageHeading mb={tokens.space.section}>Security Event Not Found</PageHeading>
          <BodyText align="center">
            {eventId ? (
              <>Could not find security event data for &ldquo;{eventId}&rdquo;.</>
            ) : (
              'No security event ID provided.'
            )}
          </BodyText>
          {eventId && <BodyText align="center">Please check the ID and try again.</BodyText>}
          <Button variant="primary" href="/">
            Go to home
          </Button>
        </Flex>
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <Breadcrumbs
        items={[{ label: 'Security Events', href: '/security-events' }, { label: event.title }]}
      />
      <SecurityEventHeader event={event} />
      <Box mt={tokens.space.section}>
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

                  {event.affectedComponentVersionsCount !== undefined && (
                    <DataList.Item>
                      <DataList.Label>Affected Component Versions</DataList.Label>
                      <DataList.Value>{event.affectedComponentVersionsCount}</DataList.Value>
                    </DataList.Item>
                  )}

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
      </Box>
    </PageLayout>
  );
}
