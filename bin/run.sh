
#!/usr/bin/env bash
#
# Original work Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
# Modified work Copyright © 2020 Elisa Oyj - Ville Ilvonen (ville.ilvonen@elisa.fi)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set +e

mvn clean package

KAFKA_CONNECT_IMAGE="confluentinc/cp-kafka-connect"
KAFKA_VERSION="$(cat docker-compose.yml | grep $KAFKA_CONNECT_IMAGE | cut -d ':' -f 3)"
echo "Running kafka-connect-snmp with $KAFKA_CONNECT_IMAGE:$KAFKA_VERSION"

export KAFKA_CONNECT_CONTAINER="$(docker ps --filter ancestor=$KAFKA_CONNECT_IMAGE:$KAFKA_VERSION -q)"

if [ -n "${KAFKA_CONNECT_CONTAINER}" ]; then
    echo "$KAFKA_CONNECT_IMAGE:$KAFKA_VERSION running with container id $KAFKA_CONNECT_CONTAINER"
else
    echo "Starting containers with 'docker-compose up -d'"
    docker-compose up -d
fi

URL=http://localhost:8083

# sync start-up
echo "Waiting for docker-compose services to start "
while [ $(curl --write-out '%{http_code}' --silent --output /dev/null $URL) -ne 200 ]; do printf . && sleep 2; done
# get kafka version - https://docs.confluent.io/current/connect/references/restapi.html#kconnect-cluster
curl -v -H "Content-Type: application/json" $URL

## configure the connector - https://docs.confluent.io/5.5.1/connect/references/restapi.html#connectors
TEST_TOPIC="test_snmp"
JSON_CONFIGURE_REQUEST_FMT='{"name":"snmp-connector","config":{"connector.class":"com.github.jcustenborder.kafka.connect.snmp.SnmpTrapSourceConnector","topic":"%s"}}\n'
JSON_REQUEST_CONFIGURE=$(printf "$JSON_CONFIGURE_REQUEST_FMT" "$TEST_TOPIC")
curl -v -X POST -H "Content-Type: application/json" --data $JSON_REQUEST_CONFIGURE $URL/connectors
