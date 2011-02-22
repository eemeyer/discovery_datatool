package com.t11e.discovery.datatool;

import java.util.Date;

public interface ChangesetExtractor
{
  void writeChangeset(
    ChangesetWriter writer,
    String changesetType,
    Date start,
    Date end);

  /**
   * If this ChangesetExtractor only supports a single type of changeset and wants to tell the client its overridden type.
   */
  boolean hasTypeOverride();

  /**
   * If this ChangesetExtractor only supports a single type of changeset and wants to tell the client its overridden type.
   * @return changeset type
   */
  String getType();
}
