package com.t11e.discovery.datatool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.userdetails.memory.InMemoryDaoImpl;

public class ExampleConfigurationTest
{
  private ConfigurationManager configurationManager;
  private List<File> configFiles;

  @Before
  public void setup()
  {
    configFiles = Arrays.asList(new File("stage/examples").listFiles(new FilenameFilter()
    {
      public boolean accept(final File dir, final String filename)
      {
        return filename.endsWith(".xml");
      }
    }));
    Assert.assertFalse(configFiles.isEmpty());
  }

  @Test
  public void testConfigOnBoot()
  {
    for (final File configFile : configFiles)
    {
      testConfigOnBoot(configFile);
    }
  }

  @Test
  public void testConfigSwapBoot()
    throws IOException
  {
    {
      final File configFile = File.createTempFile("exampleConfigTest", ".xml");
      FileUtils.copyFile(configFiles.get(configFiles.size() - 1), configFile);
      testConfigOnBoot(configFile);
    }
    for (final File configFile : configFiles)
    {
      testConfigSwap(configFile, false);
    }
    for (final File configFile : configFiles)
    {
      testConfigSwap(configFile, true);
    }
  }

  public void testConfigOnBoot(final File configFile)
  {
    configurationManager = new ConfigurationManager();
    configurationManager.setWorkingDirectory("stage");
    configurationManager.setConfigurationFile(configFile.getPath());
    configurationManager.setBypassAuthenticationFilter(new BypassAuthenticationFilter());
    configurationManager.setInMemoryDaoImpl(new InMemoryDaoImpl());
    configurationManager.onPostConstruct();
    Assert.assertNotNull(configurationManager.getBean(ChangesetPublisherManager.class));
  }

  private void testConfigSwap(final File configFile, final boolean persist)
    throws FileNotFoundException
  {
    final boolean persisted =
      configurationManager.loadConfiguration(new FileInputStream(configFile), persist);
    if (persist)
    {
      Assert.assertTrue("Configuration was not saved", persisted);
    }
    else
    {
      Assert.assertFalse("Configuration should not have been saved", persisted);
    }
  }
}
