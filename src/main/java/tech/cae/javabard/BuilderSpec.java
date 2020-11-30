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
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

/**
 * Generate a builder for the given class
 *
 * @author Peter Harman, CAE Tech Limited, peter.harman@cae.tech
 */
public class BuilderSpec {

    public static BuilderSpec.Builder forType(String packageName, TypeSpec.Builder typeSpecBuilder) {
        return new Builder(packageName, typeSpecBuilder);
    }

    public static class Builder {

        private final String packageName;
        private final TypeSpec.Builder typeSpecBuilder;
        private final TypeSpec typeSpec;
        private String builderClassName = "Builder";
        private Modifier[] buildModifiers = new Modifier[]{Modifier.PUBLIC, Modifier.FINAL};
        private final List<MethodSpec> additionalMethods = new ArrayList<>();
        private final List<FieldSpec> additionalFields = new ArrayList<>();

        Builder(String packageName, TypeSpec.Builder typeSpecBuilder) {
            this.packageName = packageName;
            this.typeSpecBuilder = typeSpecBuilder;
            this.typeSpec = typeSpecBuilder.build();
        }

        public Builder withBuildMethodModifiers(Modifier... mods) {
            buildModifiers = mods;
            return this;
        }

        public Builder withAdditionalMethod(MethodSpec spec) {
            additionalMethods.add(spec);
            return this;
        }

        public Builder withAdditionalField(FieldSpec spec) {
            additionalFields.add(spec);
            return this;
        }

        public Builder withBuilderClassName(String name) {
            builderClassName = name;
            return this;
        }

        public TypeName getTargetClassName() {
            return ClassName.get(packageName, typeSpec.name);
        }

        public TypeName getBuilderClassName() {
            return ClassName.get(packageName, typeSpec.name, builderClassName);
        }

        public TypeSpec.Builder build() {
            TypeSpec.Builder builder = TypeSpec.classBuilder(builderClassName);
            builder.addMethod(MethodSpec.constructorBuilder().build());
            builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
            MethodSpec.Builder constructor = MethodSpec.constructorBuilder();
            TypeSpec classSpec = typeSpecBuilder.build();
            ClassName builderName = ClassName.get(packageName, classSpec.name, builderClassName);
            StringBuilder constructorStatement = new StringBuilder("return new $N(");
            List<Object> constructorObjects = new ArrayList<>();
            constructorObjects.add(getTargetClassName().toString());
            for (int i = 0; i < classSpec.fieldSpecs.size(); i++) {
                FieldSpec field = FieldSpec.builder(
                        classSpec.fieldSpecs.get(i).type,
                        classSpec.fieldSpecs.get(i).name)
                        .addModifiers(Modifier.PRIVATE)
                        .addJavadoc(classSpec.fieldSpecs.get(i).javadoc)
                        .build();
                builder.addField(field);
                builder.addMethod(builderGetter(field));
                builder.addMethod(builderSetter(builderName, field));
                constructor.addParameter(field.type, field.name);
                constructor.addStatement("this.$N = $N", field.name, field.name);
                constructorStatement.append("$N");
                if (i < classSpec.fieldSpecs.size() - 1) {
                    constructorStatement.append(",");
                }
                constructorObjects.add(field.name);
            }
            constructorStatement.append(")");
            builder.addMethod(MethodSpec.methodBuilder("build")
                    .addModifiers(buildModifiers)
                    .addStatement(constructorStatement.toString(), constructorObjects.toArray())
                    .returns(ClassName.get(packageName, classSpec.name))
                    .build());
            additionalMethods.forEach((additionalMethod) -> {
                builder.addMethod(additionalMethod);
            });
            MethodSpec.Builder builderMethod = MethodSpec.methodBuilder("builder")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                    .addAnnotation(JsonIgnore.class)
                    .returns(builderName)
                    .addStatement("$N builder = new $N()", builderClassName, builderClassName);
            additionalFields.stream().map((field) -> {
                builderMethod.addParameter(field.type, field.name);
                return field;
            }).map((field) -> {
                builder.addField(field);
                return field;
            }).forEachOrdered((field) -> {
                builderMethod.addStatement("builder.$N = $N", field.name, field.name);
            });
            builderMethod.addStatement("return builder");
            typeSpecBuilder.addType(builder.build());
            typeSpecBuilder.addMethod(builderMethod.build());
            typeSpecBuilder.addMethod(constructor.build());
            return typeSpecBuilder;
        }
    }

    static MethodSpec builderGetter(FieldSpec field) {
        return MethodSpec.methodBuilder(field.name)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Get " + field.javadoc + "\n@return " + field.javadoc + "\n")
                .addAnnotation(JsonIgnore.class)
                .returns(field.type)
                .addStatement("return this.$N", field.name)
                .build();
    }

    static MethodSpec builderSetter(ClassName builderName, FieldSpec field) {
        return MethodSpec.methodBuilder(field.name)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builderName)
                .addJavadoc("Set " + field.javadoc + "\n@param value " + field.javadoc + "\n@return the Builder object.\n")
                .addParameter(field.type, "value")
                .addStatement("this.$N = value", field.name)
                .addStatement("return this")
                .build();
    }
}
