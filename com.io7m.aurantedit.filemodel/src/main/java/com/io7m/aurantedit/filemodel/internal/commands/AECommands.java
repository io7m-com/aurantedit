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


package com.io7m.aurantedit.filemodel.internal.commands;

import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * The command directory.
 */

public final class AECommands
{
  private AECommands()
  {

  }

  /**
   * Find a suitable command for the given state.
   *
   * @param <S>        The type of state
   * @param serialized The serialized command and state
   *
   * @return The command
   */

  public static <S extends AECommandStateType> AECommandType<S, ?> forJSON(
    final AECommandSerialized serialized)
  {
    final var commandName =
      serialized.commandName();
    final var factories =
      ServiceLoader.load(AECommandFactoryType.class);

    for (final AECommandFactoryType<?, ?> factory : factories) {
      if (Objects.equals(factory.commandClass(), commandName)) {
        final var constructor =
          (Function<S, ? extends AECommandType<?, ?>>) factory.constructor();
        final var stateT =
          (S) (Object) serialized.state();
        return (AECommandType<S, ?>) constructor.apply(stateT);
      }
    }

    throw new IllegalStateException(
      "No command available with name %s".formatted(commandName)
    );
  }
}
