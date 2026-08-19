/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Heading, Text, Card, Flex, Link } from '@radix-ui/themes';
import { useSref } from '@uirouter/react';
import { StatusIcons } from 'MainRoot/nosc';

export default function HelloWorld1() {
  return (
    <Flex direction="column" gap="4" p="4">
      <Heading size="6">Nexus One — Hello World 1</Heading>
      <Card>
        <Flex align="center" gap="2">
          <StatusIcons.Info size={16} />
          <Text>This is a placeholder page demonstrating the Nexus One SPA skeleton.</Text>
        </Flex>
      </Card>
      <Card>
        <Flex align="center" gap="2">
          <Link {...useSref('hello2')}>Go to Hello World 2</Link>
        </Flex>
      </Card>
    </Flex>
  );
}
