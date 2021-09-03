FROM quay.io/strimzi/kafka:0.24.0-kafka-2.8.0

COPY kafka-connect-snmp/usr/share/kafka-connect/kafka-connect-snmp/ /opt/kafka/plugins/java/kafka-connect-snmp/

