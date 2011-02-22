package com.t11e.discovery.datatool;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class SqlChangesetExtractor
  implements ChangesetExtractor
{
  private List<SqlAction> sqlActions;
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Override
  public void writeChangeset(
    final ChangesetWriter writer,
    final String changesetType,
    final Date start,
    final Date end)
  {
    for (final SqlAction sqlAction : sqlActions)
    {
      final Set<String> filters = sqlAction.getFilter();
      if (filters.contains("any") || filters.contains(changesetType))
      {
        process(writer, sqlAction, changesetType, start, end);
      }
    }
  }

  private void process(
    final ChangesetWriter writer,
    final SqlAction sqlAction,
    final String kind,
    final Date start,
    final Date end)
  {
    final Map<String, Object> params = new HashMap<String, Object>();
    params.put("start", start);
    params.put("end", end);
    params.put("kind", kind);
    final RowCallbackHandler callbackHandler;
    if ("create".equals(sqlAction.getAction()))
    {
      callbackHandler =
          new CreateActionRowCallbackHandler(
            jdbcTemplate,
            writer,
            sqlAction.getIdColumn(),
            sqlAction.getIdPrefix(),
            sqlAction.getIdSuffix(),
            sqlAction.isUseLowerCaseColumnNames(),
            sqlAction.getJsonColumnNames(),
            sqlAction.getSubqueries());
    }
    else if ("delete".equals(sqlAction.getAction()))
    {
      callbackHandler =
          new DeleteActionRowCallbackHandler(writer, sqlAction.getIdColumn());
    }
    else
    {
      throw new RuntimeException("Unknown action: " + sqlAction.getAction());
    }
    jdbcTemplate.query(sqlAction.getQuery(), params, callbackHandler);
  }

  @Override
  public boolean hasTypeOverride()
  {
    return false;
  }

  @Override
  public String getType()
  {
    return null;
  }

  @Required
  public void setDataSource(final DataSource dataSource)
  {
    jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  @Required
  public void setActions(final List<SqlAction> actions)
  {
    sqlActions = actions;
  }
}
