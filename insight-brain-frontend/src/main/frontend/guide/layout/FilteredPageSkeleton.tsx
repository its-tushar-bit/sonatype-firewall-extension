/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Box, Flex, Skeleton } from '@radix-ui/themes';
import { PageLayout, ComponentCardSkeleton, VulnerabilityResultCardSkeleton } from '@guide/ui-core';
import { tokens, MAX_SKELETON_CARDS } from '@guide/ui-core/utils';

type SkeletonVariant = 'components' | 'vulnerabilities' | 'search';

interface FilteredPageSkeletonProps {
  variant: SkeletonVariant;
}

function ToolbarSkeleton() {
  return (
    <Box width={{ initial: '100%', md: 'auto' }}>
      <Box display={{ initial: 'block', md: 'none' }}>
        <Flex direction="column" gap={tokens.space.inline} width="100%">
          <Skeleton width="100%" height={tokens.skeleton.height.inputMobile} />
          <Flex gap={tokens.space.inline}>
            <Skeleton width="35%" height={tokens.skeleton.height.inputMobile} />
            <Skeleton width="65%" height={tokens.skeleton.height.inputMobile} />
          </Flex>
        </Flex>
      </Box>
      <Box display={{ initial: 'none', md: 'block' }}>
        <Flex gap={tokens.space.inline}>
          <Skeleton width="300px" height={tokens.skeleton.height.input} />
          <Skeleton width="200px" height={tokens.skeleton.height.input} />
        </Flex>
      </Box>
    </Box>
  );
}

function SidebarSkeleton({ variant }: { variant: SkeletonVariant }) {
  if (variant === 'search') {
    return (
      <Flex direction="column" gap={tokens.space.item}>
        <Skeleton width="100%" height="150px" />
        <Skeleton width="100%" height="120px" />
        <Skeleton width="100%" height="100px" />
      </Flex>
    );
  }
  return <Skeleton width="100%" height="200px" />;
}

export function FilteredPageSkeleton({ variant }: FilteredPageSkeletonProps) {
  if (variant === 'search') {
    return (
      <PageLayout aria-busy="true">
        <div role="status" aria-label="Loading page content" data-testid="page-skeleton">
          <Flex direction="column" gap={tokens.space.section}>
            {/* Header */}
            <Flex
              direction={{ initial: 'column', md: 'row' }}
              justify="between"
              align={{ initial: 'start', md: 'center' }}
              gap={tokens.space.item}
            >
              <Flex align="center" gap={tokens.space.inline} wrap="wrap">
                <Skeleton width="150px" height="32px" />
                <Skeleton width="200px" height="24px" />
              </Flex>
            </Flex>

            {/* Toolbar */}
            <Flex
              direction={{ initial: 'column', md: 'row' }}
              gap={tokens.space.inline}
              align={{ initial: 'stretch', md: 'center' }}
            >
              <Box flexGrow="1">
                <Skeleton width="100%" height="40px" />
              </Box>
              <Box width={{ initial: '100%', md: '200px' }}>
                <Skeleton width="100%" height="40px" />
              </Box>
            </Flex>

            {/* Tabs */}
            <Flex gap={tokens.space.inline}>
              <Skeleton width="100px" height="36px" />
              <Skeleton width="120px" height="36px" />
              <Skeleton width="140px" height="36px" />
            </Flex>

            {/* Sidebar + results */}
            <Flex gap={tokens.space.section}>
              <Box flexShrink="0" width="280px" display={{ initial: 'none', md: 'block' }}>
                <SidebarSkeleton variant="search" />
              </Box>
              <Box flexBasis="1" minWidth="0" width="100%">
                <Flex direction="column" gap={tokens.space.item}>
                  {Array.from({ length: MAX_SKELETON_CARDS }, (_, i) =>
                    i % 2 === 0
                      ? <VulnerabilityResultCardSkeleton key={i} />
                      : <ComponentCardSkeleton key={i} />
                  )}
                  <Flex justify="between" align="center" mt={tokens.space.item}>
                    <Skeleton width="180px" height="20px" />
                    <Flex gap={tokens.space.inline}>
                      <Skeleton width="36px" height="36px" />
                      <Skeleton width="36px" height="36px" />
                    </Flex>
                  </Flex>
                </Flex>
              </Box>
            </Flex>
          </Flex>
        </div>
      </PageLayout>
    );
  }

  const titleWidth = variant === 'vulnerabilities' ? '220px' : '180px';
  const CardSkeleton = variant === 'vulnerabilities' ? VulnerabilityResultCardSkeleton : ComponentCardSkeleton;

  return (
    <PageLayout aria-busy="true">
      <div role="status" aria-label="Loading page content" data-testid="page-skeleton">
        <Flex gap={tokens.space.section}>
          <Box width="250px" flexShrink="0" display={{ initial: 'none', md: 'block' }}>
            <SidebarSkeleton variant={variant} />
          </Box>
          <Box flexBasis="1" minWidth="0" width="100%">
            <Flex direction="column" gap={tokens.space.section}>
              <Flex
                direction={{ initial: 'column', md: 'row' }}
                justify={{ md: 'between' }}
                align={{ initial: 'start', md: 'center' }}
                gap={{ initial: tokens.space.item, md: tokens.space.jumbo }}
                data-testid="header-toolbar-container"
              >
                <Flex align="center" gap={tokens.space.inline}>
                  <Skeleton width={titleWidth} height="32px" />
                </Flex>
                <ToolbarSkeleton />
              </Flex>
              <Flex direction="column" gap={tokens.space.item}>
                {Array.from({ length: MAX_SKELETON_CARDS }, (_, i) => (
                  <CardSkeleton key={i} />
                ))}
              </Flex>
            </Flex>
          </Box>
        </Flex>
      </div>
    </PageLayout>
  );
}
