package com.github.jcustenborder.kafka.connect.snmp;

import org.apache.kafka.common.utils.SystemTime;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.smi.Address;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.jcustenborder.kafka.connect.snmp.PDUGen.createTrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class PDUConverterTest {

  private CommandResponderEvent<Address> event;
  private Address addr;
  private PDUConverter converter;

  @BeforeEach
  public void setup(){
    event = (CommandResponderEvent<Address>) mock(CommandResponderEvent.class);
    addr = mock(Address.class);

    Map<String, String> settings = SnmpTrapSourceConnectorConfigTest.settings();
    SnmpTrapSourceConnectorConfig conf = new SnmpTrapSourceConnectorConfig(settings);

    converter = new PDUConverter(new SystemTime(), conf);
  }

  @Test
  public void testConvert() {

    String oid = "1.2.3.4.5";
    String oidv = "string";
    PDU pdu = createTrap(oid, oidv);


    when(event.getPDU()).thenReturn(pdu);
    when(event.getSecurityName()).thenReturn("secName".getBytes(StandardCharsets.UTF_8));

    when(addr.toString()).thenReturn("0.0.0.0:1234");
    when(event.getPeerAddress()).thenReturn(addr);

    SourceRecord convert = converter.convert(event);

    assertNotNull(convert.key(), "Key should not be null after conversion");
    assertNotNull(convert.value(), "Value should not be null after conversion");

    Struct struct = (Struct) convert.value();

    List<Struct> fieldVariables = ((List<Struct>) struct.get(PDUConverter.ValueSchemaConstants.FIELD_VARIABLES));

    Set<Object> octetStrings = fieldVariables
            .stream()
            .map(e -> e.get(PDUConverter.VariableBindingConstants.FIELD_OCTETSTRING))
            .collect(Collectors.toSet());

    Set<Object> oids = fieldVariables
            .stream()
            .map(e -> e.get(PDUConverter.VariableBindingConstants.FIELD_OID))
            .collect(Collectors.toSet());

    assertNotNull(struct.get(PDUConverter.ValueSchemaConstants.FIELD_PEER_ADDRESS), "Peer address should be included in the converted sourceRecord");
    assertNotNull(struct.get(PDUConverter.ValueSchemaConstants.FIELD_SECURITY_NAME), "Security name should be included in the converted sourceRecord");

    assertTrue(oids.contains(oid), "Conversion should contain the custom oid in trap");
    assertTrue(octetStrings.contains(oidv), "Conversion should contain the octetString in trap");

    assertEquals(pdu.getAll().size(), fieldVariables.size(), "All field variables should be included");

  }

}