package com.t11e.discovery.datatool;

import java.io.InputStream;
import java.util.Arrays;

import org.dom4j.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class VerticalTableTest
  extends EndToEndTestBase
{
  @Override
  protected InputStream getConfigurationXml()
  {
    return getClass().getResourceAsStream("VerticalTableTest.xml");
  }

  @Before
  public void setup()
  {
    executeSqlScripts("VerticalTableTestCreate.sql");
  }

  @After
  public void teardown()
  {
    executeSqlScripts("VerticalTableTestDrop.sql");
  }

  @Test
  public void testSnapshot()
  {
    final Document doc = assertChangeset("test-simple", "", "snapshot",
      Arrays.asList("1", "2"),
      Arrays.asList("4", "5"),
      false);
    Assert.assertEquals("red",
      doc.selectSingleNode(
        "/changeset/set-item[@id='1']/properties/struct/entry[@name='color']/string/text()")
        .getText());

    Assert.assertEquals(
      Arrays.asList("orange", "yellow"),
      nodesAsStrings(doc,
        "/changeset/set-item[@id='1']/properties/struct/entry[@name='color']/array/element/string/text()"));

  }
}
