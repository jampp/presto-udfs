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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.airlift.json.ObjectMapperProvider;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.prestosql.spi.block.Block;
import io.prestosql.spi.block.BlockBuilder;
import io.prestosql.spi.function.AccumulatorStateSerializer;
import io.prestosql.spi.type.Type;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;

import static com.jampp.presto.udfs.aggregation.utils.JSONAggregationUtils.mapAsJSONString;
import static io.prestosql.spi.type.VarcharType.VARCHAR;

public class JSONAggregationStateSerializer
        implements AccumulatorStateSerializer<JSONAggregationState>
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProvider().get();

    @Override
    public Type getSerializedType()
    {
        return VARCHAR;
    }

    @Override
    public void serialize(JSONAggregationState state, BlockBuilder out)
    {
        if (state.getMap() == null) {
            out.appendNull();
        }
        else {
            String mapAsString = mapAsJSONString(state.getMap());
            VARCHAR.writeSlice(out, Slices.utf8Slice(mapAsString));
        }
    }

    @Override
    public void deserialize(Block block, int index, JSONAggregationState state)
    {
        Slice slice = VARCHAR.getSlice(block, index);
        LinkedHashMap<String, Object> jsonState;
        try {
            jsonState = new LinkedHashMap<>(OBJECT_MAPPER.readValue(slice.getBytes(),
                                                new TypeReference<LinkedHashMap<String, Object>>() {}));
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        state.setMap(jsonState);
    }
}
