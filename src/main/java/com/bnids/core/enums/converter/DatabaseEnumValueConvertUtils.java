package com.bnids.core.enums.converter;

import com.bnids.core.enums.model.EnumIntegerModel;
import com.bnids.core.exception.NotFoundException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.EnumSet;


/**
 * @author yannishin
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DatabaseEnumValueConvertUtils {
    public static <T extends Enum<T> & EnumIntegerModel> T ofDatabaseCode(Class<T> enumClass, Integer databaseCode) {
        if (databaseCode == null) {
            return null;
        }

        return EnumSet.allOf(enumClass).stream()
                .filter(v -> v.getKey().equals(databaseCode))
                .findAny()
                .orElseThrow(NotFoundException::new);
    }

    public static <T extends Enum<T> & EnumIntegerModel> Integer toDatabaseCode(T enumValue) {
        if (enumValue == null) {
            return null;
        }

        return enumValue.getKey();
    }
}
