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


package com.io7m.aurantedit.ui.internal.database;

import com.io7m.darco.api.DDatabaseUnit;
import org.jooq.DSLContext;

import java.nio.file.Path;
import java.time.Instant;

import static com.io7m.aurantedit.ui.internal.Tables.RECENT_FILES;

/**
 * Add recent files.
 */

public final class AERecentFileAdd
  extends AEDatabaseQueryAbstract<Path, DDatabaseUnit>
  implements AERecentFileAddType
{
  AERecentFileAdd(
    final AEDatabaseTransactionType t)
  {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static AEDatabaseQueryProviderType<
      Path, DDatabaseUnit, AERecentFileAddType>
  provider()
  {
    return AEDatabaseQueryProvider.provide(
      AERecentFileAddType.class,
      AERecentFileAdd::new
    );
  }

  @Override
  protected DDatabaseUnit onExecute(
    final AEDatabaseTransactionType transaction,
    final Path parameters)
  {
    final var context =
      transaction.get(DSLContext.class);

    context.insertInto(RECENT_FILES)
      .set(RECENT_FILES.RF_NAME, parameters.toAbsolutePath().toString())
      .set(RECENT_FILES.RF_TIME, (float) Instant.now().toEpochMilli())
      .onDuplicateKeyIgnore()
      .execute();

    context.deleteFrom(RECENT_FILES)
      .where(RECENT_FILES.RF_INDEX.notIn(
        context.select(RECENT_FILES.RF_INDEX)
          .from(RECENT_FILES)
          .orderBy(RECENT_FILES.RF_TIME.desc())
          .limit(50)
      ))
      .execute();

    return DDatabaseUnit.UNIT;
  }
}
