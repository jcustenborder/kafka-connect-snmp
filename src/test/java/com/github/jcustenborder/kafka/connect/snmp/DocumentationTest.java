package com.github.jcustenborder.kafka.connect.snmp;

import com.github.jcustenborder.kafka.connect.utils.BaseDocumentationTest;
import org.apache.kafka.connect.data.Schema;

import java.util.Arrays;
import java.util.List;

public class DocumentationTest extends BaseDocumentationTest {
  @Override
  protected List<Schema> schemas() {
    return Arrays.asList(PDUConverter.KEY_SCHEMA, PDUConverter.VALUE_SCHEMA, PDUConverter.VARIABLE_BINDING_SCHEMA);
  }

  @Override
  protected String[] packages() {
    return new String[]{this.getClass().getPackage().getName()};
  }
}
