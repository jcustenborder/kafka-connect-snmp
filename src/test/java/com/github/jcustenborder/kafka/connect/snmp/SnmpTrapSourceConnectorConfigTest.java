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


import com.github.jcustenborder.kafka.connect.utils.config.MarkdownFormatter;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SnmpTrapSourceConnectorConfigTest {

  public static final String listeningPort = "11111";
  public static final int batchSize = 10;
  public static final String username = "username";
  public static final String privacyPassphrase = "privpass";
  public static final String authenticationPassphrase = "authpass";

  public static Map<String, String> settingsV2() {

    HashMap<String, String> map = new HashMap<>();
    map.put(SnmpTrapSourceConnectorConfig.LISTEN_PORT_CONF, listeningPort);
    map.put(SnmpTrapSourceConnectorConfig.TOPIC_CONF, "testing");
    map.put(SnmpTrapSourceConnectorConfig.BATCH_SIZE_CONF, String.format("%d", batchSize));

    return map;
  }


  public static Map<String, String> settingsV3() {
    Map<String, String> map = settingsV2();
    map.put(SnmpTrapSourceConnectorConfig.USM_USERNAME, username);
    map.put(SnmpTrapSourceConnectorConfig.USM_PRIVACY_PASSPHRASE, privacyPassphrase);
    map.put(SnmpTrapSourceConnectorConfig.USM_AUTHENTICATION_PASSPHRASE, authenticationPassphrase);
    map.put(SnmpTrapSourceConnectorConfig.MPV3_ENABLED_CONF, "true");

    return map;
  }

  @Test
  public void doc() {

    System.out.println(
        MarkdownFormatter.toMarkdown(SnmpTrapSourceConnectorConfig.conf())
    );
  }

  @Test
  public void shouldConvertLists() {
    Map<String, String> m = settingsV2();
    SnmpTrapSourceConnectorConfig c = new SnmpTrapSourceConnectorConfig(m);
    assertEquals(Set.of(AuthenticationProtocol.MD5, AuthenticationProtocol.SHA), c.authenticationProtocols);
    assertEquals(Set.of(PrivacyProtocol.DES3, PrivacyProtocol.AES128), c.privacyProtocols);

    m = settingsV2();

    m.put(SnmpTrapSourceConnectorConfig.TOPIC_CONF, "topic");
    m.put(SnmpTrapSourceConnectorConfig.AUTHENTICATION_PROTOCOLS, "MD5");

    c = new SnmpTrapSourceConnectorConfig(m);
    assertEquals(Set.of(AuthenticationProtocol.MD5), c.authenticationProtocols);

  }

  @Test
  public void shouldNotAcceptInvalidListValues() {
    Map<String, String> m = Map.of(
        SnmpTrapSourceConnectorConfig.TOPIC_CONF, "topic",
        SnmpTrapSourceConnectorConfig.AUTHENTICATION_PROTOCOLS, "aaa,bbb",
        SnmpTrapSourceConnectorConfig.PRIVACY_PROTOCOLS, "ccc"
    );
    assertThrows(org.apache.kafka.common.config.ConfigException.class, () -> new SnmpTrapSourceConnectorConfig(m));

  }

  @Test
  public void shouldNotConvertInvalidProtocols() {
    assertThrows(ConfigException.class, () -> {
      Map<String, String> m = settingsV3();
      m.put(SnmpTrapSourceConnectorConfig.USM_PRIVACY_PROTOCOL, "XT1");
      new SnmpTrapSourceConnectorConfig(m);
    });

    assertThrows(ConfigException.class, () -> {
      Map<String, String> m = settingsV3();
      m.put(SnmpTrapSourceConnectorConfig.USM_AUTHENTICATION_PROTOCOL, "XT1");
      new SnmpTrapSourceConnectorConfig(m);
    });

  }
}