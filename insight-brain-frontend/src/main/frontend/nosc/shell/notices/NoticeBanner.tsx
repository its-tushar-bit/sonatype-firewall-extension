/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Flex, IconButton, Text, Link, Theme } from '@radix-ui/themes';
import { ActionIcons, StatusIcons } from 'MainRoot/nosc/icons';

export type NoticeIcon = 'alert-triangle' | 'info';

const ICON: Record<NoticeIcon, React.ComponentType<{ size?: number }>> = {
  'alert-triangle': StatusIcons.Warning,
  info: StatusIcons.Info,
};

export interface NoticeBannerProps {
  readonly children: React.ReactNode;
  readonly icon?: NoticeIcon;
  /** Inline actionable link rendered after the message (e.g. "Configure Base URL"). Omit when there's nowhere to send the user yet. */
  readonly linkText?: string;
  readonly linkHref?: string;
  /** role="alert" (implicit aria-live="assertive") interrupts screen readers for imminent-outage-class messages; defaults to role="status" (polite). */
  readonly assertive?: boolean;
  /**
   * Caller-owned dismiss callback (e.g. persisted per-windowId dismissal for the MTIQ announcement banner).
   * Omit to render a non-dismissible notice matching Classic's behavior for SystemNotice, DefaultAdminPasswordNotice,
   * and BaseUrlNotSetNotice banners.
   */
  readonly onDismiss?: () => void;
  readonly dismissLabel?: string;
  readonly testId?: string;
}

/**
 * Shared presentation for the Nexus One shell's header notices — see
 * {@link NoticeStrip} for the list of notices that use it.
 *
 * Design matches nexus-one-ux-prototype's Pattern 4 "System Admin Banner"
 * (`src/components/nexus-one/SystemAdminBanner.tsx` and its
 * `system-alert.md` skill doc) — the final UX design for this strip: a
 * single muted-orange treatment for every notice, `AlertTriangle`/`Info`
 * at 16px, an inline actionable link, and an optional dismiss X
 * pinned to the far right edge.
 *
 * Built as a plain `Flex` row rather than Radix's `Callout` — `Callout.Root`
 * is a CSS grid with auto-sized (content-width) tracks and
 * `justify-content: flex-start`, so a `width="100%"` child inside it never
 * actually spans the full row; it only matches the grid track's own
 * content-hugging width. That squeezed the dismiss button up against the
 * text instead of pinning it to the edge, and fought the icon's vertical
 * centering. A single flex row with `align="center"` on the icon+text group
 * gives predictable alignment, matching the prototype's own `.inner` layout
 * (`display: flex; align-items: center`) instead of Callout's grid.
 *
 * A nested `<Theme accentColor="orange">` scopes the accent so `--accent-*`
 * vars resolve to orange regardless of the app's actual theme accent (matching
 * the same technique `ClassicToggleButton` uses to force a specific accent
 * locally), allowing Radix components inside to default to orange without
 * explicit `color` props.
 */
export function NoticeBanner({
  children,
  icon = 'alert-triangle',
  linkText,
  linkHref,
  assertive = false,
  onDismiss,
  dismissLabel = 'Dismiss notice',
  testId,
}: NoticeBannerProps): JSX.Element {
  const Icon = ICON[icon];
  const role = assertive ? 'alert' : 'status';

  return (
    <Theme accentColor="orange" hasBackground={false}>
      <Flex
        role={role}
        data-testid={testId}
        align="center"
        justify="between"
        width="100%"
        gap="3"
        style={{
          minHeight: '40px',
          padding: 'var(--space-2) var(--space-4)',
          backgroundColor: 'var(--accent-3)',
          borderBottom: '1px solid var(--accent-6)',
        }}
      >
        <Flex align="center" gap="2">
          <Icon size={16} style={{ color: 'var(--accent-11)', flexShrink: 0 }} />
          <Text size="2" style={{ color: 'var(--accent-12)' }}>
            {children}
            {linkText && linkHref && (
              <>
                {' '}
                <Link
                  href={linkHref}
                  underline="always"
                  highContrast
                  weight="medium"
                  style={{ whiteSpace: 'nowrap' }}
                >
                  {linkText}
                </Link>
              </>
            )}
          </Text>
        </Flex>
        {onDismiss && (
          <IconButton
            size="1"
            variant="ghost"
            onClick={onDismiss}
            aria-label={dismissLabel}
            data-testid={testId ? `${testId}-dismiss` : undefined}
            style={{ flexShrink: 0 }}
          >
            <ActionIcons.Cancel size={14} />
          </IconButton>
        )}
      </Flex>
    </Theme>
  );
}
