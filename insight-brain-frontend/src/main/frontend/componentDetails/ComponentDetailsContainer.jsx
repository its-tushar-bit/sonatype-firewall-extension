/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { path, pick } from 'ramda';

import ComponentDetails from './ComponentDetails';
import { loadReportAndSelectComponentByHash } from '../applicationReport/applicationReportActions';
import { stateGo } from '../reduxUiRouter/routerActions';

const formatFromComponent = path(['componentIdentifier', 'format']);
const reportMetaData = pick(['reportTime', 'reportTitle']);

const deriveComponentDetails = (component, metadata) => ({
  name: component.derivedComponentName,
  dependencyType: component.derivedDependencyType,
  isInnerSource: component.innerSource || !!component.innerSourceData,
  format: formatFromComponent(component),
  applicationName: metadata.application.name,
  organizationName: metadata.application.organization.name,
  ...reportMetaData(metadata),
});

function mapStateToProps(state) {
  const {
    router: {
      currentParams: { hash, publicId, scanId, unknownjs, tabId },
    },
    applicationReport: { selectedComponent, metadata },
  } = state;

  let componentDetails = null;
  if (selectedComponent && metadata) {
    componentDetails = {
      ...deriveComponentDetails(selectedComponent, metadata),
      labels: [], //TODO: load labels when selecting component Jira: CLM-18516
    };
  }

  return { hash, publicId, scanId, unknownjs, tabId, componentDetails };
}

const mapDispatchToProps = { loadReportAndSelectComponentByHash, stateGo };

const ComponentDetailsContainer = connect(mapStateToProps, mapDispatchToProps)(ComponentDetails);
export default ComponentDetailsContainer;
