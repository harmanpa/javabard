/*
 * Copyright 2022 CAE Tech Limited.
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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;

/**
 *
 * @author peter
 */
public class VisitorSpec {

    public static Builder builder(String packageName, String rootName) {
        return new Builder(packageName, rootName);
    }

    public static class Builder {

        private final String packageName;
        private final String rootName;
        private final Map<TypeName, List<TypeSpec.Builder>> implementingTypes = new LinkedHashMap<>();
        private final Map<TypeName, TypeSpec.Builder> visitedTypes = new LinkedHashMap<>();

        Builder(String packageName, String rootName) {
            this.packageName = packageName;
            this.rootName = rootName;
        }

        public Builder withVisitingType(String packageName, TypeSpec.Builder type, TypeSpec.Builder... implementors) {
            visitedTypes.put(ClassName.get(packageName == null ? this.packageName : packageName, type.build().name), type);
            return withImplementors(packageName, type, implementors);
        }

        public Builder withVisitingType(TypeSpec.Builder type) {
            return withVisitingType(null, type);
        }

        public Builder withImplementors(String packageName, TypeSpec.Builder of, TypeSpec.Builder... implementors) {
            if (implementors.length > 0) {
                implementingTypes.merge(ClassName.get(packageName == null ? this.packageName : packageName, of.build().name),
                        Arrays.asList(implementors),
                        (a, b) -> {
                            List<TypeSpec.Builder> c = new ArrayList<>(a);
                            c.addAll(b);
                            return c;
                        });
            }
            return this;
        }

        public Builder withImplementors(TypeSpec.Builder of, TypeSpec.Builder... implementors) {
            return withImplementors(null, of, implementors);
        }

        public VisitorInterfaceAndBase build() {
            TypeSpec.Builder visitor = TypeSpec.interfaceBuilder(rootName + "Visitor")
                    .addModifiers(Modifier.PUBLIC);
            TypeName visitorName = ClassName.get(packageName, rootName + "Visitor");
            TypeSpec.Builder base = TypeSpec.classBuilder(rootName + "VisitorBase")
                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                    .addSuperinterface(visitorName);
            for (Map.Entry<TypeName, TypeSpec.Builder> entry : visitedTypes.entrySet()) {
                Map<TypeName, String> visitingFields = new LinkedHashMap<>();
                for (FieldSpec field : entry.getValue().fieldSpecs) {
                    if (visitedTypes.containsKey(field.type)) {
                        visitingFields.put(field.type, field.name);
                    }
                }
                if (entry.getValue().build().kind == Kind.INTERFACE) {

                } else {
                    MethodSpec.Builder visitMethod = MethodSpec.methodBuilder("visit")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addParameter(visitorName, "visitor");
                    visitMethod.addCode("visitor.enter$T(this)", entry.getKey());
                    for (Map.Entry<TypeName, String> visitingField : visitingFields.entrySet()) {
                        visitMethod.beginControlFlow("if($T==null)", visitingField.getValue());
                        visitMethod.addCode("visitor.enter$T(null)", visitingField.getKey());
                        visitMethod.addCode("visitor.exit$T(null)", visitingField.getKey());
                        visitMethod.nextControlFlow("else");
                        visitMethod.addCode("$N.visit(visitor)", visitingField.getValue());
                        visitMethod.endControlFlow();
                    }
                    visitMethod.addCode("visitor.exit$T(this)", entry.getKey());
                    entry.getValue().addMethod(visitMethod.build());
                }
                String typeName = entry.getValue().build().name;
                visitor.addMethod(MethodSpec.methodBuilder("enter" + typeName)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(entry.getKey(), "visited")
                        .build());
                base.addMethod(MethodSpec.methodBuilder("enter" + typeName)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(entry.getKey(), "visited")
                        .addAnnotation(Override.class)
                        .build());
                visitor.addMethod(MethodSpec.methodBuilder("exit" + typeName)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(entry.getKey(), "visited")
                        .build());
                base.addMethod(MethodSpec.methodBuilder("exit" + typeName)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(entry.getKey(), "visited")
                        .addAnnotation(Override.class)
                        .build());
            }
            return new VisitorInterfaceAndBase(visitor, base);
        }

        Map<TypeName, String> getVisitingFields(TypeName typeName, TypeSpec.Builder typeSpec) {
            Map<TypeName, String> visitingFields = new LinkedHashMap<>();
            for (FieldSpec field : typeSpec.fieldSpecs) {
                if (visitedTypes.containsKey(field.type)) {
                    visitingFields.put(field.type, field.name);
                }
            }
            return visitingFields;
        }
    }

    public static class VisitorInterfaceAndBase {

        private final TypeSpec.Builder visitor;
        private final TypeSpec.Builder base;

        VisitorInterfaceAndBase(TypeSpec.Builder visitor, TypeSpec.Builder base) {
            this.visitor = visitor;
            this.base = base;
        }

        public TypeSpec.Builder getVisitor() {
            return visitor;
        }

        public TypeSpec.Builder getBase() {
            return base;
        }

    }
}
