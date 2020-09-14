/*******************************************************************************
 * Copyright 2020, Bank Of Italy
 *
 * Licensed under the EUPL, Version 1.2 (the "License");
 * You may not use this work except in compliance with the
 * License.
 * You may obtain a copy of the License at:
 *
 * https://joinup.ec.europa.eu/sites/default/files/custom-page/attachment/2020-03/EUPL-1.2%20EN.txt
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the License is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 *
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *******************************************************************************/
package it.bancaditalia.oss.vtl.impl.types.data.date;

import static it.bancaditalia.oss.vtl.impl.types.data.date.PeriodHolder.Formatter.WEEK_PERIOD_FORMATTER;
import static it.bancaditalia.oss.vtl.impl.types.domain.Domains.WEEKSDS;
import static it.bancaditalia.oss.vtl.impl.types.domain.DurationDomains.W;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoUnit.WEEKS;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.bancaditalia.oss.vtl.impl.types.domain.DurationDomains;
import it.bancaditalia.oss.vtl.model.domain.TimePeriodDomainSubset;

public class WeekPeriodHolder extends YearPeriodHolder<WeekPeriodHolder>
{
	private final static Logger LOGGER = LoggerFactory.getLogger(WeekPeriodHolder.class);
	private static final long serialVersionUID = 1L;

	private final long weekOfYear;

	public WeekPeriodHolder(TemporalAccessor other)
	{
		super(other);
		this.weekOfYear = other.getLong(ALIGNED_WEEK_OF_YEAR);
	}

	@Override
	public long getLong(TemporalField field)
	{
		return field == ALIGNED_WEEK_OF_YEAR ? weekOfYear : super.getLong(field);
	}
	
	@Override
	public boolean isSupported(TemporalField field)
	{
		return field == ALIGNED_WEEK_OF_YEAR || super.isSupported(field);
	}
	
	@Override
	public int compareTo(PeriodHolder<?> other)
	{
		int c = super.compareTo(other);
		if (c == 0)
			c = Integer.compare(get(ALIGNED_WEEK_OF_YEAR), other.get(ALIGNED_WEEK_OF_YEAR));
		LOGGER.trace("Comparing {} and {} yield {}.", this, other, c);
		return c;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (weekOfYear ^ (weekOfYear >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		WeekPeriodHolder other = (WeekPeriodHolder) obj;
		if (weekOfYear != other.weekOfYear)
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return WEEK_PERIOD_FORMATTER.get().format(this);
	}
	
	@Override
	public DurationDomains getPeriod()
	{
		return W;
	}

	@Override
	public PeriodHolder<?> wrapImpl(DurationDomains frequency)
	{
		switch (frequency)
		{
			case A: return new YearPeriodHolder<>(this);
			case S: return new SemesterPeriodHolder(this);
			case Q: return new QuarterPeriodHolder(this);
		default:
			throw new UnsupportedOperationException("Cannot wrap " + this + " with duration " + frequency + " or wrapping time_period not implemented"); 
		}
	}

	@Override
	public boolean isSupported(TemporalUnit unit)
	{
		return unit == WEEKS || super.isSupported(unit);
	}

	@Override
	public Temporal plus(long amount, TemporalUnit unit)
	{
		throw new UnsupportedOperationException("plus");
	}

	@Override
	public TimePeriodDomainSubset getDomain()
	{
		return WEEKSDS;
	}

	@Override
	protected TemporalUnit smallestUnit()
	{
		return WEEKS;
	}
}