/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Heading, Text, Card, Flex } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc';

export default function HelloWorld2() {
  return (
    <Flex direction="column" gap="4" p="4">
      <Heading size="6">Nexus One — Hello World 2</Heading>
      <Card>
        <Flex align="center" gap="2">
          <ActionIcons.Search size={16} />
          <Text>This is a second placeholder page demonstrating the Nexus One SPA skeleton.</Text>
        </Flex>
      </Card>
    </Flex>
  );
}
