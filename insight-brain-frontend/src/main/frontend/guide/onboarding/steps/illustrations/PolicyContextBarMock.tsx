/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Badge, Card, Flex, Separator, Text } from '@radix-ui/themes';
import { ChevronDown } from 'lucide-react';
import { BodyText } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import { CalloutPin } from './CalloutPin';
import styles from './illustrations.module.css';

/**
 * Static replica of the real `PolicyContextBar` / `PolicyContextPicker` (the "Policy context"
 * trigger row shown at the top of content pages, above the page header/filters). Mirrors the
 * label + surface button (current owner + type badge) so the tour points at the shipped picker.
 * No "stage" — the picker selects an org or app only.
 */
export function PolicyContextBarMock() {
  return (
    <Card size={tokens.card.small} className={styles.frame}>
      <Flex direction="column" gap={tokens.space.item} className={styles.contextBar}>
        <Flex align="center" gap={tokens.space.item}>
          <Text size={tokens.sizes.body.sm} color="gray">
            Policy context
          </Text>
          <div className={styles.pinnedInline}>
            <Flex align="center" gap={tokens.space.tight} className={styles.pickerPlaceholder}>
              <BodyText size="sm">Root Organization</BodyText>
              <Badge color="gray" variant="soft" size={tokens.sizes.body.xs}>
                ORG
              </Badge>
              <ChevronDown size={tokens.icon.small} />
            </Flex>
            <CalloutPin
              number={1}
              title="Policy context picker"
              body="Switch org or app anytime — the page updates to that scope."
              placement="right"
              // The label's containing block only hugs the trigger, so shrink-to-fit would
              // collapse it to one word per line. Pin an explicit width so it reads normally.
              labelStyle={{ width: 190 }}
            />
          </div>
        </Flex>
        <Separator size="4" />
      </Flex>
    </Card>
  );
}
