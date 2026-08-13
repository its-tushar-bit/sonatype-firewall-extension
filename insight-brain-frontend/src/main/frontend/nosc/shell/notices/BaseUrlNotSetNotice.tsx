/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
// @ts-expect-error — legacy slice file is intentionally .js
import { actions } from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSlice';
import { selectShouldDisplayNotice } from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';
import { selectIsBaseUrlConfigurationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectCurrentUser } from 'MainRoot/user/userSessionSelectors';
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';
import { NoticeBanner } from './NoticeBanner';

/**
 * Nexus One port of Classic's `BaseUrlNotSetNotice`. Reuses
 * `selectShouldDisplayNotice` rather than re-deriving visibility — the two
 * gates (feature flag + authenticated user) live in the selector already.
 * Links to the Settings hub's existing Base URL configuration embed
 * (`baseUrlConfiguration` state, `/baseUrl`).
 */
export function BaseUrlNotSetNotice(): JSX.Element | null {
  const dispatch = useDispatch();
  const currentUser = useSelector(selectCurrentUser);
  const isBaseUrlConfigurationEnabled = useSelector(selectIsBaseUrlConfigurationEnabled);
  const shouldDisplayNotice = useSelector(selectShouldDisplayNotice);

  useEffect(() => {
    if (currentUser && currentUser.authenticated && isBaseUrlConfigurationEnabled) {
      dispatch(actions.load());
    }
  }, [currentUser, isBaseUrlConfigurationEnabled, dispatch]);

  if (!shouldDisplayNotice) {
    return null;
  }

  return (
    <NoticeBanner
      linkText="Configure Base URL"
      linkHref={bundleIndexUrl('nexus-one', '/baseUrl')}
      testId="nosc-base-url-not-set-notice"
    >
      <strong>The Base URL is not configured. </strong>
      <span>This setting is required for features such as email, SCM, and Jira integration.</span>
    </NoticeBanner>
  );
}
