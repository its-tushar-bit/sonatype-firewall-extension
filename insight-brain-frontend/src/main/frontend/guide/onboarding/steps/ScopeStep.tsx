/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Flex, Callout } from '@radix-ui/themes';
import { Info } from 'lucide-react';
import { BodyText } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';

export function ScopeStep() {
  return (
    <Flex direction="column" gap={tokens.space.item}>
      <BodyText size="md" color="gray">
        AI Developer evaluates each component against the policies of a{' '}
        <strong>policy context</strong> — either an <strong>organization</strong> or a specific{' '}
        <strong>application</strong>. Different orgs and apps can have different rules, so what
        counts as “compliant” depends on the scope you choose.
      </BodyText>

      <Callout.Root size="1" color="gray" variant="surface">
        <Callout.Icon>
          <Info size={tokens.icon.small} />
        </Callout.Icon>
        <Callout.Text>
          Your selection sets the policy context used across AI Developer — the compliance you see in
          search results and on component detail pages reflects that org or app. Nothing selected
          means <strong>Root Organization</strong> (all policies).
        </Callout.Text>
      </Callout.Root>
    </Flex>
  );
}
