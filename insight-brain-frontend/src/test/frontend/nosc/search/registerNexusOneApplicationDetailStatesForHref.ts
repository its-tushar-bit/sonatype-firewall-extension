/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';

/**
 * Registers the minimal application-detail state tree needed for
 * `router.stateService.href('nexusOneApplicationsDetail.overview', …)` in search
 * navigation tests. Matches the abstract parent + default-tab child from
 * {@link nexusOneApplicationDetailStates} without pulling in route components.
 */
export function registerNexusOneApplicationDetailStatesForHref(): void {
  if (!router.stateRegistry.get('nexusOneApplicationsDetail')) {
    router.stateRegistry.register({
      name: 'nexusOneApplicationsDetail',
      url: '/applications/{publicId}',
      abstract: true,
    });
  }
  if (!router.stateRegistry.get('nexusOneApplicationsDetail.overview')) {
    router.stateRegistry.register({
      name: 'nexusOneApplicationsDetail.overview',
      url: '',
    });
  }
}
