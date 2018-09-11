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

package com.jampp.presto.udfs.aggregation;

import com.facebook.presto.spi.block.BlockBuilder;
import com.facebook.presto.spi.function.AggregationFunction;
import com.facebook.presto.spi.function.AggregationState;
import com.facebook.presto.spi.function.CombineFunction;
import com.facebook.presto.spi.function.InputFunction;
import com.facebook.presto.spi.function.OutputFunction;
import com.facebook.presto.spi.function.SqlType;
import com.facebook.presto.spi.type.StandardTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jampp.presto.udfs.aggregation.state.JSONAggregationState;
import io.airlift.json.ObjectMapperProvider;
import io.airlift.slice.Slice;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.facebook.presto.spi.type.VarcharType.VARCHAR;
import static com.jampp.presto.udfs.aggregation.utils.JSONAggregationUtils.mapAsJSONString;
import static com.jampp.presto.udfs.aggregation.utils.JSONAggregationUtils.merge;

@AggregationFunction("json_sum")
public class JSONAggregation
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProvider().get();

    private JSONAggregation() {}

    @InputFunction
    public static void inputVarchar(@AggregationState JSONAggregationState state, @SqlType(StandardTypes.VARCHAR) Slice field)
    {
        genericInput(state, field);
    }

    @InputFunction
    public static void inputJson(@AggregationState JSONAggregationState state, @SqlType(StandardTypes.JSON) Slice json)
    {
        genericInput(state, json);
    }

    private static void genericInput(@AggregationState JSONAggregationState state, Slice json)
    {
        Map<String, Object> stateMap = state.getMap();
        LinkedHashMap<String, Object> jsonMap;
        try {
            jsonMap = new LinkedHashMap<>(OBJECT_MAPPER.readValue(json.getBytes(),
                    new TypeReference<LinkedHashMap<String, Object>>() {}));
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (stateMap == null) {
            stateMap = jsonMap;
            state.setMap(stateMap);
        }
        else {
            state.setMap(merge(stateMap, jsonMap));
        }
    }

    @CombineFunction
    public static void combine(@AggregationState JSONAggregationState state, @AggregationState JSONAggregationState otherState)
    {
        if (state.getMap() != null && otherState.getMap() != null) {
            Map<String, Object> stateMap = state.getMap();
            Map<String, Object> otherStateMap = otherState.getMap();
            state.setMap(merge(stateMap, otherStateMap));
        }
        else if (state.getMap() == null) {
            state.setMap(otherState.getMap());
        }
    }

    @OutputFunction(StandardTypes.VARCHAR)
    public static void output(@AggregationState JSONAggregationState state, BlockBuilder out)
    {
        Map<String, Object> stateMap = state.getMap();
        if (stateMap == null) {
            out.appendNull();
        }
        else {
            VARCHAR.writeString(out, mapAsJSONString(stateMap));
        }
    }
}
