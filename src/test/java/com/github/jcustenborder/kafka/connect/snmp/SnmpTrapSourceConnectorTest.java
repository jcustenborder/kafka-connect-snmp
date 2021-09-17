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


import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SnmpTrapSourceConnectorTest {

  SnmpTrapSourceConnector connector;

  @BeforeEach
  public void start() {
    this.connector = new SnmpTrapSourceConnector();
    this.connector.start(SnmpTrapSourceConnectorConfigTest.settingsV2());
  }

  @Test
  public void taskConfigs() {
    final List<Map<String, String>> expected = ImmutableList.of(
        SnmpTrapSourceConnectorConfigTest.settingsV2()
    );
    List<Map<String, String>> actual = this.connector.taskConfigs(1);
    assertEquals(expected, actual);
    actual = this.connector.taskConfigs(5);
    assertEquals(expected, actual);
  }

  @Test
  public void version() {
    assertNotNull(this.connector.version());
  }

  @Test
  public void taskClass() {
    assertEquals(SnmpTrapSourceTask.class, this.connector.taskClass());
  }

  @AfterEach
  public void stop() {
    this.connector.stop();
  }
}
