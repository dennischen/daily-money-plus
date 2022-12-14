package com.colaorange.commons.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

/**
 * @author Dennis
 */
public class JsonBase implements Serializable, Cloneable {


    public String toJson() {
        return toJson(false);
    }

    public String toJson(boolean pretty) {
        return Jsons.toJson(this, pretty);
    }


    public String toString() {
        return toJson();
    }

}
