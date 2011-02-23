package com.t11e.discovery.datatool;

import java.io.InputStream;
import java.util.Collections;

import org.junit.Test;

public class BulkAndFullChangesetTest
  extends EndToEndTestBase
{

  @Override
  protected InputStream getConfigurationXml()
  {
    return getClass().getResourceAsStream("BulkAndFullChangesetTest.xml");
  }

  @Override
  protected String[] getSetupScripts()
  {
    return new String[]{"BulkAndFullChangesetTestCreate.sql"};
  }

  @Override
  protected String[] getCleanupScripts()
  {
    return new String[]{"BulkAndFullChangesetTestDrop.sql"};
  }

  @Test
  public void testBulk()
  {
    assertChangeset("test-bulk", "", "bulk",
      CollectionsFactory.makeList("1", "2", "3"),
      Collections.<String> emptyList(), false);
  }

  @Test
  public void testBulkAndDelta()
  {
    assertChangeset("test-bulk-and-delta", "", "bulk",
      CollectionsFactory.makeList("1", "2", "3"),
      Collections.<String> emptyList(), false);
  }

  @Test
  public void testFull()
  {
    assertChangeset("test-full", "", "full",
      CollectionsFactory.makeList("1", "2", "3"),
      Collections.<String> emptyList(), false);
  }

  @Test
  public void testFullAndDelta()
  {
    assertChangeset("test-full-and-delta", "", "full",
      CollectionsFactory.makeList("1", "2", "3"),
      Collections.<String> emptyList(), false);
  }

  @Test
  public void testSnapshot()
  {
    assertChangeset("test-snapshot", "", "snapshot",
      CollectionsFactory.makeList("1", "2", "3"),
      CollectionsFactory.makeList("4", "5"), false);
  }

  @Test
  public void testSnapshotAndDelta()
  {
    assertChangeset("test-snapshot-and-delta", "", "snapshot",
      CollectionsFactory.makeList("1", "2", "3"),
      CollectionsFactory.makeList("4", "5"), false);
  }

}
