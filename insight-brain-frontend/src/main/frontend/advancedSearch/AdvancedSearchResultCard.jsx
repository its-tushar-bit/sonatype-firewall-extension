/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as PropTypes from 'prop-types';
import React from 'react';
import {
  faCogs,
  faExclamationTriangle,
  faFile,
  faSitemap,
  faTerminal,
  faText,
  faUniversity,
} from '@fortawesome/pro-regular-svg-icons';
import { faHexagon, faTag } from '@fortawesome/pro-solid-svg-icons';
import { NxFontAwesomeIcon, NxThreatIndicator } from '@sonatype/react-shared-components';

export default function AdvancedSearchResultCard({ searchResultItem, groupIdentifier, isSbomManager, $state }) {
  // The following constants are conditions whether this particular row should be presented in result card or not
  const organizationName =
      searchResultItem.organizationName &&
      (searchResultItem.itemType === 'ORGANIZATION' || groupIdentifier !== 'ORGANIZATION_NAME'),
    applicationName =
      searchResultItem.applicationName &&
      (searchResultItem.itemType === 'APPLICATION' || groupIdentifier !== 'APPLICATION_NAME'),
    applicationVersion = searchResultItem.applicationVersion,
    applicationCategory = searchResultItem.applicationCategoryName,
    componentName = searchResultItem.componentName && groupIdentifier !== 'COMPONENT_NAME',
    componentLabel = searchResultItem.componentLabelId,
    report = searchResultItem.policyEvaluationStage,
    securityIssue = searchResultItem.vulnerabilityId && groupIdentifier !== 'VULNERABILITY_ID',
    vulnerabilityDescription = searchResultItem.vulnerabilityId && groupIdentifier !== 'VULNERABILITY_ID',
    policy =
      searchResultItem.policyId &&
      searchResultItem.policyName &&
      searchResultItem.policyThreatLevel !== undefined &&
      searchResultItem.policyThreatCategory;

  // generator functions for the various links that can appear in the results
  const getOrgHref = () =>
      $state.href($state.get(`${isSbomManager ? 'sbomManager.' : ''}management.view.organization`), {
        organizationId: searchResultItem.organizationId,
      }),
    getAppHref = () =>
      $state.href($state.get(`${isSbomManager ? 'sbomManager.' : ''}management.view.application`), {
        applicationPublicId: searchResultItem.applicationPublicId,
      }),
    getCategoryHref = () =>
      $state.href($state.get('management.edit.organization.category'), {
        organizationId: searchResultItem.organizationId,
        categoryId: searchResultItem.applicationCategoryId,
      }),
    getOrgLabelHref = () =>
      $state.href($state.get('management.edit.organization.label'), {
        organizationId: searchResultItem.organizationId,
        labelId: searchResultItem.componentLabelId,
      }),
    getAppLabelHref = () =>
      $state.href($state.get('management.edit.application.label'), {
        applicationPublicId: searchResultItem.applicationPublicId,
        labelId: searchResultItem.componentLabelId,
      }),
    getReportHref = () =>
      $state.href($state.get('applicationReport.policy'), {
        publicId: searchResultItem.applicationPublicId,
        scanId: searchResultItem.reportId,
      }),
    getVulnHref = () =>
      $state.href($state.get('vulnerabilitySearchDetail'), {
        id: searchResultItem.vulnerabilityId,
      }),
    getOrgPolicyHref = () =>
      $state.href($state.get('management.edit.organization.policy'), {
        organizationId: searchResultItem.organizationId,
        policyId: searchResultItem.policyId,
      }),
    getAppPolicyHref = () =>
      $state.href($state.get('management.edit.application.policy'), {
        applicationPublicId: searchResultItem.applicationPublicId,
        policyId: searchResultItem.policyId,
      });

  return (
    <table className="nx-table nx-table--fixed-layout nx-table--advanced-search">
      <tbody>
        {organizationName && (
          <tr className="nx-table-row">
            <td className="nx-cell">
              <NxFontAwesomeIcon icon={faSitemap} />
            </td>
            <td className="nx-cell">Organization</td>
            <td className="nx-cell">
              <a href={getOrgHref()}>{searchResultItem.organizationName}</a>
            </td>
          </tr>
        )}

        {applicationName && (
          <tr className="nx-table-row">
            <td className="nx-cell">
              <NxFontAwesomeIcon icon={faTerminal} />
            </td>
            <td className="nx-cell">Application</td>
            <td className="nx-cell">
              <a href={getAppHref()}>{searchResultItem.applicationName}</a>
            </td>
          </tr>
        )}

        {applicationVersion && (
          <tr className="nx-table-row">
            <td className="nx-cell">
              {/* TODO use correct icon */}
              <NxFontAwesomeIcon icon={faTerminal} />
            </td>
            <td className="nx-cell">Application Version</td>
            <td className="nx-cell">{searchResultItem.applicationVersion}</td>
          </tr>
        )}

        {applicationCategory && (
          <tr className="nx-table-row">
            <td className="nx-cell">
              <NxFontAwesomeIcon icon={faHexagon} className={searchResultItem.applicationCategoryColor} />
            </td>
            <td className="nx-cell">Application Category</td>
            <td className="nx-cell">
              <a href={getCategoryHref()}>{searchResultItem.applicationCategoryName}</a>
              {searchResultItem.applicationCategoryDescription &&
                ` - ${searchResultItem.applicationCategoryDescription}`}
            </td>
          </tr>
        )}

        {componentName && (
          <tr className="nx-table-row">
            <td className="nx-cell">
              <NxFontAwesomeIcon icon={faCogs} />
            </td>
            <td className="nx-cell">Component Name</td>
            <td className="nx-cell">{searchResultItem.componentName}</td>
          </tr>
        )}

        {componentLabel && (
          <tr className="nx-table-row">
            <td className="nx-cell">
              <NxFontAwesomeIcon icon={faTag} className={searchResultItem.componentLabelColor} />
            </td>
            <td className="nx-cell">Component Label</td>
            <td className="nx-cell">
              {searchResultItem.organizationId && <a href={getOrgLabelHref()}>{searchResultItem.componentLabelName}</a>}
              {searchResultItem.applicationPublicId && (
                <a href={getAppLabelHref()}>{searchResultItem.componentLabelName}</a>
              )}
              {searchResultItem.componentLabelDescription && ` - ${searchResultItem.componentLabelDescription}`}
            </td>
          </tr>
        )}

        {report && (
          <tr className="nx-table-row">
            <td className="nx-cell">
              <NxFontAwesomeIcon icon={faFile} />
            </td>
            <td className="nx-cell">Report</td>
            <td className="nx-cell">
              <a href={getReportHref()}>{searchResultItem.policyEvaluationStage}</a>
            </td>
          </tr>
        )}

        {securityIssue && (
          <tr className="nx-table-row">
            <td className="nx-cell">
              <NxFontAwesomeIcon icon={faExclamationTriangle} />
            </td>
            <td className="nx-cell">Security Issue</td>
            <td className="nx-cell">
              <a href={getVulnHref()}>{searchResultItem.vulnerabilityId}</a>
            </td>
          </tr>
        )}

        {vulnerabilityDescription && (
          <tr className="nx-table-row">
            <td className="nx-cell">
              <NxFontAwesomeIcon icon={faText} />
            </td>
            <td className="nx-cell">Vulnerability Description</td>
            <td className="nx-cell">{searchResultItem.vulnerabilityDescription}</td>
          </tr>
        )}

        {policy && (
          <tr className="nx-table-row">
            <td className="nx-cell">
              <NxFontAwesomeIcon icon={faUniversity} />
            </td>
            <td className="nx-cell">Policy</td>
            <td className="nx-cell">
              <NxThreatIndicator policyThreatLevel={searchResultItem.policyThreatLevel} />
              {searchResultItem.policyThreatLevel} - {searchResultItem.policyThreatCategory} -{' '}
              {searchResultItem.organizationId && <a href={getOrgPolicyHref()}>{searchResultItem.policyName}</a>}
              {searchResultItem.applicationPublicId && <a href={getAppPolicyHref()}>{searchResultItem.policyName}</a>}
            </td>
          </tr>
        )}
      </tbody>
    </table>
  );
}

AdvancedSearchResultCard.propTypes = {
  searchResultItem: PropTypes.object,
  isSbomManager: PropTypes.bool,
  groupIdentifier: PropTypes.string,
  $state: PropTypes.object.isRequired,
};
