package com.bnids.core.enums.mapper;

import com.bnids.core.enums.model.EnumStringModel;
import com.bnids.core.enums.model.EnumStringValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author yannishin
 */
public class EnumStringMapper {
    private Map<String, List<EnumStringValue>> factory = new HashMap<>();

    private List<EnumStringValue> toEnumValues(Class<? extends EnumStringModel> e) {
        return Arrays.stream(e.getEnumConstants())
                .map(EnumStringValue::new)
                .collect(Collectors.toList());
    }

    public void put(String key, Class<? extends EnumStringModel> e) {
        factory.put(key, toEnumValues(e));
    }

    public Map<String, List<EnumStringValue>> getAll(){
        return factory;
    }

    public Map<String, List<EnumStringValue>> get(String keys){
        return Arrays
                .stream(keys.split(","))
                .collect(Collectors.toMap(Function.identity(), key -> factory.get(key)));
    }
}
