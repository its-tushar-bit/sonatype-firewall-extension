/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import axios from 'axios';
import { prop } from 'ramda';
import { LookerEmbedSDK } from '@looker/embed-sdk';
import { useDebounceCallback } from '@react-hook/debounce';

import {
  selectBaseUrl as defaultSelectBaseUrl,
  selectSelectedDashboard as defaultSelectSelectedDashboard,
} from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSelectors';
import {
  actions as filterActions,
  FILTER_STATES,
} from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSlice';
import { selectEnterpriseReportingFilter } from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSelectors';
import { findFilterByName } from 'MainRoot/enterpriseReporting/utils';
import {
  getEnterpriseReportingAcquireEmbedSessionUrl,
  getEnterpriseReportingGenerateEmbedTokensUrl,
} from 'MainRoot/util/CLMLocation';

export default function useLookerDashboard(iframeContainerId = '#dashboard', customSelectors = {}) {
  const dispatch = useDispatch();
  // Use custom selectors if provided, otherwise use defaults
  const selectBaseUrl = customSelectors.selectBaseUrl || defaultSelectBaseUrl;
  const selectSelectedDashboard = customSelectors.selectSelectedDashboard || defaultSelectSelectedDashboard;

  const baseUrl = useSelector(selectBaseUrl);
  const selectedDashboard = useSelector(selectSelectedDashboard);
  const { appliedFilter, appliedFilterName, filterState, loadingAllFilters, savedFilters } = useSelector(
    selectEnterpriseReportingFilter
  );

  const [iframeError, setIframeError] = useState(false);
  const [loadingDashboard, setLoadingDashboard] = useState(true);
  // Both 'enterprise' and 'firewall' category dashboards support filter embedding
  const supportsFilters = selectedDashboard?.category === 'enterprise' || selectedDashboard?.category === 'firewall';

  const tokens = useRef({});
  const dashboardCommunicationRef = useRef(null);
  // Track current dashboard to prevent stale iframe events from interfering with new dashboard if user moves
  // between pages before dashboard finishes loading
  const currentDashboardId = useRef(null);
  // Incremented on every embed attempt; callbacks check this to discard events from superseded embeds
  const embedGeneration = useRef(0);

  // Update filters values & force iframe reload if filterState = 'applying'
  useEffect(() => {
    if (filterState === FILTER_STATES.APPLYING && dashboardCommunicationRef.current && appliedFilter) {
      dashboardCommunicationRef.current.send('dashboard:filters:update', { filters: appliedFilter });
      dashboardCommunicationRef.current.send('dashboard:run');
    }
  }, [filterState, dispatch]);

  // Determines which filters to apply on dashboard load. If appliedFilterName !== null, find matching savedFilter,
  // otherwise, return an empty object to allow Looker to load its default filter values.
  const filtersToApplyOnLoad = useMemo(() => {
    if (appliedFilterName) {
      const savedFilter = findFilterByName(appliedFilterName, savedFilters);
      return savedFilter?.filter || {};
    }
    return {};
  }, [appliedFilterName, savedFilters]);

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
  // Embed a Looker dashboard without filter support (used for non-enterprise dashboards)
  const embedDashboard = async () => {
    const generation = embedGeneration.current;
    try {
      setLoadingDashboard(true);
      await LookerEmbedSDK.createDashboardWithId(selectedDashboard.dashboardPath)
        .appendTo(iframeContainerId)
        .withParams({
          _theme: '{"show_title": false}',
        })
        .withDynamicIFrameHeight()
        .build()
        .connect();
    } catch (error) {
      setIframeError(true);
    } finally {
      if (embedGeneration.current === generation) {
        setLoadingDashboard(false);
      }
    }
  };
  // Embed a Looker dashboard with filter support and event handlers (used for enterprise dashboards)
  const embedDashboardWithFilters = async () => {
    try {
      setLoadingDashboard(true);
      // Capture the dashboardId and generation for current iframe instance to prevent race conditions from stale
      // listeners — both when the user navigates away and when clearIframeContainer starts a new embed before
      // the previous connect() promise resolves
      const selectedId = selectedDashboard.dashboardId;
      currentDashboardId.current = selectedId;
      const generation = embedGeneration.current;

      const dashboard = await LookerEmbedSDK.createDashboardWithId(selectedDashboard.dashboardPath)
        .appendTo(iframeContainerId)
        .withFilters(filtersToApplyOnLoad)
        .withParams({
          _theme: '{"show_title": false}',
        })
        .withDynamicIFrameHeight()
        // Fires once when dashboard initially loads
        .on('dashboard:loaded', (evt) => {
          if (currentDashboardId.current !== selectedId || embedGeneration.current !== generation) {
            return;
          }
          dispatch(filterActions.handleDashLoaded(evt.dashboard.dashboard_filters));
        })
        // Fires immediately when user updates a filter in the iframe prior to refreshing the iframe
        .on('dashboard:filters:changed', () => {
          if (embedGeneration.current !== generation) {
            return;
          }
          dispatch(filterActions.handleDashChanged());
        })
        // Fires when dashboard is loading (either initial load or any re-load of iframe)
        .on('dashboard:run:start', () => {
          if (embedGeneration.current !== generation) {
            return;
          }
          dispatch(filterActions.setLoadingIframe(true));
        })
        // Fires when dashboard finishes running with new data
        .on('dashboard:run:complete', (evt) => {
          if (currentDashboardId.current !== selectedId || embedGeneration.current !== generation) {
            return;
          }
          dispatch(filterActions.handleDashUpdated(evt.dashboard.dashboard_filters));
        })
        .build()
        .connect();
      // Guard against stale connect() resolving after a newer embed has already started
      if (embedGeneration.current === generation) {
        dashboardCommunicationRef.current = dashboard;
      }
    } catch (error) {
      setIframeError(true);
    } finally {
      if (embedGeneration.current === generation) {
        setLoadingDashboard(false);
      }
    }
  };

  const clearIframeContainer = () => {
    embedGeneration.current += 1;
    const container = document.querySelector(iframeContainerId);
    if (container) {
      container.innerHTML = '';
    }
  };

  // This prevents dashboards loading twice if link is double clicked, or breaking if navigating
  // too quickly between dashboards
  const runLookerQuery = useDebounceCallback(function runLookerQuery() {
    clearIframeContainer();
    LookerEmbedSDK.initCookieless(baseUrl, acquireEmbedSession, generateEmbedTokens);
    if (supportsFilters) {
      embedDashboardWithFilters();
    } else {
      embedDashboard();
    }
  }, 300);

  useEffect(() => {
    if (baseUrl && selectedDashboard) {
      // Reset the dashboardCommunicationRef & currentDashboardId when selectedDashboard changes to invalidate old event handlers
      currentDashboardId.current = null;
      dashboardCommunicationRef.current = null;

      // Wait for filters to initialize before loading filter-supporting dashboards
      if (!supportsFilters || !loadingAllFilters) {
        setIframeError(false);
        runLookerQuery();
      }
    }
  }, [baseUrl, selectedDashboard, loadingAllFilters]);

  return { loadingDashboard, iframeError };
}
