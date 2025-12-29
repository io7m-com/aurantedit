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

import com.io7m.darco.api.DDatabaseCreate;
import com.io7m.darco.api.DDatabaseTelemetryType;
import com.io7m.darco.api.DDatabaseUpgrade;
import com.io7m.darco.api.DRoles;
import com.io7m.darco.api.DUsernamePassword;
import com.io7m.darco.sqlite.DSDatabaseConfigurationType;
import com.io7m.jxe.core.JXEHardenedSAXParsers;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * The database configuration.
 *
 * @param saxParsers The SAX parsers
 * @param telemetry The telemetry
 * @param create    The database creation flag
 * @param upgrade   The database upgrade flag
 * @param file      The database file
 * @param roles The database roles
 */

public record AEDatabaseConfiguration(
  Optional<JXEHardenedSAXParsers> saxParsers,
  DDatabaseTelemetryType telemetry,
  DDatabaseCreate create,
  DDatabaseUpgrade upgrade,
  Path file,
  DRoles roles)
  implements DSDatabaseConfigurationType
{
  /**
   * The database configuration.
   *
   * @param telemetry The telemetry
   * @param create    The database creation flag
   * @param upgrade   The database upgrade flag
   * @param file      The database file
   */

  public AEDatabaseConfiguration
  {
    Objects.requireNonNull(saxParsers, "saxParsers");
    Objects.requireNonNull(telemetry, "telemetry");
    Objects.requireNonNull(create, "create");
    Objects.requireNonNull(upgrade, "upgrade");
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(roles, "roles");
  }

  @Override
  public DUsernamePassword defaultRole()
  {
    return new DUsernamePassword("nobody", "");
  }
}
