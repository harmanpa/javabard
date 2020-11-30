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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

/**
 *
 * @author Peter Harman, CAE Tech Limited, peter.harman@cae.tech
 */
public class GetterSpec {

    /**
     *
     * @param field
     * @return
     */
    public static MethodSpec.Builder forField(FieldSpec field) {
        return forField(field, "get$N");
    }

    /**
     *
     * @param field
     * @param namingConvention
     * @return
     */
    public static MethodSpec.Builder forField(FieldSpec field, String namingConvention) {
        return MethodSpec.methodBuilder(makeName(namingConvention, field.name))
                .addJavadoc("Get " + field.javadoc + "\n@return " + field.javadoc + "\n")
                .addAnnotation(JsonIgnore.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(field.type)
                .addStatement("return this.$N", field.name);
    }

    private static String makeName(String namingConvention, String name) {
        if (namingConvention.indexOf("$N") > 0) {
            return namingConvention.replace("$N", toCamelCase(name));
        }
        return namingConvention.replace("$N", name);
    }

    private static String toCamelCase(String a) {
        return new String(new char[]{Character.toUpperCase(a.charAt(0))}) + a.substring(1);
    }

    /**
     *
     * @param typeSpecBuilder
     * @return
     */
    public static GetterSpec.Builder forType(TypeSpec.Builder typeSpecBuilder) {
        return new Builder(typeSpecBuilder);
    }

    /**
     *
     */
    public static class Builder {

        private final TypeSpec.Builder typeSpecBuilder;
        private final TypeSpec typeSpec;
        private String namingConvention = "get$N";

        Builder(TypeSpec.Builder typeSpecBuilder) {
            this.typeSpecBuilder = typeSpecBuilder;
            this.typeSpec = typeSpecBuilder.build();
        }

        public Builder withNamingConvention(String n) {
            this.namingConvention = n;
            return this;
        }

        public TypeSpec.Builder build() {
            typeSpec.fieldSpecs.forEach((field) -> {
                typeSpecBuilder.addMethod(forField(field, namingConvention).build());
            });
            return typeSpecBuilder;
        }
    }
}
