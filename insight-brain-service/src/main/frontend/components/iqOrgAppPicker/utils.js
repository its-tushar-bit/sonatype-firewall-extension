/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Given a selected Set, returns predicate functions to filter selected items.
 * @param selected Set
 */
export const isSelected = selected => item => selected.has(item.id);

/**
 * @param selected Set of item ids
 * @param items Array
 * @returns {boolean} do all provided items exist in selected
 */
export function areAllSelected(selected, items) {
  return !items.some(item => !selected.has(item.id));
}

/**
 * Groups array of apps into array of objects: {orgId:String, apps:Array}
 * @param apps Array
 * @returns Array of {orgId:String, apps:Array}
 */
export function groupAppsByOrgId(apps) {
  const appsMappedToOrgId = apps.reduce((obj, app) => {
    if (obj[app.organizationId]) {
      obj[app.organizationId].push(app);
    } else {
      obj[app.organizationId] = [app];
    }
    return obj;
  }, {});

  return Object.keys(appsMappedToOrgId).map(orgId => {
    return {
      orgId,
      apps: appsMappedToOrgId[orgId]
    };
  });
}

/**
 * @param obj
 * @returns Set of keys, which have value 'true' in provided object.
 */
export function selectedMapToSet(obj) {
  return new Set(Object.keys(obj).filter(key => obj[key] === true));
}
