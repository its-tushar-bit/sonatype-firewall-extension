/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { CSSProperties, HTMLAttributes } from 'react';
import styles from './LoadingSkeleton.module.css';

interface LoadingSkeletonProps extends Omit<HTMLAttributes<HTMLDivElement>, 'style'> {
  /** Placeholder height. Number is treated as px. Defaults to 80px. */
  readonly height?: number | string;
  /** Accessible name announced by screen readers while content loads. */
  readonly label?: string;
}

/**
 * Shared loading placeholder for the Nexus One (nosc) feature. Replaces the
 * hand-rolled `<Box style={{ height, backgroundColor: var(--gray-3),
 * animation: 'pulse ...' }}>` blocks that were duplicated across many tiles
 * and pages — the color + animation now live in one CSS module instead of
 * being re-specified inline everywhere (CLM-39709 review).
 *
 * Exposes the `status` role with `aria-busy` so the placeholder is announced
 * as loading rather than being an invisible, semantics-free `<div>`.
 */
export function LoadingSkeleton({ height = 80, label = 'Loading…', ...rest }: LoadingSkeletonProps): JSX.Element {
  const cssHeight = typeof height === 'number' ? `${height}px` : height;
  return (
    <div
      role="status"
      aria-busy="true"
      aria-label={label}
      className={styles.skeleton}
      style={{ '--nosc-skeleton-height': cssHeight } as CSSProperties}
      {...rest}
    />
  );
}
