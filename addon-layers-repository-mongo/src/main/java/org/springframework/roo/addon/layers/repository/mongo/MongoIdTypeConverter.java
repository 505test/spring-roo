package org.springframework.roo.addon.layers.repository.mongo;

import java.math.BigInteger;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.util.StringUtils;

/**
 * Custom id type converter for {@link MongoIdType} to limit options in
 * {@link MongoCommands}
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class MongoIdTypeConverter implements Converter<MongoIdType> {

    public boolean supports(final Class<?> type, final String optionContext) {
        return MongoIdType.class.isAssignableFrom(type);
    }

    public MongoIdType convertFromText(final String value,
            final Class<?> targetType, final String optionContext) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return new MongoIdType(value);
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> targetType, final String existingData,
            final String optionContext, final MethodTarget target) {
        SortedSet<String> types = new TreeSet<String>();
        types.add(BigInteger.class.getName());
        types.add("org.bson.types.ObjectId");

        for (String type : types) {
            if (type.startsWith(existingData) || existingData.startsWith(type)) {
                completions.add(new Completion(type));
            }
        }
        return false;
    }
}
