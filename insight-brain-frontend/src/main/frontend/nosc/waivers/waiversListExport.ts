/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { AnaWaiverRow } from 'MainRoot/nosc/waivers/waiversListTypes';

/**
 * CSV export helpers for the Ana waivers list (CLM-43204). There is no dashboard export
 * endpoint for waivers, so CSV is client-side and covers the CURRENT page rows only.
 * The header/label above the download surfaces this limitation to the user; a follow-up
 * (paginated batch export) would replace this once the backend exposes a POST/CSV path.
 */

const CSV_COLUMNS: ReadonlyArray<{
  readonly header: string;
  readonly value: (row: AnaWaiverRow) => string | number | null | undefined;
}> = [
  { header: 'Waiver ID', value: (row) => row.id },
  { header: 'Threat Level', value: (row) => row.threatLevel },
  { header: 'Policy', value: (row) => row.policyName ?? '' },
  { header: 'Policy ID', value: (row) => row.policyId ?? '' },
  { header: 'Auto', value: (row) => (row.isAuto ? 'true' : 'false') },
  { header: 'Reason', value: (row) => row.reason ?? '' },
  { header: 'Comment', value: (row) => row.comment ?? '' },
  { header: 'Created At', value: (row) => row.createdAt ?? '' },
  { header: 'Expires At', value: (row) => row.expiresAt ?? '' },
  { header: 'Scope Owner Type', value: (row) => row.scopeOwnerType ?? '' },
  { header: 'Scope Owner ID', value: (row) => row.scopeOwnerId ?? '' },
  { header: 'Waived By', value: (row) => row.waivedBy ?? '' },
  { header: 'Organization', value: (row) => row.organizationName ?? '' },
  { header: 'Application', value: (row) => row.applicationName ?? '' },
];

function csvEscape(value: string | number | null | undefined): string {
  if (value === null || value === undefined) return '';
  const text = String(value);
  if (/[",\n\r]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`;
  }
  return text;
}

export function buildWaiversCsv(rows: ReadonlyArray<AnaWaiverRow>): string {
  const header = CSV_COLUMNS.map((col) => csvEscape(col.header)).join(',');
  const body = rows
    .map((row) => CSV_COLUMNS.map((col) => csvEscape(col.value(row))).join(','))
    .join('\r\n');
  // CRLF matches Excel's expected line ending for CSVs so a raw double-click opens cleanly.
  return body.length > 0 ? `${header}\r\n${body}\r\n` : `${header}\r\n`;
}

/**
 * Trigger a browser download of the given rows as a CSV file. Uses an object URL +
 * anchor click since there's no backend export endpoint for waivers; safe for
 * page-sized batches (default pageSize=50) which is our current export scope.
 */
export function downloadWaiversCsv(
  rows: ReadonlyArray<AnaWaiverRow>,
  filename = 'waivers.csv',
): void {
  const csv = buildWaiversCsv(rows);
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.style.display = 'none';
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  // Revoke on the next tick so browsers that dispatch the download asynchronously still have
  // a live URL to fetch. Firefox in particular can 404 if the URL is revoked synchronously.
  setTimeout(() => URL.revokeObjectURL(url), 0);
}
