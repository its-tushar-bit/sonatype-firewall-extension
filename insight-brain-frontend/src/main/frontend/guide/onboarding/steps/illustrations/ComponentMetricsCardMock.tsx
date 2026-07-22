/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Card, Flex, Grid, Text, Badge } from '@radix-ui/themes';
import { ThumbsUp, Check } from 'lucide-react';
import { BodyText } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import { CalloutPin } from './CalloutPin';
import styles from './illustrations.module.css';

/**
 * Static replica of the right-hand metrics card that `ComponentMetricsCard`
 * from `@guide/ui-core` renders inside each row of the Components list. Mirrors
 * the "Trust Score" + "Meets Policy" layout so the designer sees the real UI
 * we're pointing at, without depending on that component's data contract.
 */
export function ComponentMetricsCardMock() {
  return (
    <Card size={tokens.card.small} className={styles.frame}>
      <Flex align="center" gap={tokens.space.item}>
        {/* Left: component summary — approximates the left side of ComponentCard */}
        <Flex direction="column" gap={tokens.space.tight} style={{ flex: 1, minWidth: 0 }}>
          <Text weight="bold" size={tokens.sizes.cardTitle}>
            react-router-dom
          </Text>
          <BodyText size="xs" color="gray">
            6.15.0 · npm
          </BodyText>
          <Flex gap={tokens.space.tight}>
            <Badge color="gray" variant="soft" size="1">MIT</Badge>
            <Badge color="gray" variant="soft" size="1">Trending</Badge>
          </Flex>
        </Flex>

        {/* Right: ComponentMetricsCard replica */}
        <Card size={tokens.card.small} className={styles.metricsCard}>
          <Flex direction="column" gap={tokens.space.tight}>
            <BodyText size="xs" color="gray">6.15.0</BodyText>

            <Grid columns="1fr auto" align="center" gap={tokens.space.inline}>
              <BodyText size="xs" color="gray">Trust Score</BodyText>
              <Badge color="green" variant="solid" radius="full" size={tokens.badge.medium}>
                <ThumbsUp size={tokens.icon.small} />
                82
              </Badge>
            </Grid>

            <Grid columns="1fr auto" align="center" gap={tokens.space.inline}>
              <BodyText size="xs" color="gray">Meets Policy</BodyText>
              <div className={styles.pinnedInline}>
                <Badge color="amber" variant="soft" radius="full" size={tokens.badge.medium}>
                  <Check size={tokens.icon.theme} />
                </Badge>
                <CalloutPin
                  number={1}
                  title="Meets Policy icon"
                  body="Green ✓ / amber ✓ / red ✗ per component — quick scan across search results."
                  placement="right"
                  // The label's containing block only hugs the icon-only badge, so shrink-to-fit
                  // would collapse it to one word per line. Pin an explicit width so it reads normally.
                  labelStyle={{ width: 190 }}
                />
              </div>
            </Grid>
          </Flex>
        </Card>
      </Flex>
    </Card>
  );
}
