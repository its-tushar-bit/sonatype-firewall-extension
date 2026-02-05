/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxThreatIndicator,
  NxTextLink,
  NxDescriptionList,
  NxLoadWrapper,
  NxThreatNumber,
  NxFontAwesomeIcon,
  NxButtonBar,
  NxH3,
  NxInfoAlert,
} from '@sonatype/react-shared-components';
import {
  selectLoading,
  selectError,
  selectComponentDetails,
  retryFetchComponentDetails,
} from '../slices/componentDetailsSlice';
import { faExclamationTriangle, faTrophy } from '@fortawesome/free-solid-svg-icons';
import { formatDate } from 'MainRoot/util/dateUtils';
import { selectComponentProperties, selectCurrentVersionIsSelected } from '../slices/componentsSlice';
import { selectSelectedApplication } from '../slices/applicationsSlice';
import getViewDetailsUrl from '../getViewDetailsUrl';

import './ComponentDetails.scss';

/**
 * Displays detailed information about the selected component version
 */
export default function ComponentDetails() {
  const loading = useSelector(selectLoading);
  const error = useSelector(selectError);
  const componentDetails = useSelector(selectComponentDetails);
  const componentProperties = useSelector(selectComponentProperties);
  const appId = useSelector(selectSelectedApplication)?.publicId;
  const isCurrentVersionSelected = useSelector(selectCurrentVersionIsSelected);
  const dispatch = useDispatch();

  const retry = () => {
    dispatch(retryFetchComponentDetails());
  };

  return (
    <NxLoadWrapper loading={loading} error={error} retryHandler={retry}>
      {() => {
        if (!componentDetails) {
          return null;
        }

        if (componentDetails.matchState === 'unknown') {
          return (
            <section
              className="iq-version-graph-component-details"
              aria-labelledby="component-details-header"
              role="region"
            >
              <NxInfoAlert>
                <h3>Unsupported Component</h3>
                <p>Component information and security data are unavailable.</p>
              </NxInfoAlert>
            </section>
          );
        }

        const { coordinates, format } = componentDetails.componentIdentifier;

        const viewDetailsLink = getViewDetailsUrl(
          appId,
          componentProperties.hash,
          componentDetails.componentIdentifier,
          isCurrentVersionSelected
        );

        // Get highest policy threat
        const highestPolicyThreat = {
          level:
            componentDetails.policyAlerts && componentDetails.policyAlerts.length > 0
              ? componentDetails.policyAlerts[0].trigger.threatLevel
              : null,
          violatedPolicies: componentDetails.policyAlerts ? componentDetails.policyAlerts.length : 0,
        };

        // Get maximum security severity
        const getMaximumSeverity = () => {
          if (!componentDetails.securityVulnerabilities || !componentDetails.securityVulnerabilities.length) {
            return 'N/A';
          }

          const maxSeverity = componentDetails.securityVulnerabilities[0].severity;
          return maxSeverity === null ? 'N/A' : maxSeverity;
        };

        // Extract component name parts
        const getComponentNameParts = () => {
          if (!componentDetails.displayName || !componentDetails.displayName.parts) {
            return [];
          }

          // Filter out parts that should not be displayed
          return componentDetails.displayName.parts.filter(
            (part) => part.field && part.field !== 'version' && part.field !== 'format'
          );
        };

        // Render license list
        const renderLicenses = (licenses) => {
          if (!licenses || !licenses.length) {
            return '-';
          }

          return licenses.map((license, index) => (
            <span className="iq-version-graph-component-details__license" key={license.licenseId || index}>
              {license.licenseName}
              {index < licenses.length - 1 ? ', ' : ''}
            </span>
          ));
        };

        const integrityClass =
          componentDetails.integrityRating?.id === 1
            ? 'iq-version-graph-component-details__suspicious-integrity'
            : null;

        const hygiene =
          componentDetails.hygieneRating?.id === 1
            ? 'exemplar'
            : componentDetails.hygieneRating?.id === 4
            ? 'laggard'
            : null;
        const hygieneClass = hygiene ? `iq-version-graph-component-details__hygiene-icon--${hygiene}` : null;

        return (
          // Note: role should be redundant but RTL fails to implement it by default
          <section
            className="iq-version-graph-component-details"
            aria-labelledby="component-details-header"
            role="region"
          >
            <header>
              <NxH3 id="component-details-header">{`Selected Version ${coordinates.version}`}</NxH3>
            </header>
            <NxDescriptionList className="iq-version-graph-component-details__list">
              <NxDescriptionList.Item id="iq-version-graph-component-details-type">
                <NxDescriptionList.Term>Type</NxDescriptionList.Term>
                <NxDescriptionList.Description>{format}</NxDescriptionList.Description>
              </NxDescriptionList.Item>

              {getComponentNameParts().map((part) => (
                <NxDescriptionList.Item
                  key={part.field}
                  id={`iq-version-graph-component-details-${part.field.toLowerCase()}`}
                >
                  <NxDescriptionList.Term>{part.field}</NxDescriptionList.Term>
                  <NxDescriptionList.Description>{part.value}</NxDescriptionList.Description>
                </NxDescriptionList.Item>
              ))}

              <NxDescriptionList.Item id="iq-version-graph-component-details-declared-license">
                <NxDescriptionList.Term>Declared License</NxDescriptionList.Term>
                <NxDescriptionList.Description>
                  {renderLicenses(componentDetails.declaredLicenses)}
                </NxDescriptionList.Description>
              </NxDescriptionList.Item>
              <NxDescriptionList.Item id="iq-version-graph-component-details-observed-license">
                <NxDescriptionList.Term>Observed License</NxDescriptionList.Term>
                <NxDescriptionList.Description>
                  {renderLicenses(componentDetails.observedLicenses)}
                </NxDescriptionList.Description>
              </NxDescriptionList.Item>
              <NxDescriptionList.Item id="iq-version-graph-component-details-effective-license">
                <NxDescriptionList.Term>Effective License</NxDescriptionList.Term>
                <NxDescriptionList.Description>
                  {renderLicenses(componentDetails.effectiveLicenses)}
                </NxDescriptionList.Description>
              </NxDescriptionList.Item>

              <NxDescriptionList.Item id="iq-version-graph-component-details-highest-policy-threat">
                <NxDescriptionList.Term>Highest Policy Threat</NxDescriptionList.Term>
                <NxDescriptionList.Description>
                  <NxThreatIndicator policyThreatLevel={highestPolicyThreat.level} />
                  {highestPolicyThreat.level !== null ? (
                    <NxThreatNumber>{highestPolicyThreat.level}</NxThreatNumber>
                  ) : (
                    'N/A'
                  )}
                  {highestPolicyThreat.violatedPolicies > 1 && (
                    <span>
                      {' '}
                      within{' '}
                      <span id="iq-version-graph-component-details-policy-count">
                        {highestPolicyThreat.violatedPolicies}
                      </span>{' '}
                      policies
                    </span>
                  )}
                </NxDescriptionList.Description>
              </NxDescriptionList.Item>

              <NxDescriptionList.Item id="iq-version-graph-component-details-highest-cvss">
                <NxDescriptionList.Term>Highest CVSS Score</NxDescriptionList.Term>
                <NxDescriptionList.Description>
                  {getMaximumSeverity()}
                  {componentDetails.securityVulnerabilities && componentDetails.securityVulnerabilities.length > 1 && (
                    <span>
                      {' '}
                      within{' '}
                      <span id="iq-version-graph-component-details-vuln-count">
                        {componentDetails.securityVulnerabilities.length}
                      </span>{' '}
                      security issues
                    </span>
                  )}
                </NxDescriptionList.Description>
              </NxDescriptionList.Item>

              {componentDetails.integrityRating && (
                <NxDescriptionList.Item id="iq-version-graph-component-details-integrity-rating">
                  <NxDescriptionList.Term>Integrity Rating</NxDescriptionList.Term>
                  <NxDescriptionList.Description className={integrityClass}>
                    {componentDetails.integrityRating.label}
                  </NxDescriptionList.Description>
                </NxDescriptionList.Item>
              )}

              {componentDetails.hygieneRating && (
                <NxDescriptionList.Item id="iq-version-graph-component-details-hygiene-rating">
                  <NxDescriptionList.Term>Hygiene Rating</NxDescriptionList.Term>
                  <NxDescriptionList.Description>
                    {hygiene === 'exemplar' && <NxFontAwesomeIcon className={hygieneClass} icon={faTrophy} />}
                    {hygiene === 'laggard' && <NxFontAwesomeIcon icon={faExclamationTriangle} />}
                    <span> {componentDetails.hygieneRating.label}</span>
                  </NxDescriptionList.Description>
                </NxDescriptionList.Item>
              )}

              <NxDescriptionList.Item id="iq-version-graph-component-details-catalog-date">
                <NxDescriptionList.Term>Cataloged</NxDescriptionList.Term>
                <NxDescriptionList.Description>
                  {componentDetails.catalogDate ? formatDate(componentDetails.catalogDate) : '-'}
                </NxDescriptionList.Description>
              </NxDescriptionList.Item>

              <NxDescriptionList.Item id="iq-version-graph-component-details-category">
                <NxDescriptionList.Term>Category</NxDescriptionList.Term>
                <NxDescriptionList.Description>
                  {componentDetails.componentCategories?.map((category, index) => (
                    <span key={index}>
                      {category.path}
                      {index < componentDetails.componentCategories.length - 1 ? ', ' : ''}
                    </span>
                  ))}
                </NxDescriptionList.Description>
              </NxDescriptionList.Item>

              {componentDetails.website && (
                <NxDescriptionList.Item id="iq-version-graph-component-details-website">
                  <NxDescriptionList.Term>Website</NxDescriptionList.Term>
                  <NxDescriptionList.Description>
                    <NxTextLink external href={componentDetails.website} />
                  </NxDescriptionList.Description>
                </NxDescriptionList.Item>
              )}
            </NxDescriptionList>
            <NxButtonBar>
              <a
                id="iq-version-graph-view-details-btn"
                className="nx-btn nx-btn--small"
                target="_blank"
                href={viewDetailsLink}
              >
                View Details
              </a>
            </NxButtonBar>
          </section>
        );
      }}
    </NxLoadWrapper>
  );
}
