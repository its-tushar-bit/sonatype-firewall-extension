/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import PropTypes from 'prop-types';
import { NxCard, NxH2, NxTextLink, NxP } from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function React2ShellAbout({ cveIds }) {
  const routerState = useRouterState();

  const renderCveLinks = () => {
    return (
      <>
        See{' '}
        {cveIds.map((cve, index) => (
          <React.Fragment key={cve}>
            <NxTextLink href={routerState.href('vulnerabilitySearchDetail', { id: cve })}>{cve}</NxTextLink>
            {index < cveIds.length - 2 && ', '}
            {index === cveIds.length - 2 && ', and '}
          </React.Fragment>
        ))}
        .
      </>
    );
  };

  return (
    <div className="iq-react2shell-about nx-card-container">
      <NxCard className="nx-card--equal" aria-label="About the React2Shell Vulnerability">
        <NxCard.Header className="iq-react2shell-about__card-header--left-aligned">
          <NxH2>About the React2Shell Vulnerability</NxH2>
        </NxCard.Header>
        <NxCard.Content className="iq-react2shell-about__card-content--left-aligned">
          <NxP>
            React2Shell is a critical vulnerability affecting certain versions of React Server Components. It allows
            attackers to execute arbitrary shell commands through specially crafted payloads in server-rendered
            components. Affected applications should be evaluated and remediated immediately. {renderCveLinks()}
          </NxP>
          <NxP className="iq-react2shell-about__blog-link">
            <NxTextLink href="https://links.sonatype.com/announcements/react2shell" external>
              Blog: Serious React2Shell Vulnerabilities Require Immediate Attention
            </NxTextLink>
          </NxP>
        </NxCard.Content>
      </NxCard>
      <NxCard className="nx-card--equal" aria-label="Steps to Evaluate & Remediate">
        <NxCard.Header className="iq-react2shell-about__card-header--spaced">
          <NxH2 className="iq-react2shell-about__card-header-title">Steps to Evaluate & Remediate</NxH2>
          <NxTextLink href="https://help.sonatype.com/en/find-and-fix-react2shell.html" external>
            Remediation Guide
          </NxTextLink>
        </NxCard.Header>
        <NxCard.Content className="iq-react2shell-about__card-content--no-padding">
          <ol className="nx-list nx-list--numbered">
            <li className="nx-list__item">
              <strong>Scan for Issues</strong>
              <br />
              Identify React2Shell-affected components. This report updates after each scan.
            </li>
            <li className="nx-list__item">
              <strong>Upgrade to a Fixed Version</strong>
              <br />
              Apply the recommended version or request a waiver if you can&apos;t update immediately.
            </li>
            <li className="nx-list__item">
              <strong>Re-scan to Confirm</strong>
              <br />
              Verify the vulnerability is resolved.
            </li>
          </ol>
        </NxCard.Content>
      </NxCard>
    </div>
  );
}

React2ShellAbout.propTypes = {
  cveIds: PropTypes.arrayOf(PropTypes.string),
};
