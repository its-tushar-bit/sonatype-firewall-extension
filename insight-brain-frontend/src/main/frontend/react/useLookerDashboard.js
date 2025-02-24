/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useRef, useState } from 'react';
import { useSelector } from 'react-redux';
import axios from 'axios';
import { prop } from 'ramda';
import { LookerEmbedSDK } from '@looker/embed-sdk';
import { useDebounceCallback } from '@react-hook/debounce';

import {
  selectBaseUrl,
  selectSelectedDashboard,
} from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSelectors';
import {
  getEnterpriseReportingAcquireEmbedSessionUrl,
  getEnterpriseReportingGenerateEmbedTokensUrl,
} from 'MainRoot/util/CLMLocation';

export default function useLookerDashboard(iframeContainerId = '#dashboard') {
  const baseUrl = useSelector(selectBaseUrl);
  const selectedDashboard = useSelector(selectSelectedDashboard);
  const [iframeError, setIframeError] = useState(false);
  const [loadingDashboard, setLoadingDashboard] = useState(true);

  const tokens = useRef({});

  const acquireEmbedSession = async () =>
    axios
      .get(getEnterpriseReportingAcquireEmbedSessionUrl(selectedDashboard.dashboardId, window.location.origin))
      .then(prop('data'))
      .then((data) => {
        tokens.current = data;
        return data;
      });
  const generateEmbedTokens = async () =>
    axios
      .put(getEnterpriseReportingGenerateEmbedTokensUrl(), {
        api_token: tokens.current.api_token,
        navigation_token: tokens.current.navigation_token,
        session_reference_token: tokens.current.session_reference_token,
      })
      .then(prop('data'))
      .then((data) => {
        tokens.current = data;
        return data;
      });
  const embedDashboard = async () => {
    try {
      setIframeError(false);
      setLoadingDashboard(true);
      await LookerEmbedSDK.createDashboardWithId(selectedDashboard.dashboardPath)
        .appendTo(iframeContainerId)
        .build()
        .connect();
    } catch (error) {
      setIframeError(true);
    } finally {
      setLoadingDashboard(false);
    }
  };

  // this prevents dashboards loading twice if link is double clicked, or breaking if navigating
  // too quickly between dashboards
  const runLookerQuery = useDebounceCallback(function runLookerQuery() {
    LookerEmbedSDK.initCookieless(baseUrl, acquireEmbedSession, generateEmbedTokens);
    embedDashboard();
  }, 300);

  useEffect(() => {
    if (baseUrl && selectedDashboard) {
      runLookerQuery();
    }
  }, [baseUrl, selectedDashboard]);

  return { loadingDashboard, iframeError };
}
