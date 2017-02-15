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
import org.snmp4j.smi.TransportIpAddress;
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
  AbstractTransportMapping transport;
  ThreadPool threadPool;
  MessageDispatcher messageDispatcher;
  Snmp snmp;
  PDUConverter converter;
  Time time = new SystemTime();
  SourceRecordConcurrentLinkedDeque recordBuffer;

  @Override
  public void start(Map<String, String> settings) {
    this.config = new SnmpTrapSourceConnectorConfig(settings);
    this.converter = new PDUConverter(this.time, config);
    this.recordBuffer = new SourceRecordConcurrentLinkedDeque();
    log.trace("start() - Setting listen address to {}", this.config.listenAddress);
    InetAddress inetAddress;
    try {
      inetAddress = InetAddress.getByName(this.config.listenAddress);
    } catch (UnknownHostException e) {
      throw new ConnectException("Exception thrown while trying to resolve " + this.config.listenAddress, e);
    }

    TransportIpAddress transportAddress;
    try {
      if ("UDP".equals(this.config.listenProtocol)) {
        transportAddress = new UdpAddress(inetAddress, this.config.listenPort);
        this.transport = new DefaultUdpTransportMapping((UdpAddress) transportAddress);
      } else {
        transportAddress = new TcpAddress(inetAddress, this.config.listenPort);
        this.transport = new DefaultTcpTransportMapping((TcpAddress) transportAddress);
      }
    } catch (IOException ex) {
      throw new ConnectException("Exception thrown while configuring transport.", ex);
    }
    log.trace("start() - Transport IP Address configured to {}", transportAddress);
    log.info("Transport IP Address configured to {}", transportAddress);

    log.trace("start() - Configuring ThreadPool DispatchPool to {} thread(s)", this.config.dispatcherThreadPoolSize);
    this.threadPool = ThreadPool.create("DispatchPool", this.config.dispatcherThreadPoolSize);

    this.messageDispatcher = new MultiThreadedMessageDispatcher(this.threadPool, new MessageDispatcherImpl());
    this.messageDispatcher.addMessageProcessingModel(new MPv1());
    this.messageDispatcher.addMessageProcessingModel(new MPv2c());
    SecurityProtocols.getInstance().addDefaultProtocols();
    SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());

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

    log.info("{}", event);

    SourceRecord sourceRecord = converter.convert(event);
    this.recordBuffer.add(sourceRecord);
  }
}