/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { setToArray } from '../../../util/jsUtil';

export function filterToJson(filter) {
  return {
    organizationFilters: setToArray(filter.organizations),
    applicationFilters: setToArray(filter.applications),
    categoryFilters: setToArray(filter.categories),
    stageTypeFilters: setToArray(filter.stages),
    progressOptionsFilters: setToArray(filter.progressOptions),
  };
}
