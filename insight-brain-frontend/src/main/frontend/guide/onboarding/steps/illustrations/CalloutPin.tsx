/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import styles from './CalloutPin.module.css';

type Placement = 'right' | 'left' | 'bottom';

interface CalloutPinProps {
  /** The number shown inside the pin — 1-based ordinal for the callout. */
  number: number;
  /** Short bolded lead-in shown in the label bubble. */
  title: string;
  /** Body text shown in the label bubble. */
  body: string;
  /** Which side of the pinned element the label bubble sits on. Defaults to right. */
  placement?: Placement;
  /** Optional inline style overrides for the pin position (top/left/right/bottom). */
  pinStyle?: React.CSSProperties;
  /** Optional inline style overrides for the label bubble position. */
  labelStyle?: React.CSSProperties;
}

/**
 * Numbered pin + attached label bubble for annotating the illustration mocks
 * used in the first-run onboarding modal. The parent must be `position: relative`
 * so pin/label absolute-positioning is scoped to the pinned element.
 */
export function CalloutPin({
  number,
  title,
  body,
  placement = 'right',
  pinStyle,
  labelStyle,
}: CalloutPinProps) {
  return (
    <>
      <div
        className={styles.pin}
        style={pinStyle}
        aria-hidden
      >
        {number}
      </div>
      <div
        className={styles.label}
        data-placement={placement}
        style={labelStyle}
        role="note"
      >
        <strong>{title}</strong>
        <span className={styles.body}>{body}</span>
      </div>
    </>
  );
}
