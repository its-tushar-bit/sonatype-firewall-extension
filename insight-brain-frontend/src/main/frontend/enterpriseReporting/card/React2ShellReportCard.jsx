/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxTextLink, NxH3, NxList, NxSmallTag, NxFontAwesomeIcon, NxCard } from '@sonatype/react-shared-components';
import { faExclamationTriangle, faCheck } from '@fortawesome/free-solid-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import './_enterpriseReportCard.scss';

export default function React2ShellReportCard() {
  const routerState = useRouterState();

  const reportUrl = routerState?.href?.('react2ShellReport') || '#';

  return (
    <NxCard
      id="enterprise-reporting-dashboard-react2shell"
      className="iq-enterprise-reporting-card iq-enterprise-reporting-card--dashboard"
      aria-labelledby="react2shell-card-title"
    >
      <NxSmallTag color="red" className="iq-enterprise-reporting-card__spotlight">
        NEW
      </NxSmallTag>

      <NxCard.Header className="iq-enterprise-reporting-card__header">
        <NxH3 id="react2shell-card-title">React2Shell Impact</NxH3>
      </NxCard.Header>

      <NxCard.Content>
        <NxCard.CallOut className="iq-enterprise-reporting-card__icon warning">
          <NxFontAwesomeIcon icon={faExclamationTriangle} />
        </NxCard.CallOut>
        <NxCard.Text>A severe flaw in React Server Components could allow attackers to run arbitrary code.</NxCard.Text>
        <NxList bulleted className="iq-enterprise-reporting-card__features">
          {['Identify affected applications', 'Prioritize remediation efforts'].map((feature, idx) => (
            <NxList.Item key={idx}>
              <NxFontAwesomeIcon className="enterprise" icon={faCheck} />
              <NxList.Text className="iq-enterprise-reporting-card__feature-item">{feature}</NxList.Text>
            </NxList.Item>
          ))}
        </NxList>
      </NxCard.Content>

      <NxCard.Footer className="iq-enterprise-reporting-card__footer">
        <NxTextLink
          href={reportUrl}
          id="react2shell-dashboard-btn"
          className="nx-btn nx-btn--tertiary iq-enterprise-reporting-card__button"
        >
          View React2Shell Impact
        </NxTextLink>
      </NxCard.Footer>
    </NxCard>
  );
}
