#!/usr/bin/env python3.7

#  Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
#  Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
#  "Sonatype" is a trademark of Sonatype, Inc.

from __future__ import annotations
from typing import List
import sys
import logging
import argparse
import re
import boto3
import tempfile
import os
from pathlib import Path
import jsonpickle
from urllib.parse import urlparse
from elasticsearch import Elasticsearch
from datetime import datetime

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(levelname)s [%(threadName)s] %(name)s - %(message)s",
                    datefmt="%Y-%m-%dT%H:%M:%S%z", stream=sys.stdout)
logger = logging.getLogger(__name__)


def main():
    urls, es_url = read_urls()

    with tempfile.TemporaryDirectory() as directory_path:
        paths = download_files_at_urls(directory_path, urls)

        endpoint_responses = list()
        for path in paths:
            endpoint_responses += read_endpoint_responses(path)
        logger.info("%d endpoint responses read", len(endpoint_responses))

        send_to_elasticsearch(es_url, endpoint_responses)


def read_urls() -> (list, str):
    parser = argparse.ArgumentParser(
        description="Reads results of performance tests and sends them to Elasticsearch")
    parser.add_argument("-u", metavar="URL", help="URL to file in S3 bucket", required=True,
                        action="append")
    parser.add_argument("-e", metavar="ES_URL", help="URL to Elasticsearch", required=True)
    parsed_args = parser.parse_args()
    urls = parsed_args.u
    es_url = parsed_args.e
    logger.info(f"URLs: {urls}")
    logger.info(f"Elasticsearch URL: {es_url}")
    return urls, es_url


def download_files_at_urls(directory_path, urls: list):
    paths = []
    for url in urls:
        pattern = "https://(.+)\\.s3\\.amazonaws\\.com/(.+)"
        p = re.compile(pattern)
        match = p.fullmatch(url)
        if match is not None:
            bucket_name = match.group(1)
            key = match.group(2)
            logger.info("bucket_name=%s, key=%s", bucket_name, key)
            path = os.path.join(directory_path, bucket_name, key)
            os.makedirs(Path(path).parent, exist_ok=True)
            logger.info("download_path=%s", path)
            s3resource = boto3.resource("s3")
            bucket = s3resource.Bucket(bucket_name)
            bucket.download_file(key, path)
            paths.append(path)
        else:
            raise Exception("Unrecognized URL format: {}. Expected: {}".format(url, pattern))
    return paths


def read_endpoint_responses(path) -> List[EndpointResponse]:
    base_endpoint_response = read_base_endpoint_response(path)
    endpoint_responses = read_endpoint_response_times(base_endpoint_response, path)
    if logger.getEffectiveLevel() == logging.DEBUG:
        for endpoint_response in endpoint_responses:
            logger.debug(endpoint_response)
    return endpoint_responses


def read_base_endpoint_response(path: str, date_time=datetime.utcnow()) -> BaseEndpointResponse:
    path = Path(path).name
    if path.startswith("release"):
        type = "release"
        pattern = "release-(\\d+\\.\\d+\\.\\d+-\\d+)-(PostgreSQL|H2)-(small|medium|large).out"
        prog = re.compile(pattern)
        match = prog.fullmatch(path)
        if match is not None:
            return BaseEndpointResponse(type, match.group(1), match.group(2), match.group(3),
                                        date_time)
        else:
            raise Exception("Malformed release filename format \"{}\". "
                            "Expected: {}".format(path, pattern))
    elif path.startswith("nightly"):
        type = "nightly"
        database = "H2"
        pattern = "nightly-(\\d+\\-\\d+\\-\\d+)-(small|medium|large).out"
        prog = re.compile(pattern)
        match = prog.fullmatch(path)
        if match is not None:
            return BaseEndpointResponse(type, match.group(1), database, match.group(2), date_time)
        else:
            raise Exception("Malformed nightly filename format \"{}\". "
                            "Expected: {}".format(path, pattern))
    else:
        raise Exception(f"Unknown filename format: \"{path}\"")


