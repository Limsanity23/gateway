package com.bnids.core.enums.converter;

import com.bnids.core.enums.model.EnumIntegerModel;
import lombok.Getter;

import javax.persistence.AttributeConverter;

/**
 * @author yannishin
 */
@Getter
//@NoArgsConstructor
//@AllArgsConstructor
public class AbstractDatabaseEnumAttributeConverter<E extends Enum<E> & EnumIntegerModel> implements AttributeConverter<E, Integer> {
    private Class<E> targetEnumClass;

    private boolean nullable;

    private String enumName;

    public AbstractDatabaseEnumAttributeConverter(boolean nullable, String enumName) {
        this.nullable = nullable;
        this.enumName = enumName;
    }

    @Override
    public Integer convertToDatabaseColumn(E attribute) {
        if (!nullable && attribute == null) {
            throw new IllegalArgumentException(String.format("%s(은)는 NULL로 지정할 수 없습니다.", enumName));
        }
        return DatabaseEnumValueConvertUtils.toDatabaseCode(attribute);
    }

    @Override
    public E convertToEntityAttribute(Integer dbData) {
        if (!nullable && dbData == null) {
            throw new IllegalArgumentException(String.format("%s(은)는 DB에 NULL로 혹은 Empty 로(%s> 지정되어 있습니다.", enumName, dbData));
        }
        return DatabaseEnumValueConvertUtils.ofDatabaseCode(targetEnumClass, dbData);
    }
}
