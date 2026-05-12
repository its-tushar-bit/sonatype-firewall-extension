/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ReactNode } from 'react';
import { Flex, Spinner } from '@radix-ui/themes';
import { useLicense } from './LicenseProvider';
import { GuideLearnMorePage } from './GuideLearnMorePage';
import type { ProductGroup } from './licenseProducts';

interface LicenseGateProps {
  requires: ProductGroup;
  children: ReactNode;
}

export function LicenseGate({ requires, children }: LicenseGateProps) {
  const { hasLicenseFor, isLoading } = useLicense();

  if (isLoading) {
    return (
      <Flex align="center" justify="center" style={{ minHeight: '100dvh' }}>
        <Spinner size="3" />
      </Flex>
    );
  }

  if (!hasLicenseFor(requires)) {
    return <GuideLearnMorePage />;
  }

  return <>{children}</>;
}
