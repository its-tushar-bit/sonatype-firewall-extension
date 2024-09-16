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
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

export function deriveEditRoute(routerState, to, params = {}) {
  return deriveRouteFromStateParams('edit', routerState, to, params);
}

export function deriveViewRoute(routerState, to, params = {}) {
  return deriveRouteFromStateParams('view', routerState, to, params);
}

function deriveRouteFromStateParams(ownerState, routerState, to, params = {}) {
  const { currentState, currentParams } = routerState;
  const isApp = includes('application', currentState.name);
  const isRepositoryContainer =
    includes('management.view.repository_container', currentState.name) ||
    includes('repository_container', currentState.name);
  const isRepositoryManager =
    includes('management.view.repository_manager', currentState.name) ||
    includes('repository_manager', currentState.name);
  const isRepository =
    includes('management.view.repository', currentState.name) || includes('repository', currentState.name);
  const isSbomManager = includes('sbomManager', currentState.name);

  const type = isRepositoryContainer
    ? 'repository_container'
    : isRepositoryManager
    ? 'repository_manager'
    : isRepository
    ? 'repository'
    : isApp
    ? 'application'
    : 'organization';
  const ownerId = isRepositoryContainer
    ? 'REPOSITORY_CONTAINER_ID'
    : isRepositoryManager
    ? 'repositoryManagerId'
    : isRepository
    ? 'repositoryId'
    : isApp
    ? 'applicationPublicId'
    : 'organizationId';

  if (isRepositoryContainer) {
    params['repositoryContainerId'] = 'REPOSITORY_CONTAINER_ID';
  } else {
    if (currentParams[ownerId]) {
      params[ownerId] = currentParams[ownerId];
    }
  }

  return {
    to: `${isSbomManager ? 'sbomManager.' : ''}management.${ownerState}.${type}${to ? '.' + to : ''}`,
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

/**
 * @param ownerHierarchyIds - the ids in the owner hierarchy starting with current owner and ending with root org.
 * @param policy - the policy to get notifications override for.
 * @returns {notificationsOverride, isCurrentOwnerOverride} object or null if there is no override.
 */
export const getNotificationsOverride = (ownerHierarchyIds, policy) => {
  if (!policy.policyNotificationsOverrideAllowed || !policy.policyNotificationsOverrides) {
    return null;
  }

  const { ownerId, policyNotificationsOverrides } = policy;
  const policyOwnerIndex = findIndex(equals(ownerId), ownerHierarchyIds);
  const ownerIdsUptoPolicyOwnerId = ownerHierarchyIds.slice(0, policyOwnerIndex);

  let notificationsOverride, isCurrentOwnerOverride;

  ownerIdsUptoPolicyOwnerId.some((id, index) => {
    if (policyNotificationsOverrides[id]) {
      notificationsOverride = policyNotificationsOverrides[id];
      isCurrentOwnerOverride = index === 0;
      return true;
    }
  });

  return notificationsOverride ? { notificationsOverride, isCurrentOwnerOverride } : null;
};

export const sortByThreatLevel = sortWith([descend(prop('threatLevel')), descend(prop('name'))]);
export const sortByThreatGroupName = sortWith([descend(prop('name')), descend(prop('threatLevel'))]);

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

/**
 * @param membersByRoles - array of roles
 * @returns [membersByRoles] the roles which has members assigned
 */
export const getRolesWithLocalMembers = (membersByRoles) => {
  if (membersByRoles) {
    return membersByRoles.filter(function (membersByRole) {
      return membersByRole.membersByOwner[0].members.length > 0;
    });
  } else {
    return [];
  }
};

/**
 * @param membersByRoles - array of roles
 * @returns [membersByRoles] returns roles with no members
 */
export const getRolesWithoutLocalMembers = (membersByRoles) => {
  if (membersByRoles) {
    return membersByRoles.filter(function (membersByRole) {
      return membersByRole.membersByOwner[0].members.length === 0;
    });
  } else {
    return [];
  }
};

export const isEmptyNonLocal = (owner) =>
  isNilOrEmpty(owner.licenseThreatGroups) && owner.inherited && owner.ownerType === 'organization';

/**
 * Converts applicable license threat groups response into array of objects
 * usable by IqCollapsibleRow component
 * @param threatGroups array of threat group objects
 */
export function formatCollapsibleThreatGroups(threatGroup) {
  const sortedByThreatLevel = sortByThreatLevel(threatGroup.licenseThreatGroups);
  const groupsWithInheritanceValue = sortedByThreatLevel.map((ltg) => {
    return {
      ...ltg,
      inherited: threatGroup.inherited,
    };
  });

  return {
    headerTitle: threatGroup.inherited
      ? `Inherited from ${threatGroup.ownerName}`
      : `Local to ${threatGroup.ownerName}`,
    emptyMessage: threatGroup.inherited
      ? `No ${threatGroup.ownerName} threat groups defined`
      : 'No local threat groups defined',
    sortedThreatGroups: groupsWithInheritanceValue,
    inherited: threatGroup.inherited,
  };
}
