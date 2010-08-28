package com.t11e.discovery.datatool;

import java.io.StringWriter;

import joptsimple.OptionParser;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;

public class WebServerDaemon
  implements Daemon
{
  private WebServerMain main;

  public void init(final DaemonContext context)
    throws Exception
  {
    final String[] args = context.getArguments();
    final OptionParser parser = new OptionParser();
    try
    {
      main = WebServerMain.fromArgs(parser, args);
      main.start();
    }
    catch (final Exception e)
    {
      final StringWriter writer = new StringWriter();
      parser.printHelpOn(writer);
      context.getController().fail(e.getLocalizedMessage() +
        " Usage: " + WebServerDaemon.class.getName() + writer.getBuffer());
    }
  }

  public void start()
    throws Exception
  {
    main.start();
  }

  public void stop()
    throws Exception
  {
    main.stop();
  }

  public void destroy()
  {
    main.destroy();
    main = null;
  }
}
