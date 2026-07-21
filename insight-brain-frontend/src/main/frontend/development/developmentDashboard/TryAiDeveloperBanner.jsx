/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import { NxTextLink, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faWandMagicSparkles } from '@fortawesome/pro-regular-svg-icons';
import {
  selectIsAiDeveloperEntitled,
  selectAiDeveloperUrl,
} from 'MainRoot/mainHeader/MenuBar/SolutionSwitcherContainer/solutionSwitcherSelectors';
import './TryAiDeveloperBanner.scss';

export default function TryAiDeveloperBanner() {
  const isAiDeveloperEntitled = useSelector(selectIsAiDeveloperEntitled);
  const aiDeveloperUrl = useSelector(selectAiDeveloperUrl);

  if (!isAiDeveloperEntitled || !aiDeveloperUrl) {
    return null;
  }

  return (
    <div className="iq-try-ai-developer-banner" role="region" aria-label="Try AI Developer notification">
      <div className="iq-try-ai-developer-banner__icon">
        <NxFontAwesomeIcon icon={faWandMagicSparkles} />
      </div>
      <div className="iq-try-ai-developer-banner__content">
        <div>
          <strong>Sonatype AI Developer</strong> is available for your organization.
        </div>
        <div>Get AI-powered remediation guidance and faster fixes right in your workflow.</div>
        <div>
          <NxTextLink href={aiDeveloperUrl} external aria-label="Try AI Developer (opens in new window)">
            Try AI Developer
          </NxTextLink>
        </div>
      </div>
    </div>
  );
}
