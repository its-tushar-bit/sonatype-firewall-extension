/* eslint-disable no-useless-escape */
/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const formatNumberLocale = (value) => (typeof value === 'number' ? value.toLocaleString('en-US') : null);
