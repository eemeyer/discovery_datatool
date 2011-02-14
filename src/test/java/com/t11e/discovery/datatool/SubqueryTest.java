package com.t11e.discovery.datatool;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.xml.stream.XMLStreamException;

import org.dom4j.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class SubqueryTest
  extends IntegrationTestBase
{
  @Override
  protected InputStream getConfigurationXml()
  {
    return getClass().getResourceAsStream("SubqueryTest.xml");
  }

  @Before
  public void setup()
  {
    executeSqlScripts("SubqueryTestCreate.sql");
  }

  @After
  public void teardown()
  {
    executeSqlScripts("SubqueryTestDrop.sql");
  }

  @Test
  public void testSnapshotWithVarcharSubquery()
  {
    final Document doc = assertChangeset("test-snapshot-varchar", "", "snapshot",
      Arrays.asList("1", "2", "3"),
      Arrays.asList("4", "5"),
      false);

    Assert.assertEquals("red",
      doc.selectSingleNode("/changeset/set-item[@id='1']/properties/struct/entry[@name='color']/string/text()")
        .getText());
    Assert.assertEquals("orange,yellow",
      doc.selectSingleNode("/changeset/set-item[@id='2']/properties/struct/entry[@name='color']/string/text()")
        .getText());
    Assert.assertNull(doc.selectSingleNode("/changeset/set-item[@id='3']/properties/struct/entry[@name='color']"));

    Assert.assertEquals(
      "red",
      doc.selectSingleNode(
        "/changeset/set-item[@id='1']/properties/struct/entry[@name='color-delimited']/string/text()")
        .getText());
    Assert.assertEquals(
      "orange|yellow",
      doc.selectSingleNode(
        "/changeset/set-item[@id='2']/properties/struct/entry[@name='color-delimited']/string/text()")
        .getText());
    Assert.assertNull(doc
      .selectSingleNode("/changeset/set-item[@id='3']/properties/struct/entry[@name='color-delimited']"));

    assertColorArraySubquery(doc, "color-default");
    assertColorArraySubquery(doc, "color-array");

  }

  private void assertColorArraySubquery(final Document doc, final String fieldName)
  {
    Assert.assertEquals("red",
      doc.selectSingleNode(
        "/changeset/set-item[@id='1']/properties/struct/entry[@name='" + fieldName + "']/array/element/string/text()")
        .getText());
    Assert.assertEquals(2,
      doc.selectNodes(
        "/changeset/set-item[@id='2']/properties/struct/entry[@name='" + fieldName + "']/array/element/string/text()")
        .size());
    Assert.assertEquals(
      Arrays.asList("orange", "yellow"),
      nodesAsStrings(doc,
        "/changeset/set-item[@id='2']/properties/struct/entry[@name='" + fieldName + "']/array/element/string/text()"));
    Assert.assertNull(doc
      .selectSingleNode("/changeset/set-item[@id='3']/properties/struct/entry[@name='" + fieldName + "']"));
  }

  @Test
  public void testSnapshotWithIntSubquery()
  {
    final Document doc = assertChangeset("test-snapshot-int", "", "snapshot",
      Arrays.asList("1", "2", "3"),
      Arrays.asList("4", "5"),
      false);
    Assert.assertEquals("10",
      doc.selectSingleNode("/changeset/set-item[@id='1']/properties/struct/entry[@name='color']/string/text()")
        .getText());
    Assert.assertEquals("20,30",
      doc.selectSingleNode("/changeset/set-item[@id='2']/properties/struct/entry[@name='color']/string/text()")
        .getText());
    Assert.assertNull(doc.selectSingleNode("/changeset/set-item[@id='3']/properties/struct/entry[@name='color']"));
  }

  @Test
  public void testSnapshotWithTimestampSubquery()
  {
    final Document doc = assertChangeset("test-snapshot-timestamp", "", "snapshot",
      Arrays.asList("1", "2", "3"),
      Arrays.asList("4", "5"),
      false);
    Assert.assertEquals("2011-01-01T00:00:00.000",
      doc.selectSingleNode("/changeset/set-item[@id='1']/properties/struct/entry[@name='color']/string/text()")
        .getText());
    Assert.assertEquals("2011-01-02T00:00:00.000,2011-01-03T00:00:00.000",
      doc.selectSingleNode("/changeset/set-item[@id='2']/properties/struct/entry[@name='color']/string/text()")
        .getText());
    Assert.assertNull(doc.selectSingleNode("/changeset/set-item[@id='3']/properties/struct/entry[@name='color']"));
  }

  @Test
  public void testInvalidSubQuery()
    throws XMLStreamException, IOException
  {
    try
    {
      changesetController.publish(new MockHttpServletRequest(), new MockHttpServletResponse(),
          "test-snapshot-invalid", null, null, "", false, false);
      Assert.fail("Subquery that returns multiple columns should cause exception.");
    }
    catch (final RuntimeException e)
    {
      Assert.assertTrue(e.getMessage().contains("Subquery returned more than one column."));
    }
  }
}
