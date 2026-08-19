/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Flex, Box, Card, Grid, Skeleton } from '@radix-ui/themes';
import { PageLayout } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';

export function ComponentDetailSkeleton() {
  return (
    <PageLayout aria-busy="true">
      {/* Header — mirrors ComponentDetailsHeader layout */}
      <Grid columns={{ initial: '1', md: '1fr auto' }} align="start" gap={tokens.space.page}>
        <Flex direction="column" gap={tokens.space.item}>
          <Skeleton width="60%" height={tokens.skeleton.height.input} />
          <Skeleton width="40%" height="20px" />
          <Flex gap={tokens.space.inline}>
            <Skeleton width="100px" height="24px" />
            <Skeleton width="100px" height="24px" />
            <Skeleton width="100px" height="24px" />
          </Flex>
        </Flex>
        <Card size={tokens.card.medium}>
          <Flex direction="column" gap={tokens.space.inline}>
            <Skeleton width="150px" height="16px" />
            <Flex align="center" gap={tokens.space.item}>
              <Skeleton width="36px" height="36px" style={{ borderRadius: '8px' }} />
              <Skeleton width="40px" height="28px" />
            </Flex>
          </Flex>
        </Card>
      </Grid>

      {/* Tabs bar */}
      <Box mt={tokens.space.section}>
        <Skeleton width="500px" height={tokens.skeleton.height.input} />
      </Box>

      {/* Overview content — 3 cards */}
      <Box mt={tokens.space.section}>
        <Grid columns={{ initial: '1', lg: '1fr 1fr 1fr' }} gap={tokens.space.section}>
          <Card size="3">
            <Flex direction="column" gap={tokens.space.item}>
              <Skeleton width="180px" height="24px" />
              <Flex direction="column" gap={tokens.space.inline}>
                <Skeleton width="100%" height="20px" />
                <Skeleton width="100%" height="20px" />
                <Skeleton width="100%" height="20px" />
              </Flex>
            </Flex>
          </Card>
          <Card size="3">
            <Flex direction="column" gap={tokens.space.item}>
              <Skeleton width="150px" height="24px" />
              <Flex direction="column" gap={tokens.space.inline}>
                <Skeleton width="100%" height="20px" />
                <Skeleton width="100%" height="20px" />
              </Flex>
            </Flex>
          </Card>
          <Card size="3">
            <Flex direction="column" gap={tokens.space.item}>
              <Skeleton width="120px" height="24px" />
              <Flex direction="column" gap={tokens.space.inline}>
                <Skeleton width="100%" height="20px" />
                <Skeleton width="100%" height="20px" />
              </Flex>
            </Flex>
          </Card>
        </Grid>
      </Box>
    </PageLayout>
  );
}
