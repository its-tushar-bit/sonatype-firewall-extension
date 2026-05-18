/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Container, Flex } from '@radix-ui/themes';
import { HeroSection, PageHeading, BodyText } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';

export function Home() {
  return (
    <Flex
      direction="column"
      align="center"
      height="100%"
    >
      <Container size={tokens.sizes.body.sm} p={tokens.space.page} style={{ justifyContent: 'center' }}>
        <HeroSection mb={tokens.space.billboard}>
          <PageHeading align="center">
            Search Component & Vulnerability Intelligence
          </PageHeading>
          <BodyText size="md" color="gray" align="center">
            Sonatype offers the most complete open source security intelligence — covering every
            ecosystem and every vulnerability, wherever you build.
          </BodyText>
        </HeroSection>
      </Container>
    </Flex>
  );
}
