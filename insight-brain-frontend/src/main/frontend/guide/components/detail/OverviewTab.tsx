/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Flex, Grid, Tabs } from '@radix-ui/themes';
import { VulnerabilitiesCard, LicenseCard } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import { PolicyComplianceCardV2 } from './PolicyComplianceCardV2';

export function OverviewTab() {
  return (
    <Tabs.Content value="overview">
      <Flex direction="column" gap={tokens.space.section}>
        <Grid columns={{ initial: '1', md: '1fr 1fr' }} gap={tokens.space.section}>
          <VulnerabilitiesCard />
          <LicenseCard />
        </Grid>
        <PolicyComplianceCardV2 />
      </Flex>
    </Tabs.Content>
  );
}
