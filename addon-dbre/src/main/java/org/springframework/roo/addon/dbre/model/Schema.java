package org.springframework.roo.addon.dbre.model;

import org.springframework.roo.support.util.StringUtils;

/**
 * Represents a schema in the database model.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Schema {
    private final String name;

    public Schema(final String name) {
        this.name = StringUtils.defaultIfEmpty(name,
                DbreModelService.NO_SCHEMA_REQUIRED);
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Schema)) {
            return false;
        }
        Schema other = (Schema) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
}
