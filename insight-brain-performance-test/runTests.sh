#!/bin/bash
mvn test
mvn test -P static-query-no-leading-wildcards
mvn test -P dynamic-query
mvn test -P dynamic-query-group-search
mvn test -P static-login
mvn test -P dynamic-login
