/**
 * Copyright © 2021 Elisa Oyj
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


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

  public static final String MPV3_ENABLED_CONF = "mpv3.enabled";
  static final String MPV3_ENABLED_DOC = "Configuration property to enable MPv3 support";
  static final boolean MPV3_ENABLED_DEFAULT = false;

  public static final String AUTHENTICATION_PROTOCOLS = "authentication.protocols";
  static final String AUTHENTICATION_PROTOCOLS_DOC = "The supported authentication protocols for MPv3";
  static final List<String> AUTHENTICATION_PROTOCOLS_DEFAULT = Arrays.stream(AuthenticationProtocol.values()).map(Enum::toString).collect(Collectors.toList());

  public static final String PRIVACY_PROTOCOLS = "privacy.protocols";
  static final String PRIVACY_PROTOCOLS_DOC = "The supported privacy protocols for MPv3";
  static final List<String> PRIVACY_PROTOCOLS_DEFAULT = Arrays.stream(PrivacyProtocol.values()).map(Enum::toString).collect(Collectors.toList());

  public static final String USM_USERNAME = "usm.username";
  static final String USM_USERNAME_DOC = "The supported privacy protocols for MPv3";
  static final String USM_USERNAME_DEFAULT =  "";

  public static final String USM_AUTHENTICATION_PASSPHRASE = "usm.passphrases.authentication";
  static final String USM_AUTHENTICATION_PASSPHRASE_DOC = "Authentication passphrase for USM with MPv3";
  static final String USM_AUTHENTICATION_PASSPHRASE_DEFAULT =  "";

  public static final String USM_PRIVACY_PASSPHRASE = "usm.passphrases.privacy";
  static final String USM_PRIVACY_PASSPHRASE_DOC = "Privacy passphrase for USM with MPv3";
  public static final String USM_PRIVACY_PASSPHRASE_DEFAULT = "";

  public static final String USM_PRIVACY_PROTOCOL = "usm.protocols.privacy";
  static final String USM_PRIVACY_PROTOCOL_DOC = "Privacy protocol for USM with MPv3";
  static final String USM_PRIVACY_PROTOCOL_DEFAULT = PrivacyProtocol.AES128.toString();

  public static final String USM_AUTHENTICATION_PROTOCOL = "usm.protocols.authentication";
  static final String USM_AUTHENTICATION_PROTOCOL_DOC = "Authentication protocl for USM with MPv3";
  static final String USM_AUTHENTICATION_PROTOCOL_DEFAULT = AuthenticationProtocol.MD5.toString();


  public final String listenAddress;
  public final int listenPort;
  public final String listenProtocol;
  public final int dispatcherThreadPoolSize;
  public final String topic;
  public final int batchSize;
  public final long pollBackoffMs;
  public final boolean mpv3Enabled;
  public final Set<AuthenticationProtocol> authenticationProtocols;
  public final Set<PrivacyProtocol> privacyProtocols;
  public final String username;
  public final String privacyPassphrase;
  public final String authenticationPassphrase;
  public final AuthenticationProtocol authenticationProtocol;
  public final PrivacyProtocol privacyProtocol;


  public SnmpTrapSourceConnectorConfig(Map<String, String> parsedConfig) {
    super(conf(), parsedConfig);

    this.listenAddress = this.getString(LISTEN_ADDRESS_CONF);
    this.listenPort = this.getInt(LISTEN_PORT_CONF);
    this.listenProtocol = this.getString(LISTEN_PROTOCOL_CONF);
    this.dispatcherThreadPoolSize = this.getInt(DISPATCHER_THREAD_POOL_SIZE_CONF);
    this.topic = this.getString(TOPIC_CONF);
    this.batchSize = this.getInt(BATCH_SIZE_CONF);
    this.pollBackoffMs = this.getLong(POLL_BACKOFF_MS_CONF);
    this.mpv3Enabled = this.getBoolean(MPV3_ENABLED_CONF);
    this.authenticationProtocols = this.getList(AUTHENTICATION_PROTOCOLS)
        .stream().map((s) -> AuthenticationProtocol.valueOf(s.toUpperCase()))
        .collect(Collectors.toSet());
    this.privacyProtocols = this.getList(PRIVACY_PROTOCOLS)
        .stream().map((s) -> PrivacyProtocol.valueOf(s.toUpperCase()))
        .collect(Collectors.toSet());
    this.username = this.getString(USM_USERNAME);
    this.authenticationPassphrase = this.getString(USM_AUTHENTICATION_PASSPHRASE);
    this.privacyPassphrase = this.getString(USM_PRIVACY_PASSPHRASE);
    this.authenticationProtocol =  AuthenticationProtocol.valueOf(this.getString(USM_AUTHENTICATION_PROTOCOL).toUpperCase());
    this.privacyProtocol = PrivacyProtocol.valueOf(this.getString(USM_PRIVACY_PROTOCOL).toUpperCase());
  }

  public static ConfigDef conf() {
    String[] authProtocols = Arrays.stream(AuthenticationProtocol.values()).map(Enum::toString).toArray(String[]::new);
    String[] privProtocols = Arrays.stream(PrivacyProtocol.values()).map(Enum::toString).toArray(String[]::new);

    return new ConfigDef()
        .define(TOPIC_CONF, Type.STRING, Importance.HIGH, TOPIC_DOC)
        .define(LISTEN_ADDRESS_CONF, Type.STRING, LISTEN_ADDRESS_DEFAULT, Importance.LOW, LISTEN_ADDRESS_DOC)
        .define(LISTEN_PORT_CONF, Type.INT, LISTEN_PORT_DEFAULT, Validators.validPort(1025, 65535), Importance.LOW, LISTEN_PORT_DOC)
        .define(LISTEN_PROTOCOL_CONF, Type.STRING, LISTEN_PROTOCOL_DEFAULT, ConfigDef.ValidString.in("UDP", "TCP"), Importance.LOW, LISTEN_PROTOCOL_DOC)
        .define(DISPATCHER_THREAD_POOL_SIZE_CONF, Type.INT, DISPATCHER_THREAD_POOL_SIZE_DEFAULT, ConfigDef.Range.between(1, 100), Importance.LOW, DISPATCHER_THREAD_POOL_SIZE_DOC)
        .define(BATCH_SIZE_CONF, Type.INT, BATCH_SIZE_DEFAULT, ConfigDef.Range.between(10, Integer.MAX_VALUE), Importance.MEDIUM, BATCH_SIZE_DOC)
        .define(POLL_BACKOFF_MS_CONF, Type.LONG, POLL_BACKOFF_MS_DEFAULT, ConfigDef.Range.between(10, Integer.MAX_VALUE), Importance.MEDIUM, POLL_BACKOFF_MS_DOC)
        .define(MPV3_ENABLED_CONF, Type.BOOLEAN, MPV3_ENABLED_DEFAULT, Importance.MEDIUM, MPV3_ENABLED_DOC)

        // MPv3 configs
        .define(AUTHENTICATION_PROTOCOLS, Type.LIST, AUTHENTICATION_PROTOCOLS_DEFAULT, ConfigDef.ValidList.in(authProtocols), Importance.MEDIUM, AUTHENTICATION_PROTOCOLS_DOC)
        .define(PRIVACY_PROTOCOLS, Type.LIST, PRIVACY_PROTOCOLS_DEFAULT, ConfigDef.ValidList.in(privProtocols), Importance.MEDIUM, PRIVACY_PROTOCOLS_DOC)
        .define(USM_USERNAME, Type.STRING, USM_USERNAME_DEFAULT, Importance.MEDIUM, USM_USERNAME_DOC)
        .define(USM_AUTHENTICATION_PASSPHRASE, Type.STRING, USM_AUTHENTICATION_PASSPHRASE_DEFAULT, Importance.MEDIUM, USM_AUTHENTICATION_PASSPHRASE_DOC)
        .define(USM_PRIVACY_PASSPHRASE, Type.STRING, USM_PRIVACY_PASSPHRASE_DEFAULT, Importance.MEDIUM, USM_PRIVACY_PASSPHRASE_DOC)
        .define(USM_AUTHENTICATION_PROTOCOL, Type.STRING, USM_AUTHENTICATION_PROTOCOL_DEFAULT, ConfigDef.ValidString.in(authProtocols), Importance.MEDIUM, USM_AUTHENTICATION_PROTOCOL_DOC)
        .define(USM_PRIVACY_PROTOCOL, Type.STRING, USM_PRIVACY_PROTOCOL_DEFAULT, ConfigDef.ValidString.in(privProtocols), Importance.MEDIUM, USM_PRIVACY_PROTOCOL_DOC);
  }

}
