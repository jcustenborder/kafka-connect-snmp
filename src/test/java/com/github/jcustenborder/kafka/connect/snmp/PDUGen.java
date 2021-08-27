package com.github.jcustenborder.kafka.connect.snmp;

import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;


public class PDUGen {
  public static VariableBinding createVarBinding(String oidString, String oidVal) {
    OID oid = new OID(oidString);
    Variable var = new OctetString(oidVal);
    return new VariableBinding(oid, var);
  }

  public static PDU createNonTrap(String oidStr, String oidVal) {
    PDU pdu = new PDU();
    pdu.setType(PDU.INFORM);
    pdu.add(createVarBinding(oidStr, oidVal));
    return pdu;
  }

  public static PDU createTrap(String oidStr, String oidVal) {

    PDU pdu = new PDU();
    pdu.setType(PDU.TRAP);
    OID oid = new OID(oidStr);

    pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, oid));
    pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(5000))); // put your uptime here
    pdu.add(new VariableBinding(SnmpConstants.sysDescr, new OctetString("System Description")));
    pdu.add(createVarBinding(oidStr, oidVal));


    return pdu;
  }
}
