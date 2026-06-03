/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxTextLink, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faLock } from '@fortawesome/pro-regular-svg-icons';
import './EnterpriseUpgradeBanner.scss';

const ENTERPRISE_DEMO_URL = 'https://www.sonatype.com/products/request-demo';
const UPGRADE_URL = 'https://www.sonatype.com/contact-us';

export default function EnterpriseUpgradeBanner({ featureName = 'policy', hideTitle = false }) {
  const featureContent = {
    policy: {
      title: (
        <>
          You&apos;re exploring <strong>Enterprise custom policy management</strong>.
        </>
      ),
      description:
        "Define policies that match your organization's risk tolerance and enforce consistent standards at scale.",
    },
    'application category': {
      title: (
        <>
          You&apos;re exploring <strong>Enterprise application categories</strong>.
        </>
      ),
      description:
        'Create and organize custom applications categories based on your business context for better reporting and policy targeting.',
    },
    'component label': {
      title: (
        <>
          You&apos;re exploring <strong>Enterprise custom component labels</strong>.
        </>
      ),
      description:
        'Go beyond default labels by creating and tagging components to prioritize remediation and align with internal risk and compliance workflows.',
    },
    'license threat group': {
      title: (
        <>
          You&apos;re exploring <strong>Enterprise custom license threat groups</strong>.
        </>
      ),
      description:
        "Define license risk based on your organization's legal policies to improve accuracy and reduce manual review.",
    },
    'auto-waiver': {
      title: (
        <>
          You&apos;re exploring <strong>Enterprise auto-waivers</strong>.
        </>
      ),
      description:
        'Automatically apply waivers to low-risk, non-reachable or known issues so teams can stay unblocked while maintaining control over risk and policy enforcement.',
    },
    'waiver-request': {
      title: (
        <>
          You&apos;re exploring <strong>Enterprise waiver requests</strong>.
        </>
      ),
      description:
        'Enable teams to request waivers directly, reducing admin workload and streamlining approvals without disrupting development.',
    },
    'bulk-waiver': {
      title: (
        <>
          You&apos;re exploring <strong>Enterprise bulk waivers</strong>.
        </>
      ),
      description:
        'Waive multiple policy violations for a component or an application at once—reducing manual effort and speed up remediation workflows.',
    },
    'golden-pr': {
      title: 'You have 1 Golden PR remaining this month.',
      description:
        'Lifecycle Pro includes a limited number of automated fixes. Upgrade to Enterprise for unlimited remediation and faster risk reduction.',
    },
  };

  const content = featureContent[featureName] || featureContent.policy;

  return (
    <div
      className="iq-policy-editor__enterprise-banner"
      role="status"
      aria-live="polite"
      aria-label="Enterprise feature preview notification"
    >
      <div className="iq-policy-editor__enterprise-banner__icon">
        <NxFontAwesomeIcon icon={faLock} />
      </div>
      <div className="iq-policy-editor__enterprise-banner__content">
        <div className="iq-policy-editor__enterprise-message" id="enterprise-banner-message">
          {!hideTitle && <div>{content.title}</div>}
          <div>{content.description}</div>
          <div>
            {featureName === 'golden-pr' ? (
              <>
                <NxTextLink href={UPGRADE_URL} external aria-label="Get unlimited Golden PRs (opens in new window)">
                  Get unlimited Golden PRs
                </NxTextLink>{' '}
                <NxTextLink href={UPGRADE_URL} external aria-label="Upgrade to Enterprise (opens in new window)">
                  Upgrade to Enterprise
                </NxTextLink>
              </>
            ) : (
              <>
                <NxTextLink href={ENTERPRISE_DEMO_URL} external aria-label="Request Demo (opens in new window)">
                  Request Demo
                </NxTextLink>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

EnterpriseUpgradeBanner.propTypes = {
  featureName: PropTypes.oneOf([
    'policy',
    'application category',
    'component label',
    'license threat group',
    'auto-waiver',
    'waiver-request',
    'bulk-waiver',
    'golden-pr',
  ]),
  hideTitle: PropTypes.bool,
};
