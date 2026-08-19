/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useRef, useState, type ReactElement } from 'react';
import axios from 'axios';
import { Box, Button, Flex, Text } from '@radix-ui/themes';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { VulnerabilitySecurityDetailsPanel } from 'MainRoot/nosc/vulnerabilities/detail/VulnerabilitySecurityDetailsPanel';
import {
  fetchVulnerabilityDetail,
  type VulnerabilityDetailDTO,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityDetailApi';
import type { PolicyWaiverDetailDTO } from './waiverTypes';

/**
 * Security Details tab of the Waiver Detail page (CLM-43365).
 *
 * Fetches `GET /api/v2/vulnerabilities/{refId}` and renders the shared
 * {@link VulnerabilitySecurityDetailsPanel}. Owner scope comes from the route
 * (normalized path segments) — the v2 waiver detail DTO exposes `scopeOwner*`
 * rather than `ownerType`/`ownerId`.
 *
 * Parent only mounts this tab when {@link vulnerabilityId} is present.
 */

type LoadStatus = 'loading' | 'ready' | 'error';

export function WaiverSecurityDetailsTab({
  waiver,
  vulnerabilityId,
  ownerType,
  ownerId,
}: {
  readonly waiver: PolicyWaiverDetailDTO;
  /** Required — the parent only mounts this tab when the waiver names a CVE. */
  readonly vulnerabilityId: string;
  /** Normalized route owner type (e.g. `application`), not the list DTO enum. */
  readonly ownerType: string | null;
  readonly ownerId: string | null;
}): ReactElement {
  const [status, setStatus] = useState<LoadStatus>('loading');
  const [detail, setDetail] = useState<VulnerabilityDetailDTO | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const load = useCallback(async (): Promise<void> => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    const { signal } = controller;

    setStatus('loading');
    try {
      const data = await fetchVulnerabilityDetail(
        vulnerabilityId,
        {
          componentIdentifier: waiver.componentIdentifier,
          ownerType,
          ownerId,
        },
        signal,
      );
      if (signal.aborted) return;
      setDetail(data);
      setStatus('ready');
    } catch (err) {
      if (axios.isCancel(err) || signal.aborted) return;
      setStatus('error');
    }
  }, [vulnerabilityId, waiver.componentIdentifier, ownerId, ownerType]);

  useEffect(() => {
    void load();
    return () => abortRef.current?.abort();
  }, [load]);

  if (status === 'loading') {
    return (
      <Box mt="4">
        <LoadingSkeleton height={240} data-testid="preview-waiver-security-loading" />
      </Box>
    );
  }

  if (status === 'error' || !detail) {
    return (
      <Box mt="4">
        <Flex direction="column" gap="3" align="start" data-testid="preview-waiver-security-error">
          <Text size="2" color="red">
            Failed to load security details for <code>{vulnerabilityId}</code>.
          </Text>
          <Button
            size="2"
            variant="soft"
            onClick={() => void load()}
            data-testid="preview-waiver-security-retry"
          >
            Retry
          </Button>
        </Flex>
      </Box>
    );
  }

  return (
    <VulnerabilitySecurityDetailsPanel
      detail={detail}
      vulnerabilityId={vulnerabilityId}
      testIdPrefix="preview-waiver-security"
    />
  );
}
