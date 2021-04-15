/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function ErrorMessage(error) {
  var responseText = error[0] || error.data,
    status = error[1] || error.status,
    headers = error[2] || error.headers;

  if (status === 0 || status >= 1000) {
    return 'Network error while contacting server';
  } else if (responseText && (headers('Content-Type') || '').indexOf('text/plain') >= 0) {
    return responseText;
  } else {
    return 'Error ' + status;
  }
}

export default function ErrorMessageFactory() {
  return ErrorMessage;
}
