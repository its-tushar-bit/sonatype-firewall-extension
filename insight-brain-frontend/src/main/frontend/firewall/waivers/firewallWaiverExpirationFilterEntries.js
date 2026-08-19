/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const FIREWALL_EXPIRATION_FILTER_ALL = 'ALL';
export const FIREWALL_EXPIRATION_FILTER_AUTO = 'AUTO';
export const FIREWALL_EXPIRATION_FILTER_IN_24_HOURS = 'IN_24_HOURS';
export const FIREWALL_EXPIRATION_FILTER_IN_7_DAYS = 'IN_7_DAYS';
export const FIREWALL_EXPIRATION_FILTER_IN_30_DAYS = 'IN_30_DAYS';
export const FIREWALL_EXPIRATION_FILTER_IN_90_DAYS = 'IN_90_DAYS';
export const FIREWALL_EXPIRATION_FILTER_IN_OVER_90_DAYS = 'IN_OVER_90_DAYS';
export const FIREWALL_EXPIRATION_FILTER_NEVER = 'NEVER';
export const FIREWALL_EXPIRATION_FILTER_EXPIRED = 'EXPIRED';

export const firewallExpirationDates = [
  { id: FIREWALL_EXPIRATION_FILTER_ALL, name: 'All' },
  { id: FIREWALL_EXPIRATION_FILTER_AUTO, name: 'Auto' },
  { id: FIREWALL_EXPIRATION_FILTER_IN_24_HOURS, name: 'In 24 Hours' },
  { id: FIREWALL_EXPIRATION_FILTER_IN_7_DAYS, name: 'In 7 Days' },
  { id: FIREWALL_EXPIRATION_FILTER_IN_30_DAYS, name: 'In 30 Days' },
  { id: FIREWALL_EXPIRATION_FILTER_IN_90_DAYS, name: 'In 90 Days' },
  { id: FIREWALL_EXPIRATION_FILTER_IN_OVER_90_DAYS, name: 'In Over 90 Days' },
  { id: FIREWALL_EXPIRATION_FILTER_NEVER, name: 'Never' },
  { id: FIREWALL_EXPIRATION_FILTER_EXPIRED, name: 'Expired' },
];
