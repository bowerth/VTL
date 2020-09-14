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
package it.bancaditalia.oss.vtl.impl.types.dataset;

import static it.bancaditalia.oss.vtl.util.Utils.toEntryWithValue;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.groupingByConcurrent;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toConcurrentMap;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.bancaditalia.oss.vtl.exceptions.VTLMissingComponentsException;
import it.bancaditalia.oss.vtl.impl.types.exceptions.VTLInvariantIdentifiersException;
import it.bancaditalia.oss.vtl.model.data.ComponentRole.Identifier;
import it.bancaditalia.oss.vtl.model.data.ComponentRole.Measure;
import it.bancaditalia.oss.vtl.model.data.ComponentRole.NonIdentifier;
import it.bancaditalia.oss.vtl.model.data.DataPoint;
import it.bancaditalia.oss.vtl.model.data.DataSet;
import it.bancaditalia.oss.vtl.model.data.DataSetMetadata;
import it.bancaditalia.oss.vtl.model.data.DataStructureComponent;
import it.bancaditalia.oss.vtl.model.data.ScalarValue;
import it.bancaditalia.oss.vtl.util.Utils;

public abstract class AbstractDataSet implements DataSet
{
	private static final long serialVersionUID = 1L;

	private final static Logger LOGGER = LoggerFactory.getLogger(AbstractDataSet.class);

	private final DataSetMetadata dataStructure;
	private SoftReference<String> cacheString  = null;

	protected AbstractDataSet(DataSetMetadata dataStructure)
	{
		this.dataStructure = dataStructure;
	}
	
	@Override
	public DataSet membership(String componentName)
	{
		final DataSetMetadata membershipStructure = dataStructure.membership(componentName);

		LOGGER.trace("Creating dataset by membership on {} from {} to {}", componentName, dataStructure, membershipStructure);

		DataStructureComponent<?, ?, ?> sourceComponent = dataStructure.getComponent(componentName)
				.orElseThrow(() -> new VTLMissingComponentsException(componentName, dataStructure));
		DataStructureComponent<? extends Measure, ?, ?> membershipMeasure = membershipStructure.getComponents(Measure.class).iterator().next();

		return mapKeepingKeys(membershipStructure, dp -> {
			if (!dp.containsKey(sourceComponent))
				throw new VTLMissingComponentsException(sourceComponent, dp);
			final ScalarValue<?, ?, ?> gotValue = dp.get(sourceComponent);
			return singletonMap(membershipMeasure, gotValue);
		});
	}

	@Override
	public Optional<DataStructureComponent<?, ?, ?>> getComponent(String name)
	{
		return dataStructure.getComponent(name);
	}

	@Override
	public DataSetMetadata getMetadata()
	{
		return dataStructure instanceof DataSetMetadata ? (DataSetMetadata) dataStructure : null;
	}

	@Override
	public DataSet filteredMappedJoin(DataSetMetadata metadata, DataSet other, BiPredicate<DataPoint,DataPoint> predicate, BinaryOperator<DataPoint> mergeOp)
	{
		Set<DataStructureComponent<Identifier, ?, ?>> commonIds = getMetadata().getComponents(Identifier.class);
		commonIds.retainAll(other.getComponents(Identifier.class));
		
		Map<Map<DataStructureComponent<Identifier, ?, ?>, ScalarValue<?, ?, ?>>, List<DataPoint>> index;
		try (Stream<DataPoint> stream = other.stream())
		{
			// performance if
			if (commonIds.equals(other.getComponents(Identifier.class)))
				index = stream.collect(toConcurrentMap(dp -> dp.getValues(commonIds, Identifier.class), Collections::singletonList));
			else
				index = stream.collect(groupingByConcurrent(dp -> dp.getValues(commonIds, Identifier.class)));
		}
		
		return new LightFDataSet<>(metadata, d -> d.stream()
				.map(dpThis -> {
					List<DataPoint> otherSubGroup = index.get(dpThis.getValues(commonIds, Identifier.class));
					if (otherSubGroup == null)
						return Stream.<DataPoint>empty();
					else
						return otherSubGroup.stream()
							.filter(dpOther -> predicate.test(dpThis, dpOther))
							.map(dpOther -> mergeOp.apply(dpThis, dpOther)); 
				}).reduce(Stream::concat)
				.orElse(Stream.empty()), this);
	}

