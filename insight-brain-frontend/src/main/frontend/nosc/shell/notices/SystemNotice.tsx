/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
// @ts-expect-error — legacy action creator file is intentionally .js
import { loadSystemNotice } from 'MainRoot/configuration/systemNoticeConfiguration/systemNoticeConfigurationActions';
import { NoticeBanner } from './NoticeBanner';

// No canonical TS type exists yet in the legacy JS slice (systemNoticeConfigurationReducer.js).
interface SystemNoticeConfigurationState {
  readonly systemNoticeConfiguration: {
    readonly serverData: {
      readonly enabled: boolean;
      readonly message: string | null;
    };
  };
}

function selectSystemNoticeMessage(state: SystemNoticeConfigurationState): string | null {
  const { serverData } = state.systemNoticeConfiguration;
  return serverData.enabled ? serverData.message : null;
}

/**
 * Nexus One port of Classic's `SystemNoticeContainer`/`SystemNotice`. Uses
 * the shared muted-orange `NoticeBanner` treatment (`Info` icon) — the
 * design no longer distinguishes System Notice's informational tone from
 * the other three notices with a separate color.
 */
export function SystemNotice(): JSX.Element | null {
  const dispatch = useDispatch();
  const message = useSelector(selectSystemNoticeMessage);

  useEffect(() => {
    dispatch(loadSystemNotice());
  }, [dispatch]);

  if (!message) {
    return null;
  }

  return (
    <NoticeBanner icon="info" testId="nosc-system-notice">
      {message}
    </NoticeBanner>
  );
}
