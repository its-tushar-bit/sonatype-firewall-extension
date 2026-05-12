/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global CLM_SERVER_VERSION */
import React from 'react';
import classnames from 'classnames';
import { UIRouterContext, UIView } from '@uirouter/react';
import { Provider as ReduxProvider, useSelector } from 'react-redux';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';
import store from 'MainRoot/reduxConfig/store';
import router from 'MainRoot/router/routerInstance';
import { selectRouterState, selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectError } from 'MainRoot/session/appErrorSelectors';
import { selectShowLoginModal } from 'MainRoot/user/LoginModal/userLoginSelectors';
import SystemNoticeContainer from './systemNotice/SystemNoticeContainer';
import ChangeDefaultAdminPasswordNotice from './changeDefaultAdminPasswordNotice/ChangeDefaultAdminPasswordNotice';
import BaseUrlNotSetNotice from './configuration/baseUrl/baseUrlNotSetNotice/BaseUrlNotSetNotice';
import AnnouncementBanner from './announcementBanner/AnnouncementBanner';
import ToastContainer from './toastContainer/ToastContainer';
import NavigationContainer from './navigationContainer/NavigationContainer';
import MainHeader from './mainHeader/MainHeader.jsx';
import Footer from './react/Footer/Footer.jsx';
import ModalContainer from './modalContainer/ModalContainer';

function PageLayout() {
  const currentState = useSelector(selectRouterState);
  const currentParams = useSelector(selectRouterCurrentParams);
  const error = useSelector(selectError);
  const showLoginModal = useSelector(selectShowLoginModal);

  const additionalPageClass = currentState?.data?.additionalPageClass;
  const viewportSized = currentState?.data?.viewportSized;
  const hideFooter = currentState?.data?.hideFooter;
  const embeddable = currentParams?.embeddable;

  return (
    <div className={classnames('nx-page', additionalPageClass)}>
      <div className="nx-system-notice-container">
        {!error && <SystemNoticeContainer />}
        {!error && <ChangeDefaultAdminPasswordNotice />}
        {!error && <BaseUrlNotSetNotice />}
        {!error && <AnnouncementBanner />}
      </div>
      <ToastContainer />
      {!embeddable && <NavigationContainer clmServerVersion={CLM_SERVER_VERSION} />}
      <MainHeader />
      <div id="iq-content" className="nx-page-content nx-page-content--full-width">
        {/* Portal target for sidebar components (OwnerDetailSidebar, OwnerSideNav, SidebarNavList).
            This pattern originated when portals were needed to render React content
            into a specific DOM location for CSS grid layout. Now that the app is fully React, the sidebar
            could instead be rendered directly here via context or route config, but the portal approach
            still works and the CSS grid relies on the :empty/:not(:empty) state of this element. */}
        <div id="iq-sidebar-container"></div>
        <div
          id="iq-footer-container"
          className={classnames('nx-global-footer-2-container', { 'nx-viewport-sized': viewportSized })}
        >
          {/* Empty page-main ensures the background behind the login modal renders correctly */}
          {showLoginModal && <div className="nx-page-main" />}
          <UIView />
          {error && (
            <div className="nx-page-main nx-page-main--error">
              <div className="iq-alert iq-alert--error">
                <NxFontAwesomeIcon icon={faExclamationTriangle} />
                <strong>Error</strong>
                <p>An unrecoverable error has occurred while loading the page.</p>
                <p>
                  Please try to reload the page, if the problem persists contact your server administrator. ({error})
                </p>
              </div>
            </div>
          )}
          {!hideFooter && <Footer clmServerVersion={CLM_SERVER_VERSION} />}
        </div>
      </div>
      <ModalContainer />
    </div>
  );
}

export default function App() {
  return (
    <ReduxProvider store={store}>
      <UIRouterContext.Provider value={router}>
        <PageLayout />
      </UIRouterContext.Provider>
    </ReduxProvider>
  );
}
