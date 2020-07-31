![Build](https://github.com/ElisaOyj/kafka-connect-snmp/workflows/Build/badge.svg)

# Introduction 

`kafka-connect-snmp` connector is used to receive SNMP traps and convert them to a record in Kafka.

See [Connectors to Kafka](https://docs.confluent.io/current/connect/managing/index.html) for more info.

# Configuration 

`kafka-connect-snmp` can be configured with properties or using [Kafka Connect REST Interface](https://docs.confluent.io/5.5.1/connect/references/restapi.html).

## SnmpTrapSourceConnector config

```properties
name=MySinkConnector
connector.class=com.github.jcustenborder.kafka.connect.snmp.SnmpTrapSourceConnector
```

### Config

| Name                        | Description                                                  | Type   | Default | Valid Values                     | Importance |
|-----------------------------|--------------------------------------------------------------|--------|---------|----------------------------------|------------|
| topic                       | topic                                                        | string |         |                                  | high       |
| batch.size                  | Number of records to return in a single batch.               | int    | 1024    | [10,...,2147483647]              | medium     |
| poll.backoff.ms             | The amount of time in ms to wait if no records are returned. | long   | 250     | [10,...,2147483647]              | medium     |
| dispatcher.thread.pool.size | Number of threads to allocate for the thread pool.           | int    | 10      | [1,...,100]                      | low        |
| listen.address              | IP address to listen for messages on.                        | string | 0.0.0.0 |                                  | low        |
| listen.port                 | Port to listen on.                                           | int    | 10161   | ValidPort{start=1025, end=65535} | low        |
| listen.protocol             | Protocol to listen with..                                    | string | UDP     | [UDP, TCP]                       | low        |


# Running in development

The development setup runs with `docker-compose`

Use
```
bin/run.sh
```
to build, start the environment and configure the connector using REST API
