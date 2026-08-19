/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useLayoutEffect, useRef } from 'react';
import { Flex } from '@radix-ui/themes';
import { NOTICE_STRIP_Z_INDEX, publishNoticeStripHeight } from 'MainRoot/nosc/shell/previewShellLayout';

/**
 * Fixed strip above {@link TopNav} that stacks the Nexus One shell's header
 * notices: default admin password warning, System Notice, Base URL Not Set,
 * and the MTIQ announcement banner.
 *
 * The strip's own height is not a constant — notices can stack and message
 * text wraps at narrow widths — so a `ResizeObserver` measures it and
 * publishes the value via {@link publishNoticeStripHeight}, the single
 * source of truth every fixed-position shell consumer (TopNav wrapper,
 * LeftNav, `usePreviewShellOffsets`, `.nosc-toast-host`) reads from instead
 * of a second hardcoded magic number.
 *
 * z-index is {@link NOTICE_STRIP_Z_INDEX}, sitting above TopNav and below
 * poppers/dialogs — see the full ladder documented on that constant in
 * `previewShellLayout.ts` and in `nexus-one.css`.
 *
 * @param showLandmark - When true, renders an ARIA region landmark (role="region"
 *   with aria-label="System notices"). When false, omits the landmark to avoid
 *   an empty region accessibility issue.
 */
export function NoticeStrip({
  children,
  showLandmark = true,
}: {
  readonly children: React.ReactNode;
  /**
   * When true, renders an ARIA region landmark. Set to false to avoid an empty
   * region accessibility issue. Defaults to true for backward compatibility.
   */
  readonly showLandmark?: boolean;
}): JSX.Element {
  const wrapperRef = useRef<HTMLDivElement>(null);

  // useLayoutEffect (not useEffect) so observe() starts before the browser's first paint —
  // per spec, a ResizeObserver's initial callback for a newly-observed element fires before
  // that paint, so the correct height reaches publishNoticeStripHeight in time and TopNav/
  // LeftNav never render a frame at the wrong (notice-free) offset. jsdom has no real paint
  // timing to verify this against; the real-browser check lives in the Playwright coverage
  // for this strip (NexusOneNoticeStripPlaywrightTest).
  useLayoutEffect(() => {
    const el = wrapperRef.current;
    if (!el || typeof ResizeObserver === 'undefined') {
      return () => publishNoticeStripHeight(0);
    }

    const observer = new ResizeObserver(([entry]) => {
      const height = Math.ceil(entry.contentRect.height);
      publishNoticeStripHeight(height);
    });
    observer.observe(el);
    return () => {
      observer.disconnect();
      publishNoticeStripHeight(0);
    };
  }, []);

  return (
    <div
      ref={wrapperRef}
      {...(showLandmark && { role: 'region', 'aria-label': 'System notices' })}
      data-testid="nosc-notice-strip"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        zIndex: NOTICE_STRIP_Z_INDEX,
      }}
    >
      <Flex direction="column">{children}</Flex>
    </div>
  );
}