def read_endpoint_response_times(base_endpoint_response: BaseEndpointResponse, path: str) \
        -> List[EndpointResponse]:
    endpoint_response_dict = {}
    with open(path, encoding="UTF-8") as file:
        is_method_block = False
        is_response_block = False
        current_endpoint_response = None
        for line in iter(file.readline, ""):
            line = line.strip()

            dashes_only_pattern = re.compile("-+")
            match = dashes_only_pattern.fullmatch(line)
            if match is not None:
                is_method_block = False
                is_response_block = False
                continue

            if is_method_block:
                continue

            if not is_response_block:
                method_block_pattern = re.compile("method: .+")
                match = method_block_pattern.fullmatch(line)
                if match is not None:
                    is_method_block = True
                    continue

            response_block_pattern = re.compile("URL : (.+)")
            match = response_block_pattern.fullmatch(line)
            if match is not None:
                is_response_block = True
                url = match.group(1)
                if url not in endpoint_response_dict:
                    current_endpoint_response = EndpointResponse(base_endpoint_response, url, [])
                    endpoint_response_dict[url] = current_endpoint_response
                else:
                    current_endpoint_response = endpoint_response_dict[url]
                continue

            if is_response_block:
                response_time_pattern = re.compile("Response Time: (\\d+)")
                match = response_time_pattern.fullmatch(line)
                if match is not None:
                    response_time = match.group(1)
                    current_endpoint_response.response_times.append(int(response_time))
                    current_endpoint_response = None

    return list(endpoint_response_dict.values())


def send_to_elasticsearch(es_url, endpoint_responses: List[EndpointResponse]):
    host, port, use_ssl = extract_es_parameters(es_url)
    bulk_data = create_es_bulk_data(endpoint_responses)
    es = Elasticsearch(hosts=[{"host": host, "port": port}], use_ssl=use_ssl)
    response = es.bulk(index="endpoint_response", body=bulk_data)
    logger.debug(response)


def extract_es_parameters(es_url):
    parse_result = urlparse(es_url)
    if not parse_result.scheme or\
            (parse_result.scheme != "http" and parse_result.scheme != "https"):
        raise Exception("The URL scheme must be either http or https: " + parse_result.scheme)
    host = parse_result.hostname
    if parse_result.port is not None:
        port = parse_result.port
    else:
        if parse_result.scheme == "http":
            port = 80
        else:
            port = 443
    use_ssl = False
    if parse_result.scheme == "https" or port == 443:
        use_ssl = True
    logger.info("Elasticsearch connection: host=%s, port=%s, use_ssl=%s", host, port, use_ssl)
    return host, port, use_ssl


def create_es_bulk_data(endpoint_responses: List[EndpointResponse]) -> str:
    bulk_data = ""
    for endpoint_response in endpoint_responses:
        bulk_data += '{"index": {}}\n'
        bulk_data += jsonpickle.encode(endpoint_response, unpicklable=False) + "\n"
    return bulk_data


class BaseEndpointResponse:

    def __init__(self, type, version, database, dataset_size, date_time: datetime):
        self.type = type
        self.version = version
        self.database = database
        self.dataset_size = dataset_size
        self.date_time = date_time


class EndpointResponse(BaseEndpointResponse):

    def __init__(self, base: BaseEndpointResponse, endpoint: str, response_times: list):
        super().__init__(base.type, base.version, base.database, base.dataset_size, base.date_time)
        self.endpoint = endpoint
        self.response_times = response_times

    def __eq__(self, other):
        logger.debug("self: %s", str(self))
        logger.debug("other: %s", str(other))
        if not isinstance(other, EndpointResponse):
            return False
        return (self.type == other.type and
                self.version == other.version and
                self.database == other.database and
                self.dataset_size == other.dataset_size and
                self.date_time == other.date_time and
                self.endpoint == other.endpoint and
                self.response_times == other.response_times)

    def __str__(self):
        return f'{self.__class__.__name__}[type="{self.type}",version="{self.version}",' \
               f'database="{self.database}",dataset_size="{self.dataset_size}",' \
               f'date_time="{self.date_time}",url="{self.endpoint},' \
               f'response_times="{self.response_times}"]'


if __name__ == '__main__':
    main()