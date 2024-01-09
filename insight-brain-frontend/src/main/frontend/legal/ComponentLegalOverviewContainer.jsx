/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';
import ComponentLegalOverviewPage from './ComponentLegalOverviewPage';
import { loadAvailableScopes, loadComponent, loadComponentByComponentIdentifier } from './advancedLegalActions';
import * as copyrightOverrideFormActions from './copyright/copyrightOverrideFormActions';
import * as originalSourcesFormActions from './originalSources/originalSourcesFormActions';
import { setShowLicenseFilesModal, setShowNoticesModal, setShowLicensesModal } from './files/advancedLegalFileActions';
import { path } from 'ramda';

function mapStateToProps({ advancedLegal, router, copyrightOverrides, originalSourcesForm }) {
  let component = advancedLegal.component;
  let availableScopes = advancedLegal.availableScopes;
  return {
    loading: component.loading || availableScopes.loading,
    error: component.error || availableScopes.error,
    availableScopes: availableScopes,
    ...pick(['component', 'licenseLegalMetadata'], component),
    obligations: component.component ? component.component.licenseLegalData.obligations : null,
    showNoticesModal: component.component ? component.component.licenseLegalData.showNoticesModal : false,
    showLicenseFilesModal: component.component ? component.component.licenseLegalData.showLicenseFilesModal : false,
    showLicensesModal: component?.component?.licenseLegalData?.showLicensesModal || false,
    noticeFiles: component.component ? component.component.licenseLegalData.noticeFiles : null,
    licenseFiles: component.component ? component.component.licenseLegalData.licenseFiles : null,
    sourceLinks: component.component ? component.component.licenseLegalData.sourceLinks : null,
    ...pick(
      [
        'hash',
        'organizationId',
        'applicationPublicId',
        'stageTypeId',
        'componentIdentifier',
        'repositoryId',
        'scanId',
        'tabId',
      ],
      router.currentParams
    ),
    ...pick(['prevState', 'prevParams'], router),
    ...pick(['showEditCopyrightOverrideModal'], copyrightOverrides),
    ...pick(['showOriginalSourcesModal'], originalSourcesForm),
    ecosystem: path(['component', 'componentIdentifier', 'format'], component),
  };
}

const mapDispatchToProps = {
  loadComponent,
  loadComponentByComponentIdentifier,
  loadAvailableScopes,
  setShowNoticesModal,
  setShowLicenseFilesModal,
  setShowLicensesModal,
  ...copyrightOverrideFormActions,
  ...originalSourcesFormActions,
};

const ComponentLegalOverviewContainer = connect(mapStateToProps, mapDispatchToProps)(ComponentLegalOverviewPage);
export default ComponentLegalOverviewContainer;
