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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PDUConverter {
  private static final Logger log = LoggerFactory.getLogger(PDUConverter.class);
  private final Time time;
  static final Schema KEY_SCHEMA = SchemaBuilder.struct()
          .name("com.github.jcustenborder.kafka.connect.snmp.TrapKey")
          .field(KeySchemaConstants.FIELD_PEER_ADDRESS, SchemaBuilder.string().doc("Remote address of the host sending the trap.").build())
          .build();
  static final Schema VARIABLE_BINDING_SCHEMA = SchemaBuilder.struct()
          .name("com.github.jcustenborder.kafka.connect.snmp.VariableBinding")
          .field(VariableBindingConstants.FIELD_OID, SchemaBuilder.string().doc("OID.").build())
          .field(VariableBindingConstants.FIELD_TYPE, SchemaBuilder.string().doc("Syntax type for variable binding.").build())
          .field(VariableBindingConstants.FIELD_COUNTER32, SchemaBuilder.int32().doc("Counter32 value.").optional().build())
          .field(VariableBindingConstants.FIELD_COUNTER64, SchemaBuilder.int64().doc("Counter64 value.").optional().build())
          .field(VariableBindingConstants.FIELD_GAUGE32, SchemaBuilder.int32().doc("Gauge32 value.").optional().build())
          .field(VariableBindingConstants.FIELD_INTEGER, SchemaBuilder.int32().doc("Integer value.").optional().build())
          .field(VariableBindingConstants.FIELD_IPADDRESS, SchemaBuilder.string().doc("IpAddress value.").optional().build())
          .field(VariableBindingConstants.FIELD_NULL, SchemaBuilder.string().doc("null value.").optional().build())
          .field(VariableBindingConstants.FIELD_OBJECTIDENTIFIER, SchemaBuilder.string().doc("OID value.").optional().build())
          .field(VariableBindingConstants.FIELD_OCTETSTRING, SchemaBuilder.string().doc("Octet string value.").optional().build())
          .field(VariableBindingConstants.FIELD_OPAQUE, SchemaBuilder.string().doc("opaque value.").optional().build())
          .field(VariableBindingConstants.FIELD_TIMETICKS, SchemaBuilder.int32().doc("timeticks value.").optional().build())
          .build();
  static final Schema VALUE_SCHEMA = SchemaBuilder.struct()
          .name("com.github.jcustenborder.kafka.connect.snmp.Trap")
          .field(ValueSchemaConstants.FIELD_PEER_ADDRESS, SchemaBuilder.string().doc("Remote address of the host sending the trap.").build())
          .field(ValueSchemaConstants.FIELD_SECURITY_NAME, SchemaBuilder.string().doc("Community name the event was sent to.").build())
          .field(ValueSchemaConstants.FIELD_VARIABLES, SchemaBuilder.array(VARIABLE_BINDING_SCHEMA).doc("Variables for this trap.").build())
          .build();

  private final SnmpTrapSourceConnectorConfig config;

  public PDUConverter(Time time, SnmpTrapSourceConnectorConfig config) {
    this.time = time;
    this.config = config;
  }

  static class KeySchemaConstants {
    public static final String FIELD_PEER_ADDRESS = "peerAddress";
  }


  static class ValueSchemaConstants {
    public static final String FIELD_PEER_ADDRESS = "peerAddress";
    public static final String FIELD_SECURITY_NAME = "securityName";
    public static final String FIELD_VARIABLES = "variables";
  }

  static class VariableBindingConstants {
    public static final String FIELD_OID = "oid";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_COUNTER32 = "counter32";
    public static final String FIELD_COUNTER64 = "counter64";
    public static final String FIELD_GAUGE32 = "gauge32";
    public static final String FIELD_INTEGER = "integer";
    public static final String FIELD_IPADDRESS = "ipaddress";
    public static final String FIELD_NULL = "null";
    public static final String FIELD_OBJECTIDENTIFIER = "objectIdentifier";
    public static final String FIELD_OCTETSTRING = "octetString";
    public static final String FIELD_OPAQUE = "opaque";
    public static final String FIELD_TIMETICKS = "timeticks";
  }

  Struct convertVariableBinding(VariableBinding binding) {
    log.trace("convertVariableBinding() - converting {}", binding);
    Struct struct = new Struct(VARIABLE_BINDING_SCHEMA);

    final String oid = binding.getOid().toDottedString();
    log.trace("convertVariableBinding() - oid = '{}'", oid);
    struct.put(VariableBindingConstants.FIELD_OID, oid);

    final Variable variable = binding.getVariable();
    final String syntaxType;
    final Object value;

    log.trace("convertVariableBinding() - binding.getSyntax() = '{}'", binding.getSyntax());
    switch (binding.getSyntax()) {
      case SMIConstants.SYNTAX_COUNTER32:
        syntaxType = VariableBindingConstants.FIELD_COUNTER32;
        value = variable.toInt();
        break;
      case SMIConstants.SYNTAX_COUNTER64:
        syntaxType = VariableBindingConstants.FIELD_COUNTER64;
        value = variable.toLong();
        break;
      case SMIConstants.SYNTAX_GAUGE32:
        syntaxType = VariableBindingConstants.FIELD_GAUGE32;
        value = variable.toInt();
        break;
      case SMIConstants.SYNTAX_INTEGER:
        syntaxType = VariableBindingConstants.FIELD_INTEGER;
        value = variable.toInt();
        break;
      case SMIConstants.SYNTAX_IPADDRESS:
        syntaxType = VariableBindingConstants.FIELD_IPADDRESS;
        value = variable.toString();
        break;
      case SMIConstants.SYNTAX_NULL:
        syntaxType = VariableBindingConstants.FIELD_NULL;
        value = null;
        break;
      case SMIConstants.SYNTAX_OBJECT_IDENTIFIER:
        syntaxType = VariableBindingConstants.FIELD_OBJECTIDENTIFIER;
        value = variable.toString();
        break;
      case SMIConstants.SYNTAX_OCTET_STRING:
        syntaxType = VariableBindingConstants.FIELD_OCTETSTRING;
        value = variable.toString();
        break;
      case SMIConstants.SYNTAX_OPAQUE:
        syntaxType = VariableBindingConstants.FIELD_OPAQUE;
        value = variable.toString();
        break;
      case SMIConstants.SYNTAX_TIMETICKS:
        syntaxType = VariableBindingConstants.FIELD_TIMETICKS;
        value = variable.toInt();
        break;
      default:
        throw new UnsupportedOperationException(
                String.format("%s is an unsupported syntaxType.", binding.getSyntax())
        );
    }

    log.trace("convertVariableBinding() - syntaxType = '{}'", syntaxType);
    struct.put(VariableBindingConstants.FIELD_TYPE, syntaxType);
    log.trace("convertVariableBinding() - Setting field '{}' to '{}'.", syntaxType, value);
    struct.put(syntaxType, value);
    struct.validate();
    return struct;
  }

  static final Map<String, Object> EMPTY = ImmutableMap.of();

  public SourceRecord convert(CommandResponderEvent<?> event) {
    Struct key = new Struct(KEY_SCHEMA);
    Struct value = new Struct(VALUE_SCHEMA);

    final PDU pdu = event.getPDU();

    final String peerAddress = event.getPeerAddress().toString();
    final String securityName = new String(event.getSecurityName(), Charsets.UTF_8);
    log.trace("convert() - peerAddress = '{}'", peerAddress);
    log.trace("convert() - securityName = '{}'", securityName);

    key.put(KeySchemaConstants.FIELD_PEER_ADDRESS, peerAddress);
    value.put(ValueSchemaConstants.FIELD_PEER_ADDRESS, peerAddress);
    value.put(ValueSchemaConstants.FIELD_SECURITY_NAME, securityName);

    List<VariableBinding> pduContents = Arrays.asList(pdu.toArray());

    if (!pduContents.isEmpty()) {
      AtomicInteger ai = new AtomicInteger(0);

      List<Struct> bindingStructs = pduContents
              .stream()
              .map((vb) -> {
                log.trace("convert() - processing VariableBinding({})", ai.getAndIncrement());
                return convertVariableBinding(vb);
              })
              .collect(Collectors.toList());

      log.trace("convert() - Setting {} variables to {}", bindingStructs.size(), ValueSchemaConstants.FIELD_VARIABLES);
      value.put(ValueSchemaConstants.FIELD_VARIABLES, bindingStructs);
    }

    return new SourceRecord(
            EMPTY,
            EMPTY,
            this.config.topic,
            null,
            KEY_SCHEMA,
            key,
            VALUE_SCHEMA,
            value,
            this.time.milliseconds()
    );
  }

}
