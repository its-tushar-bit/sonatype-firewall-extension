/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { NxButton, NxFontAwesomeIcon, NxSelectableTag } from '@sonatype/react-shared-components';
import { faCaretDown, faCaretRight } from '@fortawesome/pro-solid-svg-icons';
import * as PropTypes from 'prop-types';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useSelector } from 'react-redux';
import { is } from 'ramda';

export default function AdvancedSearchCriteriaBuilder(props) {
  const { setCurrentQuery, currentQuery, showCriteriaBuilder, setShowCriteriaBuilder, inputFieldId } = props;
  const isSbomManager = useSelector(selectIsSbomManager);

  function prefixTagOnClickHandler(prefix) {
    return () => {
      setCurrentQuery(currentQuery.trim() + (currentQuery.trim() !== '' ? ' ' : '') + prefix + ':');
      document.getElementById(inputFieldId).focus();
    };
  }

  function prefixTag(prefix) {
    return (
      <NxSelectableTag
        key={prefix}
        id={'advanced-search-query-builder-tag-' + prefix}
        onSelect={prefixTagOnClickHandler(prefix)}
        selected={currentQuery.indexOf(prefix) !== -1}
      >
        {prefix}
      </NxSelectableTag>
    );
  }

  function queryBuilderGroup(header, ...prefixList) {
    const headerIdString = header.replace(/\s+/g, '-').toLowerCase();
    return (
      <div
        className="iq-adv-search__query-group"
        aria-labelledby={`iq-adv-search__query-group${headerIdString}`}
        role="group"
      >
        <h4 id={'iq-adv-search__query-group' + headerIdString}>{header}</h4>
        {prefixList.filter(is(String)).map((prefix) => prefixTag(prefix))}
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
            <NxFontAwesomeIcon icon={showCriteriaBuilder ? faCaretDown : faCaretRight} /> Add Search Terms
          </NxButton>
        </div>
      </div>
      {showCriteriaBuilder && (
        <div
          id="advanced-search-query-builder-container"
          className="iq-adv-search__query-builder"
          aria-label="search term tags"
        >
          {queryBuilderGroup('Organization', 'organizationId', 'organizationName')}

          {queryBuilderGroup(
            'Application',
            'applicationId',
            'applicationName',
            !isSbomManager && 'applicationPublicId',
            isSbomManager && 'applicationVersion',
            isSbomManager && 'sbomSpecification'
          )}

          {!isSbomManager &&
            queryBuilderGroup(
              'Application Category',
              'applicationCategoryId',
              'applicationCategoryName',
              'applicationCategoryColor',
              'applicationCategoryDescription'
            )}

          {queryBuilderGroup(
            'Component',
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
            'componentCoordinatePlatform'
          )}

          {!isSbomManager &&
            queryBuilderGroup(
              'Component Label',
              'componentLabelId',
              'componentLabelName',
              'componentLabelColor',
              'componentLabelDescription'
            )}

          {!isSbomManager &&
            queryBuilderGroup('Policy', 'policyId', 'policyName', 'policyThreatCategory', 'policyThreatLevel')}

          {queryBuilderGroup(
            'Security Vulnerability',
            !isSbomManager && 'reportId',
            !isSbomManager && 'policyEvaluationStage',
            'vulnerabilityId',
            !isSbomManager && 'vulnerabilityStatus',
            'vulnerabilitySeverity',
            'vulnerabilityDescription'
          )}

          {!isSbomManager && queryBuilderGroup('Other', 'itemType')}
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
