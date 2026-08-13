/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { render, screen, act, waitFor } from '@testing-library/react';
import {
  usePreviewShellOffsets,
  useNoticeStripHeight,
  publishNoticeStripHeight,
  TOP_NAV_HEIGHT_PX,
  LEFT_NAV_EXPANDED_WIDTH_PX,
  LEFT_NAV_COLLAPSED_WIDTH_PX,
  NOTICE_STRIP_HEIGHT_CSS_VAR,
} from 'MainRoot/nosc/shell/previewShellLayout';
import { COLLAPSED_KEY } from 'MainRoot/nosc/shell/useLeftNavCollapsed';

/**
 * Phase 1 / CLM-39545. Regression guard for the LeftNav-collapse layout
 * bug: every Preview page used to hardcode `left: 256` so collapsing the
 * rail (256→64) left a visible 192px gap of blank background between the
 * collapsed rail and the page edge.
 *
 * usePreviewShellOffsets() must:
 *   - Return left=256 / top=56 when LeftNav is expanded.
 *   - Return left=64 when LeftNav is collapsed.
 *   - React to in-tab collapse-state broadcasts (CustomEvent dispatched
 *     by useLeftNavCollapsed) within the same React tree.
 */
function Probe(): JSX.Element {
  const offsets = usePreviewShellOffsets();
  return (
    <div data-testid="probe">
      <span data-testid="left">{offsets.left}</span>
      <span data-testid="top">{offsets.top}</span>
    </div>
  );
}

function NoticeStripHeightProbe(): JSX.Element {
  const heightPx = useNoticeStripHeight();
  return <span data-testid="notice-strip-height">{heightPx}</span>;
}

describe('usePreviewShellOffsets', () => {
  beforeEach(() => {
    window.localStorage.removeItem(COLLAPSED_KEY);
    // Reset notice strip height before each test
    publishNoticeStripHeight(0);
  });

  it('returns expanded offsets by default', () => {
    render(<Probe />);
    expect(screen.getByTestId('top')).toHaveTextContent(String(TOP_NAV_HEIGHT_PX));
    expect(screen.getByTestId('left')).toHaveTextContent(String(LEFT_NAV_EXPANDED_WIDTH_PX));
  });

  it('returns collapsed offsets when localStorage has it set on first read', () => {
    window.localStorage.setItem(COLLAPSED_KEY, 'true');
    render(<Probe />);
    expect(screen.getByTestId('left')).toHaveTextContent(String(LEFT_NAV_COLLAPSED_WIDTH_PX));
  });

  it('reacts to a same-tab collapse broadcast (CustomEvent)', () => {
    render(<Probe />);
    expect(screen.getByTestId('left')).toHaveTextContent(String(LEFT_NAV_EXPANDED_WIDTH_PX));

    act(() => {
      window.dispatchEvent(
        new CustomEvent('nosc.leftnav.collapsed.change', { detail: { collapsed: true } }),
      );
    });
    expect(screen.getByTestId('left')).toHaveTextContent(String(LEFT_NAV_COLLAPSED_WIDTH_PX));

    act(() => {
      window.dispatchEvent(
        new CustomEvent('nosc.leftnav.collapsed.change', { detail: { collapsed: false } }),
      );
    });
    expect(screen.getByTestId('left')).toHaveTextContent(String(LEFT_NAV_EXPANDED_WIDTH_PX));
  });
});

describe('publishNoticeStripHeight / useNoticeStripHeight', () => {
  beforeEach(() => {
    publishNoticeStripHeight(0);
  });

  it('updates the CSS custom property on document.documentElement', () => {
    publishNoticeStripHeight(48);
    expect(document.documentElement.style.getPropertyValue(NOTICE_STRIP_HEIGHT_CSS_VAR)).toBe('48');
  });

  it('updates useNoticeStripHeight() return value via CustomEvent', async () => {
    render(<NoticeStripHeightProbe />);
    expect(screen.getByTestId('notice-strip-height')).toHaveTextContent('0');

    act(() => {
      publishNoticeStripHeight(48);
    });

    await waitFor(() => {
      expect(screen.getByTestId('notice-strip-height')).toHaveTextContent('48');
    });
  });
});

describe('usePreviewShellOffsets with notice strip height', () => {
  beforeEach(() => {
    window.localStorage.removeItem(COLLAPSED_KEY);
    publishNoticeStripHeight(0);
  });

  it('includes notice strip height in top offset', async () => {
    render(<Probe />);
    expect(screen.getByTestId('top')).toHaveTextContent(String(TOP_NAV_HEIGHT_PX));

    act(() => {
      publishNoticeStripHeight(40);
    });

    await waitFor(() => {
      expect(screen.getByTestId('top')).toHaveTextContent(String(TOP_NAV_HEIGHT_PX + 40));
    });
  });

  it('top offset changes when notice strip height changes from 0 to non-zero', async () => {
    render(<Probe />);
    expect(screen.getByTestId('top')).toHaveTextContent(String(TOP_NAV_HEIGHT_PX));

    act(() => {
      publishNoticeStripHeight(48);
    });

    await waitFor(() => {
      expect(screen.getByTestId('top')).toHaveTextContent(String(TOP_NAV_HEIGHT_PX + 48));
    });

    act(() => {
      publishNoticeStripHeight(0);
    });

    await waitFor(() => {
      expect(screen.getByTestId('top')).toHaveTextContent(String(TOP_NAV_HEIGHT_PX));
    });
  });
});
