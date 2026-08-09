/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { Badge, Button, Card, Flex, Heading, Inset, Table, Text } from '@radix-ui/themes';
import { useRouter } from '@uirouter/react';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import { useEstateComponentDetailShellContext } from './estateComponentDetailContext';
import { estateComponentDetailStateNameForTab } from './estateComponentDetailUtils';
import { fetchEstateComponentVersionRows, resolveEstateComponentVersionHash } from './estateComponentDetailsApi';
import type { EstateComponentVersionRow } from './estateComponentDetailsApi';

function VersionEstateAction({
  row,
  resolving,
  unavailable,
  onResolve,
}: {
  readonly row: EstateComponentVersionRow;
  readonly resolving: boolean;
  readonly unavailable: boolean;
  readonly onResolve: (version: string) => void;
}): JSX.Element {
  const handleActivate = (): void => {
    onResolve(row.version);
  };

  if (unavailable) {
    return (
      <Button size="1" variant="soft" disabled aria-label={`Estate component unavailable for ${row.version}`}>
        Unavailable
      </Button>
    );
  }

  return (
    <Button
      type="button"
      size="1"
      variant="soft"
      disabled={resolving}
      onClick={handleActivate}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          handleActivate();
        }
      }}
      aria-label={`Check estate availability for ${row.version}`}
      data-testid="nosc-estate-component-versions-row-resolve"
    >
      {resolving ? 'Checking...' : 'Check availability'}
    </Button>
  );
}

export function EstateComponentVersionExplorerTab(): JSX.Element {
  const { componentHash, hdsStatus, details, retryHds } = useEstateComponentDetailShellContext();
  const { stateService } = useRouter();
  const [rows, setRows] = useState<ReadonlyArray<EstateComponentVersionRow>>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);
  const [resolvingVersion, setResolvingVersion] = useState<string | null>(null);
  const [unavailableVersions, setUnavailableVersions] = useState<ReadonlySet<string>>(new Set());
  const [availabilityMessage, setAvailabilityMessage] = useState<string | null>(null);
  const resolveAbortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (hdsStatus !== 'ready' || !details) {
      setRows([]);
      setError(null);
      setLoading(false);
      return undefined;
    }

    const controller = new AbortController();
    setLoading(true);
    setError(null);
    setUnavailableVersions(new Set());
    setAvailabilityMessage(null);

    void fetchEstateComponentVersionRows(details, controller.signal)
      .then((nextRows) => {
        if (controller.signal.aborted) return;
        setRows(nextRows);
        setLoading(false);
      })
      .catch((err) => {
        if (axios.isCancel(err) || controller.signal.aborted) return;
        console.error('Failed to load component version explorer data', { err });
        setRows([]);
        setError('Version information is temporarily unavailable.');
        setLoading(false);
      });

    return () => controller.abort();
  }, [details, hdsStatus, reloadToken]);

  useEffect(() => {
    setResolvingVersion(null);
    setAvailabilityMessage(null);
    return () => {
      resolveAbortRef.current?.abort();
      resolveAbortRef.current = null;
      // Cleanup nulls the ref before the aborted request's finally runs; clear UI state here.
      setResolvingVersion(null);
    };
  }, [componentHash]);

  const retryVersions = useCallback(() => {
    setReloadToken((token) => token + 1);
  }, []);

  const resolveVersion = useCallback(
    async (version: string): Promise<void> => {
      if (!details || resolvingVersion) {
        return;
      }
      resolveAbortRef.current?.abort();
      const controller = new AbortController();
      resolveAbortRef.current = controller;
      setResolvingVersion(version);
      setAvailabilityMessage(null);
      try {
        const hash = await resolveEstateComponentVersionHash(details, version, controller.signal);
        if (controller.signal.aborted) return;
        if (hash) {
          await stateService.go(estateComponentDetailStateNameForTab('overview'), { componentHash: hash });
          return;
        }
        setUnavailableVersions((current) => new Set(current).add(version));
        setAvailabilityMessage('No estate component found for this version.');
      } catch (err) {
        if (axios.isCancel(err) || controller.signal.aborted) return;
        console.error('Failed to resolve component version hash', { version, err });
        setAvailabilityMessage('Could not check estate availability for this version.');
      } finally {
        if (resolveAbortRef.current === controller) {
          resolveAbortRef.current = null;
        }
        // Clear even when cleanup nulled the ref before this finally ran.
        setResolvingVersion((current) => (current === version ? null : current));
      }
    },
    [details, resolvingVersion, stateService],
  );

  if (hdsStatus === 'loading') {
    return (
      <AsyncPageState loading error={null} loadingHeight={180} loadingTestId="nosc-estate-component-versions-loading">
        {null}
      </AsyncPageState>
    );
  }

  if (hdsStatus === 'error') {
    return (
      <Flex direction="column" gap="3" align="start" mt="4" data-testid="nosc-estate-component-versions-error">
        <Text size="2" color="red">
          Version details are temporarily unavailable because component catalog details could not be loaded.
        </Text>
        <Button size="2" variant="soft" onClick={retryHds} data-testid="nosc-estate-component-versions-retry">
          Retry details
        </Button>
      </Flex>
    );
  }

  if (hdsStatus === 'empty' || !details) {
    return (
      <Flex direction="column" gap="2" mt="4" data-testid="nosc-estate-component-versions-empty">
        <Text size="2" color="gray">
          No version information was found for this component.
        </Text>
      </Flex>
    );
  }

  return (
    <Flex direction="column" gap="3" mt="4" data-testid="nosc-estate-component-versions">
      <Flex direction="column" gap="1">
        <Heading as="h2" size="4">
          Versions
        </Heading>
        <Text size="2" color="gray">
          Catalog-known versions for this component coordinate. Check a version to open it only when a component hash is
          available.
        </Text>
        {availabilityMessage && (
          <Text size="2" color="gray" data-testid="nosc-estate-component-versions-availability-message">
            {availabilityMessage}
          </Text>
        )}
      </Flex>
      <AsyncPageState
        loading={loading}
        error={error}
        onRetry={retryVersions}
        loadingHeight={180}
        loadingTestId="nosc-estate-component-versions-loading"
        errorTestId="nosc-estate-component-versions-error"
        errorTitle="Failed to load versions"
      >
        {rows.length === 0 ? (
          <Flex direction="column" gap="2" data-testid="nosc-estate-component-versions-empty">
            <Text size="2" color="gray">
              No sibling versions were reported for this component.
            </Text>
          </Flex>
        ) : (
          <Card>
            <Inset>
              <Table.Root data-testid="nosc-estate-component-versions-table">
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeaderCell>Version</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Catalog status</Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>Estate availability</Table.ColumnHeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {rows.map((row) => {
                    const unavailable = unavailableVersions.has(row.version);
                    return (
                      <Table.Row key={row.version} data-testid="nosc-estate-component-versions-row">
                        <Table.Cell>
                          <Text size="2">{row.version}</Text>
                        </Table.Cell>
                        <Table.Cell>
                          <Badge size="1" color="gray" variant="soft">
                            {unavailable ? 'Unavailable' : 'Catalog only'}
                          </Badge>
                        </Table.Cell>
                        <Table.Cell>
                          <VersionEstateAction
                            row={row}
                            resolving={resolvingVersion === row.version}
                            unavailable={unavailable}
                            onResolve={resolveVersion}
                          />
                        </Table.Cell>
                      </Table.Row>
                    );
                  })}
                </Table.Body>
              </Table.Root>
            </Inset>
          </Card>
        )}
      </AsyncPageState>
    </Flex>
  );
}
