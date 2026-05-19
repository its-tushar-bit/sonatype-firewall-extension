/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Grid, Tabs } from '@radix-ui/themes';
import { PolicyComplianceCard, VulnerabilitiesCard, LicenseCard } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';

export function OverviewTab() {
  return (
    <Tabs.Content value="overview">
      <Grid columns={{ initial: '1', lg: '1fr 1fr 1fr' }} gap={tokens.space.section}>
        <PolicyComplianceCard />
        <VulnerabilitiesCard />
        <LicenseCard />
      </Grid>
    </Tabs.Content>
  );
}
