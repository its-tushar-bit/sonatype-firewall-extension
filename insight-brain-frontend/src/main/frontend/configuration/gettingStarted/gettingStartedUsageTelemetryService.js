/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const PURPOSE = 'GETTING_STARTED_USAGE';

export const DEPARTED_ACTION = 'DEPARTED';
export const REDIRECTED_ACTION = 'REDIRECTED';
export const VISITED_ACTION = 'VISITED';
export const LINK_CLICKED_ACTION = 'LINK_CLICKED';

export default function gettingStartedUsageTelemetryService($ngRedux, telemetryService) {
  function submitData(action, attrs, sync) {
    // don't need to connect to redux store - we don't need to subscribe to state changes
    const prevPage = $ngRedux.getState().router.prevState.name;

    telemetryService.submitData(
      PURPOSE,
      {
        action,
        pageNavigatedFrom: prevPage && 'systemMenu',
        ...attrs,
      },
      sync
    );
  }

  return {
    submitData,
  };
}

gettingStartedUsageTelemetryService.$inject = ['$ngRedux', 'telemetryService'];
