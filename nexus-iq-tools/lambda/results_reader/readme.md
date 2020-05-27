<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# To test locally
* Install dependencies:
```bash
pipenv install
```
* Run Elasticsearch with Kibana:  
```bash
docker network create somenetwork
docker run -d --name elasticsearch --net somenetwork -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:7.7.0
docker run -d --name kibana --net somenetwork -p 5601:5601 kibana:7.7.0
```
* Activate Pipenv environment:

```bash
pipenv shell
```
* Run script:
```bash
python results_reader.py -u https://iq-perf-results.s3.amazonaws.com/releases/release-1.90.0-01/PostgreSQL/release-1.90.0-01-PostgreSQL-small.out -e http://localhost:9200
```