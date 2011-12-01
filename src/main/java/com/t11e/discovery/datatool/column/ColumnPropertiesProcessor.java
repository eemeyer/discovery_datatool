package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class ColumnPropertiesProcessor
  implements IColumnPropertiesProcessor
{
  private final IColumnProcessor delegate;

  public ColumnPropertiesProcessor(final IColumnProcessor delegate)
  {
    this.delegate = delegate;
  }

  @Override
  public void processColumn(final Map<String, Object> target, final ResultSet rs, final int column,
    final String propname)
    throws SQLException
  {
    final Object value = delegate.processColumn(rs, column);
    if (value != null)
    {
      target.put(propname, value);
    }
  }

}
