/*
 * Copyright © 2020 Banca D'Italia
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
 */
package it.bancaditalia.oss.vtl.impl.transform.ops;

import static it.bancaditalia.oss.vtl.impl.types.data.date.VTLTimePatterns.parseString;
import static it.bancaditalia.oss.vtl.impl.types.data.date.VTLTimePatterns.parseTemporal;
import static it.bancaditalia.oss.vtl.impl.types.domain.Domains.DATE;
import static it.bancaditalia.oss.vtl.impl.types.domain.Domains.INTEGER;
import static it.bancaditalia.oss.vtl.impl.types.domain.Domains.NULL;
import static it.bancaditalia.oss.vtl.impl.types.domain.Domains.NUMBER;
import static it.bancaditalia.oss.vtl.impl.types.domain.Domains.STRING;
import static it.bancaditalia.oss.vtl.impl.types.domain.Domains.TIME_PERIODS;
import static java.util.Collections.singletonMap;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Set;

import it.bancaditalia.oss.vtl.exceptions.VTLNestedException;
import it.bancaditalia.oss.vtl.impl.transform.UnaryTransformation;
import it.bancaditalia.oss.vtl.impl.transform.exceptions.VTLExpectedComponentException;
import it.bancaditalia.oss.vtl.impl.types.data.DateValue;
import it.bancaditalia.oss.vtl.impl.types.data.DoubleValue;
import it.bancaditalia.oss.vtl.impl.types.data.IntegerValue;
import it.bancaditalia.oss.vtl.impl.types.data.NullValue;
import it.bancaditalia.oss.vtl.impl.types.data.StringValue;
import it.bancaditalia.oss.vtl.impl.types.data.TimePeriodValue;
import it.bancaditalia.oss.vtl.impl.types.data.date.DateHolder;
import it.bancaditalia.oss.vtl.impl.types.data.date.PeriodHolder;
import it.bancaditalia.oss.vtl.impl.types.dataset.DataStructureBuilder;
import it.bancaditalia.oss.vtl.impl.types.dataset.DataStructureComponentImpl;
import it.bancaditalia.oss.vtl.impl.types.domain.Domains;
import it.bancaditalia.oss.vtl.impl.types.domain.NullDomain;
import it.bancaditalia.oss.vtl.impl.types.lineage.LineageNode;
import it.bancaditalia.oss.vtl.model.data.ComponentRole.Identifier;
import it.bancaditalia.oss.vtl.model.data.ComponentRole.Measure;
import it.bancaditalia.oss.vtl.model.data.DataSet;
import it.bancaditalia.oss.vtl.model.data.DataSetMetadata;
import it.bancaditalia.oss.vtl.model.data.DataStructureComponent;
import it.bancaditalia.oss.vtl.model.data.NumberValue;
import it.bancaditalia.oss.vtl.model.data.ScalarValue;
import it.bancaditalia.oss.vtl.model.data.ScalarValueMetadata;
import it.bancaditalia.oss.vtl.model.data.VTLValue;
import it.bancaditalia.oss.vtl.model.data.VTLValueMetadata;
import it.bancaditalia.oss.vtl.model.data.ValueDomainSubset;
import it.bancaditalia.oss.vtl.model.domain.NumberDomainSubset;
import it.bancaditalia.oss.vtl.model.domain.StringDomainSubset;
import it.bancaditalia.oss.vtl.model.domain.TimeDomainSubset;
import it.bancaditalia.oss.vtl.model.transform.Transformation;
import it.bancaditalia.oss.vtl.model.transform.TransformationScheme;
import it.bancaditalia.oss.vtl.util.Utils;

public class CastTransformation extends UnaryTransformation
{
	private static final long serialVersionUID = 1L;
	
	private final Domains target;
	private final String mask;
	private transient DecimalFormat numberFormatter;

	public CastTransformation(Transformation operand, Domains target, String mask)
	{
		super(operand);
		this.target = target;
		this.mask = mask != null ? mask.substring(1, mask.length() - 1) : "";
	}

	public CastTransformation(Transformation operand, String targetDomainName, String mask)
	{
		this(operand, Utils.getStream(Domains.values())
				.filter(domain -> domain.name().equalsIgnoreCase(targetDomainName))
				.findAny()
				.orElseThrow(() -> new UnsupportedOperationException("Cast with non-basic domain name '" + targetDomainName + "' not implemented")),
			mask);
	}

	@Override
	protected VTLValue evalOnScalar(ScalarValue<?, ?, ?, ?> scalar, VTLValueMetadata metadata)
	{
		return castScalar(scalar);
	}

