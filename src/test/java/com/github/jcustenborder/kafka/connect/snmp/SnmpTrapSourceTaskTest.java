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


import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Map;

import static com.github.jcustenborder.kafka.connect.snmp.PDUGen.createNonTrap;
import static com.github.jcustenborder.kafka.connect.snmp.PDUGen.createTrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SnmpTrapSourceTaskTest {
  private SnmpTrapSourceTask task;
  private CommunityTarget<Address> target;
  private Snmp snmp;
  private Map<String, String> settings;

  @BeforeEach
  public void start() throws SocketException, InterruptedException {
    task = new SnmpTrapSourceTask();
    settings = SnmpTrapSourceConnectorConfigTest.settings();
    task.start(settings);

    // Specify receiver
    Address targetAddress = new UdpAddress("127.0.0.1/10161");
    target = new CommunityTarget<>();
    target.setCommunity(new OctetString("public"));
    target.setVersion(SnmpConstants.version2c);
    target.setAddress(targetAddress);

    snmp = new Snmp(new DefaultUdpTransportMapping());

    Thread.sleep(1000);


  }

  @AfterEach
  public void stop() {
    this.task.stop();
  }

  @Test
  public void shouldHaveVersion() {
    assertNotNull(this.task.version());
  }

  @Test
  public void shouldBufferTraps() throws InterruptedException, IOException {

    int i;

    for (i = 0; i < 10; i++) {

      PDU trap = createTrap("1.2.3.4.5", "some string");
      snmp.send(trap, target, null, null);
    }

    Thread.sleep(5000);
    assertEquals(i, task.getRecordBuffer().size(), "Sent traps should be equal to buffered records");
  }


  @Test
  public void shouldNotBufferNonTraps() throws IOException {
    PDU nonTrap = createNonTrap("1.2.3.4.5", "some string");
    // Send
    snmp.send(nonTrap, target, null, null);
    // There should be no records buffered since it only accepts traps
    assertEquals(0, task.getRecordBuffer().size(), "There shouldn't be non traps in buffer.");
  }


  @Test
  public void shouldBufferOnlyTraps() throws InterruptedException, IOException {

    int trapCount = 0;
    int nonTrapCount = 0;

    for (int i = 0; i < 10; i++) {
      String oid = "1.2.3.4.5";
      String oidVal = "some string";
      PDU pdu;
      if (i % 3 == 0) {
        nonTrapCount++;
        pdu = createNonTrap("1.2.3.4.5", "some string");
      } else {
        trapCount++;
        pdu = createTrap("1.2.3.4.5", "some string");

      }
      snmp.send(pdu, target, null, null);
    }

    Thread.sleep(5000);

    assertEquals(trapCount, task.getRecordBuffer().size(), "Sent traps should be equal to buffered records");
    assertTrue(nonTrapCount > 0, "To adhere to test there should be at least one non trap sent");
  }

  @Test
  public void shouldReturnBufferWithPoll() throws IOException, InterruptedException {

    PDU trap = createTrap("1.2.3.4.5", "some string");
    snmp.send(trap, target, null, null);
    assertFalse(this.task.poll().isEmpty(), "There should be trap in buffer");
    assertTrue(this.task.getRecordBuffer().isEmpty(), "Buffer should be empty after polling.");

  }

  @Test
  public void shouldRespectBatchSizeWithPoll() throws IOException, InterruptedException {
    // Batch size is 10 in test config

    int i;

    for (i = 0; i < 15; i++) {
      PDU trap = createTrap("1.2.3.4.5", "some string");
      snmp.send(trap, target, null, null);
    }

    Thread.sleep(5000);
    assertEquals(i, task.getRecordBuffer().size(), "Sent traps should be equal to buffered records before poll");

    List<SourceRecord> poll = this.task.poll();

    int batchSize = this.task.getConfig().batchSize;
    assertFalse(this.task.getRecordBuffer().isEmpty(), "There should be remaining records in the buffer as it exceeds batchSize");
    assertEquals(poll.size(), batchSize, "There should be equal amount of records polled as batchSize");
    assertEquals(i - batchSize, this.task.getRecordBuffer().size(), "There should be rest of the records over batchSize in buffer");

  }


}