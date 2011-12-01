package com.t11e.discovery.datatool.column;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.t11e.discovery.datatool.PropertyCase;

public class UnscopedJsonPropertiesProcessor
  implements IColumnPropertiesProcessor
{
  private final JsonColumnProcessor delegate;
  private final PropertyCase propertyCase;

  public UnscopedJsonPropertiesProcessor(final JsonColumnProcessor delegate, final PropertyCase propertyCase)
  {
    this.delegate = delegate;
    this.propertyCase = propertyCase;
  }

  @Override
  public void processColumn(final Map<String, Object> target, final ResultSet rs, final int column,
    final String propname)
    throws SQLException
  {
    final Object value = delegate.processColumn(rs, column);
    if (value != null)
    {
      if (value instanceof Map)
      {
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map) value;
        if (propertyCase != PropertyCase.PRESERVE)
        {
          for (final String key : new ArrayList<String>(map.keySet()))
          {
            final String newKey = propertyCase.convert(key);
            if (!StringUtils.equals(key, newKey))
            {
              map.put(newKey, map.remove(key));
            }
          }
        }
        target.putAll(map);
      }
      else
      {
        target.put(propname, value);
      }
    }
  }

}
