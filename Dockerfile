FROM quay.io/strimzi/kafka:0.27.1-kafka-2.8.1

COPY kafka-connect-snmp/usr/share/kafka-connect/kafka-connect-snmp/ /opt/kafka/plugins/java/kafka-connect-snmp/

