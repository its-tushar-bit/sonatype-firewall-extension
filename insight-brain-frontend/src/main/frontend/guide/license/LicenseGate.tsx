/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ReactNode } from 'react';
import { Flex, Spinner } from '@radix-ui/themes';
import { useLicense } from './LicenseProvider';
import { GuideLearnMorePage } from './GuideLearnMorePage';
import type { SolutionId } from '../layout/ProductSwitcher/productMetadata';

const GUIDE_SOLUTION_ID: SolutionId = 'guide';

interface LicenseGateProps {
  children: ReactNode;
}

export function LicenseGate({ children }: LicenseGateProps) {
  const { hasSolution, isLoading } = useLicense();

  if (isLoading) {
    return (
      <Flex align="center" justify="center" style={{ minHeight: '100dvh' }}>
        <Spinner size="3" />
      </Flex>
    );
  }

  if (!hasSolution(GUIDE_SOLUTION_ID)) {
    return <GuideLearnMorePage />;
  }

  return <>{children}</>;
}
