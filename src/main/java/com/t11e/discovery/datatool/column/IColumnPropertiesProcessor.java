package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public interface IColumnPropertiesProcessor
{
  public void processColumn(final Map<String, Object> target, ResultSet rs, int column, String propname)
    throws SQLException;
}
