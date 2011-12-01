package com.t11e.discovery.datatool;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.t11e.discovery.datatool.column.BooleanColumnProcessor;
import com.t11e.discovery.datatool.column.ColumnPropertiesProcessor;
import com.t11e.discovery.datatool.column.DateColumnProcessor;
import com.t11e.discovery.datatool.column.IColumnPropertiesProcessor;
import com.t11e.discovery.datatool.column.JsonColumnProcessor;
import com.t11e.discovery.datatool.column.LowerCaseStringColumnProcessor;
import com.t11e.discovery.datatool.column.StringColumnProcessor;
import com.t11e.discovery.datatool.column.TimeColumnProcessor;
import com.t11e.discovery.datatool.column.TimestampColumnProcessor;
import com.t11e.discovery.datatool.column.UnscopedJsonPropertiesProcessor;
import com.t11e.discovery.datatool.column.UpperCaseStringColumnProcessor;

public class ResultSetConvertor
{
  private final PropertyCase propertyCase;
  private final Set<String> scopedJsonColumns;
  private final Set<String> unscopedJsonColumns;
  private final Set<String> changeValueCaseColumns;
  private IColumnPropertiesProcessor[][] columnProcessors;
  private String[] columnNames;

  public ResultSetConvertor(final PropertyCase propertyCase, final Set<String> scopedJsonColumns,
    final Set<String> unscopedJsonColumns,
    final Set<String> potentialChangeValueCaseColumns)
  {
    this.propertyCase = propertyCase;
    if (PropertyCase.LEGACY != propertyCase && PropertyCase.PRESERVE != propertyCase && potentialChangeValueCaseColumns != null)
    {
      changeValueCaseColumns = new HashSet<String>(potentialChangeValueCaseColumns.size());
      for (final String columnLabel : potentialChangeValueCaseColumns)
      {
        changeValueCaseColumns.add(propertyCase.convert(columnLabel));
      }
    }
    else
    {
      changeValueCaseColumns = Collections.emptySet();
    }
    this.scopedJsonColumns = scopedJsonColumns != null ? scopedJsonColumns : Collections.<String> emptySet();
    this.unscopedJsonColumns = unscopedJsonColumns != null ? unscopedJsonColumns : Collections.<String> emptySet();
  }

  public Map<String, Object> getRowAsMap(final ResultSet rs)
    throws SQLException
  {
    lazyInitialize(rs);
    final Map<String, Object> properties;
    properties = new LinkedHashMap<String, Object>();
    for (int idx = 0; idx < columnProcessors.length; ++idx)
    {
      final int column = idx + 1;
      final IColumnPropertiesProcessor[] processors = columnProcessors[idx];
      if (processors != null)
      {
        final String name = columnNames[idx];
        for (final IColumnPropertiesProcessor processor : processors)
        {
          processor.processColumn(properties, rs, column, name);
        }
      }
    }
    return properties;
  }

  private void lazyInitialize(final ResultSet rs)
    throws SQLException
  {
    if (columnProcessors == null)
    {
      final ResultSetMetaData metaData = rs.getMetaData();
      final IColumnPropertiesProcessor[][] processors = new IColumnPropertiesProcessor[metaData.getColumnCount()][];
      final String[] names = new String[processors.length];
      for (int idx = 0; idx < processors.length; idx++)
      {
        final int column = idx + 1;
        final String columnName = metaData.getColumnLabel(column);
        names[idx] = propertyCase.convert(columnName);
        processors[idx] = getColumnProcessor(metaData, column, names[idx]);
      }
      columnProcessors = processors;
      columnNames = names;
    }
  }

  private IColumnPropertiesProcessor[] getColumnProcessor(
    final ResultSetMetaData md,
    final int column,
    final String columnLabel)
    throws SQLException
  {
    IColumnPropertiesProcessor[] output;
    switch (md.getColumnType(column))
    {
      case java.sql.Types.BIT:
      case java.sql.Types.BOOLEAN:
        output = new ColumnPropertiesProcessor[]{new ColumnPropertiesProcessor(BooleanColumnProcessor.INSTANCE)};
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
        output = new ColumnPropertiesProcessor[]{new ColumnPropertiesProcessor(StringColumnProcessor.INSTANCE)};
        break;
      case java.sql.Types.CHAR:
      case java.sql.Types.VARCHAR:
      case java.sql.Types.LONGVARCHAR:
      case java.sql.Types.CLOB:
      {
        final String columnLabelLower = StringUtils.lowerCase(columnLabel);
        if (columnLabel != null &&
          (scopedJsonColumns.contains(columnLabelLower)
          || unscopedJsonColumns.contains(columnLabelLower)))
        {
          final List<IColumnPropertiesProcessor> jsonProcessors = new ArrayList<IColumnPropertiesProcessor>(2);
          if (scopedJsonColumns.contains(columnLabelLower))
          {
            jsonProcessors.add(new ColumnPropertiesProcessor(JsonColumnProcessor.INSTANCE));
          }
          if (unscopedJsonColumns.contains(columnLabelLower))
          {
            jsonProcessors.add(new UnscopedJsonPropertiesProcessor(JsonColumnProcessor.INSTANCE, propertyCase));
          }
          output = jsonProcessors.toArray(new IColumnPropertiesProcessor[jsonProcessors.size()]);
        }
        else if (columnLabel != null && changeValueCaseColumns.contains(columnLabel))
        {
          switch (propertyCase)
          {
            case LOWER:
              output = new ColumnPropertiesProcessor[]{new ColumnPropertiesProcessor(
                LowerCaseStringColumnProcessor.INSTANCE)};
              break;
            case UPPER:
              output = new ColumnPropertiesProcessor[]{new ColumnPropertiesProcessor(
                UpperCaseStringColumnProcessor.INSTANCE)};
              break;
            default:
              output = new ColumnPropertiesProcessor[]{new ColumnPropertiesProcessor(StringColumnProcessor.INSTANCE)};
              break;
          }
        }
        else
        {
          output = new ColumnPropertiesProcessor[]{new ColumnPropertiesProcessor(StringColumnProcessor.INSTANCE)};
        }
        break;
      }
      case java.sql.Types.DATE:
        output = new ColumnPropertiesProcessor[]{new ColumnPropertiesProcessor(DateColumnProcessor.INSTANCE)};
        break;
      case java.sql.Types.TIME:
        output = new ColumnPropertiesProcessor[]{new ColumnPropertiesProcessor(TimeColumnProcessor.INSTANCE)};
        break;
      case java.sql.Types.TIMESTAMP:
        output = new ColumnPropertiesProcessor[]{new ColumnPropertiesProcessor(TimestampColumnProcessor.INSTANCE)};
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
