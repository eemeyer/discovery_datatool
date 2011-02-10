package com.t11e.discovery.datatool;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

public class CreateActionRowCallbackHandler
  implements RowCallbackHandler
{
  private final ChangesetWriter writer;
  private final String idColumn;
  private final String idPrefix;
  private final String idSuffix;
  private final List<SubQuery> subqueries;
  private final NamedParameterJdbcOperations jdbcTemplate;
  private final ResultSetConvertor resultSetConvertor;
  private final List<ResultSetConvertor> subqueryConvertors;

  public CreateActionRowCallbackHandler(
    final NamedParameterJdbcOperations jdbcTemplate,
    final ChangesetWriter writer,
    final String idColumn,
    final String idPrefix,
    final String idSuffix,
    final boolean lowerCaseColumnNames,
    final Set<String> jsonColumns,
    final List<SubQuery> subqueries)
  {
    this.jdbcTemplate = jdbcTemplate;
    this.writer = writer;
    this.idColumn = idColumn;
    this.idPrefix = idPrefix;
    this.idSuffix = idSuffix;
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
      final List<String> values = new ArrayList<String>();
      jdbcTemplate.query(subquery.getQuery(), properties,
        new SubqueryRowCallbackHandler(values, subquery, subqueryConvertors.get(i)));
      if (!values.isEmpty())
      {
        final Object value = SubQuery.Type.DELIMITED.equals(subquery.getType())
          ? StringUtils.join(values, subquery.getDelimiter())
          : values;
        properties.put(subquery.getField(), value);
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

  private static final class SubqueryRowCallbackHandler
    implements RowCallbackHandler
  {
    private final List<String> values;
    private final SubQuery subquery;
    private final ResultSetConvertor convertor;

    private SubqueryRowCallbackHandler(final List<String> values, final SubQuery subquery,
      final ResultSetConvertor convertor)
    {
      this.values = values;
      this.subquery = subquery;
      this.convertor = convertor;
    }

    @Override
    public void processRow(final ResultSet rs)
      throws SQLException
    {
      final Map<String, Object> row = convertor.getRowAsMap(rs);
      if (row.size() != 1)
      {
        throw new RuntimeException("Subquery returned more than one column. "
          + StringUtils.trimToEmpty(subquery.getQuery()));
      }
      values.add((String) row.entrySet().iterator().next().getValue());
    }
  }
}
