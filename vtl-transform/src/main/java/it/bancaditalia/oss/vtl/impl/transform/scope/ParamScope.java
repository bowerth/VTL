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
package it.bancaditalia.oss.vtl.impl.transform.scope;

import java.util.Map;

import it.bancaditalia.oss.vtl.engine.Statement;
import it.bancaditalia.oss.vtl.model.data.VTLValue;
import it.bancaditalia.oss.vtl.model.data.VTLValueMetadata;
import it.bancaditalia.oss.vtl.model.transform.Transformation;
import it.bancaditalia.oss.vtl.model.transform.TransformationScheme;
import it.bancaditalia.oss.vtl.session.MetadataRepository;

public class ParamScope implements TransformationScheme
{
	private final TransformationScheme parent;
	private final Map<String, Transformation> params;

	public ParamScope(TransformationScheme parent, Map<String, Transformation> params)
	{
		this.parent = parent;
		this.params = params;
	}

	@Override
	public VTLValue resolve(String node)
	{
		if (params.containsKey(node))
			return params.get(node).eval(parent);
		else
			return parent.resolve(node);
	}

	@Override
	public VTLValueMetadata getMetadata(String node)
	{
		if (params.containsKey(node))
			return params.get(node).getMetadata(parent);
		else
			return parent.getMetadata(node);
	}

	@Override
	public Statement getRule(String node)
	{
		return parent.getRule(node);
	}

	@Override
	public MetadataRepository getRepository()
	{
		return parent.getRepository();
	}
}
