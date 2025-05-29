/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { toURIParams, uriTemplate } from './urlUtil';

/**
 * This file is similar to CLMLocation except importable in bundles that do not use Angular. It contains just a few
 * URL functions that are used in those bundles.
 */
export const getComponentDetailsUrl = ({
  clientType,
  ownerType,
  ownerId,
  componentIdentifier,
  hash,
  matchState,
  proprietary,
  pathname,
  identificationSource,
  scanId,
}) => {
  const params = toURIParams({
    componentIdentifier,
    hash,
    matchState,
    proprietary,
    pathname,
    identificationSource,
    scanId,
  });
  return uriTemplate`/rest/${clientType}/componentDetails/${ownerType}/${encodeURIComponent(ownerId)}?` + params;
};

export function getApplicationNamesUrl() {
  return uriTemplate`/rest/application/services/names`;
}
