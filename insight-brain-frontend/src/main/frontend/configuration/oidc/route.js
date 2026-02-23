/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import OidcConfigurationPage from './OidcConfigurationPage';
import { selectIsDirty } from './oidcConfigurationSelectors';

router.stateRegistry.register({
  name: 'oidc',
  url: '/oidc',
  component: OidcConfigurationPage,
  data: {
    title: 'OIDC',
    isDirty: selectIsDirty,
  },
});
