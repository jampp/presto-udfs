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

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JSONAggregationUtils
{
    private JSONAggregationUtils() {}

    // Merges two arbitrarily deep maps, adding their values.
    // Doesn't support Maps with lists as values.
    public static Map<String, Object> merge(Map<String, Object> rightMap, Map<String, Object> leftMap)
    {
        return Stream.of(rightMap, leftMap)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (value1, value2) -> {
                            if (value1 instanceof Map<?, ?>) {
                                return merge((Map<String, Object>) value1, (Map<String, Object>) value2);
                            }
                            else if (value1 instanceof Double || value2 instanceof Double) {
                                return ((Number) value1).doubleValue() + ((Number) value2).doubleValue();
                            }
                            else {
                                return ((Number) value1).longValue() + ((Number) value2).longValue();
                            }
                        }));
    }

    private static String jsonify(String key, Object value)
    {
        return '"' + key + "\":" + value;
    }

    // Converts an arbitrarily deep map into a json string.
    public static String mapAsJSONString(Map<String, Object> map)
    {
        return map.keySet()
                .stream()
                .map(key -> {
                    if (map.get(key) instanceof Map<?, ?>) {
                        return jsonify(key, mapAsJSONString((Map<String, Object>) map.get(key)));
                    }
                    return jsonify(key, (Number) map.get(key));
                })
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
