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

import com.github.jcustenborder.kafka.connect.utils.VersionUtil;
import com.github.jcustenborder.kafka.connect.utils.config.Description;
import com.github.jcustenborder.kafka.connect.utils.config.DocumentationImportant;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@DocumentationImportant("This connector listens on a network port. Running more than one task or running in distributed " +
        "mode can cause some undesired effects if another task already has the port open. It is recommended that you run this " +
        "connector in :term:`Standalone Mode`.")
@Description("Connector is used to receive syslog messages over UDP.")
public class SnmpTrapSourceConnector extends SourceConnector {

  private static Logger log = LoggerFactory.getLogger(SnmpTrapSourceConnector.class);
  private SnmpTrapSourceConnectorConfig config;
  private Map<String, String> settings;

  @Override
  public String version() {
    return VersionUtil.version(this.getClass());
  }

  @Override
  public void start(Map<String, String> settings) {
    this.config = new SnmpTrapSourceConnectorConfig(settings);
    this.settings = settings;
  }

  @Override
  public Class<? extends Task> taskClass() {
    return SnmpTrapSourceTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int i) {
    if (i > 1) {
      log.warn("This task only supports one instance of the connector.");
    }
    return Collections.singletonList(this.settings);
  }

  @Override
  public void stop() {

  }

  @Override
  public ConfigDef config() {
    return SnmpTrapSourceConnectorConfig.conf();
  }
}
