/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.aurantedit.filemodel.internal;

import com.io7m.darco.api.DDatabaseException;
import com.io7m.darco.sqlite.DSDatabaseFactory;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.lanark.core.RDottedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.io.InputStream;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * The main database factory.
 */

public final class AEDatabaseFactory
  extends DSDatabaseFactory<
  AEDatabaseConfiguration,
  AEDatabaseConnectionType,
  AEDatabaseTransactionType,
  AEDatabaseQueryProviderType<?, ?, ?>,
  AEDatabaseType>
  implements AEDatabaseFactoryType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AEDatabaseFactory.class);

  /**
   * The main database factory.
   */

  public AEDatabaseFactory()
  {

  }

  @Override
  protected RDottedName applicationId()
  {
    return new RDottedName("com.io7m.laurel");
  }

  @Override
  protected Logger logger()
  {
    return LOG;
  }

  @Override
  protected AEDatabaseType onCreateDatabase(
    final AEDatabaseConfiguration configuration,
    final SQLiteDataSource source,
    final List<AEDatabaseQueryProviderType<?, ?, ?>> queryProviders,
    final CloseableCollectionType<DDatabaseException> resources)
  {
    return new AEDatabase(
      configuration,
      source,
      queryProviders,
      resources
    );
  }

  @Override
  protected InputStream onRequireDatabaseSchemaXML()
  {
    return AEDatabaseFactory.class.getResourceAsStream(
      "/com/io7m/aurantedit/filemodel/internal/database.xml"
    );
  }

  @Override
  protected void onEvent(
    final String message)
  {

  }

  @Override
  protected void onAdjustSQLiteConfig(
    final SQLiteConfig config)
  {

  }

  @Override
  protected List<AEDatabaseQueryProviderType<?, ?, ?>> onRequireDatabaseQueryProviders()
  {
    return ServiceLoader.load(AEDatabaseQueryProviderType.class)
      .stream()
      .map(ServiceLoader.Provider::get)
      .map(x -> (AEDatabaseQueryProviderType<?, ?, ?>) x)
      .collect(Collectors.toList());
  }
}
