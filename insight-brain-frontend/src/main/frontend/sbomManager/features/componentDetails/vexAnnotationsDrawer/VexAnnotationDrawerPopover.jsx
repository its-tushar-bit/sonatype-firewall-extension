/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import PropTypes from 'prop-types';
import { isNil } from 'ramda';
import VexAnnotationPopoverHeader from 'MainRoot/sbomManager/features/componentDetails/vexAnnotationsDrawer/VexAnnotationPopoverHeader';
import VexAnnotationDrawer from 'MainRoot/sbomManager/features/componentDetails/vexAnnotationsDrawer/VexAnnotationDrawer';
import { IqPopover } from 'MainRoot/react/IqPopover';
import { NxLoadWrapper } from '@sonatype/react-shared-components';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

import './_vexAnnotationDrawerPopover.scss';

export default function VexAnnotationDrawerPopover(props) {
  const {
    // data
    showVexAnnotationFormPopover,
    // isRowAnnotated prop for VexAnnotationDrawer could be provided or not inside this object
    vulnerabilityRowObject,
    onClose,
    componentPurl,
    componentHash,
    internalAppId,
    sbomVersion,
    responsesOptions,
    analysisStatusesOptions,
    justificationsOptions,
    isVulnerabilityReferenceDataLoading,
    errorLoadingAnalysisReferenceData,
    // functions
    loadVexReferenceData,
    reloadComponentDetails,
    openVulnerabilityDetailsModal,
  } = props;

  if (!showVexAnnotationFormPopover || isNilOrEmpty(vulnerabilityRowObject)) {
    return null;
  }

  const isDropdownsReferenceDataReady =
    !isNilOrEmpty(responsesOptions) && !isNilOrEmpty(justificationsOptions) && !isNilOrEmpty(analysisStatusesOptions);

  const errorDropdownsContentEmpty = isDropdownsReferenceDataReady ? null : 'Please retry.';
  const popOverContentError = !isNil(errorLoadingAnalysisReferenceData)
    ? errorLoadingAnalysisReferenceData
    : errorDropdownsContentEmpty;

  const preSaveMaskActions = () => {
    reloadComponentDetails();
  };

  const postSaveMaskActions = () => {
    onClose();
  };

  const onLearnMoreClick = () => {
    onClose();
    openVulnerabilityDetailsModal(vulnerabilityRowObject);
  };

  return (
    <IqPopover size="medium" id="vex-annotation-popover">
      <VexAnnotationPopoverHeader
        headerTitle={`Annotate ${vulnerabilityRowObject.issue}`}
        headerSize={'h2'}
        onClose={onClose}
        className={'vex-annotation-popover__header'}
        componentPurl={componentPurl}
      ></VexAnnotationPopoverHeader>
      <NxLoadWrapper
        retryHandler={loadVexReferenceData}
        loading={isVulnerabilityReferenceDataLoading}
        error={popOverContentError}
      >
        <VexAnnotationDrawer
          {...vulnerabilityRowObject}
          componentHash={componentHash}
          internalAppId={internalAppId}
          sbomVersion={sbomVersion}
          responsesOptions={responsesOptions}
          analysisStatusesOptions={analysisStatusesOptions}
          justificationsOptions={justificationsOptions}
          preSaveMaskActions={preSaveMaskActions}
          onLearnMoreClick={onLearnMoreClick}
          postSaveMaskActions={postSaveMaskActions}
        ></VexAnnotationDrawer>
      </NxLoadWrapper>
    </IqPopover>
  );
}

VexAnnotationDrawerPopover.propTypes = {
  showVexAnnotationFormPopover: PropTypes.bool.isRequired,
  vulnerabilityRowObject: PropTypes.object,
  onClose: PropTypes.func,
  componentPurl: PropTypes.string,
  componentHash: PropTypes.string,
  internalAppId: PropTypes.string,
  sbomVersion: PropTypes.string,
  responsesOptions: PropTypes.array.isRequired,
  analysisStatusesOptions: PropTypes.array.isRequired,
  justificationsOptions: PropTypes.array.isRequired,
  isVulnerabilityReferenceDataLoading: PropTypes.bool,
  errorLoadingAnalysisReferenceData: PropTypes.string,
  loadVexReferenceData: PropTypes.func,
  reloadComponentDetails: PropTypes.func,
  openVulnerabilityDetailsModal: PropTypes.func,
};
