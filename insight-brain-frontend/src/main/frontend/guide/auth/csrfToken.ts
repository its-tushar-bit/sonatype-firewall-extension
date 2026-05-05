/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getCookieValue } from './cookieUtils';

const COOKIE_NAME = 'CLM-CSRF-TOKEN';

export function getCsrfToken(): string | undefined {
  return getCookieValue(COOKIE_NAME);
}
