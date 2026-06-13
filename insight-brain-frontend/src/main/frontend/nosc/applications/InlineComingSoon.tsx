/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Badge, Card, Flex, Text } from '@radix-ui/themes';
import { SectionHeading } from '@sonatype/nexus-one-components';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { ButtonLink } from 'MainRoot/nosc/components/ButtonLink';

/** Inline "Coming Soon" panel for a tab. Lighter than the full-page
 *  ComingSoonPage; matches its visual language (eyebrow badge, hero size 6,
 *  description, Continue-in-Classic button) but renders inside the existing
 *  Tabs.Content area without the fixed-position chrome. */
export function InlineComingSoon({
  testId,
  label,
  description,
  classicHref,
}: {
  readonly testId: string;
  readonly label: string;
  readonly description: string;
  readonly classicHref: string;
}): JSX.Element {
  return (
    <Flex mt="4" px="2" py="5" justify="center" data-testid={testId}>
      <Card size="3" maxWidth="560px" width="100%">
        <Flex direction="column" gap="4" align="center" p="3">
          <Badge size="2" color="gray" variant="soft">
            {label}
          </Badge>
          <SectionHeading align="center" mb="0">
            Coming Soon
          </SectionHeading>
          <Text size="2" align="center" color="gray" highContrast maxWidth="420px" style={{ lineHeight: 1.5 }}>
            {description}
          </Text>
          <Text size="1" align="center" color="gray" maxWidth="420px">
            We&apos;re still building the Nexus One version of {label}. In the meantime, do everything you need
            to in Classic IQ.
          </Text>
          <Flex align="center" gap="3" mt="2" wrap="wrap" justify="center">
            <ButtonLink
              href={classicHref}
              newTab
              size="2"
              variant="solid"
              color="green"
              aria-label={`Open ${label} in Classic IQ in a new tab`}
              data-testid={`${testId}-classic-newtab-button`}
            >
              <Flex align="center" gap="2">
                <ActionIcons.ExternalLink size={14} />
                <span>Open in Classic (new tab)</span>
              </Flex>
            </ButtonLink>
            <ButtonLink
              href={classicHref}
              size="2"
              variant="soft"
              color="gray"
              aria-label={`Continue to ${label} in Classic IQ in this tab`}
              data-testid={`${testId}-classic-samewindow-button`}
            >
              <Flex align="center" gap="2">
                <ActionIcons.Swap size={14} />
                <span>Continue in Classic</span>
              </Flex>
            </ButtonLink>
          </Flex>
        </Flex>
      </Card>
    </Flex>
  );
}
