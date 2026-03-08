package com.helger.phoss.ap.api.datetime;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;

import com.helger.base.tostring.ToStringGenerator;
import com.helger.datetime.helper.PDTFactory;

/**
 * Default implementation of {@link IAPTimestampManager}.
 *
 * @author Philip Helger
 */
public class APTimestampManager implements IAPTimestampManager
{
  @NonNull
  public OffsetDateTime getCurrentDateTime ()
  {
    // Use maximum precision
    return PDTFactory.getCurrentOffsetDateTime ();
  }

  @NonNull
  public OffsetDateTime getCurrentDateTimeUTC ()
  {
    // Use maximum precision
    return PDTFactory.getCurrentOffsetDateTimeUTC ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
