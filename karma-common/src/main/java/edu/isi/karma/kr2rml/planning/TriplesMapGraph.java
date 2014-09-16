/*******************************************************************************
 * Copyright 2012 University of Southern California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This code was developed by the Information Integration Group as part 
 * of the Karma project at the Information Sciences Institute of the 
 * University of Southern California.  For more information, publications, 
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/

package edu.isi.karma.kr2rml.planning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TriplesMapGraph {
	private Set<TriplesMapLink> links;
	private Map<String, List<TriplesMapLink>> neighboringTriplesMapCache;
	private Map<String, TriplesMap> triplesMapIndex;

	public TriplesMapGraph() {
		this.links = new HashSet<TriplesMapLink>();
		this.neighboringTriplesMapCache = new HashMap<String, List<TriplesMapLink>>();
		this.triplesMapIndex = new HashMap<String, TriplesMap>();
	}

	public Set<TriplesMapLink> getLinks() {
		return links;
	}

	public void addTriplesMap(TriplesMap triplesMap)
	{
		triplesMapIndex.put(triplesMap.getId(), triplesMap);
		List<TriplesMapLink> neighbouringLinks = neighboringTriplesMapCache.get(triplesMap.getId());
		if (neighbouringLinks == null) {
			neighbouringLinks = new ArrayList<TriplesMapLink>();
			neighboringTriplesMapCache.put(triplesMap.getId(), neighbouringLinks);
		}
	}

	public void addLink(TriplesMapLink link) {
		links.add(link);
		updateCache(link);
	}

	public static TriplesMapGraph mergeGraphs(TriplesMapGraph graphToMerge1, TriplesMapGraph graphToMerge2)
	{
		TriplesMapGraph newGraph = new TriplesMapGraph();

		for(TriplesMapLink link : graphToMerge1.getLinks())
		{
			newGraph.addLink(link);
		}
		for(TriplesMapLink link : graphToMerge2.getLinks())
		{
			newGraph.addLink(link);
		}
		return newGraph;
	}
	private void updateCache(TriplesMapLink link) {
		// Add source neighboring links to the cache
		List<TriplesMapLink> sourceNeighbouringLinks = neighboringTriplesMapCache.get(link.getSourceMap().getId());
		if (sourceNeighbouringLinks == null) {
			sourceNeighbouringLinks = new ArrayList<TriplesMapLink>();
		}
		sourceNeighbouringLinks.add(link);
		triplesMapIndex.put(link.getSourceMap().getId(),link.getSourceMap());
		neighboringTriplesMapCache.put(link.getSourceMap().getId(), sourceNeighbouringLinks);

		// Add target neighboring links to the cache
		List<TriplesMapLink> targetNeighbouringLinks = neighboringTriplesMapCache.get(link.getTargetMap().getId());
		if (targetNeighbouringLinks == null) {
			targetNeighbouringLinks = new ArrayList<TriplesMapLink>();
		}
		targetNeighbouringLinks.add(link);
		triplesMapIndex.put(link.getTargetMap().getId(),link.getTargetMap());
		neighboringTriplesMapCache.put(link.getTargetMap().getId(), targetNeighbouringLinks);
	}

	public List<TriplesMapLink> getAllNeighboringTriplesMap(String triplesMapId) {
		if (neighboringTriplesMapCache.get(triplesMapId) == null)
			return new ArrayList<TriplesMapLink>();

		return neighboringTriplesMapCache.get(triplesMapId);
	}

	public List<String> removeLink(TriplesMapLink link) {
		List<String> removedTriplesMaps = new LinkedList<String>();
		links.remove(link);
		removedTriplesMaps.addAll(removeLinkFromCache(link, link.getTargetMap().getId()));
		removedTriplesMaps.addAll(removeLinkFromCache(link, link.getSourceMap().getId()));
		return removedTriplesMaps;
	}

	private List<String> removeLinkFromCache(TriplesMapLink link, String mapId) {
		List<String> removedTriplesMaps = new LinkedList<String>();
		List<TriplesMapLink> targetLinks = neighboringTriplesMapCache.get(mapId);
		targetLinks.remove(link);
		if(targetLinks.isEmpty())
		{
			neighboringTriplesMapCache.remove(mapId);
			triplesMapIndex.remove(mapId);
			removedTriplesMaps.add(mapId);
		}
		return removedTriplesMaps;
	}

	public Set<String> getTriplesMapIds()
	{
		return triplesMapIndex.keySet();
	}
	public String findRoot(RootStrategy strategy)
	{
		return strategy.findRoot(this);
	}

	public TriplesMap getTriplesMap(String triplesMapId)
	{
		return this.triplesMapIndex.get(triplesMapId);
	}

	public List<String> removeTriplesMap(String triplesMapId) {

		List<String> removedTriplesMaps = new LinkedList<String>();
		List<TriplesMapLink> targetLinks = neighboringTriplesMapCache.get(triplesMapId);
		if(targetLinks != null)
		{
			for(TriplesMapLink link : targetLinks)
			{
				removedTriplesMaps.addAll(removeLink(link));
			}	
		}
		else
		{
			removedTriplesMaps.add(triplesMapId);
		}
		neighboringTriplesMapCache.remove(triplesMapId);
		triplesMapIndex.remove(triplesMapId);

		return removedTriplesMaps;

	}

	public void killTriplesMap(String triplesMapId) {
		removeTriplesMap(triplesMapId);
		for (Entry<String, List<TriplesMapLink>> entry : neighboringTriplesMapCache.entrySet()) {
			List<TriplesMapLink> links = entry.getValue();
			if (links != null) {
				Iterator<TriplesMapLink> itr = links.iterator();
				while (itr.hasNext()) {
					TriplesMapLink link = itr.next();
					if (link.getTargetMap().getId().equals(triplesMapId)) {
						itr.remove();
					}
				}
			}
		}
	}
	
	public TriplesMapGraph copyGraph() {
		TriplesMapGraph newGraph = new TriplesMapGraph();
		newGraph.links.addAll(links);
		for (Entry<String, List<TriplesMapLink> >entry : neighboringTriplesMapCache.entrySet()) {
			newGraph.neighboringTriplesMapCache.put(entry.getKey(), new ArrayList<TriplesMapLink>(entry.getValue()));
		}
		newGraph.triplesMapIndex.putAll(triplesMapIndex);
		return newGraph;
	}
}
