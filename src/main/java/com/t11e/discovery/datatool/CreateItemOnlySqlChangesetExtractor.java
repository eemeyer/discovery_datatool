package com.t11e.discovery.datatool;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class CreateItemOnlySqlChangesetExtractor
  implements ChangesetExtractor
{
  private List<SqlAction> sqlActions;
  private NamedParameterJdbcTemplate jdbcTemplate;
  private String type;

  @Override
  public void writeChangeset(
    final ChangesetWriter writer,
    final String changesetType,
    final Date start,
    final Date end)
  {
    for (final SqlAction sqlAction : sqlActions)
    {
      process(writer, sqlAction, changesetType, start, end);
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
    final RowCallbackHandler callbackHandler =
        new CreateActionRowCallbackHandler(
          jdbcTemplate,
          writer,
          sqlAction.getIdColumn(),
          sqlAction.getIdPrefix(),
          sqlAction.getIdSuffix(),
          sqlAction.isUseLowerCaseColumnNames(),
          sqlAction.getJsonColumnNames(),
          sqlAction.getSubqueries());
    jdbcTemplate.query(sqlAction.getQuery(), params, callbackHandler);
  }

  @Override
  public String getType()
  {
    return type;
  }

  @Override
  public boolean hasTypeOverride()
  {
    return true;
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

  @Required
  public void setType(final String type)
  {
    this.type = type;
  }
}
