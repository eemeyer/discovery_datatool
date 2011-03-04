package com.t11e.discovery.datatool;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

public class CreateActionRowCallbackHandler
  implements RowCallbackHandler
{
  private static final Logger logger = Logger.getLogger(CreateActionRowCallbackHandler.class.getName());
  private final ChangesetWriter writer;
  private final String idColumn;
  private final String idPrefix;
  private final String idSuffix;
  private final List<SubQuery> subqueries;
  private final NamedParameterJdbcOperations jdbcTemplate;
  private final ResultSetConvertor resultSetConvertor;
  private final List<ResultSetConvertor> subqueryConvertors;
  private final boolean shouldRecordTimings;
  private long totalTime;
  private int numSubQueries;

  public CreateActionRowCallbackHandler(
    final NamedParameterJdbcOperations jdbcTemplate,
    final ChangesetWriter writer,
    final String idColumn,
    final String idPrefix,
    final String idSuffix,
    final boolean lowerCaseColumnNames,
    final Set<String> jsonColumns,
    final List<SubQuery> subqueries,
    final boolean shouldRecordTimings)
  {
    this.jdbcTemplate = jdbcTemplate;
    this.writer = writer;
    this.idColumn = idColumn;
    this.idPrefix = idPrefix;
    this.idSuffix = idSuffix;
    this.shouldRecordTimings = shouldRecordTimings;
    this.subqueries = subqueries != null ? subqueries : Collections.<SubQuery> emptyList();
    resultSetConvertor = new ResultSetConvertor(lowerCaseColumnNames, jsonColumns);
    if (this.subqueries.isEmpty())
    {
      subqueryConvertors = Collections.emptyList();
    }
    else
    {
      subqueryConvertors = new ArrayList<ResultSetConvertor>(this.subqueries.size());
      for (int i = 0; i < this.subqueries.size(); ++i)
      {
        subqueryConvertors.add(new ResultSetConvertor(lowerCaseColumnNames, Collections.<String> emptySet()));
      }
    }
  }

  @Override
  public void processRow(final ResultSet rs)
    throws SQLException
  {
    final String id = getId(rs);
    final Map<String, Object> properties = resultSetConvertor.getRowAsMap(rs);
    for (int i = 0; i < subqueries.size(); ++i)
    {
      final SubQuery subquery = subqueries.get(i);
      final List<Object> values = new ArrayList<Object>();
      {
        final StopWatch watch = StopWatchHelper.startTimer(shouldRecordTimings);
        jdbcTemplate.query(subquery.getQuery(), properties,
          new SubqueryRowCallbackHandler(values, subqueryConvertors.get(i)));
        recordQueryTime(watch);
      }
      if (!values.isEmpty())
      {
        if (StringUtils.isNotBlank(subquery.getDiscriminator()))
        {
          final Map<String, Object> groupedbyDiscriminator = new LinkedHashMap<String, Object>();
          for (final Object value : values)
          {
            @SuppressWarnings("unchecked")
            final Map<String, Object> row = (Map<String, Object>) value;
            final String discriminatorValue = (String) row.remove(subquery.getDiscriminator());
            if (discriminatorValue != null)
            {
              groupedbyDiscriminator.put(discriminatorValue, row);
            }
          }
          if (!groupedbyDiscriminator.isEmpty())
          {
            properties.put(subquery.getField(), groupedbyDiscriminator);
          }
        }
        else
        {
          final Object value;
          if (values.size() == 1)
          {
            value = values.get(0);
          }
          else
          {
            value = SubQuery.Type.DELIMITED.equals(subquery.getType())
              ? StringUtils.join(values, subquery.getDelimiter())
              : values;
          }
          properties.put(subquery.getField(), value);
        }
      }
    }
    try
    {
      writer.setItem(id, properties);
    }
    catch (final XMLStreamException e)
    {
      throw new RuntimeException(e);
    }
  }

  private void recordQueryTime(final StopWatch watch)
  {
    if (shouldRecordTimings && watch != null)
    {
      watch.stop();
      totalTime += watch.getTime();
      ++numSubQueries;
      if (logger.isLoggable(Level.FINEST))
      {
        logger.finest("Subquery took [" + watch.getTime() + "]ms [" + watch + "]");
      }
    }
  }

  private String getId(final ResultSet rs)
    throws SQLException
  {
    final String id;
    {
      final StringBuilder builder = new StringBuilder();
      if (StringUtils.isNotBlank(idPrefix))
      {
        builder.append(idPrefix);
      }
      builder.append(rs.getString(idColumn));
      if (StringUtils.isNotBlank(idSuffix))
      {
        builder.append(idSuffix);
      }
      id = builder.toString();
    }
    return id;
  }

  public long getTotalTime()
  {
    return totalTime;
  }

  public int getNumSubQueries()
  {
    return numSubQueries;
  }

  private static final class SubqueryRowCallbackHandler
    implements RowCallbackHandler
  {
    private final List<Object> values;
    private final ResultSetConvertor convertor;

    private SubqueryRowCallbackHandler(final List<Object> values, final ResultSetConvertor convertor)
    {
      this.values = values;
      this.convertor = convertor;
    }

    @Override
    public void processRow(final ResultSet rs)
      throws SQLException
    {
      final Map<String, Object> row = convertor.getRowAsMap(rs);
      if (row.size() == 1)
      {
        values.add(row.entrySet().iterator().next().getValue());
      }
      else
      {
        values.add(row);
      }
    }
  }
}
