/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Box, Flex, Link as RadixLink, Text } from '@radix-ui/themes';
import WaiversTable from 'MainRoot/nosc/waivers/WaiversTable';
import { classicHref } from './applicationDetailUtils';

/**
 * Waivers tab inside the Application Detail page. Reads live waivers
 * from POST /rest/dashboard/policy/policyWaivers?includeAutoWaivers=true
 * with `applicationIds=[applicationInternalId]`, which restricts the
 * dashboard query to waivers whose scope is this application or any
 * ancestor org/root that applies. The empty-state copy is application-
 * specific so users understand they're seeing zero rather than a load
 * error.
 *
 * Renders nothing meaningful until the parent has resolved the
 * application's internal id from /rest/application/{publicId}.
 */
interface AppWaiversTabProps {
  applicationInternalId: string | undefined;
  publicId: string;
  /** Live waiver data, lifted to ApplicationDetail so the count can be
   *  shown in the Waivers tab trigger badge (mirrors how the Violations
   *  tab shows its count). The hook is called once at the parent level;
   *  the tab is purely presentational. */
  waivers: ReadonlyArray<import('MainRoot/nosc/waivers/waiverTypes').PolicyWaiverDTO>;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function AppWaiversTab({
  applicationInternalId,
  publicId,
  waivers,
  loading,
  error,
  refetch,
}: AppWaiversTabProps): JSX.Element {
  return (
    <Box pt="3" data-testid="nosc-app-detail-waivers-tab">
      <Flex justify="between" align="center" mb="3">
        <Text size="2" color="gray">
          Active waivers that apply to <strong>{publicId}</strong> — including
          waivers inherited from parent organizations and the root.
        </Text>
        <RadixLink
          size="2"
          href={classicHref(`/management/view/application/${encodeURIComponent(publicId)}/waivers`)}
          data-testid="nosc-app-detail-waivers-classic-link"
        >
          Manage in Classic →
        </RadixLink>
      </Flex>
      <WaiversTable
        waivers={waivers}
        loading={loading || !applicationInternalId}
        error={error}
        onRetry={refetch}
        emptyMessage="No waivers apply to this application"
        emptySubMessage="Waivers created on this application or any parent organization will appear here. Use 'Manage in Classic' to add a new one from a violation."
        testId="nosc-app-detail-waivers-table"
      />
    </Box>
  );
}