	@Override
	public DataSet mapKeepingKeys(DataSetMetadata metadata,
			Function<? super DataPoint, ? extends Map<? extends DataStructureComponent<? extends NonIdentifier, ?, ?>, ? extends ScalarValue<?, ?, ?>>> operator)
	{
		final Set<DataStructureComponent<Identifier, ?, ?>> identifiers = dataStructure.getComponents(Identifier.class);
		if (!metadata.getComponents(Identifier.class).equals(identifiers))
			throw new VTLInvariantIdentifiersException("map", identifiers, metadata.getComponents(Identifier.class));
		
		LOGGER.trace("Creating dataset by mapping from {} to {}", dataStructure, metadata);
		
		UnaryOperator<DataPoint> extendingOperator = dp -> new DataPointBuilder(dp.getValues(Identifier.class))
				.addAll(operator.apply(dp))
				.build(metadata);
		
		return new AbstractDataSet(metadata)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Stream<DataPoint> streamDataPoints()
			{
				return AbstractDataSet.this.stream().map(extendingOperator);
			}
		};
	}

	@Override
	public <A, T, TT> Stream<T> streamByKeys(Set<DataStructureComponent<Identifier, ?, ?>> keys, 
			Map<DataStructureComponent<Identifier, ?, ?>, ScalarValue<?, ?, ?>> filter,
			Collector<DataPoint, A, TT> groupCollector,
			BiFunction<TT, Map<DataStructureComponent<Identifier, ?, ?>, ScalarValue<?, ?, ?>>, T> finisher)
	{
		class DecoratedCollector implements Collector<Entry<DataPoint, Map<DataStructureComponent<Identifier, ?, ?>, ScalarValue<?, ?, ?>>>, A, T> {
			private Map<DataStructureComponent<Identifier, ?, ?>, ScalarValue<?, ?, ?>> keyValues = null;

			@Override
			public Supplier<A> supplier()
			{
				return groupCollector.supplier();
			}

			@Override
			public BiConsumer<A, Entry<DataPoint, Map<DataStructureComponent<Identifier, ?, ?>, ScalarValue<?, ?, ?>>>> accumulator()
			{
				return (a, e) -> {
					keyValues = e.getValue();
					groupCollector.accumulator().accept(a, e.getKey());
				};
			}

			@Override
			public BinaryOperator<A> combiner()
			{
				return groupCollector.combiner();
			}
			
			@Override
			public Function<A, T> finisher()
			{
				return groupCollector.finisher().andThen(tt -> {
					Objects.requireNonNull(keyValues, "streamByKeys: There must be at least one datapoint in a group but there is none.");
					return finisher.apply(tt, keyValues);
				});
			}
			
			@Override
			public Set<Characteristics> characteristics()
			{
				return groupCollector.characteristics();
			} 
		}
		
		try (Stream<DataPoint> stream = stream())
		{
			return Utils.getStream(stream
					.filter(dp -> dp.matches(filter))
					.map(toEntryWithValue(dp -> dp.getValues(keys, Identifier.class)))
					.collect(groupingByConcurrent(e -> e.getValue(), new DecoratedCollector()))
					.values()
				);
		}
	}

	@Override
	public DataSet filter(Predicate<DataPoint> predicate)
	{
		return new LightDataSet(dataStructure, () -> stream().filter(predicate));
	}

	@Override
	public final Stream<DataPoint> stream()
	{
		LOGGER.trace("Streaming dataset of {}", dataStructure);

		return streamDataPoints();
	}

	public String toString()
	{
		String result = null;
		if (cacheString != null)
			result = cacheString.get();

		if (result != null)
			return result;

		try (Stream<DataPoint> stream = stream())
		{
			result = stream
					.peek(Objects::requireNonNull)
					.map(DataPoint::toString)
					.collect(joining(",\n\t", "(" + getMetadata() + ") -> {\n\t", "\n}"));
		}

		cacheString = new SoftReference<>(result);
		return result;
	}

	protected abstract Stream<DataPoint> streamDataPoints();
}