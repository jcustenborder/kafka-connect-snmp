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

import com.github.jcustenborder.kafka.connect.utils.data.SourceRecordConcurrentLinkedDeque;
import org.apache.kafka.common.utils.SystemTime;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SnmpTrapSourceTask extends SourceTask implements CommandResponder {
  static final Logger log = LoggerFactory.getLogger(SnmpTrapSourceTask.class);


  @Override
  public String version() {
    return VersionUtil.getVersion();
  }

  SnmpTrapSourceConnectorConfig config;
  AbstractTransportMapping<?> transport;
  ThreadPool threadPool;
  MessageDispatcher messageDispatcher;
  Snmp snmp;
  PDUConverter converter;
  Time time = new SystemTime();
  private SourceRecordConcurrentLinkedDeque recordBuffer;

  @Override
  public void start(Map<String, String> settings) {
    this.config = new SnmpTrapSourceConnectorConfig(settings);
    this.converter = new PDUConverter(this.time, config);
    this.recordBuffer = new SourceRecordConcurrentLinkedDeque(this.config.batchSize, 0);
    log.info("start() - Setting listen address with {} on {}:{}", this.config.listenProtocol, this.config.listenAddress, this.config.listenPort);

    this.transport = setupTransport(this.config.listenAddress, this.config.listenProtocol, this.config.listenPort);

    log.info("start() - Configuring ThreadPool DispatchPool to {} thread(s)", this.config.dispatcherThreadPoolSize);
    this.threadPool = ThreadPool.create("DispatchPool", this.config.dispatcherThreadPoolSize);
    this.messageDispatcher = createMessageDispatcher(this.threadPool);

    try {
      this.transport.listen();
    } catch (IOException e) {
      throw new ConnectException("Exception thrown while calling transport.listen()", e);
    }

    this.snmp = new Snmp(this.messageDispatcher, this.transport);
    this.snmp.addCommandResponder(this);
  }


  @Override
  public List<SourceRecord> poll() throws InterruptedException {
    List<SourceRecord> records = new ArrayList<>(this.config.batchSize);

    while (!this.recordBuffer.drain(records)) {
      log.trace("poll() - no records found sleeping {} ms.", this.config.pollBackoffMs);
      this.time.sleep(this.config.pollBackoffMs);
    }

    log.debug("poll() - returning {} record(s).", records.size());
    return records;
  }

  @Override
  public void stop() {
    log.trace("stop() - stopping threadpool");
    this.threadPool.stop();

    log.trace("stop() - closing transport.");
    try {
      this.transport.close();
    } catch (IOException e) {
      log.error("Exception thrown while closing transport.", e);
    }

  }

  @Override
  public void processPdu(CommandResponderEvent event) {
    log.trace("processPdu() - Received event from {}", event.getPeerAddress());
    PDU pdu = event.getPDU();

    if (null == pdu) {
      log.warn("Null PDU received from {}", event.getPeerAddress());
      return;
    }

    if (PDU.TRAP != pdu.getType()) {
      log.trace("Message received from {} was not a trap. message={}", event.getPeerAddress(), event);
      return;
    }

    SourceRecord sourceRecord = converter.convert(event);
    this.recordBuffer.add(sourceRecord);
  }

  private static AbstractTransportMapping<?> setupTransport(String address, String listenProtocol, int port) {
    InetAddress inetAddress = setupAddress(address);

    try {
      if ("UDP".equals(listenProtocol)) {
        return setupUdpTransport(inetAddress, port);
      } else {
        return setupTcpTransport(inetAddress, port);
      }
    } catch (IOException ex) {
      throw new ConnectException("Exception thrown while configuring transport.", ex);
    }
  }

  private static DefaultUdpTransportMapping setupUdpTransport(InetAddress addr, int port) throws IOException {
    UdpAddress udpAddress = new UdpAddress(addr, port);
    return new DefaultUdpTransportMapping(udpAddress);
  }

  private static DefaultTcpTransportMapping setupTcpTransport(InetAddress addr, int port) throws IOException {
    TcpAddress tcpAddress = new TcpAddress(addr, port);
    return new DefaultTcpTransportMapping(tcpAddress);
  }

  private static InetAddress setupAddress(String listenAddress) throws ConnectException {
    try {
      return InetAddress.getByName(listenAddress);
    } catch (UnknownHostException e) {
      throw new ConnectException("Exception thrown while trying to resolve " + listenAddress, e);
    }
  }

  private static void setupSecurityProtocols() {
    SecurityProtocols.getInstance().addDefaultProtocols();
    SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());
  }

  private static MessageDispatcher createMessageDispatcher(ThreadPool threadPool) {
    // TODO Add MPv3 (usm, processingmodel)
    MultiThreadedMessageDispatcher md = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());
    md.addMessageProcessingModel(new MPv1());
    md.addMessageProcessingModel(new MPv2c());
    setupSecurityProtocols();
    return md;
  }

  public SourceRecordConcurrentLinkedDeque getRecordBuffer() {
    return recordBuffer;
  }

  public SnmpTrapSourceConnectorConfig getConfig() {
    return config;
  }
}