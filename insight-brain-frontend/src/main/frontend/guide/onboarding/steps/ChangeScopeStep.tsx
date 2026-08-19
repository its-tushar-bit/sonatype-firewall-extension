/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Flex } from '@radix-ui/themes';
import { BodyText } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import { PolicyContextBarMock } from './illustrations/PolicyContextBarMock';

export function ChangeScopeStep() {
  return (
    <Flex direction="column" gap={tokens.space.section}>
      <BodyText size="md" color="gray">
        You can change scope anytime. The <strong>Policy context</strong> picker sits at the top of
        your Components and search pages — switch to any organization or application without leaving
        what you’re looking at. Your selection is remembered across AI Developer.
      </BodyText>

      <PolicyContextBarMock />

      <BodyText size="sm" color="gray">
        Want this tour again? Open the Policy context picker and choose <strong>Need help?</strong>
      </BodyText>
    </Flex>
  );
}
