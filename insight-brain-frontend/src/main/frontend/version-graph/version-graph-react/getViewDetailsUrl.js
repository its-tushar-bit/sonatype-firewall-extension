/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { toURIParams } from 'MainRoot/util/urlUtil';

/**
 * @return the URL to the viewdetails bundle for the given parameters
 */
export default function getViewDetailsUrl(appId, hash, componentIdentifier, isCurrentVersionSelected) {
  const viewDetailsParams = {
    appId,
    hash: isCurrentVersionSelected ? hash : null,
    componentIdentifier: JSON.stringify(componentIdentifier),
  };

  return `./viewdetails.html?${toURIParams(viewDetailsParams)}`;
}
