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
package it.bancaditalia.oss.vtl.session;

import java.util.Collection;

import it.bancaditalia.oss.vtl.model.data.ValueDomainSubset;

public interface MetadataRepository
{
	public Collection<ValueDomainSubset<?>> getValueDomains();
	
	public boolean isDomainDefined(String alias);
	
	public ValueDomainSubset<?> getDomain(String alias);
	
	public default <T extends ValueDomainSubset<?>> T registerDomain(String alias, T domain)
	{
		throw new UnsupportedOperationException("registerDomain");
	}
	
	public default <T extends ValueDomainSubset<?>> T defineDomain(String alias, Class<T> domainClass, Object param)
	{
		throw new UnsupportedOperationException("defineDomain");
	}
	
	public default MetadataRepository init(Object... params)
	{
		return this;
	}
}
