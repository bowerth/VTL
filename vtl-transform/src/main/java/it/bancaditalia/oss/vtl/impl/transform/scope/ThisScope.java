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

import it.bancaditalia.oss.vtl.engine.Statement;
import it.bancaditalia.oss.vtl.model.data.DataSet;
import it.bancaditalia.oss.vtl.model.data.VTLDataSetMetadata;
import it.bancaditalia.oss.vtl.model.data.VTLValue;
import it.bancaditalia.oss.vtl.model.data.VTLValueMetadata;
import it.bancaditalia.oss.vtl.model.transform.TransformationScheme;
import it.bancaditalia.oss.vtl.session.MetadataRepository;

public class ThisScope implements TransformationScheme
{
	public static final String THIS = "$$THIS";
	
	private final VTLValue thisValue;
	private final VTLValueMetadata thisMetadata;
	private final TransformationScheme parent;
	
	public ThisScope(VTLValue thisValue, TransformationScheme parent)
	{
		this.thisValue = thisValue;
		this.thisMetadata = thisValue.getMetadata();
		this.parent = parent;
	}

	public ThisScope(VTLValueMetadata thisMetadata, TransformationScheme parent)
	{
		this.thisValue = null;
		this.thisMetadata = thisMetadata;
		this.parent = parent;
	}

	@Override
	public VTLValueMetadata getMetadata(String node)
	{
		if (THIS.equals(node))
			return thisMetadata;
		else
		{
			String stripped = node.replaceAll("^'(.*)'$", "$1");
			if (thisMetadata instanceof VTLDataSetMetadata && ((VTLDataSetMetadata) thisMetadata).getComponent(stripped).isPresent())
				return ((VTLDataSetMetadata) thisMetadata).membership(stripped);
			else 
				return parent.getMetadata(node);
		}
	}

	@Override
	public VTLValue resolve(String node)
	{
		if (THIS.equals(node))
			return thisValue;
		else 
		{
			String stripped = node.replaceAll("^'(.*)'$", "$1");
			if (thisValue instanceof DataSet && ((DataSet) thisValue).getComponent(node).isPresent())
				return ((DataSet) thisValue).membership(stripped);
			else 
				return parent.resolve(node);
		}
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
