/**
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.kafka.connect.snmp;

import com.github.jcustenborder.kafka.connect.utils.config.validators.Validators;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import java.util.Map;


public class SnmpTrapSourceConnectorConfig extends AbstractConfig {

  public static final String LISTEN_ADDRESS_CONF = "listen.address";
  static final String LISTEN_ADDRESS_DEFAULT = "0.0.0.0";
  static final String LISTEN_ADDRESS_DOC = "IP address to listen for messages on.";

  public static final String LISTEN_PROTOCOL_CONF = "listen.protocol";
  static final String LISTEN_PROTOCOL_DEFAULT = "UDP";
  static final String LISTEN_PROTOCOL_DOC = "Protocol to listen with..";


  public static final String LISTEN_PORT_CONF = "listen.port";
  static final int LISTEN_PORT_DEFAULT = 10161;
  static final String LISTEN_PORT_DOC = "Port to listen on.";

  public static final String DISPATCHER_THREAD_POOL_SIZE_CONF = "dispatcher.thread.pool.size";
  static final int DISPATCHER_THREAD_POOL_SIZE_DEFAULT = 10;
  static final String DISPATCHER_THREAD_POOL_SIZE_DOC = "Number of threads to allocate for the thread pool.";

  public static final String TOPIC_CONF = "topic";
  static final String TOPIC_DOC = "topic";

  public static final String BATCH_SIZE_CONF = "batch.size";
  static final String BATCH_SIZE_DOC = "Number of records to return in a single batch.";
  static final int BATCH_SIZE_DEFAULT = 1024;

  public static final String POLL_BACKOFF_MS_CONF = "poll.backoff.ms";
  static final String POLL_BACKOFF_MS_DOC = "The amount of time in ms to wait if no records are returned.";
  static final long POLL_BACKOFF_MS_DEFAULT = 250;

  public final String listenAddress;
  public final int listenPort;
  public final String listenProtocol;
  public final int dispatcherThreadPoolSize;
  public final String topic;
  public final int batchSize;
  public final long pollBackoffMs;

  public SnmpTrapSourceConnectorConfig(Map<String, String> parsedConfig) {
    super(conf(), parsedConfig);

    this.listenAddress = this.getString(LISTEN_ADDRESS_CONF);
    this.listenPort = this.getInt(LISTEN_PORT_CONF);
    this.listenProtocol = this.getString(LISTEN_PROTOCOL_CONF);
    this.dispatcherThreadPoolSize = this.getInt(DISPATCHER_THREAD_POOL_SIZE_CONF);
    this.topic = this.getString(TOPIC_CONF);
    this.batchSize = this.getInt(BATCH_SIZE_CONF);
    this.pollBackoffMs = this.getLong(POLL_BACKOFF_MS_CONF);
  }

  public static ConfigDef conf() {
    return new ConfigDef()
            .define(TOPIC_CONF, Type.STRING, Importance.HIGH, TOPIC_DOC)
            .define(LISTEN_ADDRESS_CONF, Type.STRING, LISTEN_ADDRESS_DEFAULT, Importance.LOW, LISTEN_ADDRESS_DOC)
            .define(LISTEN_PORT_CONF, Type.INT, LISTEN_PORT_DEFAULT, Validators.validPort(161, 65535), Importance.LOW, LISTEN_PORT_DOC)
            .define(LISTEN_PROTOCOL_CONF, Type.STRING, LISTEN_PROTOCOL_DEFAULT, ConfigDef.ValidString.in("UDP", "TCP"), Importance.LOW, LISTEN_PROTOCOL_DOC)
            .define(DISPATCHER_THREAD_POOL_SIZE_CONF, Type.INT, DISPATCHER_THREAD_POOL_SIZE_DEFAULT, ConfigDef.Range.between(1, 100), Importance.LOW, DISPATCHER_THREAD_POOL_SIZE_DOC)
            .define(BATCH_SIZE_CONF, Type.INT, BATCH_SIZE_DEFAULT, ConfigDef.Range.between(10, Integer.MAX_VALUE), Importance.MEDIUM, BATCH_SIZE_DOC)
            .define(POLL_BACKOFF_MS_CONF, Type.LONG, POLL_BACKOFF_MS_DEFAULT, ConfigDef.Range.between(10, Integer.MAX_VALUE), Importance.MEDIUM, POLL_BACKOFF_MS_DOC);
  }

}
