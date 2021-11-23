/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import MenuBarBackButton from '../mainHeader/MenuBar/MenuBarBackButton';

import { useRouterState } from '../react/RouterStateContext';

export default function AddAndRequestWaiversBackButton(props) {
  const { violationId, prevStateName, prevParams } = props;
  const { hash, scanId, publicId } = prevParams;

  const uiRouterState = useRouterState();

  let backButtonHref;
  let backButtonTitle = 'Back to Waivers';

  /*
  The logic flow below describe where the back button will navigate to, and what the text of
  the back button will be.

  If a user navigates to the Request Waiver Page from the Waivers for Violation page,
  the state will have a hash, scanId, publicId, and prevStateName, so
  the back button from the Request Waivers Page will navigate to the Waivers for Violation Page
  with a back button that navigates to Component Details.

  If a user navigates to the Request Waiver Page directly from the Violation Details popover/page,
  the state will have a hash, scanId, publicId, and prevStateName, so
  the back button from the Request Waivers Page will navigate to the Violation Details popover/page.

  If a user navigates to the Request Waiver Page directly by copy/pasting the Request Waiver URL
  directly into the browser, i.e. if the Request Waiver page is accessed through a shareable URL,
  the state will not have a hash, scanId, publicId or prevStateName, so
  the back button from the Request Waivers Page will navigate to the Waivers for Violation Page
  with a back button that navigates to Violation Details.
  */

  if (hash && scanId && publicId && prevStateName) {
    //Navigated from Waivers for Violation page
    if (prevStateName === 'applicationReport.violationWaivers') {
      backButtonHref = uiRouterState.href('applicationReport.violationWaivers', { ...prevParams });
    }
    //Navigated from Violation Details page/popover
    else if (prevStateName === 'applicationReport.componentDetails.violations') {
      backButtonHref = uiRouterState.href('applicationReport.componentDetails.violations', {
        hash,
        scanId,
        publicId,
      });
      backButtonTitle = 'Back to Component Details';
    }
    //Navigated from any other page
    else {
      backButtonHref = uiRouterState.href('listWaivers', { violationId });
    }
  }
  //Navigated from a shareable URL
  else {
    backButtonHref = uiRouterState.href('listWaivers', { violationId });
  }

  return <MenuBarBackButton text={backButtonTitle} href={backButtonHref} />;
}

AddAndRequestWaiversBackButton.propTypes = {
  violationId: PropTypes.string,
  prevStateName: PropTypes.string,
  prevParams: PropTypes.shape({
    publicId: PropTypes.string,
    scanId: PropTypes.string,
    hash: PropTypes.string,
    sidebarReference: PropTypes.string,
    type: PropTypes.string,
  }),
};
