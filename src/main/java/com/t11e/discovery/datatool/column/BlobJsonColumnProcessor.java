package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

import com.t11e.discovery.datatool.JsonUtil;

public class BlobJsonColumnProcessor
  implements IColumnProcessor
{
  public static final IColumnProcessor INSTANCE = new BlobJsonColumnProcessor();

  @Override
  public Object processColumn(final ResultSet rs, final int column)
    throws SQLException
  {
    Object result = null;
    final String value = BlobColumnProcessor.INSTANCE.processColumn(rs, column);
    if (StringUtils.isNotBlank(value))
    {
      try
      {
        result = JsonUtil.decode(value);
      }
      catch (final Exception e)
      {
        // Swallow
      }
    }
    return result;
  }
}
