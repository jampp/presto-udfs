/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jampp.presto.udfs.aggregation.state;

import io.trino.array.ObjectBigArray;
import io.trino.spi.function.AccumulatorStateFactory;
import io.trino.spi.function.GroupedAccumulatorState;

import java.util.LinkedHashMap;
import java.util.Map;

public class JSONAggregationStateFactory
        implements AccumulatorStateFactory<JSONAggregationState>
{
    public JSONAggregationStateFactory() {}

    @Override
    public JSONAggregationState createSingleState()
    {
        return new SingleJSONAggregationState();
    }

    @Override
    public JSONAggregationState createGroupedState()
    {
        return new GroupedJSONAggregationState();
    }

    public static class GroupedJSONAggregationState
            implements GroupedAccumulatorState, JSONAggregationState
    {
        private final ObjectBigArray<Map<String, Object>> maps;
        private int groupId;

        public GroupedJSONAggregationState()
        {
            this.maps = new ObjectBigArray<Map<String, Object>>();
        }

        @Override
        public void setGroupId(int groupId)
        {
            this.groupId = groupId;
        }

        @Override
        public void ensureCapacity(int size)
        {
            maps.ensureCapacity(size);
        }

        @Override
        public long getEstimatedSize()
        {
            return maps.sizeOf();
        }

        @Override
        public Map<String, Object> getMap()
        {
            return maps.get(groupId);
        }

        @Override
        public void setMap(Map<String, Object> newMap)
        {
            maps.ensureCapacity(groupId);
            maps.set(groupId, newMap);
        }
    }

    public static class SingleJSONAggregationState
            implements JSONAggregationState
    {
        private Map<String, Object> stateMap;

        public SingleJSONAggregationState()
        {
            this.stateMap = new LinkedHashMap<>();
        }

        @Override
        public Map<String, Object> getMap()
        {
            return stateMap;
        }

        @Override
        public void setMap(Map<String, Object> newMap)
        {
            this.stateMap = newMap;
        }

        @Override
        public long getEstimatedSize()
        {
            if (this.stateMap != null) {
                return this.stateMap.size();
            }
            else {
                return 0L;
            }
        }
    }
}
