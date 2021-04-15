/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import NxButton from '@sonatype/react-shared-components/components/NxButton/NxButton';
import {
  faCaretDown,
  faCaretRight,
  faPlusCircle,
} from '@fortawesome/pro-solid-svg-icons';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';

export default function AdvancedSearchCriteriaBuilder(props) {
  const {
    setCurrentQuery,
    currentQuery,
    showCriteriaBuilder,
    setShowCriteriaBuilder,
    inputFieldId,
  } = props;

  function prefixTagOnClickHandler(prefix) {
    return () => {
      setCurrentQuery(
        currentQuery.trim() +
          (currentQuery.trim() !== '' ? ' ' : '') +
          prefix +
          ':'
      );
      document.getElementById(inputFieldId).focus();
    };
  }

  function prefixTag(prefix) {
    return (
      <span
        key={prefix}
        id={'advanced-search-query-builder-tag-' + prefix}
        className={classnames('iq-tag', {
          ['selected']: currentQuery.indexOf(prefix) !== -1,
        })}
        onClick={prefixTagOnClickHandler(prefix)}
      >
        {prefix}
        <NxFontAwesomeIcon icon={faPlusCircle} />
      </span>
    );
  }

  function queryBuilderGroup(header, ...prefixList) {
    return (
      <div className="iq-adv-search__query-group">
        <h4>{header}</h4>
        {prefixList.map((prefix) => {
          return prefixTag(prefix);
        })}
      </div>
    );
  }

  return (
    <Fragment>
      <div className="nx-form-row">
        <div className="nx-btn-bar">
          <NxButton
            id="advanced-search-query-builder-toggle-button"
            onClick={() => {
              setShowCriteriaBuilder(!showCriteriaBuilder);
            }}
          >
            <NxFontAwesomeIcon
              icon={showCriteriaBuilder ? faCaretDown : faCaretRight}
            />{' '}
            Add Search Terms
          </NxButton>
        </div>
      </div>
      {showCriteriaBuilder && (
        <div
          id="advanced-search-query-builder-container"
          className="iq-adv-search__query-builder"
        >
          {queryBuilderGroup(
            'Organization',
            'organizationId',
            'organizationName'
          )}

          {queryBuilderGroup(
            'Application',
            'applicationId',
            'applicationName',
            'applicationPublicId'
          )}

          {queryBuilderGroup(
            'Application Category',
            'applicationCategoryId',
            'applicationCategoryName',
            'applicationCategoryColor',
            'applicationCategoryDescription'
          )}

          {queryBuilderGroup(
            'Component Label',
            'componentLabelId',
            'componentLabelName',
            'componentLabelColor',
            'componentLabelDescription'
          )}

          {queryBuilderGroup(
            'Policy',
            'policyId',
            'policyName',
            'policyThreatCategory',
            'policyThreatLevel'
          )}

          {queryBuilderGroup(
            'Security Vulnerability',
            'reportId',
            'policyEvaluationStage',
            'componentHash',
            'componentFormat',
            'componentName',
            'componentCoordinateGroupId',
            'componentCoordinateArtifactId',
            'componentCoordinateVersion',
            'componentCoordinateClassifier',
            'componentCoordinateExtension',
            'componentCoordinateName',
            'componentCoordinateQualifier',
            'componentCoordinatePackageId',
            'componentCoordinateArchitecture',
            'componentCoordinatePlatform',
            'vulnerabilityId',
            'vulnerabilityStatus',
            'vulnerabilitySeverity',
            'vulnerabilityDescription'
          )}

          {queryBuilderGroup('Other', 'itemType')}
        </div>
      )}
    </Fragment>
  );
}

AdvancedSearchCriteriaBuilder.propTypes = {
  setCurrentQuery: PropTypes.func.isRequired,
  currentQuery: PropTypes.string.isRequired,
  showCriteriaBuilder: PropTypes.bool.isRequired,
  setShowCriteriaBuilder: PropTypes.func.isRequired,
  inputFieldId: PropTypes.string.isRequired,
};
