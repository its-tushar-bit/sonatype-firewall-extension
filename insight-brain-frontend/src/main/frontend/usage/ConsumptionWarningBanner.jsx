/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { NxTextLink, NxWarningAlert, NxErrorAlert } from '@sonatype/react-shared-components';
import { selectRouterState } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { fetchConsumptionSummary } from './usageApi';

const BANNER_DISMISSED_KEY = 'iq-consumption-banner-dismissed';

export function resetBannerDismissed() {
  sessionStorage.removeItem(BANNER_DISMISSED_KEY);
}

function formatNumberWithCommas(num) {
  return num?.toLocaleString('en-US') ?? '0';
}

function formatDate(dateString) {
  if (!dateString) return '';
  const [year, month, day] = dateString.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  return date.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' });
}

function getBannerType(percentUsed, threshold) {
  if (percentUsed >= 100) {
    return 'alert';
  }
  if (typeof threshold === 'number' && percentUsed >= threshold) {
    return 'warning';
  }
  return null;
}

export default function ConsumptionWarningBanner() {
  const [consumptionData, setConsumptionData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showAlert, setShowAlert] = useState(sessionStorage.getItem(BANNER_DISMISSED_KEY) !== 'true');

  const currentState = useSelector(selectRouterState);
  const routerState = useRouterState();

  const isUsagePage = currentState?.name?.startsWith('usage');

  useEffect(() => {
    if (isUsagePage) {
      setLoading(false);
      return;
    }

    const loadSummary = async () => {
      try {
        const response = await fetchConsumptionSummary();
        setConsumptionData(response.data);
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    };

    loadSummary();
  }, [isUsagePage]);

  const handleDismiss = () => {
    sessionStorage.setItem(BANNER_DISMISSED_KEY, 'true');
    setShowAlert(false);
  };

  const handleViewDashboard = (e) => {
    e.preventDefault();
    routerState.go('usage');
  };

  if (loading || !showAlert || error || !consumptionData) {
    return null;
  }

  const { consumed, limit, warningThresholdPct, percentUsed, resetDate } = consumptionData;

  if (!limit || limit <= 0) {
    return null;
  }

  if (isUsagePage) {
    return null;
  }

  const bannerType = getBannerType(percentUsed, warningThresholdPct);

  if (!bannerType) {
    return null;
  }

  const isAlert = bannerType === 'alert';
  const hasExceeded = percentUsed > 100;
  const overage = hasExceeded ? consumed - limit : 0;

  const AlertComponent = isAlert ? NxErrorAlert : NxWarningAlert;

  const messageContent = isAlert ? (
    hasExceeded ? (
      <>
        <strong>Usage Limit Exceeded.</strong> You&apos;ve exceeded your monthly limit by{' '}
        {formatNumberWithCommas(overage)} components. {formatNumberWithCommas(consumed)} components consumed of{' '}
        {formatNumberWithCommas(limit)} limit. Resets {formatDate(resetDate)}.
      </>
    ) : (
      <>
        <strong>Usage Limit Reached.</strong> You&apos;ve reached your monthly limit. {formatNumberWithCommas(consumed)}{' '}
        of {formatNumberWithCommas(limit)} components consumed. Resets {formatDate(resetDate)}.
      </>
    )
  ) : (
    <>
      <strong>Usage Warning.</strong> You&apos;ve used {Math.round(percentUsed)}% of your monthly limit.{' '}
      {formatNumberWithCommas(consumed)} of {formatNumberWithCommas(limit)} components consumed. Resets{' '}
      {formatDate(resetDate)}.
    </>
  );

  return (
    <AlertComponent onClose={handleDismiss}>
      {messageContent}{' '}
      <NxTextLink href={routerState.href('usage')} onClick={handleViewDashboard}>
        View Dashboard
      </NxTextLink>
      {' | '}
      <NxTextLink href="mailto:sales@sonatype.com?subject=Consumption%20Limit%20Inquiry">Contact Sales</NxTextLink>
    </AlertComponent>
  );
}
