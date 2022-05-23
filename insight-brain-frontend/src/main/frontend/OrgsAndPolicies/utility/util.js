/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { compose, includes, propEq, propOr, find, invertObj } from 'ramda';

export function deriveEditRoute(routerState, to, params = {}) {
  return deriveRouteFromStateParams('edit', routerState, to, params);
}

export function deriveViewRoute(routerState, to, params = {}) {
  return deriveRouteFromStateParams('view', routerState, to, params);
}

function deriveRouteFromStateParams(ownerState, routerState, to, params = {}) {
  const { currentState, currentParams } = routerState;
  const isApp = includes('application', currentState.name);
  const isRepositories = includes('repositories', currentState.name);

  const type = isApp ? 'application' : isRepositories ? 'repositories' : 'organization';
  const ownerId = isApp ? 'applicationPublicId' : 'organizationId';

  if (currentParams[ownerId]) {
    params[ownerId] = currentParams[ownerId];
  }

  return {
    to: `management.${ownerState}.${type}${to ? '.' + to : ''}`,
    params,
  };
}
//Returns a function that receives a list of applications or orgs and returns the owner's name that matches the ownerId
export const getOwnerName = (ownerId) => compose(propOr('', 'name'), find(propEq('publicId', ownerId)));

export const rscToAngularColorMap = {
  purple: 'light-purple',
  pink: 'light-red',
  blue: 'dark-blue',
  red: 'dark-red',
  turquoise: 'dark-green',
  orange: 'orange',
  yellow: 'yellow',
  kiwi: 'light-green',
  sky: 'light-blue',
  indigo: 'dark-purple',
};

export const angularToRscColorMap = invertObj(rscToAngularColorMap);
