package com.bnids.core.enums.mapper;

import com.bnids.core.enums.model.EnumIntegerModel;
import com.bnids.core.enums.model.EnumIntegerValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author yannishin
 */
public class EnumIntegerMapper {
    private Map<String, List<EnumIntegerValue>> factory = new HashMap<>();

    private List<EnumIntegerValue> toEnumValues(Class<? extends EnumIntegerModel> e) {
        return Arrays.stream(e.getEnumConstants())
                .map(EnumIntegerValue::new)
                .collect(Collectors.toList());
    }

    public void put(String key, Class<? extends EnumIntegerModel> e) {
        factory.put(key, toEnumValues(e));
    }

    public Map<String, List<EnumIntegerValue>> getAll(){
        return factory;
    }

    public Map<String, List<EnumIntegerValue>> get(String keys){
        return Arrays
                .stream(keys.split(","))
                .collect(Collectors.toMap(Function.identity(), key -> factory.get(key)));
    }
}
