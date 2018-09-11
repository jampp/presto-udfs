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

package com.jampp.presto.udfs.aggregation.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.airlift.json.ObjectMapperProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.jampp.presto.udfs.aggregation.utils.JSONAggregationUtils.mapAsJSONString;
import static com.jampp.presto.udfs.aggregation.utils.JSONAggregationUtils.merge;
import static org.testng.Assert.assertEquals;

public class TestJSONAggregationUtils
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProvider().get();

    @Test
    public void bothJSONsEmpty()
    {
        Map<String, Object> emptyMap = readJSON("{}");
        assertEquals(mapAsJSONString(merge(emptyMap, emptyMap)), "{}");
    }

    @Test
    public void oneJSONEmpty()
    {
        Map<String, Object> leftJSON = readJSON("{}");
        Map<String, Object> rightJSON = readJSON("{\"a\":1, \"b\":2}");
        assertEquals(mapAsJSONString(merge(leftJSON, rightJSON)), "{\"a\":1, \"b\":2}");
        assertEquals(mapAsJSONString(merge(rightJSON, leftJSON)), "{\"a\":1, \"b\":2}");
    }

    @Test
    public void oneJSONEmptyWithDoubles()
    {
        Map<String, Object> leftJSON = readJSON("{}");
        Map<String, Object> rightJSON = readJSON("{\"a\":1.1, \"b\":2}");
        assertEquals(mapAsJSONString(merge(leftJSON, rightJSON)), "{\"a\":1.1, \"b\":2}");
        assertEquals(mapAsJSONString(merge(rightJSON, leftJSON)), "{\"a\":1.1, \"b\":2}");
    }

    @Test
    public void bothJSONsShallow()
    {
        Map<String, Object> leftJSON = readJSON("{\"c\":1, \"b\":1}");
        Map<String, Object> rightJSON = readJSON("{\"a\":1, \"b\":2}");
        assertEquals(mapAsJSONString(merge(leftJSON, rightJSON)), "{\"a\":1, \"b\":3, \"c\":1}");
        assertEquals(mapAsJSONString(merge(rightJSON, leftJSON)), "{\"a\":1, \"b\":3, \"c\":1}");
    }

    @Test
    public void bothJSONsShallowWithDoubles()
    {
        Map<String, Object> leftJSON = readJSON("{\"c\":1.1, \"b\":1}");
        Map<String, Object> rightJSON = readJSON("{\"a\":1, \"b\":2.1}");
        assertEquals(mapAsJSONString(merge(leftJSON, rightJSON)), "{\"a\":1, \"b\":3.1, \"c\":1.1}");
        assertEquals(mapAsJSONString(merge(rightJSON, leftJSON)), "{\"a\":1, \"b\":3.1, \"c\":1.1}");
    }

    @Test
    public void oneJSONDeep()
    {
        Map<String, Object> leftJSON = readJSON("{\"c\":1, \"d\":{\"a\":2}}");
        Map<String, Object> rightJSON = readJSON("{\"a\":1, \"b\":2}");
        assertEquals(mapAsJSONString(merge(leftJSON, rightJSON)), "{\"a\":1, \"b\":2, \"c\":1, \"d\":{\"a\":2}}");
        assertEquals(mapAsJSONString(merge(rightJSON, leftJSON)), "{\"a\":1, \"b\":2, \"c\":1, \"d\":{\"a\":2}}");
    }

    @Test
    public void oneJSONDeepWithDoubles()
    {
        Map<String, Object> leftJSON = readJSON("{\"c\":1, \"d\":{\"a\":2.1}}");
        Map<String, Object> rightJSON = readJSON("{\"a\":1, \"b\":2}");
        assertEquals(mapAsJSONString(merge(leftJSON, rightJSON)), "{\"a\":1, \"b\":2, \"c\":1, \"d\":{\"a\":2.1}}");
        assertEquals(mapAsJSONString(merge(rightJSON, leftJSON)), "{\"a\":1, \"b\":2, \"c\":1, \"d\":{\"a\":2.1}}");
    }

    @Test
    public void bothJSONsDeep()
    {
        Map<String, Object> leftJSON = readJSON("{\"c\":1, \"b\":{\"a\":2}}");
        Map<String, Object> rightJSON = readJSON("{\"a\":1, \"b\":{\"a\":2, \"b\":3}}");
        assertEquals(mapAsJSONString(merge(leftJSON, rightJSON)), "{\"a\":1, \"b\":{\"a\":4, \"b\":3}, \"c\":1}");
        assertEquals(mapAsJSONString(merge(rightJSON, leftJSON)), "{\"a\":1, \"b\":{\"a\":4, \"b\":3}, \"c\":1}");
    }

    @Test
    public void bothJSONsDeepWithDoubles()
    {
        Map<String, Object> leftJSON = readJSON("{\"c\":1.2, \"b\":{\"a\":2.3}}");
        Map<String, Object> rightJSON = readJSON("{\"a\":1, \"b\":{\"a\":2, \"b\":3}}");
        assertEquals(mapAsJSONString(merge(leftJSON, rightJSON)), "{\"a\":1, \"b\":{\"a\":4.3, \"b\":3}, \"c\":1.2}");
        assertEquals(mapAsJSONString(merge(rightJSON, leftJSON)), "{\"a\":1, \"b\":{\"a\":4.3, \"b\":3}, \"c\":1.2}");
    }

    @Test
    public void oneJSONDeepOneEmpty()
    {
        Map<String, Object> leftJSON = readJSON("{}");
        Map<String, Object> rightJSON = readJSON("{\"a\":1, \"b\":{\"a\":2, \"b\":3}}");
        assertEquals(mapAsJSONString(merge(leftJSON, rightJSON)), "{\"a\":1, \"b\":{\"a\":2, \"b\":3}}");
        assertEquals(mapAsJSONString(merge(rightJSON, leftJSON)), "{\"a\":1, \"b\":{\"a\":2, \"b\":3}}");
    }

    @Test
    public void oneJSONDeepOneEmptyWithDoubles()
    {
        Map<String, Object> leftJSON = readJSON("{}");
        Map<String, Object> rightJSON = readJSON("{\"a\":1.1, \"b\":{\"a\":2.2, \"b\":3}}");
        assertEquals(mapAsJSONString(merge(leftJSON, rightJSON)), "{\"a\":1.1, \"b\":{\"a\":2.2, \"b\":3}}");
        assertEquals(mapAsJSONString(merge(rightJSON, leftJSON)), "{\"a\":1.1, \"b\":{\"a\":2.2, \"b\":3}}");
    }

    private static Map<String, Object> readJSON(String jsonString)
    {
        LinkedHashMap<String, Object> jsonMap;
        try {
            jsonMap = new LinkedHashMap<>(OBJECT_MAPPER.readValue(jsonString.getBytes(),
                    new TypeReference<LinkedHashMap<String, Object>>() {}));
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return jsonMap;
    }
}
