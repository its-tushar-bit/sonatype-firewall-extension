/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { useRouterState } from '../../../react/RouterStateContext';
import { NxTable, NxTextLink, NxButton } from '@sonatype/react-shared-components';

export const RiskRemediation = ({ directDependency, ancestors, routeName }) => {
  const uiRouterState = useRouterState();

  const listOfAncestors = () => {
    const emptyListOfAncestors = (
      <ul className="nx-list">
        <li className="nx-list__item nx-list__item--empty">
          <span className="nx-list__text">This list is empty</span>
        </li>
      </ul>
    );

    if (!ancestors.length) {
      return emptyListOfAncestors;
    }

    return (
      <ul className="nx-list">
        {ancestors.map(({ hash, derivedComponentName }) => (
          <li className="nx-list__item" key={hash}>
            <NxTextLink href={uiRouterState.href(routeName, { hash })}> {derivedComponentName} </NxTextLink>
          </li>
        ))}
      </ul>
    );
  };

  const compareVersionsSection = (
    <section className="iq-compare-versions nx-tile">
      <header className="nx-tile-header">
        <h3 className="nx-h3 nx-tile-header__title">Compare Versions TODO</h3>
      </header>
      <div className="nx-tile-content">
        <NxTable>
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell></NxTable.Cell>
              <NxTable.Cell>CURRENT</NxTable.Cell>
              <NxTable.Cell>SELECTED</NxTable.Cell>
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body>
            <NxTable.Row>
              <NxTable.Cell>Version</NxTable.Cell>
              <NxTable.Cell>2.1.2</NxTable.Cell>
              <NxTable.Cell>--</NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row>
              <NxTable.Cell>Highest Policy Threat</NxTable.Cell>
              <NxTable.Cell>10</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row className="iq-compare-versions__secutiry-category">
              <NxTable.Cell>Security Violation Threat</NxTable.Cell>
              <NxTable.Cell>10</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row className="iq-compare-versions__secutiry-category">
              <NxTable.Cell>Highest CVSS Score</NxTable.Cell>
              <NxTable.Cell>9</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row className="iq-compare-versions__legal-category">
              <NxTable.Cell>Legal Violation Threat</NxTable.Cell>
              <NxTable.Cell>10</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row className="iq-compare-versions__legal-category">
              <NxTable.Cell>Effective License</NxTable.Cell>
              <NxTable.Cell>Apache-2.0</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row className="iq-compare-versions__quality-category">
              <NxTable.Cell>Quality Violation Threat</NxTable.Cell>
              <NxTable.Cell>8</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row className="iq-compare-versions__quality-category">
              <NxTable.Cell>Hygiene Rating</NxTable.Cell>
              <NxTable.Cell>Laggard</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row>
              <NxTable.Cell>Other Violation Threat</NxTable.Cell>
              <NxTable.Cell>none</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
          </NxTable.Body>
        </NxTable>
      </div>
    </section>
  );

  const versionExplorerSection = (
    <section className="iq-version-explorer nx-tile">
      <header className="nx-tile-header">
        <h3 className="nx-h3 nx-tile-header__title">Version Explorer</h3>
      </header>
      <div className="nx-tile-content">
        <h3>TODO</h3>
      </div>
    </section>
  );

  const recommendedVersionSection = (
    <section className="iq-recommended-version nx-tile">
      <header className="nx-tile-header">
        <h3 className="nx-h3 nx-tile-header__title">Recommended Versions TODO</h3>
      </header>
      <div className="nx-tile-content">
        <ul className="nx-list">
          <li className="nx-list__item">
            <span className="nx-list__text">Upgrade to X.Y.Z</span>
            <span className="nx-list__subtext">Next version without build violations (It is an example)</span>
            <div className="nx-list__actions">
              <NxButton title="Compare" variant="tertiary">
                Compare
              </NxButton>
            </div>
          </li>
        </ul>
      </div>
    </section>
  );

  const dependencyInformationSection = (
    <section className="iq-dependency-information nx-tile">
      <header className="nx-tile-header">
        <h3 className="nx-h3 nx-tile-header__title">Dependency Information</h3>
      </header>
      <div className="nx-tile-content">
        <p className="nx-p">
          This dependency was brought in by the listed component(s). Clicking the component will take you to the
          associated component detail page
        </p>
        {listOfAncestors()}
      </div>
    </section>
  );

  const overviewComponentRiskRemediationTile_header = (
    <header className="nx-tile-header">
      <div className="nx-tile-header__title">
        <h2 className="nx-h2">Risk Remediation</h2>
      </div>
    </header>
  );

  const overviewComponentRiskRemediationTile_contentDirectDependency = (
    <div className="nx-tile-content">
      <div className="nx-grid-row">
        <div className="nx-grid-col nx-grid-col--50">{recommendedVersionSection}</div>
        <div className="nx-grid-col nx-grid-col--50">
          <div className="nx-grid-row">
            <div className="nx-grid-col iq-grid-col--100">{versionExplorerSection}</div>
          </div>
          <div className="nx-grid-row">
            <div className="nx-grid-col iq-grid-col--100">{compareVersionsSection}</div>
          </div>
        </div>
      </div>
    </div>
  );

  const overviewComponentRiskRemediationTile_contentTransitiveDependency = (
    <div className="nx-tile-content">
      <div className="nx-grid-row">
        <div className="nx-grid-col nx-grid-col--50">{dependencyInformationSection}</div>
        <div className="nx-grid-col nx-grid-col--50">{versionExplorerSection}</div>
      </div>
      <div className="nx-grid-row">
        <div className="nx-grid-col nx-grid-col--50">{recommendedVersionSection}</div>
        <div className="nx-grid-col nx-grid-col--50">{compareVersionsSection}</div>
      </div>
    </div>
  );

  if (directDependency) {
    return (
      <section id="overview-component-risk-remediation-tile" className="nx-tile iq-component-risk-remediation-tile">
        {overviewComponentRiskRemediationTile_header}
        {overviewComponentRiskRemediationTile_contentDirectDependency}
      </section>
    );
  } else {
    return (
      <section id="overview-component-risk-remediation-tile" className="nx-tile iq-component-risk-remediation-tile">
        {overviewComponentRiskRemediationTile_header}
        {overviewComponentRiskRemediationTile_contentTransitiveDependency}
      </section>
    );
  }
};

RiskRemediation.propTypes = {
  directDependency: PropTypes.bool.isRequired,
  routeName: PropTypes.string.isRequired,
  ancestors: PropTypes.arrayOf(
    PropTypes.shape({
      hash: PropTypes.string.isRequired,
      derivedComponentName: PropTypes.string.isRequired,
      componentIdentifier: PropTypes.shape({
        format: PropTypes.string.isRequired,
        coordinates: PropTypes.shape({
          artifactId: PropTypes.string.isRequired,
          classifier: PropTypes.string,
          extension: PropTypes.string.isRequired,
          groupId: PropTypes.string.isRequired,
          version: PropTypes.string.isRequired,
        }),
      }),
    })
  ),
};
