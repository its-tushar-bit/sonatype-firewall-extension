/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { getTelemetryUrl } from './CLMLocation';

const getCookie = (name) => `; ${document.cookie}`.split(`; ${name}=`).pop().split(';').shift();

export function submitTelemetryData(purpose, attributes, sync) {
  const xhr = new XMLHttpRequest();
  xhr.open('POST', getTelemetryUrl(), sync !== true);
  xhr.setRequestHeader('Content-Type', 'application/json');
  xhr.setRequestHeader('X-CSRF-TOKEN', getCookie('CLM-CSRF-TOKEN'));
  xhr.send(
    JSON.stringify({
      purpose,
      attributes,
      timestamp: new Date().getTime(),
    })
  );
}
