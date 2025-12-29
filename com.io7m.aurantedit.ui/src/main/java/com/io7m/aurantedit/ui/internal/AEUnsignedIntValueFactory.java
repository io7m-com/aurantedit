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


package com.io7m.aurantedit.ui.internal;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.SpinnerValueFactory;

/**
 * A spinner value factory for 32-bit unsigned integers.
 */

public final class AEUnsignedIntValueFactory
  extends SpinnerValueFactory<Long>
  implements ChangeListener<String>
{
  /**
   * A spinner value factory for 32-bit unsigned integers.
   */

  public AEUnsignedIntValueFactory()
  {

  }

  @Override
  public void decrement(
    final int steps)
  {
    this.setValue(
      StrictMath.clamp(this.getValue() - steps, 0L, 4294967295L)
    );
  }

  @Override
  public void increment(
    final int steps)
  {
    this.setValue(
      StrictMath.clamp(this.getValue() + steps, 0L, 4294967295L)
    );
  }

  @Override
  public void changed(
    final ObservableValue<? extends String> observable,
    final String oldValue,
    final String newValue)
  {
    try {
      this.setValue(Long.parseUnsignedLong(newValue));
    } catch (final NumberFormatException e) {
      // User entered nonsense.
    }
  }
}
