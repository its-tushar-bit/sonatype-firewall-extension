/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ReactNode } from 'react';
import { Navigate } from 'react-router';
import { Flex, Spinner } from '@radix-ui/themes';
import { useFeatureFlags } from './FeatureFlagProvider';
import type { FeatureFlag } from './featureFlags';

interface FeatureGateProps {
  flag: FeatureFlag;
  children: ReactNode;
}

export function FeatureGate({ flag, children }: FeatureGateProps) {
  const { isFeatureEnabled, isLoading } = useFeatureFlags();

  if (isLoading) {
    return (
      <Flex align="center" justify="center" style={{ minHeight: '100dvh' }}>
        <Spinner size="3" />
      </Flex>
    );
  }

  if (!isFeatureEnabled(flag)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
