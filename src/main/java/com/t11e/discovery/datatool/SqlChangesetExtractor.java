package com.t11e.discovery.datatool;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class SqlChangesetExtractor
  implements ChangesetExtractor
{
  private Collection<SqlAction> filteredActions = Collections.emptyList();
  private Collection<SqlAction> completeActions = Collections.emptyList();
  private Collection<SqlAction> incrementalActions = Collections.emptyList();
  private NamedParameterJdbcTemplate jdbcTemplate;
  private String completeActionType;

  @Override
  public void writeChangeset(
    final ChangesetWriter writer,
    final String changesetType,
    final Date start,
    final Date end)
  {
    for (final SqlAction action : filteredActions)
    {
      final Set<String> filters = action.getFilter();
      if (filters.contains("any") || filters.contains(changesetType))
      {
        process(writer, action, changesetType, start, end);
      }
    }
    if (start == null)
    {
      for (final SqlAction action : completeActions)
      {
        process(writer, action, changesetType, start, end);
      }
    }
    else
    {
      for (final SqlAction action : incrementalActions)
      {
        process(writer, action, changesetType, start, end);
      }
    }
  }

  @Override
  public String determineType(final Date start)
  {
    String result = start == null ? "snapshot" : "delta";
    if (!completeActions.isEmpty())
    {
      if (start == null)
      {
        result = completeActionType;
      }
      else if (incrementalActions.isEmpty())
      {
        result = completeActionType;
      }
    }
    return result;
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

  @Required
  public void setDataSource(final DataSource dataSource)
  {
    jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  @Required
  public void setFilteredActions(final Collection<SqlAction> actions)
  {
    filteredActions = actions;
  }

  @Required
  public void setCompleteActions(final Collection<SqlAction> completeActions)
  {
    this.completeActions = completeActions;
    if (!completeActions.isEmpty())
    {
      final Set<String> filter = completeActions.iterator().next().getFilter();
      if (!filter.isEmpty())
      {
        completeActionType = filter.iterator().next();
      }
    }
    else
    {
      completeActionType = "";
    }
  }

  @Required
  public void setIncrementalActions(final Collection<SqlAction> incrementalActions)
  {
    this.incrementalActions = incrementalActions;
  }
}
