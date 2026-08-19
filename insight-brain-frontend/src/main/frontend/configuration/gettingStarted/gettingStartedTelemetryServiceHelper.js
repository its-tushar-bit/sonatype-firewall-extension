/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { submitTelemetryData } from '../../util/telemetryUtils';
const PURPOSE = 'GETTING_STARTED_USAGE';

export const DEPARTED_ACTION = 'DEPARTED';
export const REDIRECTED_ACTION = 'REDIRECTED';
export const VISITED_ACTION = 'VISITED';
export const LINK_CLICKED_ACTION = 'LINK_CLICKED';

export function submitData(action, attrs, prevPage) {
  return submitTelemetryData(PURPOSE, {
    action,
    pageNavigatedFrom: prevPage ? 'systemMenu' : '',
    ...attrs,
  });
}
