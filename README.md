![Build](https://github.com/elisaautomate/kafka-connect-snmp/workflows/Build/badge.svg)

# Introduction

`kafka-connect-snmp` connector is used to receive SNMP traps and convert them to a record in Kafka. 

Supported MPv3 standards are
- MPv1
- MPv2c
- MPv3 (though this requires specific configuration properties below)

See [Connectors to Kafka](https://docs.confluent.io/current/connect/managing/index.html) for more info.

# Configuration

`kafka-connect-snmp` can be configured with properties or
using [Strimzi CRD properties](https://strimzi.io/docs/operators/latest/overview.html#configuration-points-connect_str) with either
KafkaConnect or KafkaConnector CRD.

Kafka related configuration properties can be deducted from [documentation](https://kafka.apache.org/documentation/#connectconfigs)

## General configuration properties

| Name                              | Description                                                   | Type   | Default      | Valid Values                      | Importance |
|-----------------------------------|---------------------------------------------------------------|--------|--------------|-----------------------------------|------------|
| topic                             | topic                                                         | string |              |                                   | high       |
| batch.size                        | Number of records to return in a single batch.                | int    | 1024         | [10,...,2147483647]               | medium     |
| poll.backoff.ms                   | The amount of time in ms to wait if no records are returned.  | long   | 250          | [10,...,2147483647]               | medium     |
| dispatcher.thread.pool.size       | Number of threads to allocate for the thread pool.            | int    | 10           | [1,...,100]                       | low        |
| listen.address                    | IP address to listen for messages on.                         | string | 0.0.0.0      |                                   | low        |
| listen.port                       | Port to listen on.                                            | int    | 10161        | ValidPort{start=1025, end=65535}  | low        |
| mpv3.enabled                      | 'true' if mpv3 is enabled                                     | boolean| false        | [true, false]                     | medium     |


## MPv3 configuration properties

| Name                              | Description                                                   | Type   | Default      | Valid Values                      | Importance |
|-----------------------------------|---------------------------------------------------------------|--------|--------------|-----------------------------------|------------|
| authentication.protocols          | Username used for USM with MPv3                               | string | MD5,SHA      | [MD5, SHA]                        | medium     |
| privacy.protocols                 | Username used for USM with MPv3                               | string | DES3,AES128  | [DES3, AES128]                    | medium     |
| usm.username                      | Username used for USM with MPv3                               | string |              |                                   | medium     |
| usm.passphrases.privacy           | Privacy passphrase for USM with MPv3                          | string |              |                                   | medium     |
| usm.passphrases.authentication    | Authentication passphrase for USM with MPv3                   | string |              |                                   | medium     |
| usm.protocols.privacy             | Privacy protocol used for MPv3 for defined user               | string | AES128       | [DES3, AES128]                    | medium     |
| usm.protocols.authentication      | Authentication protocol used for MPv3 for defined user        | string | MD5          | [MD5, SHA]                        | medium     |
