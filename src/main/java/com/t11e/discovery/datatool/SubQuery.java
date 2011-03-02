package com.t11e.discovery.datatool;

public class SubQuery
{
  public enum Type
  {
    ARRAY,
    DELIMITED
  }

  private final Type type;
  private final String query;
  private final String field;
  private final String delimiter;
  private final String discriminator;

  public SubQuery(final Type type, final String query, final String field, final String delimiter,
    final String discriminator)
  {
    this.type = type;
    this.query = query;
    this.field = field;
    this.delimiter = delimiter;
    this.discriminator = discriminator;
  }

  public Type getType()
  {
    return type;
  }

  public String getQuery()
  {
    return query;
  }

  public String getField()
  {
    return field;
  }

  public String getDelimiter()
  {
    return delimiter;
  }

  public String getDiscriminator()
  {
    return discriminator;
  }
}
