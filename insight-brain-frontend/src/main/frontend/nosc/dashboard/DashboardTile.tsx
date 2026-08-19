/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { ReactNode, useId } from 'react';
import { Box, Button, Card, Flex, Heading, Text } from '@radix-ui/themes';
import { ActionIcons, StatusIcons } from 'MainRoot/nosc/icons';
import { TileStatus } from './useTile';

/**
 * Generic dashboard-tile chrome (CLM-39641 / P1-F6).
 *
 * Every Preview-Dashboard tile composes inside this chrome so the user gets
 * a consistent header, skeleton, error, and retry experience regardless of
 * what the tile renders. The body slot is opaque — each tile component owns
 * how it renders its data.
 *
 * Failure isolation: per F6 Epic §6 (AT-F6-003) each tile owns its own
 * fetch/error state so one tile's failure never affects another.
 */
export interface DashboardTileProps {
  /** Human-readable title rendered in the tile header. */
  title: string;
  /** Loading / ready / error state — typically forwarded from useTile(). */
  status: TileStatus;
  /** Body rendered when status === 'ready'. Tile owns its own layout. */
  children: ReactNode;
  /** Optional inline error text rendered when status === 'error'. */
  errorMessage?: string;
  /** Click handler for the Retry button (error state only). */
  onRetry: () => void;
  /** Optional secondary slot in the header (e.g., "View details →"). */
  headerExtra?: ReactNode;
}

export function DashboardTile({
  title,
  status,
  children,
  errorMessage,
  onRetry,
  headerExtra,
}: DashboardTileProps) {
  const headingId = useId();

  return (
    <Card asChild>
      <section aria-labelledby={headingId} data-testid="dashboard-tile">
        <Box p="4">
          <Flex align="center" justify="between" mb="3">
            <Heading id={headingId} size="3" trim="start">
              {title}
            </Heading>
            {headerExtra ?? null}
          </Flex>

          {status === 'loading' && (
            <Box
              data-testid="dashboard-tile-skeleton"
              style={{
                height: 80,
                backgroundColor: 'var(--gray-3)',
                borderRadius: 'var(--radius-2)',
                animation: 'pulse 1.5s ease-in-out infinite',
              }}
            />
          )}

          {status === 'error' && (
            <Flex direction="column" gap="2" data-testid="dashboard-tile-error">
              <Flex gap="2" align="center">
                <StatusIcons.Error size={16} color="var(--red-9)" />
                <Text size="2" color="red">
                  {errorMessage ?? 'Failed to load'}
                </Text>
              </Flex>
              <Box>
                <Button size="1" variant="soft" onClick={onRetry}>
                  <ActionIcons.Refresh size={14} /> Retry
                </Button>
              </Box>
            </Flex>
          )}

          {status === 'ready' && <Box>{children}</Box>}
        </Box>
      </section>
    </Card>
  );
}
