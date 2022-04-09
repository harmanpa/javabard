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

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.lang.model.element.Modifier;

/**
 *
 * @author peter
 */
public class CopySpec {
    
    public static CopySpec.Builder forType(TypeSpec.Builder typeSpecBuilder) {
        return new Builder(typeSpecBuilder);
    }
    
    public static class Builder {

        private final TypeSpec.Builder typeSpecBuilder;
        private final TypeSpec typeSpec;
        private String typeName;
        private Function<FieldSpec, FieldSpec> fieldMapper = (f) -> f;
        private Predicate<FieldSpec> fieldFilter = (f) -> true;
        private Function<MethodSpec, MethodSpec> methodMapper = (m) -> m;
        private Predicate<MethodSpec> methodFilter = (m) -> true;
        private Function<TypeName, TypeName> superinterfaceMapper = (t) -> t;
        private Predicate<TypeName> superinterfaceFilter = (t) -> true;
        private boolean removeSuperclass = false;

        Builder(TypeSpec.Builder typeSpecBuilder) {
            this.typeSpecBuilder = typeSpecBuilder;
            this.typeSpec = this.typeSpecBuilder.build();
        }
        
        public Builder withName(String name) {
            this.typeName = name;
            return this;
        }
        
        public Builder modifyFields(Function<FieldSpec,FieldSpec> mapper) {
            this.fieldMapper = mapper;
            return this;
        }
        
        public Builder filterFields(Predicate<FieldSpec> filter) {
            this.fieldFilter = filter;
            return this;
        }
        
        public Builder modifyMethods(Function<MethodSpec,MethodSpec> mapper) {
            this.methodMapper = mapper;
            return this;
        }
        
        public Builder filterMethods(Predicate<MethodSpec> filter) {
            this.methodFilter = filter;
            return this;
        }
        
        public Builder modifySuperinterfaces(Function<TypeName,TypeName> mapper) {
            this.superinterfaceMapper = mapper;
            return this;
        }
        
        public Builder filterSuperinterfaces(Predicate<TypeName> filter) {
            this.superinterfaceFilter = filter;
            return this;
        }
        
        public Builder withoutSuperclass() {
            this.removeSuperclass = true;
            return this;
        }

        private String getTargetName() {
            return typeName==null ? this.typeSpec.name : typeName;
        }

        public TypeSpec.Builder build() {
            TypeSpec.Builder newType;
            switch(typeSpec.kind) {
                case ANNOTATION:
                    newType = TypeSpec.annotationBuilder(getTargetName());
                    break;
                case CLASS:
                    newType = TypeSpec.classBuilder(getTargetName());
                    break;
                case ENUM:
                    newType = TypeSpec.enumBuilder(getTargetName());
                    break;
                default:
                    newType = TypeSpec.interfaceBuilder(getTargetName());
                    break;
            }
            newType.addAnnotations(typeSpec.annotations);
            typeSpec.methodSpecs.stream().filter(methodFilter).map(methodMapper).forEach(m -> newType.addMethod(m));
            typeSpec.fieldSpecs.stream().filter(fieldFilter).map(fieldMapper).forEach(f -> newType.addField(f));
            newType.addTypeVariables(typeSpec.typeVariables);
            typeSpec.enumConstants.forEach((name, spec) -> newType.addEnumConstant(name, spec));
            newType.alwaysQualify(typeSpec.alwaysQualifiedNames.toArray(new String[0]));
            if(!typeSpec.initializerBlock.isEmpty()) {
                newType.addInitializerBlock(typeSpec.initializerBlock);
            }
            newType.addJavadoc(typeSpec.javadoc);
            newType.addModifiers(typeSpec.modifiers.toArray(new Modifier[0]));
            typeSpec.originatingElements.forEach(oe -> newType.addOriginatingElement(oe));
            if(!typeSpec.staticBlock.isEmpty()) {
                newType.addStaticBlock(typeSpec.staticBlock);
            }
            typeSpec.superinterfaces.stream().filter(superinterfaceFilter).map(superinterfaceMapper).forEach(si -> newType.addSuperinterface(si));
            newType.addTypes(typeSpec.typeSpecs);
            if(!removeSuperclass && typeSpec.superclass!=null) {
                newType.superclass(typeSpec.superclass);
            }
            return newType;
        }
    }
}
