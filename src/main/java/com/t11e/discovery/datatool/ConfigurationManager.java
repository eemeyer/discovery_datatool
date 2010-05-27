package com.t11e.discovery.datatool;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationManager
{
  private ConfigurableApplicationContext currentContext;
  @Resource(name="configurationDirectory")
  private String configurationDirectory;

  @PostConstruct
  public void onPostConstruct()
  {
    final File configFile = new File(new File(configurationDirectory), "config.xml");
    if (configFile.exists())
    {
      try
      {
        loadConfiguration(new FileInputStream(configFile), false);
      }
      catch (final FileNotFoundException e)
      {
        throw new RuntimeException(e);
      }
    }
  }

  public void loadConfiguration(
    final InputStream is,
    final boolean persist)
  {
    byte[] config;
    try
    {
      config = IOUtils.toByteArray(is);
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }
    IOUtils.closeQuietly(is);
    final GenericApplicationContext newContext = createApplicationContext(new ByteArrayInputStream(config));
    newContext.start();

    synchronized(this)
    {
      if (persist)
      {
        swapConfigFiles(config);
      }
      if (currentContext != null)
      {
        currentContext.stop();
        currentContext.close();
        currentContext = null;
      }
      currentContext = newContext;
    }
  }

  private void swapConfigFiles(final byte[] config)
  {
    try
    {
      final File newConfig = File.createTempFile("config", ".xml", new File(configurationDirectory));
      FileUtils.writeByteArrayToFile(newConfig, config);
      final File configFile = new File(new File(configurationDirectory), "config.xml");
      if (configFile.exists())
      {
        FileUtils.moveFile(configFile, new File(new File(configurationDirectory), "config.xml.bak"));
      }
      FileUtils.moveFile(newConfig, configFile);
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public ChangesetPublisher getChangesetPublisher(final String name)
  {
    ChangesetPublisher result = null;
    synchronized(this)
    {
      if (currentContext != null)
      {
        final ChangesetPublisherManager mgr = currentContext.getBean(ChangesetPublisherManager.class);
        result = mgr.getChangesetPublisher(name);
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static GenericApplicationContext createApplicationContext(final InputStream is)
  {
    final GenericApplicationContext applicationContext = new GenericApplicationContext();

    final SAXReader xmlReader = new SAXReader();
    final Document document;
    try
    {
      document = xmlReader.read(is);
    }
    catch (final DocumentException e)
    {
      throw new RuntimeException(e);
    }
    {
      for (final Node node : (List<Node>) document.selectNodes("/config/dataSources/dataSource"))
      {
        final String name = node.valueOf("@name");
        final String clazz = node.valueOf("@class");
        final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        for (final Node child : (List<Node>) node.selectNodes("*"))
        {
          builder.addPropertyValue(
            child.getName(), StringUtils.trimToEmpty(child.getText()));
        }
        applicationContext.registerBeanDefinition("dataSource-" + name, builder.getBeanDefinition());
      }
    }
    {
      final List<ChangesetPublisher> publishers = new ArrayList<ChangesetPublisher>();
      for (final Node node : (List<Node>) document.selectNodes("/config/publishers/sqlPublisher"))
      {
        final List<SqlAction> actions = new ArrayList<SqlAction>();
        for (final Node action : (List<Node>) node.selectNodes("action"))
        {
          final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SqlAction.class);
          builder.addPropertyValue("action", action.valueOf("@type"));
          builder.addPropertyValue("filter", action.valueOf("@filter"));
          builder.addPropertyValue("query", StringUtils.trimToEmpty(action.valueOf("query/text()")));
          builder.addPropertyValue("idColumn", action.valueOf("@idColumn"));
          builder.addPropertyValue("jsonColumnNames", action.valueOf("@jsonColumnNames"));
          final String beanName = "SqlAction-" + System.identityHashCode(builder);
          applicationContext.registerBeanDefinition(beanName, builder.getBeanDefinition());
          actions.add(applicationContext.getBean(beanName, SqlAction.class));
        }
        BeanDefinition sqlChangesetExtractor;
        {
          final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SqlChangesetExtractor.class);
          builder.addPropertyReference("dataSource", "dataSource-" + node.valueOf("@dataSource"));
          builder.addPropertyValue("actions", actions);
          sqlChangesetExtractor = builder.getBeanDefinition();
        }
        {
          final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ChangesetPublisher.class);
          final String name = node.valueOf("@name");
          builder.addPropertyValue("name", name);
          builder.addPropertyValue("changesetExtractor", sqlChangesetExtractor);
          final String beanName = "Publisher-" + name;
          applicationContext.registerBeanDefinition(beanName, builder.getBeanDefinition());
          publishers.add(applicationContext.getBean(beanName, ChangesetPublisher.class));
        }
      }
      {
        final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ChangesetPublisherManager.class);
        builder.addPropertyValue("publishers", publishers);
        applicationContext.registerBeanDefinition("ChangesetPublisherManager", builder.getBeanDefinition());
      }
    }
    applicationContext.refresh();
    return applicationContext;
  }
}
