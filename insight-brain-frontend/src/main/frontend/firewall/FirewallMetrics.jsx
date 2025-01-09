/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';
import { NxTile, NxTextLink, NxGrid, NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';

import { useRouterState } from 'MainRoot/react/RouterStateContext';

const FirewallMetricsContent = (props) => {
  const { id, title, tooltipTitle, value, valueLabel, link, linkLabel, isExternalLink, onLinkButtonClick } = props;

  const linkOrButtonLink = () =>
    typeof onLinkButtonClick === 'function' ? (
      <button className="nx-text-link" onClick={onLinkButtonClick}>
        {linkLabel}
      </button>
    ) : (
      <NxTextLink href={link} external={isExternalLink}>
        {linkLabel}
      </NxTextLink>
    );

  return (
    <div className="iq-firewall-metrics-content" id={id}>
      <header className="iq-firewall-metrics-content__header">
        <h3 className="iq-firewall-metrics-content__title">
          <span>{title}</span>
          <NxTooltip title={tooltipTitle}>
            <NxFontAwesomeIcon className="iq-firewall-metrics-content__icon" icon={faInfoCircle} />
          </NxTooltip>
        </h3>
      </header>
      <div className="iq-firewall-metrics-content__values">
        <span>{value.toLocaleString('en-US')}</span>
        {valueLabel}
      </div>
      <footer>{linkOrButtonLink()}</footer>
    </div>
  );
};

FirewallMetricsContent.propTypes = {
  id: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
  tooltipTitle: PropTypes.string.isRequired,
  value: PropTypes.number.isRequired,
  valueLabel: PropTypes.string.isRequired,
  link: PropTypes.string.isRequired,
  linkLabel: PropTypes.string.isRequired,
  onLinkButtonClick: PropTypes.func,
  isExternalLink: PropTypes.bool,
};

export default function FirewallMetrics(props) {
  const {
    supplyChainAttacksBlocked,
    namespaceAttacksBlocked,
    componentsQuarantined,
    componentsAutoReleased,
    saferVersionsSelectedAutomatically,
    waivedComponents,
    onSupplyChainAttacksBlockedLinkClick,
    onNamespaceAttacksBlockedLinkClick,
    onComponentsQuarantinedLinkClick,
    onViewWaivedComponentsClick,
  } = props;

  const uiRouterState = useRouterState();
  const componentsAutoReleasedLink = uiRouterState.href('firewall.firewallAutoUnquarantinePage');

  return (
    <NxTile id="firewall-metrics" className="nx-grid iq-firewall-metrics">
      <NxGrid.Row className="iq-firewall-metrics__row">
        <NxGrid.Column className="nx-grid-col--33 iq-firewall-metrics-column">
          <FirewallMetricsContent
            id="firewall-metrics-content-supply-chain-attacks-blocked"
            title="Supply chain attacks blocked"
            tooltipTitle={`Firewall has detected these violations for "security-malicious" policy condition and is blocking malicious components.`}
            value={supplyChainAttacksBlocked}
            valueLabel="(all time)"
            link="#"
            linkLabel="See details below"
            onLinkButtonClick={onSupplyChainAttacksBlockedLinkClick}
          />
          <FirewallMetricsContent
            id="firewall-metrics-content-namespace-attacks-blocked"
            title="Namespace attacks blocked"
            tooltipTitle={`Firewall has detected these violations for "namespace-conflict" policy condition and is blocking components with namespace conflict.`}
            value={namespaceAttacksBlocked}
            valueLabel="(all time)"
            link="#"
            linkLabel="See details below"
            onLinkButtonClick={onNamespaceAttacksBlockedLinkClick}
          />
        </NxGrid.Column>
        <NxGrid.Column className="nx-grid-col--33 iq-firewall-metrics-column">
          <FirewallMetricsContent
            id="firewall-metrics-content-components-quarantined"
            title="Components quarantined"
            tooltipTitle="Firewall has detected and blocked these components that violate your governance policies."
            value={componentsQuarantined}
            valueLabel="Last 12 months"
            link="#"
            linkLabel="See details below"
            onLinkButtonClick={onComponentsQuarantinedLinkClick}
          />
          <FirewallMetricsContent
            id="firewall-metrics-content-components-auto-released"
            title="Components auto-released"
            tooltipTitle="Firewall has cleared these previously blocked components for use."
            value={componentsAutoReleased}
            valueLabel="Last 12 months"
            link={componentsAutoReleasedLink}
            linkLabel="View auto-released components"
          />
        </NxGrid.Column>
        <NxGrid.Column className="nx-grid-col--33 iq-firewall-metrics-column">
          <FirewallMetricsContent
            id="firewall-metrics-content-safe-components-auto-selected"
            title="Safe components auto-selected"
            tooltipTitle="Firewall auto-selected these policy compliant components found when installing dependencies."
            value={saferVersionsSelectedAutomatically}
            valueLabel="Last 12 months"
            link="https://links.sonatype.com/nexus-firewall/policy-compliant-component-selection"
            linkLabel="Learn more"
            isExternalLink={true}
          />
          <FirewallMetricsContent
            id="firewall-metrics-content-components-waived"
            title="Components waived"
            tooltipTitle="Firewall has waived the failing policy violations for these components."
            value={waivedComponents}
            valueLabel="Last 12 months"
            link="#"
            linkLabel="View waived components"
            onLinkButtonClick={onViewWaivedComponentsClick}
          />
        </NxGrid.Column>
      </NxGrid.Row>
    </NxTile>
  );
}

FirewallMetrics.propTypes = {
  supplyChainAttacksBlocked: PropTypes.number.isRequired,
  namespaceAttacksBlocked: PropTypes.number.isRequired,

  componentsQuarantined: PropTypes.number.isRequired,
  componentsAutoReleased: PropTypes.number.isRequired,

  saferVersionsSelectedAutomatically: PropTypes.number.isRequired,
  waivedComponents: PropTypes.number.isRequired,

  onSupplyChainAttacksBlockedLinkClick: PropTypes.func.isRequired,
  onNamespaceAttacksBlockedLinkClick: PropTypes.func.isRequired,
  onComponentsQuarantinedLinkClick: PropTypes.func.isRequired,

  onViewWaivedComponentsClick: PropTypes.func.isRequired,
};
