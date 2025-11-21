/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import ModalContainer from './ModalContainer';

export default angular
  .module('modalContainerModule', [])
  .component('modalContainer', iqReact2Angular(ModalContainer, [], []));
