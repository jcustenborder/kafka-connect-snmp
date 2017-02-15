/**
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.kafka.connect.snmp;


import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SnmpTrapSourceTaskTest {
  SnmpTrapSourceTask task;

  @BeforeEach
  public void start() {
    this.task = new SnmpTrapSourceTask();
    this.task.start(SnmpTrapSourceConnectorConfigTest.settings());
  }

  @AfterEach
  public void stop() {
    this.task.stop();
  }

  @Test
  public void version() {
    assertNotNull(this.task.version());
  }

  @Test
  public void test() throws InterruptedException, IOException {
    Thread.sleep(1000);

    Snmp snmp = new Snmp(new DefaultUdpTransportMapping());

    for (int i = 0; i < 10; i++) {

      PDU trap = new PDU();
      trap.setType(PDU.TRAP);

      OID oid = new OID("1.2.3.4.5");
      trap.add(new VariableBinding(SnmpConstants.snmpTrapOID, oid));
      trap.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(5000))); // put your uptime here
      trap.add(new VariableBinding(SnmpConstants.sysDescr, new OctetString("System Description")));

      //Add Payload
      Variable var = new OctetString("some string");
      trap.add(new VariableBinding(oid, var));


      // Specify receiver
      Address targetAddress = new UdpAddress("127.0.0.1/10161");
      CommunityTarget target = new CommunityTarget();
      target.setCommunity(new OctetString("public"));
      target.setVersion(SnmpConstants.version2c);
      target.setAddress(targetAddress);

      // Send

      snmp.send(trap, target, null, null);
    }

    Thread.sleep(5000);
  }
}