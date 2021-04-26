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
package it.bancaditalia.oss.vtl.impl.transform.util;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import it.bancaditalia.oss.vtl.model.data.VTLValueMetadata;
import it.bancaditalia.oss.vtl.model.transform.Transformation;
import it.bancaditalia.oss.vtl.model.transform.TransformationScheme;

public class MetadataHolder extends ConcurrentHashMap<Transformation, VTLValueMetadata>
{
	private static final long serialVersionUID = 1L;
	private static final Map<TransformationScheme, MetadataHolder> HOLDERS = new WeakHashMap<>(); 

	private MetadataHolder()
	{
		super();
	}
	
	public static MetadataHolder getInstance(TransformationScheme scheme)
	{
		MetadataHolder holder = HOLDERS.get(scheme);
		if (holder == null)
			synchronized (HOLDERS)
			{
				holder = new MetadataHolder();
				HOLDERS.put(scheme, holder);
			}

		return holder;
	}
}