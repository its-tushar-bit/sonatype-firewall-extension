/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import store from 'MainRoot/reduxConfig/store';
import { selectIsUsageDashboardEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { load as loadProductLicense } from 'MainRoot/configuration/license/productLicenseActions';
import UsageDashboard from './UsageDashboard';
import { getValidPermissions } from '../util/permissionService';

router.stateRegistry.register({
  name: 'usage',
  url: '/usage',
  component: UsageDashboard,
  data: {
    title: 'Usage',
  },
  resolve: {
    isAuthorized: () => getValidPermissions(['CONFIGURE_SYSTEM', 'VIEW_USAGE']).then((valid) => valid.length > 0),
  },
});

router.transitionService.onBefore({ to: 'usage' }, () => {
  return Promise.all([
    store.dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded()),
    store.dispatch(loadProductLicense()),
  ]).then(() => {
    if (!selectIsUsageDashboardEnabled(store.getState())) {
      return router.stateService.target('dashboard.overview.violations');
    }
  });
});
