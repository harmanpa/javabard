/*
 * Copyright 2017 CAE Tech Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.cae.javabard;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;

/**
 *
 * @author Peter Harman, CAE Tech Limited, peter.harman@cae.tech
 */
public class ValueClassSpec {

    public static ValueClassSpec.Builder forFields(String packageName, String name, Map<String, String> nameTypeMap) {
        Map<String, Class<?>> resolved = new LinkedHashMap<String, Class<?>>(nameTypeMap.size());
        for (Map.Entry<String, String> entry : nameTypeMap.entrySet()) {
            resolved.put(entry.getKey(), guessClass(entry.getValue()));
        }
        return forResolvedFields(packageName, name, resolved);
    }

    public static ValueClassSpec.Builder forResolvedFields(String packageName, String name, Map<String, Class<?>> nameTypeMap) {
        return new Builder(packageName, name, nameTypeMap);
    }

    /**
     * Find a possible class for a simple name
     *
     * @param className
     * @return
     */
    static Class<?> guessClass(String className) {
        List<String> packages = Arrays.asList("", "java.lang.", "java.util.");
        for (String pkg : packages) {
            try {
                return Class.forName(pkg + className);
            } catch (ClassNotFoundException ex) {
            }
        }
        // Handle primitives
        if (className.indexOf('.') < 0 && Character.isLowerCase(className.charAt(0))) {
            if ("char".equals(className)) {
                return guessClass("Character");
            }
            return guessClass(new String(new char[]{Character.toUpperCase(className.charAt(0))}) + className.substring(1));
        }
        return null;
    }

    public static class Builder {

        private final String name;
        private final Map<String, Class<?>> nameTypeMap;
        private boolean builder = false;
        private boolean getter = false;
        private boolean setter = false;
        private final String packageName;

        public Builder(String packageName, String name, Map<String, Class<?>> nameTypeMap) {
            this.packageName = packageName;
            this.name = name;
            this.nameTypeMap = nameTypeMap;
        }

        public Builder withBuilder() {
            builder = true;
            return this;
        }

        public Builder withGetters() {
            getter = true;
            return this;
        }

        public Builder withSetters() {
            setter = true;
            return this;
        }

        public TypeSpec.Builder build() {
            TypeSpec.Builder tsb = TypeSpec.classBuilder(name);
            for (Map.Entry<String, Class<?>> entry : nameTypeMap.entrySet()) {
                FieldSpec field = FieldSpec.builder(
                        ClassName.get(entry.getValue()), entry.getKey(), Modifier.PRIVATE)
                        .build();
                tsb.addField(field);
            }
            if (getter) {
                tsb = GetterSpec.forType(tsb).build();
            }
            if (setter) {
                tsb = SetterSpec.forType(tsb).build();
            }
            if (builder) {
                tsb = BuilderSpec.forType(packageName, tsb).build();
            }
            return tsb;
        }
    }
}
