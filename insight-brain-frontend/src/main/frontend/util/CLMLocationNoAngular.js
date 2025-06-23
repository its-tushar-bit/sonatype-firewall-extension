/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { BASE_URL, toURIParams, uriTemplate } from './urlUtil';

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
  dependencyType,
}) => {
  const params = toURIParams({
    componentIdentifier,
    hash,
    matchState,
    proprietary,
    pathname,
    identificationSource,
    scanId,
    dependencyType,
  });
  return uriTemplate`/rest/${clientType}/componentDetails/${ownerType}/${encodeURIComponent(ownerId)}?` + params;
};

export const getVersionGraphUrl = ({
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
  stageId,
  dependencyType,
}) => {
  const params = toURIParams({
    componentIdentifier,
    hash,
    matchState,
    proprietary,
    pathname,
    identificationSource,
    scanId,
    stageId,
    dependencyType,
  });
  return (
    uriTemplate`/rest/${clientType}/componentDetails/${ownerType}/${encodeURIComponent(ownerId)}/allVersions?` + params
  );
};

export function getApplicationNamesUrl() {
  return uriTemplate`/rest/application/services/names`;
}

function getUserTelemetryPrefix() {
  const isRM = BASE_URL.includes('rest/healthcheck/clm');

  // use the RM proxy endpoint if we are in RM.  The normal one will get blocked
  return isRM ? uriTemplate`/rest/rm/user-telemetry` : uriTemplate`/rest/user-telemetry`;
}

export const getUserTelemetryConfig = () => `${getUserTelemetryPrefix()}/config`;
export const getUserTelemetryJavascript = () => `${getUserTelemetryPrefix()}/javascript`;
export const getUserTelemetryProxy = () => `${getUserTelemetryPrefix()}/events`;
