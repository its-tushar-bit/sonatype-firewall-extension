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
  const selectBaseUrl = customSelectors.selectBaseUrl || defaultSelectBaseUrl;
  const selectSelectedDashboard = customSelectors.selectSelectedDashboard || defaultSelectSelectedDashboard;

  const baseUrl = useSelector(selectBaseUrl);
  const selectedDashboard = useSelector(selectSelectedDashboard);
  const { appliedFilter, appliedFilterName, filterState, loadingAllFilters, savedFilters } = useSelector(
    selectEnterpriseReportingFilter
  );

  const [iframeError, setIframeError] = useState(false);
  const [loadingDashboard, setLoadingDashboard] = useState(true);
  const supportsFilters = selectedDashboard?.category === 'enterprise' || selectedDashboard?.category === 'firewall';

  const tokens = useRef({});
  const dashboardCommunicationRef = useRef(null);
  const currentDashboardId = useRef(null);
  // Incremented on every embed attempt; callbacks check this to discard events from superseded embeds
  const embedGeneration = useRef(0);

  useEffect(() => {
    if (filterState === FILTER_STATES.APPLYING && dashboardCommunicationRef.current && appliedFilter) {
      dashboardCommunicationRef.current.send('dashboard:filters:update', { filters: appliedFilter });
      dashboardCommunicationRef.current.send('dashboard:run');
    }
  }, [filterState, dispatch]);

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
      if (embedGeneration.current !== generation) {
        removeStaleIframe();
      } else {
        removeLeadingStaleIframe();
      }
    } catch (error) {
      setIframeError(true);
    } finally {
      if (embedGeneration.current === generation) {
        setLoadingDashboard(false);
      }
    }
  };
  const embedDashboardWithFilters = async () => {
    const generation = embedGeneration.current;
    try {
      setLoadingDashboard(true);
      const selectedId = selectedDashboard.dashboardId;
      currentDashboardId.current = selectedId;

      const dashboard = await LookerEmbedSDK.createDashboardWithId(selectedDashboard.dashboardPath)
        .appendTo(iframeContainerId)
        .withFilters(filtersToApplyOnLoad)
        .withParams({
          _theme: '{"show_title": false}',
        })
        .withDynamicIFrameHeight()
        .on('dashboard:loaded', (evt) => {
          if (currentDashboardId.current !== selectedId || embedGeneration.current !== generation) {
            return;
          }
          dispatch(filterActions.handleDashLoaded(evt.dashboard.dashboard_filters));
        })
        .on('dashboard:filters:changed', () => {
          if (embedGeneration.current !== generation) {
            return;
          }
          dispatch(filterActions.handleDashChanged());
        })
        .on('dashboard:run:start', () => {
          if (embedGeneration.current !== generation) {
            return;
          }
          dispatch(filterActions.setLoadingIframe(true));
        })
        .on('dashboard:run:complete', (evt) => {
          if (currentDashboardId.current !== selectedId || embedGeneration.current !== generation) {
            return;
          }
          dispatch(filterActions.handleDashUpdated(evt.dashboard.dashboard_filters));
        })
        .build()
        .connect();
      if (embedGeneration.current !== generation) {
        removeStaleIframe();
      } else {
        removeLeadingStaleIframe();
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

  // SDK has no disconnect API — stale connect() appends its iframe as lastChild; remove it.
  // Only removes when > 1 children exist: if only 1 child is present it must be the active embed
  // (triple-rapid-switch: gen=N stale resolves when gen=N+2 active is the only iframe).
  // Using > 0 here would wrongly delete that active iframe — > 1 is the intentional safe minimum.
  const removeStaleIframe = () => {
    const container = document.querySelector(iframeContainerId);
    if (container?.children.length > 1) {
      container.removeChild(container.lastElementChild);
    }
  };

  // When the active embed resolves but a stale iframe resolved first (stale-before-active ordering),
  // the stale iframe sits at firstElementChild. Remove it if two element children are present.
  const removeLeadingStaleIframe = () => {
    const container = document.querySelector(iframeContainerId);
    if (container?.children.length > 1) {
      container.removeChild(container.firstElementChild);
    }
  };

  const clearIframeContainer = () => {
    embedGeneration.current += 1;
    setLoadingDashboard(true);
    const container = document.querySelector(iframeContainerId);
    if (container) {
      container.innerHTML = '';
    }
  };

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
      currentDashboardId.current = null;
      dashboardCommunicationRef.current = null;

      if (!supportsFilters || !loadingAllFilters) {
        setIframeError(false);
        runLookerQuery();
      }
    }
  }, [baseUrl, selectedDashboard, loadingAllFilters]);

  return { loadingDashboard, iframeError };
}
