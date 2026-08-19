/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Card, Flex, Text, Badge, Separator, Link } from '@radix-ui/themes';
import { Check } from 'lucide-react';
import { BodyText } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import { CalloutPin } from './CalloutPin';
import styles from './illustrations.module.css';

// Mock data for illustration only — not a real CVE.
const VIOLATIONS = [
  { name: 'Security-High', hint: 'CVE-2024-1234' },
  { name: 'License-Copyleft', hint: 'GPL-3.0' },
  { name: 'Age-Old', hint: 'Waived' },
];

/**
 * Static replica of `PolicyComplianceCardV2` — the compliance card shown on
 * every component detail page. Numbered callouts point at the tri-state
 * badge, the policy context link, and the violations list.
 */
export function PolicyComplianceCardMock() {
  return (
    <Card size={tokens.card.medium} className={styles.frame}>
      <Flex direction="column" gap={tokens.space.item}>
        {/* Header row — SectionHeading + PolicyBadgeV2 */}
        <Flex justify="between" align="center">
          <Text weight="bold" size={tokens.sizes.cardTitle}>
            Policy Compliance
          </Text>
          <div className={styles.pinnedInline}>
            <Badge color="amber" variant="soft" radius="full" size={tokens.badge.medium}>
              <Check size={tokens.icon.theme} />
              Partial
            </Badge>
            <CalloutPin
              number={2}
              title="Compliance badge"
              body="Tri-state verdict for this version — green PASS · amber PARTIAL · red FAIL."
              placement="left"
            />
          </div>
        </Flex>

        {/* Policy context line */}
        <div className={styles.pinnedBlock}>
          <BodyText size="sm" color="gray">
            Policy context:{' '}
            <Link color="blue" href="#" onClick={(e) => e.preventDefault()}>
              Root Organization · Release Stage · via Lifecycle
            </Link>
          </BodyText>
          <CalloutPin
            number={3}
            title="Policy context"
            body="Which org & stage this component was evaluated against — the link jumps into Lifecycle."
            placement="bottom"
            pinStyle={{ top: '-11px', left: '78px', right: 'auto' }}
          />
        </div>

        <Separator size="4" />

        {/* Violations list */}
        <div className={styles.pinnedBlock}>
          <Flex direction="column" gap={tokens.space.tight}>
            <Text weight="bold" size={tokens.sizes.caption}>
              Violations ({VIOLATIONS.length})
            </Text>
            {VIOLATIONS.map((v) => (
              <Flex key={v.name} justify="between">
                <BodyText size="sm">{v.name}</BodyText>
                <BodyText size="sm" color="gray">{v.hint}</BodyText>
              </Flex>
            ))}
          </Flex>
          <CalloutPin
            number={4}
            title="Violations"
            body="Grouped by policy, with waived state shown inline."
            placement="left"
            pinStyle={{ top: '-11px', right: '-11px', left: 'auto' }}
          />
        </div>
      </Flex>
    </Card>
  );
}
