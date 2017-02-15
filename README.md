# Introduction 

This connector is used to receive data from devices via SNMP. This connector will receive SNMP traps and convert them
to a record in Kafka. 

# Configuration 

## SnmpTrapSourceConnector

```properties
name=MySinkConnector
connector.class=com.github.jcustenborder.kafka.connect.snmp.SnmpTrapSourceConnector
```

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


The [docker-compose.yml](docker-compose.yml) that is included in this repository is based on the Confluent Platform Docker
images. Take a look at the [quickstart](http://docs.confluent.io/3.0.1/cp-docker-images/docs/quickstart.html#getting-started-with-docker-client)
for the Docker images. 

The hostname `confluent` must be resolvable by your host. You will need to determine the ip address of your docker-machine using `docker-machine ip confluent` 
and add this to your `/etc/hosts` file. For example if `docker-machine ip confluent` returns `192.168.99.100` add this:

```
192.168.99.100  confluent
```


```
docker-compose up -d
```


Start the connector with debugging enabled.
 
```
./bin/debug.sh
```

Start the connector with debugging enabled. This will wait for a debugger to attach.

```
export SUSPEND='y'
./bin/debug.sh
```