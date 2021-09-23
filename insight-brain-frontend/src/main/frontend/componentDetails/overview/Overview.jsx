/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { join } from 'ramda';

import { formatTimeAgoUpToDay } from '../../util/dateUtils';
import { RiskRemediation } from './riskRemediation/RiskRemediation';
import OccurrencesPopoverContainer from './occurrencesPopover/OccurrencesPopoverContainer';
import InnerSourceProducerReportModalContainer from './InnerSourceProducerReportModal/InnerSourceProducerReportModalContainer';
import InnerSourceProducerPermissionsModalContainer from './InnerSourceProducerPermissionsModal/InnerSourceProducerPermissionsModalContainer';
import InnerSourceProducerAlertContainer from './InnerSourceProducerAlert/InnerSourceProducerAlertContainer';
import { RemediationPropTypes, AncestorPropTypes } from './overviewTypes';

export default function Overview(props) {
  const {
    componentInformation,
    ancestors,
    routeName,
    requestVersionGraphData,
    versionExplorerData,
    toggleShowOccurrencesPopover,
    actualVersion,
    stageId,
    remediation,
    loadInnerSourceProducerData,
  } = props;
  const {
    componentIdentifier,
    displayName,
    createTime,
    matchState,
    identificationSource,
    componentCategories = [],
    pathnames = [],
  } = componentInformation;

  useEffect(() => {
    loadInnerSourceProducerData();
  }, []);

  const isUnknown = !matchState || matchState === 'unknown';
  const format = isUnknown ? '' : componentIdentifier.format;
  const catalogedDateAgo = createTime ? formatTimeAgoUpToDay(createTime) : '';
  const version =
    displayName && displayName.parts && displayName.parts.find((part) => part.field === 'Version')
      ? displayName.parts.find((part) => part.field === 'Version').value
      : '';
  const joinedComponentCategories = join(
    ',',
    componentCategories.map((category) => category.path)
  );

  const generalInfoSectionContent = (
    <dl className="nx-read-only nx-read-only--grid iq-general-info-definition-list">
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Type</dt>
        <dd className="nx-read-only__data">{format}</dd>
      </div>

      {displayName.parts.map(
        ({ field, value }) =>
          field && (
            <div className="nx-read-only__item" key={`${field}${value}`}>
              <dt className="nx-read-only__label">{`${field}`}</dt>
              <dd className="nx-read-only__data">{value}</dd>
            </div>
          )
      )}
    </dl>
  );

  const identificationInfoSectionContent = (
    <dl className="nx-read-only nx-read-only--grid iq-identification-info-definition-list">
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Cataloged</dt>
        <dd className="nx-read-only__data">{catalogedDateAgo}</dd>
      </div>
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Match State</dt>
        <dd className="nx-read-only__data">{matchState}</dd>
      </div>
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Occurrences</dt>
        <dd className="nx-read-only__data">
          <a
            className="iq-identification-info-definition-list__occurrences-link"
            onClick={toggleShowOccurrencesPopover}
          >
            {pathnames.length} File Matches
          </a>
        </dd>
      </div>
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Identification Source</dt>
        <dd className="nx-read-only__data">{identificationSource}</dd>
      </div>
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Category</dt>
        <dd className="nx-read-only__data">{joinedComponentCategories}</dd>
      </div>
    </dl>
  );

  const overviewComponentInformationTile = (
    <section id="overview-component-information-tile" className="nx-tile iq-component-information-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Component Information</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <div className="nx-grid-row">
          <section className="nx-grid-col iq-component-data-col">
            <header className="nx-grid-header">
              <h3 className="nx-h3 nx-grid-header__title">General Info</h3>
            </header>
            {generalInfoSectionContent}
          </section>
          <section className="nx-grid-col iq-component-data-col">
            <header className="nx-grid-header">
              <h3 className="nx-h3 nx-grid-header__title">Identification Info</h3>
            </header>
            {identificationInfoSectionContent}
          </section>
        </div>
      </div>
    </section>
  );

  if (isUnknown) {
    return <div>{overviewComponentInformationTile}</div>;
  }

  return (
    <div>
      <OccurrencesPopoverContainer occurrences={pathnames} />
      <InnerSourceProducerReportModalContainer />
      <InnerSourceProducerPermissionsModalContainer />
      <InnerSourceProducerAlertContainer />
      {overviewComponentInformationTile}
      <RiskRemediation
        ancestors={ancestors}
        routeName={routeName}
        actualVersion={actualVersion}
        stageId={stageId}
        remediation={remediation}
        requestVersionGraphData={requestVersionGraphData}
        versionExplorerData={{
          ...versionExplorerData,
          data: {
            ...versionExplorerData.data,
            version,
          },
        }}
      />
    </div>
  );
}

Overview.propTypes = {
  componentInformation: PropTypes.shape({
    componentIdentifier: PropTypes.shape({
      format: PropTypes.string,
    }),
    displayName: PropTypes.shape({
      parts: PropTypes.array,
    }).isRequired,
    createTime: PropTypes.number,
    matchState: PropTypes.string.isRequired,
    identificationSource: PropTypes.string,
    componentCategories: PropTypes.arrayOf(
      PropTypes.shape({
        path: PropTypes.string.isRequired,
      })
    ),
    pathnames: PropTypes.arrayOf(PropTypes.string).isRequired,
    dependencyInfo: PropTypes.shape({
      isDirectDependency: PropTypes.bool.isRequired,
    }),
  }),
  routeName: PropTypes.string.isRequired,
  ancestors: PropTypes.arrayOf(AncestorPropTypes),
  actualVersion: PropTypes.string.isRequired,
  stageId: PropTypes.string.isRequired,
  remediation: RemediationPropTypes,
  requestVersionGraphData: PropTypes.func,
  versionExplorerData: PropTypes.shape({
    data: PropTypes.shape({
      version: PropTypes.string,
      versions: PropTypes.array,
    }),
    loading: PropTypes.bool,
    loadError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  }),
  toggleShowOccurrencesPopover: PropTypes.func.isRequired,
  loadInnerSourceProducerData: PropTypes.func.isRequired,
};
