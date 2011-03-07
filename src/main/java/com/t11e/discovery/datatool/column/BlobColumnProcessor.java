package com.t11e.discovery.datatool.column;

import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;

public class BlobColumnProcessor
  implements IColumnProcessor
{
  public static final BlobColumnProcessor INSTANCE =
      new BlobColumnProcessor();

  @Override
  public String processColumn(final ResultSet rs, final int column)
    throws SQLException
  {
    String output = null;
    final Blob value = rs.getBlob(column);
    if (!rs.wasNull())
    {
      try
      {
        output = IOUtils.toString(value.getBinaryStream(), "utf8").trim();
      }
      catch (final IOException e)
      {
        throw new RuntimeException(e);
      }
    }
    return output;
  }
}
