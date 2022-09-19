/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  compose,
  includes,
  propEq,
  propOr,
  find,
  findIndex,
  equals,
  invertObj,
  sortWith,
  descend,
  ascend,
  prop,
  isNil,
} from 'ramda';

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

/**
 * Depending on the sorting key passed returns function which will be used for further sorting of policies
 * @param {any} field - policy value for predefined key (f.e.: name, threatLevel, build, etc)
 * @param {String} key - sorting key
 * @returns sorting function
 */
export const policiesComparator = (field, key) => {
  switch (key) {
    case 'name':
      return [ascend(field)];
    case 'threatLevel':
      return [ascend(field), ascend(prop('name'))];
    default:
      return [
        ascend((policy) => {
          const property = isNil(policy.hasLocalActionsOverrides) ? 'actions' : 'enforcementAction';
          return ['fail', 'warn', undefined].indexOf(policy[property][key]);
        }),
        ascend(prop('name')),
      ];
  }
};

//Returns a function that receives a list of applications or orgs and returns the owner's name that matches the ownerId
export const getOwnerName = (ownerId) => compose(propOr('', 'name'), find(propEq('publicId', ownerId)));

/**
 * @param ownerHierarchyIds - the ids in the owner hierarchy starting with current owner and ending with root org.
 * @param policy - the policy to get actions override for.
 * @returns {actionsOverride, isCurrentOwnerOverride} object or null if there is no override.
 */
export const getActionsOverride = (ownerHierarchyIds, policy) => {
  if (!policy.policyActionsOverrideAllowed || !policy.policyActionsOverrides) {
    return null;
  }

  const { ownerId, policyActionsOverrides } = policy;
  const policyOwnerIndex = findIndex(equals(ownerId), ownerHierarchyIds);
  const ownerIdsUptoPolicyOwnerId = ownerHierarchyIds.slice(0, policyOwnerIndex);

  let actionsOverride, isCurrentOwnerOverride;

  ownerIdsUptoPolicyOwnerId.some((id, index) => {
    if (policyActionsOverrides[id]) {
      actionsOverride = policyActionsOverrides[id];
      isCurrentOwnerOverride = index === 0;
      return true;
    }
  });

  return actionsOverride ? { actionsOverride, isCurrentOwnerOverride } : null;
};

export const sortByThreatLevel = sortWith([descend(prop('threatLevel')), descend(prop('name'))]);

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
