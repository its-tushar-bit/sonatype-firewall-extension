#!/usr/bin/env python3.7

#  Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
#  Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
#  "Sonatype" is a trademark of Sonatype, Inc.

import logging
import unittest
import re
from datetime import datetime
from results_reader import read_base_endpoint_response, read_endpoint_response_times, \
    extract_es_parameters, create_es_bulk_data, BaseEndpointResponse, EndpointResponse

logger = logging.getLogger(__name__)


class ResultsReaderTest(unittest.TestCase):

    release_pattern = "release-(\\d+\\.\\d+\\.\\d+-\\d+)-(PostgreSQL|H2)-(small|medium|large).out"
    nightly_pattern = "nightly-(\\d+\\-\\d+\\-\\d+)-(small|medium|large).out"

    def test_readBaseEndpointResponse_releaseAPresent_returnsReleaseAResult(self):
        release = "release-1.90.0-01-PostgreSQL-small.out"
        date_time = datetime.utcnow()
        result = read_base_endpoint_response(release, date_time=date_time)
        self.assertEqual(result.type, "release")
        self.assertEqual(result.version, "1.90.0-01")
        self.assertEqual(result.database, "PostgreSQL")
        self.assertEqual(result.dataset_size, "small")
        self.assertEqual(result.date_time, date_time)

    def test_readBaseEndpointResponse_releaseBPresent_returnsReleaseBResult(self):
        release = "release-99.101.99-100-H2-large.out"
        date_time = datetime.utcnow()
        result = read_base_endpoint_response(release, date_time=date_time)
        self.assertEqual(result.type, "release")
        self.assertEqual(result.version, "99.101.99-100")
        self.assertEqual(result.database, "H2")
        self.assertEqual(result.dataset_size, "large")
        self.assertEqual(result.date_time, date_time)

    def test_readBaseEndpointResponse_nightlyAPresent_returnsNightlyAResult(self):
        nightly = "nightly-2020-05-18-medium.out"
        date_time = datetime.utcnow()
        result = read_base_endpoint_response(nightly, date_time=date_time)
        self.assertEqual(result.type, "nightly")
        self.assertEqual(result.version, "2020-05-18")
        self.assertEqual(result.database, "H2")
        self.assertEqual(result.dataset_size, "medium")
        self.assertEqual(result.date_time, date_time)

    def test_readBaseEndpointResponse_nightlyBPresent_returnsNightlyBResult(self):
        nightly = "nightly-2025-12-31-small.out"
        date_time = datetime.utcnow()
        result = read_base_endpoint_response(nightly, date_time=date_time)
        self.assertEqual(result.type, "nightly")
        self.assertEqual(result.version, "2025-12-31")
        self.assertEqual(result.database, "H2")
        self.assertEqual(result.dataset_size, "small")
        self.assertEqual(result.date_time, date_time)

    def test_readBaseEndpointResponse_unknownPresent_raisesException(self):
        self.assertRaisesRegex(Exception, "^Unknown filename format: \"unknown-file.txt\"$",
                               read_base_endpoint_response, "unknown-file.txt")

    def test_readBaseEndpointResponse_malformedReleasePresent_raisesException(self):
        self.assertRaisesRegex(Exception,
                               "^Malformed release filename format \"release-asd.txt\". "
                               "Expected: {}$".format(re.escape(ResultsReaderTest.release_pattern)),
                               read_base_endpoint_response, "release-asd.txt")

    def test_readBaseEndpointResponse_malformedNightlyPresent_raisesException(self):
        self.assertRaisesRegex(Exception,
                               "^Malformed nightly filename format \"nightly-asd.txt\". "
                               "Expected: {}$".format(re.escape(ResultsReaderTest.nightly_pattern)),
                               read_base_endpoint_response, "nightly-asd.txt")

    def test_readBaseEndpointResponse_releaseWithParentsPresent_returnsReleaseResult(self):
        release = "/one/two/three/release-1.90.0-01-PostgreSQL-small.out"
        date_time = datetime.utcnow()
        result = read_base_endpoint_response(release, date_time=date_time)
        self.assertEqual(result.type, "release")
        self.assertEqual(result.version, "1.90.0-01")
        self.assertEqual(result.database, "PostgreSQL")
        self.assertEqual(result.dataset_size, "small")
        self.assertEqual(result.date_time, date_time)

    def test_readBaseEndpointResponse_nightlyWithParentsPresent_returnsNightlyResult(self):
        nightly = "one/two/nightly-2020-05-18-medium.out"
        date_time = datetime.utcnow()
        result = read_base_endpoint_response(nightly, date_time=date_time)
        self.assertEqual(result.type, "nightly")
        self.assertEqual(result.version, "2020-05-18")
        self.assertEqual(result.database, "H2")
        self.assertEqual(result.dataset_size, "medium")
        self.assertEqual(result.date_time, date_time)

    def test_readEndpointResponseTimes_releasePresent_returnsReleaseEndpointResponses(self):
        base_endpoint_response = BaseEndpointResponse("release", "1.90.0-01", "PostgreSQL", "small",
                                                      datetime.utcnow())
        expected_endpoint_responses = list()
        expected_endpoint_responses.append(EndpointResponse(base_endpoint_response,
                                                            endpoint=
                                                            "rest/application/services/summary",
                                                            response_times=[335, 106, 86]))
        expected_endpoint_responses.append(EndpointResponse(base_endpoint_response,
                                                            endpoint="rest/application",
                                                            response_times=[43, 38, 34, 30]))
        expected_endpoint_responses.append(EndpointResponse(base_endpoint_response,
                                                            endpoint="rest/tag/application",
                                                            response_times=[64, 47]))

        endpoint_responses = read_endpoint_response_times(base_endpoint_response,
                                                          "resources/test/"
                                                          "release-1.90.0-01-PostgreSQL-small.out")

        self.assertListEqual(expected_endpoint_responses, endpoint_responses)

    def test_readEndpointResponseTimes_nightlyPresent_returnsNightlyEndpointResponses(self):
        base_endpoint_response = BaseEndpointResponse("nightly", "2020-05-18", "H2", "small",
                                                      datetime.utcnow())
        expected_endpoint_responses = list()
        expected_endpoint_responses.append(EndpointResponse(base_endpoint_response,
                                                            endpoint="rest/report/oKiM3Q1DC/"
                                                                     "43d24d8c8a3e4cb3932edbff13a1c"
                                                                     "833/metadata",
                                                            response_times=[21, 7]))
        expected_endpoint_responses.append(EndpointResponse(base_endpoint_response,
                                                            endpoint="api/v2/policyViolations?"
                                                                     "p=683df14c92384ffb92908fb946b"
                                                                     "93709",
                                                            response_times=[264, 186]))
        expected_endpoint_responses.append(EndpointResponse(base_endpoint_response,
                                                            endpoint="rest/membershipMapping/"
                                                                     "organization/4a9da2324e5e45da"
                                                                     "8fea06625e013c61",
                                                            response_times=[17, 7]))

        endpoint_responses = read_endpoint_response_times(base_endpoint_response,
                                                          "resources/test/"
                                                          "nightly-2020-05-18-small.out")

        self.assertListEqual(expected_endpoint_responses, endpoint_responses)

    def test_extractEsParameters_httpPresent_returnsSuccessfully(self):
        es_url = "http://localhost:9200"
        host, port, use_ssl = extract_es_parameters(es_url)
        self.assertEqual(host, "localhost")
        self.assertEqual(port, 9200)
        self.assertEqual(use_ssl, False)

    def test_extractEsParameters_httpNoPortPresent_returns80Port(self):
        es_url = "http://localhost"
        host, port, use_ssl = extract_es_parameters(es_url)
        self.assertEqual(host, "localhost")
        self.assertEqual(port, 80)
        self.assertEqual(use_ssl, False)

    def test_extractEsParameters_NoScheme_raisesException(self):
        es_url = "localhost:9200"
        self.assertRaisesRegex(Exception, "^The URL scheme must be either http or https",
                               extract_es_parameters, es_url)

    def test_extractEsParameters_UnsupportedScheme_raisesException(self):
        es_url = "ftp://localhost:9200"
        self.assertRaisesRegex(Exception, "^The URL scheme must be either http or https",
                               extract_es_parameters, es_url)

    def test_extractEsParameters_httpsPresent_returnsSuccessfully(self):
        es_url = "https://localhost:9200"
        host, port, use_ssl = extract_es_parameters(es_url)
        self.assertEqual(host, "localhost")
        self.assertEqual(port, 9200)
        self.assertEqual(use_ssl, True)

    def test_extractEsParameters_httpsNoPortPresent_returns443Port(self):
        es_url = "https://localhost"
        host, port, use_ssl = extract_es_parameters(es_url)
        self.assertEqual(host, "localhost")
        self.assertEqual(port, 443)
        self.assertEqual(use_ssl, True)

    def test_createEsBulkData(self):
        expected_bulk_data = '{"index": {}}\n{"type": "release", "version": "1.2.3", ' \
                             '"database": "PostgreSQL", "dataset_size": "small", ' \
                             '"date_time": "2020-05-22T16:25:52.331615", ' \
                             '"endpoint": "rest/a", "response_times": [100, 80, 60]}\n'
        endpoint_responses = [
            EndpointResponse(BaseEndpointResponse(type="release", version="1.2.3",
                                                  database="PostgreSQL",
                                                  dataset_size="small",
                                                  date_time=datetime(2020, 5, 22, 16, 25, 52,
                                                                     331615)),
                             endpoint="rest/a", response_times=[100, 80, 60])]

        bulk_data = create_es_bulk_data(endpoint_responses)

        self.assertEqual(bulk_data, expected_bulk_data)


if __name__ == '__main__':
    unittest.main()