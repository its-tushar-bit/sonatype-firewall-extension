/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Flex, Text, Card } from '@radix-ui/themes';
import { Search, ShieldCheck, ScanSearch } from 'lucide-react';
import { BodyText } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';

const FEATURES = [
  {
    Icon: Search,
    title: 'Component intelligence',
    body: 'Search millions of open-source components and dive into version history, dependencies, and popularity.',
  },
  {
    Icon: ScanSearch,
    title: 'Vulnerability research',
    body: 'Explore vulnerabilities with Sonatype-curated advisory notes, affected versions, and remediation guidance.',
  },
  {
    Icon: ShieldCheck,
    title: 'Policy compliance',
    body: 'See at a glance whether each component meets your organization’s policies before you adopt it.',
  },
];

export function WelcomeStep() {
  return (
    <Flex direction="column" gap={tokens.space.item}>
      <BodyText size="md" color="gray">
        Sonatype AI Developer gives you fast answers about open-source components and
        vulnerabilities — for every ecosystem you build with. Here’s a quick tour of what you can do.
      </BodyText>

      <Flex direction="column" gap={tokens.space.inline}>
        {FEATURES.map(({ Icon, title, body }) => (
          <Card key={title} size={tokens.card.small}>
            <Flex gap={tokens.space.item} align="start">
              <Icon size={tokens.icon.large} strokeWidth={tokens.iconStroke.default} />
              <Flex direction="column" gap={tokens.space.tight}>
                <Text weight="bold" size={tokens.sizes.cardTitle}>
                  {title}
                </Text>
                <BodyText size="sm" color="gray">
                  {body}
                </BodyText>
              </Flex>
            </Flex>
          </Card>
        ))}
      </Flex>
    </Flex>
  );
}
