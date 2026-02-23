/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import SastScanPage from 'MainRoot/sastScan/SastScanPage';

router.stateRegistry.register({
  name: 'sastScan',
  url: `/application/{applicationPublicId}/sastScan/{sastScanId}`,
  component: SastScanPage,
});
