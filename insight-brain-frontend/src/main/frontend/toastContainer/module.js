/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import ToastContainer from 'MainRoot/toastContainer/ToastContainer';

export default angular
  .module('toastContainerModule', ['reduxConfig'])
  .component('toastContainer', iqReact2Angular(ToastContainer, [], ['$ngRedux']));
