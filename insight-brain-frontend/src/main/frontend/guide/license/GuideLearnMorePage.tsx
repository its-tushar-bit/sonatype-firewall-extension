/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Flex, Text, Link, Callout } from '@radix-ui/themes';

export function GuideLearnMorePage() {
  return (
    <Flex align="center" justify="center" style={{ minHeight: '100dvh' }}>
      <Callout.Root size="3" color="blue">
        <Callout.Text>
          <Text>
            Sonatype AI Developer is not currently enabled for your organization.
          </Text>
          <br />
          <Link href="https://links.sonatype.com/products/sonatype-guide-marketing" target="_blank" rel="noopener noreferrer">
            Learn more about Sonatype AI Developer.
          </Link>
        </Callout.Text>
      </Callout.Root>
    </Flex>
  );
}
