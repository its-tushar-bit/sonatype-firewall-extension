/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';
import { actions } from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSlice';
import { selectShouldDisplayNotice } from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';
import { selectIsBaseUrlConfigurationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectCurrentUser } from 'MainRoot/user/userSessionSelectors';

export default function BaseUrlNotSetNotice() {
  const dispatch = useDispatch();
  const currentUser = useSelector(selectCurrentUser);
  const isBaseUrlConfigurationEnabled = useSelector(selectIsBaseUrlConfigurationEnabled);
  const shouldDisplayNotice = useSelector(selectShouldDisplayNotice);
  const loadConf = () => dispatch(actions.load());

  useEffect(() => {
    if (currentUser && currentUser.authenticated && isBaseUrlConfigurationEnabled) {
      loadConf();
    }
  }, [currentUser, isBaseUrlConfigurationEnabled]);

  return shouldDisplayNotice ? (
    <>
      <div id="base-url-not-set-notice" className="nx-system-notice nx-system-notice--alert">
        <NxFontAwesomeIcon icon={faExclamationTriangle} />
        <strong> The Base URL is not configured.</strong>
        <span> This setting is required for features such as email, SCM, and Jira integration.</span>
      </div>
    </>
  ) : null;
}
