/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Flex, Text } from '@radix-ui/themes';
import { BodyText } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import { ComponentMetricsCardMock } from './illustrations/ComponentMetricsCardMock';
import { PolicyComplianceCardMock } from './illustrations/PolicyComplianceCardMock';

export function PolicyStep() {
  return (
    <Flex direction="column" gap={tokens.space.section}>
      <BodyText size="md" color="gray">
        AI Developer surfaces policy compliance in two places: alongside every component in search
        results, and on the detail page for each component version.
      </BodyText>

      <Flex direction="column" gap={tokens.space.tight}>
        <Text size={tokens.sizes.caption} color="gray" weight="bold">
          On the Components list
        </Text>
        <ComponentMetricsCardMock />
      </Flex>

      <Flex direction="column" gap={tokens.space.tight}>
        <Text size={tokens.sizes.caption} color="gray" weight="bold">
          On a Component detail page
        </Text>
        <PolicyComplianceCardMock />
      </Flex>
    </Flex>
  );
}
