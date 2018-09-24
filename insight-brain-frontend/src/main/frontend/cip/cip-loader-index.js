import 'angular';
import 'angular-route';
import 'angular-ui-bootstrap/ui-bootstrap-tpls';

import './cip-index';

import './proprietary.matchers.modal/proprietary.matchers.module';

import './cip-loader';
import './cip-component-util';
import './ci-label-tab';
import './ci-policy-violations-tab';
import './ci-license-tab';
import './ci-vulnerability-tab';
import './ci-version-graph';

import Base64 from '../lib/Base64';
window.Base64 = Base64;
