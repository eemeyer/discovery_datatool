package com.t11e.discovery.datatool;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.t11e.discovery.datatool.column.BooleanColumnProcessor;
import com.t11e.discovery.datatool.column.DateColumnProcessor;
import com.t11e.discovery.datatool.column.IColumnProcessor;
import com.t11e.discovery.datatool.column.JsonColumnProcessor;
import com.t11e.discovery.datatool.column.StringColumnProcessor;
import com.t11e.discovery.datatool.column.TimeColumnProcessor;
import com.t11e.discovery.datatool.column.TimestampColumnProcessor;

public class ResultSetConvertor
{
  private final boolean lowerCaseColumnNames;
  private final Set<String> jsonColumns;
  private ResultSetMetaData metadata;
  private IColumnProcessor[] columnProcessors;

  public ResultSetConvertor(final boolean lowerCaseColumnNames, final Set<String> jsonColumns)
  {
    this.lowerCaseColumnNames = lowerCaseColumnNames;
    this.jsonColumns = jsonColumns != null ? jsonColumns : Collections.<String> emptySet();
  }

  public Map<String, Object> getRowAsMap(final ResultSet rs)
    throws SQLException
  {
    lazyInitialize(rs);
    final Map<String, Object> properties;
    properties = new LinkedHashMap<String, Object>();
    for (int column = 1; column < columnProcessors.length; column++)
    {
      final IColumnProcessor columnProcessor = columnProcessors[column];
      if (columnProcessor != null)
      {
        String name = metadata.getColumnName(column);
        if (lowerCaseColumnNames)
        {
          name = name.toLowerCase();
        }
        final Object value = columnProcessor.processColumn(rs, column);
        if (value != null)
        {
          properties.put(name, value);
        }
      }
    }
    return properties;
  }

  private void lazyInitialize(final ResultSet rs)
    throws SQLException
  {
    if (metadata == null)
    {
      metadata = rs.getMetaData();
      columnProcessors = getColumnProcessors(metadata);
    }
  }

  private IColumnProcessor[] getColumnProcessors(
    final ResultSetMetaData md)
    throws SQLException
  {
    final IColumnProcessor[] output = new IColumnProcessor[md.getColumnCount() + 1];
    for (int column = 1; column < output.length; column++)
    {
      output[column] = getColumnProcessor(md, column);
    }
    return output;
  }

  private IColumnProcessor getColumnProcessor(
    final ResultSetMetaData md,
    final int column)
    throws SQLException
  {
    IColumnProcessor output;
    switch (md.getColumnType(column))
    {
      case java.sql.Types.BIT:
      case java.sql.Types.BOOLEAN:
        output = BooleanColumnProcessor.INSTANCE;
        break;
      case java.sql.Types.TINYINT:
      case java.sql.Types.SMALLINT:
      case java.sql.Types.INTEGER:
      case java.sql.Types.BIGINT:
      case java.sql.Types.FLOAT:
      case java.sql.Types.REAL:
      case java.sql.Types.DOUBLE:
      case java.sql.Types.NUMERIC:
      case java.sql.Types.DECIMAL:
        output = StringColumnProcessor.INSTANCE;
        break;
      case java.sql.Types.CHAR:
      case java.sql.Types.VARCHAR:
      case java.sql.Types.LONGVARCHAR:
      case java.sql.Types.CLOB:
      {
        final String columnName = md.getColumnName(column);
        if (columnName != null && jsonColumns.contains(columnName.toLowerCase()))
        {
          output = JsonColumnProcessor.INSTANCE;
        }
        else
        {
          output = StringColumnProcessor.INSTANCE;
        }
        break;
      }
      case java.sql.Types.DATE:
        output = DateColumnProcessor.INSTANCE;
        break;
      case java.sql.Types.TIME:
        output = TimeColumnProcessor.INSTANCE;
        break;
      case java.sql.Types.TIMESTAMP:
        output = TimestampColumnProcessor.INSTANCE;
        break;
      case java.sql.Types.BINARY:
      case java.sql.Types.VARBINARY:
      case java.sql.Types.LONGVARBINARY:
      case java.sql.Types.NULL:
      case java.sql.Types.OTHER:
      case java.sql.Types.JAVA_OBJECT:
      case java.sql.Types.DISTINCT:
      case java.sql.Types.STRUCT:
      case java.sql.Types.ARRAY:
      case java.sql.Types.BLOB:
      case java.sql.Types.REF:
      case java.sql.Types.DATALINK:
      default:
        output = null;
        break;
    }
    return output;
  }
}
