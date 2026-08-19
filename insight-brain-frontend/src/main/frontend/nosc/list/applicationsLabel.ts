/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export type ApplicationsLabelOptions = {
  /**
   * When true, count is a lower bound from a capped collect window (e.g. vuln list vs Impact tab).
   */
  readonly atLeast?: boolean;
};

/** Shared card chrome for application counts on Components / Vulnerabilities lists. */
export function applicationsLabel(count: number, options?: ApplicationsLabelOptions): string {
  if (options?.atLeast) {
    return count === 1 ? 'At least 1 Application' : `At least ${count} Applications`;
  }
  return count === 1 ? '1 Application' : `${count} Applications`;
}
