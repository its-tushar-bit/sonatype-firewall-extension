/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

import { originNamesForAddRequestPages } from 'MainRoot/util/waiverUtils';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function AddAndRequestWaiversBackButton(props) {
  const { violationId, prevStateName, prevParams } = props;
  const { hash, scanId, publicId, sidebarReference, type } = prevParams;

  const uiRouterState = useRouterState();

  let backButtonHref;
  let backButtonTitle = 'Back to Waivers';

  /*
  The logic flow below describe where the back button on the Add/Request Waiver page will navigate to, 
  and what the text of the back button will be.

  Violation Details POPOVER (via an application report's Component Details Page)
  The state will have a hash, scanId, publicId, and prevStateName. There are two possible routes to 
  the Add/Request Waiver Page from the popover:
    1. Click Manage Waivers to go to the Waivers for Violation Page, then Add/Request Waiver
      The back button will say "Back to Waivers" and will navigate back to the Waivers 
      for Violation Page, with a back button that navigates to the Component Details Page
    2. Click the dropdown toggle on the Manage Waivers button, then Add or Request Waiver
      The back button will say "Back to Component Details" and will navigate back to
      the Component Details Page, with a back button that navigates to the Application Report

  Violation Details PAGE (via the Dashboard's Violations tab),
  The state will have a sidebarReference and type. There are two possible routes to the Add/Request 
  Waiver Page via this route:
    1. Click Manage Waivers to go to the Waivers for Violation Page, then Add/Request Waiver
      The back button will say "Back to Waivers" and will navigate back to the Waivers 
      for Violation Page, with a back button that navigates to the Violation Details Page
    2. Click the dropdown toggle on the Manage Waivers button, then Add or Request Waiver
      The back button will say "Back to Violation Details" and will navigate back to
      the Violation Details Page, with a back button that navigates to the Dashboard's Violations tab

  If a user navigates directly to the Add/Request Waiver URL, the the state will only have a
  violationId, so the back button will navigate to the Waivers for Violation Page with a
  back button that navigates to Violation Details Page (then back to the Dashboard's Violations tab).
  */

  // No previous state information
  if (!prevStateName) {
    backButtonHref = uiRouterState.href(originNamesForAddRequestPages.WAIVERS_FOR_VIOLATION, { violationId });
  }

  // Navigated from app report
  else if (hash && scanId && publicId) {
    // Navigated from Waivers for Violation page
    if (prevStateName === originNamesForAddRequestPages.APP_REPORT_VIOLATION_WAIVERS) {
      backButtonHref = uiRouterState.href(originNamesForAddRequestPages.APP_REPORT_VIOLATION_WAIVERS, {
        ...prevParams,
      });
    }
    // Navigated from Violation Details page/popover
    else if (prevStateName === originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS) {
      backButtonHref = uiRouterState.href(originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS, {
        hash,
        scanId,
        publicId,
      });
      backButtonTitle = 'Back to Component Details';
      // Navigated from any other page
    } else {
      backButtonHref = uiRouterState.href(originNamesForAddRequestPages.WAIVERS_FOR_VIOLATION, { violationId });
    }
  }

  // Navigated from Dashboard
  else if (sidebarReference && type) {
    // Navigated from Violation Details page/popover
    if (prevStateName === originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW) {
      backButtonHref = uiRouterState.href(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
        ...prevParams,
      });
      backButtonTitle = 'Back to Violation Details';
      // Navigated from Waivers for Violation page
    } else {
      backButtonHref = uiRouterState.href(originNamesForAddRequestPages.WAIVERS_FOR_VIOLATION, { ...prevParams });
    }
  }

  // Navigated from a shareable URL
  else {
    backButtonHref = uiRouterState.href(originNamesForAddRequestPages.WAIVERS_FOR_VIOLATION, { violationId });
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