	@Override
	protected VTLValue evalOnDataset(DataSet dataset, VTLValueMetadata metadata)
	{
		DataStructureComponent<Measure, ?, ?> oldMeasure = dataset.getComponents(Measure.class).iterator().next();
		DataStructureComponent<Measure, ?, ?> measure = DataStructureComponentImpl.of(target.getDomain().getVarName(), Measure.class, target.getDomain()).as(Measure.class);
		DataSetMetadata structure = new DataStructureBuilder(dataset.getComponents(Identifier.class))
				.addComponent(measure)
				.build();
		return dataset.mapKeepingKeys(structure, dp -> LineageNode.of(this, dp.getLineage()), dp -> singletonMap(measure, castScalar(dp.get(oldMeasure))));
	}

	@Override
	public VTLValueMetadata computeMetadata(TransformationScheme session)
	{
		VTLValueMetadata meta = operand.getMetadata(session);
		ValueDomainSubset<?, ?> domain;
		
		if (meta instanceof ScalarValueMetadata)
			domain = ((ScalarValueMetadata<?, ?>) meta).getDomain();
		else
		{
			DataSetMetadata dataset = (DataSetMetadata) meta;
			
			Set<? extends DataStructureComponent<? extends Measure, ?, ?>> measures = dataset.getComponents(Measure.class);
			if (measures.size() != 1)
				throw new VTLExpectedComponentException(Measure.class, measures);
			
			DataStructureComponent<? extends Measure, ?, ?> measure = measures.iterator().next();
			
			domain = measure.getDomain();
		}

		if (domain instanceof StringDomainSubset && target == DATE)
			return DATE;
		else if (domain instanceof StringDomainSubset && TIME_PERIODS.contains(target))
			return target;
		else if (domain instanceof StringDomainSubset && target == INTEGER)
			return INTEGER;
		else if (domain instanceof StringDomainSubset && target == NUMBER)
			return NUMBER;
		else if (domain instanceof TimeDomainSubset && target == STRING)
			return STRING;
		else if (domain instanceof NullDomain)
			return NULL;
		else if (domain instanceof NumberDomainSubset && target == INTEGER)
			return INTEGER;
		else
			throw new UnsupportedOperationException();
	}

	private synchronized DecimalFormat getNumberFormatter()
	{
		if (numberFormatter != null)
			return numberFormatter;
		
		synchronized (this)
		{
			if (numberFormatter != null)
				return numberFormatter;
			
			return numberFormatter = new DecimalFormat(mask);
		}
	}

	private ScalarValue<?, ?, ?, ?> castScalar(ScalarValue<?, ?, ?, ?> scalar)
	{
		try
		{
			if (scalar instanceof NullValue)
				return target.getDomain().cast(scalar);
			else if (scalar instanceof StringValue && target == DATE)
				return DateValue.of(parseString(scalar.get().toString(), mask));
			else if (scalar instanceof StringValue && TIME_PERIODS.contains(target))
				return TimePeriodValue.of(scalar.get().toString(), mask);
			else if (scalar instanceof DateValue && target == STRING)
				return StringValue.of(parseTemporal((DateHolder<?>) scalar.get(), mask));
			else if (scalar instanceof TimePeriodValue && target == STRING)
				return StringValue.of(parseTemporal((PeriodHolder<?>) scalar.get(), mask));
			else if (scalar instanceof StringValue && target == INTEGER)
				return IntegerValue.of(Long.parseLong((String) scalar.get()));
			else if (scalar instanceof StringValue && target == NUMBER)
				return DoubleValue.of(getNumberFormatter().parse((String) scalar.get()).doubleValue());
			else if (scalar instanceof NumberValue && target == INTEGER)
				return IntegerValue.of(((Number) scalar.get()).longValue());
			else
				throw new UnsupportedOperationException(scalar.getClass() + " " + target.getClass() + " " + scalar);
		}
		catch (ParseException e)
		{
			throw new VTLNestedException("Number '" + scalar.get() + "' unparseable with mask '" + getNumberFormatter() + "'", e);
		}
	}
	
	@Override
	public String toString()
	{
		return "cast(" + operand + ", " + target + ", \"" + mask + "\")";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((mask == null) ? 0 : mask.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (!(obj instanceof CastTransformation)) return false;
		CastTransformation other = (CastTransformation) obj;
		if (mask == null)
		{
			if (other.mask != null) return false;
		}
		else if (!mask.equals(other.mask)) return false;
		if (target != other.target) return false;
		return true;
	}
}
