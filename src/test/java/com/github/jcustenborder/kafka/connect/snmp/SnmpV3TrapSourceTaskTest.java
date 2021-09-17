/**
 * Copyright © 2021 Elisa Oyj
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.DefaultCounterListener;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.github.jcustenborder.kafka.connect.snmp.PDUGen.createV2Trap;
import static com.github.jcustenborder.kafka.connect.snmp.PDUGen.createV3Trap;
import static org.junit.jupiter.api.Assertions.*;

public class SnmpV3TrapSourceTaskTest {
  private SnmpTrapSourceTask task;
  private CommunityTarget<Address> v2Target;

  private List<UserTarget<Address>> validV3Targets;
  private List<UserTarget<Address>> invalidV3Targets;

  private Snmp sendingSnmp;

  private final String defaultPrivacyPassphrase = "_0987654321_";
  private final String defaultAuthPassphrase = "_12345678_";

  private DefaultCounterListener defaultCounterListener;


  List<User> validUsers = List.of(
      new User("user1", defaultPrivacyPassphrase, defaultAuthPassphrase, AuthMD5.ID),
      new User("user2", defaultPrivacyPassphrase, defaultAuthPassphrase, AuthSHA.ID)
  );
  List<User> invalidUsers = List.of(
      new User("fake", defaultPrivacyPassphrase, defaultAuthPassphrase)
  );

  static {
    SNMP4JSettings.setForwardRuntimeExceptions(true);
    SNMP4JSettings.setSnmp4jStatistics(SNMP4JSettings.Snmp4jStatistics.extended);
    try {
      setupBeforeClass();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @BeforeAll
  public static void setupBeforeClass() throws Exception {
    SNMP4JSettings.setExtensibilityEnabled(true);
    SecurityProtocols.getInstance().addDefaultProtocols();
  }

  @AfterAll
  public static void tearDownAfterClass() throws Exception {
    SecurityProtocols.setSecurityProtocols(null);
    SNMP4JSettings.setExtensibilityEnabled(false);
  }

  private static class User {
    public String username;
    public String authPassphrase;
    public String privacyPassphrase;
    public OID authProtocol = AuthMD5.ID;
    public OID privacyProtocol = PrivDES.ID;

    public User(String username, String privacyPassphrase, String authPassphrase) {
      this.username = username;
      this.authPassphrase = authPassphrase;
      this.privacyPassphrase = privacyPassphrase;
    }

    public User(String username, String authPassphrase, String privacyPassphrase, OID authProtocol) {
      this.username = username;
      this.authPassphrase = authPassphrase;
      this.privacyPassphrase = privacyPassphrase;
      this.authProtocol = authProtocol;
    }

    public User(String username, String authPassphrase, String privacyPassphrase, OID authProtocol, OID privacyProtocol) {
      this.username = username;
      this.authPassphrase = authPassphrase;
      this.privacyPassphrase = privacyPassphrase;
      this.authProtocol = authProtocol;
      this.privacyProtocol = privacyProtocol;
    }

    public UsmUser toUsm() {
      return new UsmUser(
          new OctetString(this.username),
          this.authProtocol,
          new OctetString(this.privacyPassphrase),
          privacyProtocol,
          new OctetString(this.privacyPassphrase)
      );
    }
  }

  private List<UsmUser> convertUsers(List<User> users) {
    return users.stream().map(User::toUsm).collect(Collectors.toList());
  }

  private void addUsmsToSending(List<UsmUser> users) {
    users.forEach(usmUser -> {
      sendingSnmp.getUSM().addUser(usmUser);
    });
  }

  private void addUsmsToReceiving(List<UsmUser> users) {
    users.forEach((usmUser) -> {
      this.task.getSnmp().getUSM().addUser(usmUser);
    });
  }

  private void addUsms() {
    addUsmsToSending(convertUsers(validUsers));
    addUsmsToReceiving(convertUsers(validUsers));

    // Let's add a invalid user only for sending to verify functionality
    addUsmsToSending(convertUsers(invalidUsers));
  }

  private static CommunityTarget<Address> createCommunityTarget(Address address, String community) {
    CommunityTarget<Address> communityTarget = new CommunityTarget<>();
    communityTarget.setCommunity(new OctetString(community));
    communityTarget.setVersion(SnmpConstants.version2c);
    communityTarget.setAddress(address);
    return communityTarget;
  }

  private static UserTarget<Address> createUserTarget(Address address, String username, byte[] authorativeEngineId) {
    UserTarget<Address> ut = new UserTarget<>(address, new OctetString(username), authorativeEngineId);
    ut.setVersion(SnmpConstants.version3);
    return ut;
  }

  @BeforeEach
  public void setUp() throws Exception {
    task = new SnmpTrapSourceTask();
    Map<String, String> settings = SnmpTrapSourceConnectorConfigTest.settingsV3();
    task.start(settings);

    // Specify receiver
    String addr = String.format("127.0.0.1/%s", SnmpTrapSourceConnectorConfigTest.listeningPort);
    Address targetAddress = new UdpAddress(addr);

    this.validV3Targets = validUsers.stream()
        .map((u) -> createUserTarget(targetAddress, u.username, new byte[0]))
        .collect(Collectors.toList());

    this.invalidV3Targets = invalidUsers.stream()
        .map((u) -> createUserTarget(targetAddress, u.username, new byte[0]))
        .collect(Collectors.toList());

    v2Target = createCommunityTarget(targetAddress, "public");

    sendingSnmp = new Snmp(new DefaultUdpTransportMapping());
    setupMpv3(sendingSnmp, "generator");

    addUsms();

    // Add counters
    defaultCounterListener = new DefaultCounterListener();
    sendingSnmp.getCounterSupport().addCounterListener(defaultCounterListener);

    assertNotSame(this.sendingSnmp.getLocalEngineID(), this.task.getSnmp().getLocalEngineID());
  }

  private void setupMpv3(Snmp snmp, String engineId) {
    SecurityModels securityModels = new SecurityModels() {
    };
    MPv3 mpv3 = (MPv3) snmp.getMessageDispatcher().getMessageProcessingModel(MPv3.ID);
    mpv3.setLocalEngineID(MPv3.createLocalEngineID(new OctetString(engineId)));
    USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(mpv3.getLocalEngineID()), 0);
    securityModels.addSecurityModel(usm);
    mpv3.setSecurityModels(securityModels);
  }

  @AfterEach
  public void tearDown() throws Exception {
    sendingSnmp.close();
    task.stop();
  }

  @Test
  public void usmsShouldBeSeparate() {
    assertNotSame(this.sendingSnmp.getUSM(), this.task.getSnmp().getUSM());
  }

  @Test
  public void shouldBufferV3Traps() throws InterruptedException, IOException {

    int i;

    for (i = 0; i < 20; i++) {

      int randomTarget = new Random().nextInt(this.validV3Targets.size());
      PDU trap = createV3Trap("1.2.3.4.5", "some string");
      sendingSnmp.send(trap, this.validV3Targets.get(randomTarget));
    }

    Thread.sleep(2500);
    assertEquals(i, task.getRecordBuffer().size(), "Sent traps should be equal to buffered records");
  }

  @Test
  public void shouldNotBufferTrapsWithInvalidUsers() throws IOException, InterruptedException {
    // should get rejected by "remote" task.snmp
    PDU trap = createV3Trap("1.2.3.4.5", "some string");

    invalidV3Targets.forEach((target) -> {
      try {
        sendingSnmp.send(trap, target);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    Thread.sleep(1000);
    assertEquals(invalidV3Targets.size(), defaultCounterListener.remove(SnmpConstants.usmStatsUnknownUserNames).getValue());

    assertTrue(this.task.getRecordBuffer().isEmpty());
  }

  @Test
  public void shouldNotBufferTrapsWithInvalidUsers2() throws IOException {
    // should result in client error
    PDU trap = createV3Trap("1.2.3.4.5", "some string");
    this.sendingSnmp.getUSM().removeAllUsers();

    this.validV3Targets.forEach((target) -> {
      assertThrows(org.snmp4j.MessageException.class, () -> sendingSnmp.send(trap, target));
    });

  }

  @Test
  public void shouldBufferMixOfTraps() throws InterruptedException, IOException {

    CompletableFuture<Integer> v3s = CompletableFuture.supplyAsync(() -> {
      Integer i = null;
      for (i = 0; i < 10; i++) {
        ScopedPDU trap = createV3Trap("1.2.3.4.5", "some string");
        try {
          int randomTarget = new Random().nextInt(this.validV3Targets.size());
          sendingSnmp.send(trap, this.validV3Targets.get(randomTarget));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return i;
    });


    CompletableFuture<Integer> invalidV3s = CompletableFuture.supplyAsync(() -> {
      Integer i = null;
      for (i = 0; i < 5; i++) {
        PDU trap = createV3Trap("1.2.3.4.5", "some string");
        try {
          int randomTarget = new Random().nextInt(this.invalidV3Targets.size());
          sendingSnmp.send(trap, invalidV3Targets.get(randomTarget));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return i;
    });

    CompletableFuture<Integer> v2s = CompletableFuture.supplyAsync(() -> {
      Integer i = null;
      for (i = 0; i < 10; i++) {
        PDU trap = createV2Trap("1.2.3.4.5", "some string");
        try {
          sendingSnmp.send(trap, v2Target);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return i;
    });


    // These are only valid traps
    Integer totalSent = v3s.thenCombine(v2s, Integer::sum).join();

    // Couple traps with invalid user as well is sent
    Integer invalidSent = invalidV3s.join();

    // Wait for a while so the receiver can catch up
    Thread.sleep(2500);
    assertEquals(totalSent, this.task.getRecordBuffer().size(), "All traps sent with valid user should be in buffer.");

    List<SourceRecord> poll = this.task.poll();
    assertEquals(poll.size(), SnmpTrapSourceConnectorConfigTest.batchSize);
    assertEquals(this.task.getRecordBuffer().size(), totalSent - SnmpTrapSourceConnectorConfigTest.batchSize, "After poll only total minus batch should remain.");

    // invalid usms should show up in SnmpConstants as unknownUserNames
    assertEquals((long) invalidSent, defaultCounterListener.remove(SnmpConstants.usmStatsUnknownUserNames).getValue(), "There should be counters incremented for invalid V3s");
    assertTrue(invalidSent > 0, "There should've been invalid user traps as well.");
  }

  private void resetUsms(USM sendingUsm, USM receivingUsm) {

    assertNotSame(receivingUsm, sendingUsm);

    sendingUsm.removeAllUsers();
    receivingUsm.removeAllUsers();

    assertTrue(sendingUsm.getUserTable().getUserEntries().isEmpty());
    assertTrue(receivingUsm.getUserTable().getUserEntries().isEmpty());
  }

  @Test
  public void shouldNotBufferInvalidPassphraseTraps() throws InterruptedException, IOException {
    USM sendingUsm = this.sendingSnmp.getUSM();
    USM receivingUsm = this.task.getSnmp().getUSM();

    resetUsms(sendingUsm, receivingUsm);

    String username = "user";
    User sendingUser = new User(username, "privacypass0", "authpass0000");
    User receivingUser = new User(username, "00000000000", "000000000000");

    assertNotSame(sendingUser, receivingUser);

    sendingUsm.addUser(sendingUser.toUsm());
    receivingUsm.addUser(receivingUser.toUsm());

    // Specify receiver
    Address address = new UdpAddress(String.format("127.0.0.1/%s", SnmpTrapSourceConnectorConfigTest.listeningPort));

    UserTarget<Address> authPrivTarget = createUserTarget(address, sendingUser.username, new byte[0]);
    authPrivTarget.setSecurityLevel(SecurityLevel.AUTH_PRIV);

    UserTarget<Address> authNoPrivTarget = createUserTarget(address, sendingUser.username, new byte[0]);
    authNoPrivTarget.setSecurityLevel(SecurityLevel.AUTH_NOPRIV);

    ScopedPDU trap = createV3Trap("1.2.3.4.5", "some string");

    sendingSnmp.send(trap, authPrivTarget);
    sendingSnmp.send(trap, authNoPrivTarget);

    Thread.sleep(1000);

    assertTrue(this.task.getRecordBuffer().isEmpty());
    assertEquals(2L, this.defaultCounterListener.remove(SnmpConstants.usmStatsWrongDigests).getValue());
  }

}
