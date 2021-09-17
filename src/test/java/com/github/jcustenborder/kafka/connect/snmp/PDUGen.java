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

import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;


public class PDUGen {
  public static VariableBinding createCustomVarBinding(String oidString, String oidVal) {
    OID oid = new OID(oidString);
    Variable var = new OctetString(oidVal);
    return new VariableBinding(oid, var);
  }

  public static PDU createNonTrap(String oidStr, String oidVal) {
    PDU pdu = new PDU();
    pdu.setType(PDU.INFORM);
    pdu.add(createCustomVarBinding(oidStr, oidVal));
    return pdu;
  }

  private static <T extends PDU> void addSomeVariableBindingsAndType(T pdu, String oidStr) {
    OID oid = new OID(oidStr);

    pdu.setType(PDU.TRAP);
    pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, oid));
    pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(5000))); // put your uptime here
    pdu.add(new VariableBinding(SnmpConstants.sysDescr, new OctetString("System Description")));

  }

  public static ScopedPDU createV3Trap(String oidStr, String oidVal) {
    ScopedPDU pdu = new ScopedPDU();
    addSomeVariableBindingsAndType(pdu, oidStr);
    pdu.add(createCustomVarBinding(oidStr, oidVal));
    return pdu;
  }

  public static PDU createV2Trap(String oidStr, String oidVal) {

    PDU pdu = new PDU();
    addSomeVariableBindingsAndType(pdu, oidStr);
    pdu.add(createCustomVarBinding(oidStr, oidVal));
    return pdu;
  }
}
