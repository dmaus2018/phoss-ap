/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.db.jdbc.executor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.RowId;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Arrays;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.array.ArrayHelper;
import com.helger.base.clone.ICloneable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.rt.OffsetDate;
import com.helger.datetime.xml.XMLOffsetDate;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.datetime.xml.XMLOffsetTime;

/**
 * Represents a single DB query result row.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class DBResultRow implements ICloneable <DBResultRow>, Serializable
{
  private final DBResultField [] m_aCols;
  private int m_nIndex;

  /**
   * Copy constructor
   *
   * @param aOther
   *        other DB row to use
   */
  protected DBResultRow (@NonNull final DBResultRow aOther)
  {
    m_aCols = ArrayHelper.getCopy (aOther.m_aCols);
    m_nIndex = aOther.m_nIndex;
  }

  /**
   * Create an empty result row with the specified number of columns.
   *
   * @param nCols
   *        Number of columns. Must be be &ge; 0.
   */
  public DBResultRow (@Nonnegative final int nCols)
  {
    ValueEnforcer.isGE0 (nCols, "Columns");
    m_aCols = new DBResultField [nCols];
    m_nIndex = 0;
  }

  /**
   * Set all columns to <code>null</code> and restart the index at 0. This is mainly intended to
   * reuse the same object in a loop.
   */
  protected void internalClear ()
  {
    Arrays.fill (m_aCols, null);
    m_nIndex = 0;
  }

  /**
   * Add a new result field in the the first free column. This method increases the index.
   *
   * @param aResultField
   *        The result field to add. May not be <code>null</code>.
   * @see #getUsedColumnIndex()
   */
  protected void internalAdd (@NonNull final DBResultField aResultField)
  {
    ValueEnforcer.notNull (aResultField, "ResultField");

    m_aCols[m_nIndex++] = aResultField;
  }

  /**
   * @return The last used column. For an empty object that is <code>null</code>.
   */
  @Nonnegative
  public int getUsedColumnIndex ()
  {
    return m_nIndex;
  }

  /**
   * @return The number of columns as provided in the constructor. Always &ge; 0.
   */
  @Nonnegative
  public int getColumnCount ()
  {
    return m_aCols.length;
  }

  /**
   * Get the result field at the specified index
   *
   * @param nIndex
   *        The 0-based index to query
   * @return The result field at the specific index.
   * @throws ArrayIndexOutOfBoundsException
   *         If the index is invalid
   */
  @Nullable
  public DBResultField get (@Nonnegative final int nIndex)
  {
    return m_aCols[nIndex];
  }

  /**
   * Get the column type of the column at the specified index.
   *
   * @param nIndex
   *        The 0-based index to query
   * @return The column type as defined in {@link java.sql.Types}.
   * @throws ArrayIndexOutOfBoundsException
   *         If the index is invalid
   * @throws NullPointerException
   *         if the column at the specified index contains a <code>null</code> value
   */
  public int getColumnType (@Nonnegative final int nIndex)
  {
    return get (nIndex).getColumnType ();
  }

  /**
   * Get the column type name of the column at the specified index.
   *
   * @param nIndex
   *        The 0-based index to query
   * @return The column type name based on the constants of {@link java.sql.Types}.
   * @throws ArrayIndexOutOfBoundsException
   *         If the index is invalid
   * @throws NullPointerException
   *         if the column at the specified index contains a <code>null</code> value
   */
  @Nullable
  public String getColumnTypeName (@Nonnegative final int nIndex)
  {
    return get (nIndex).getColumnTypeName ();
  }

  /**
   * Get the column name of the column at the specified index.
   *
   * @param nIndex
   *        The 0-based index to query
   * @return The name of the column. Neither <code>null</code> nor empty.
   * @throws ArrayIndexOutOfBoundsException
   *         If the index is invalid
   * @throws NullPointerException
   *         if the column at the specified index contains a <code>null</code> value
   */
  @NonNull
  @Nonempty
  public String getColumnName (@Nonnegative final int nIndex)
  {
    return get (nIndex).getColumnName ();
  }

  @Nullable
  public Object getValue (@Nonnegative final int nIndex)
  {
    return get (nIndex).getValue ();
  }

  @Nullable
  public String getAsString (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsString ();
  }

  @Nullable
  public BigDecimal getAsBigDecimal (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsBigDecimal ();
  }

  @Nullable
  public BigInteger getAsBigInteger (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsBigInteger ();
  }

  public boolean getAsBoolean (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsBoolean ();
  }

  public boolean getAsBoolean (@Nonnegative final int nIndex, final boolean bDefault)
  {
    return get (nIndex).getAsBoolean (bDefault);
  }

  public byte getAsByte (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsByte ();
  }

  public byte getAsByte (@Nonnegative final int nIndex, final byte nDefault)
  {
    return get (nIndex).getAsByte (nDefault);
  }

  @Nullable
  public byte [] getAsByteArray (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsByteArray ();
  }

  public char getAsChar (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsChar ();
  }

  public char getAsChar (@Nonnegative final int nIndex, final char cDefault)
  {
    return get (nIndex).getAsChar (cDefault);
  }

  public double getAsDouble (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsDouble ();
  }

  public double getAsDouble (@Nonnegative final int nIndex, final double dDefault)
  {
    return get (nIndex).getAsDouble (dDefault);
  }

  public float getAsFloat (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsFloat ();
  }

  public float getAsFloat (@Nonnegative final int nIndex, final float fDefault)
  {
    return get (nIndex).getAsFloat (fDefault);
  }

  public int getAsInt (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsInt ();
  }

  public int getAsInt (@Nonnegative final int nIndex, final int nDefault)
  {
    return get (nIndex).getAsInt (nDefault);
  }

  public long getAsLong (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsLong ();
  }

  public long getAsLong (@Nonnegative final int nIndex, final long nDefault)
  {
    return get (nIndex).getAsLong (nDefault);
  }

  public short getAsShort (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsShort ();
  }

  public short getAsShort (@Nonnegative final int nIndex, final short nDefault)
  {
    return get (nIndex).getAsShort (nDefault);
  }

  @Nullable
  public Boolean getAsBooleanObj (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsBooleanObj ();
  }

  @Nullable
  public Byte getAsByteObj (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsByteObj ();
  }

  @Nullable
  public Character getAsCharObj (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsCharObj ();
  }

  @Nullable
  public Double getAsDoubleObj (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsDoubleObj ();
  }

  @Nullable
  public Float getAsFloatObj (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsFloatObj ();
  }

  @Nullable
  public Integer getAsIntObj (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsIntObj ();
  }

  @Nullable
  public Long getAsLongObj (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsLongObj ();
  }

  @Nullable
  public Short getAsShortObj (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsShortObj ();
  }

  @Nullable
  public Blob getAsBlob (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsSqlBlob ();
  }

  @Nullable
  public Clob getAsClob (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsSqlClob ();
  }

  @Nullable
  private Date _getAsDate (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsSqlDate ();
  }

  @Nullable
  public Date getAsDate (@Nonnegative final int nIndex)
  {
    return _getAsDate (nIndex);
  }

  @Nullable
  public LocalDate getAsLocalDate (@Nonnegative final int nIndex)
  {
    final Date ret = _getAsDate (nIndex);
    return ret == null ? null : ret.toLocalDate ();
  }

  @Nullable
  public OffsetDate getAsOffsetDate (@Nonnegative final int nIndex)
  {
    final Date ret = _getAsDate (nIndex);
    return ret == null ? null : PDTFactory.createOffsetDate (ret);
  }

  @Nullable
  public XMLOffsetDate getAsXMLOffsetDate (@Nonnegative final int nIndex)
  {
    final Date ret = _getAsDate (nIndex);
    return ret == null ? null : PDTFactory.createXMLOffsetDate (ret);
  }

  @Nullable
  public NClob getAsNClob (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsSqlNClob ();
  }

  @Nullable
  public RowId getAsRowId (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsSqlRowId ();
  }

  @Nullable
  private Time _getAsTime (@Nonnegative final int nIndex)
  {
    return get (nIndex).getAsSqlTime ();
  }

  @Nullable
  public Time getAsTime (@Nonnegative final int nIndex)
  {
    return _getAsTime (nIndex);
  }

  private static boolean _isOracleValue (@Nullable final Object o)
  {
    return o != null && o.getClass ().getPackageName ().equals ("oracle.sql");
  }

  @Nullable
  public LocalTime getAsLocalTime (@Nonnegative final int nIndex)
  {
    final Time ret = _getAsTime (nIndex);
    return ret == null ? null : ret.toLocalTime ();
  }

  @Nullable
  public OffsetTime getAsOffsetTime (@Nonnegative final int nIndex)
  {
    final Time ret = _getAsTime (nIndex);
    return ret == null ? null : PDTFactory.createOffsetTime (ret);
  }

  @Nullable
  public XMLOffsetTime getAsXMLOffsetTime (@Nonnegative final int nIndex)
  {
    final Time ret = _getAsTime (nIndex);
    return ret == null ? null : PDTFactory.createXMLOffsetTime (ret);
  }

  @Nullable
  public Timestamp getAsTimestamp (@Nonnegative final int nIndex)
  {
    final DBResultField aField = get (nIndex);
    final Object aValue = aField.getValue ();
    if (aValue instanceof final Timestamp aTimestamp)
      return aTimestamp;
    if (aValue instanceof final LocalDateTime aLDT)
      return Timestamp.valueOf (aLDT);
    if (_isOracleValue (aValue))
      return DBOracleHelper.getInstance ().getAsTimestamp (aValue);

    return aField.getAsSqlTimestamp ();
  }

  @Nullable
  public LocalDateTime getAsLocalDateTime (@Nonnegative final int nIndex)
  {
    final DBResultField aField = get (nIndex);
    final Object aValue = aField.getValue ();
    if (aValue == null)
      return null;

    if (aValue instanceof final LocalDateTime aLDT)
      return aLDT;
    if (aValue instanceof final Timestamp aTimestamp)
      return aTimestamp.toLocalDateTime ();
    if (_isOracleValue (aValue))
      return DBOracleHelper.getInstance ().getAsLocalDateTime (aValue);

    final Timestamp ret = aField.getAsSqlTimestamp ();
    return ret == null ? null : ret.toLocalDateTime ();
  }

  @Nullable
  public OffsetDateTime getAsOffsetDateTime (@Nonnegative final int nIndex)
  {
    final Timestamp ret = getAsTimestamp (nIndex);
    return ret == null ? null : PDTFactory.createOffsetDateTime (ret);
  }

  @Nullable
  public XMLOffsetDateTime getAsXMLOffsetDateTime (@Nonnegative final int nIndex)
  {
    final Timestamp ret = getAsTimestamp (nIndex);
    return ret == null ? null : PDTFactory.createXMLOffsetDateTime (ret);
  }

  /**
   * @return A map that contains the mapping from column name to the respective index
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsMap <String, Integer> getColumnNameToIndexMap ()
  {
    final ICommonsMap <String, Integer> ret = new CommonsHashMap <> ();
    for (int i = 0; i < m_aCols.length; ++i)
      ret.put (m_aCols[i].getColumnName (), Integer.valueOf (i));
    return ret;
  }

  @NonNull
  public DBResultRow getClone ()
  {
    return new DBResultRow (this);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Cols", m_aCols).append ("Index", m_nIndex).getToString ();
  }
}
