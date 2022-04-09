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

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Collection;
import javax.lang.model.element.Modifier;

/**
 *
 * @author peter
 */
public class InterfaceSpec {
    
    public static InterfaceSpec.Builder forType(String interfacePackageName, TypeSpec.Builder typeSpecBuilder) {
        return new Builder(interfacePackageName, typeSpecBuilder);
    }
    
    public static class Builder {
        
        private final String packageName;
        private final TypeSpec.Builder typeSpecBuilder;
        private final TypeSpec typeSpec;
        private String interfaceNamingConvention = "$N";
        private String implementingTypeNamingConvention = "$N";
        
        Builder(String packageName, TypeSpec.Builder typeSpecBuilder) {
            this.packageName = packageName;
            this.typeSpecBuilder = typeSpecBuilder;
            this.typeSpec = typeSpecBuilder.build();
        }
        
        public Builder withInterfaceNamingConvention(String name) {
            this.interfaceNamingConvention = name;
            return this;
        }
        
        public Builder withImplementingTypeNamingConvention(String name) {
            this.implementingTypeNamingConvention = name;
            return this;
        }
        
        public ClassName getInterfaceName() {
            return ClassName.get(packageName, makeName(interfaceNamingConvention));
        }
        
        private String makeName(String convention) {
            return convention.replace("$N", typeSpec.name);
        }
        
        public InterfaceAndImplementingType build() {
            TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(getInterfaceName());
            interfaceBuilder.addModifiers(typeSpec.modifiers.toArray(new Modifier[0]));
            typeSpec.superinterfaces.forEach(si -> interfaceBuilder.addSuperinterface(si));
            typeSpec.methodSpecs.stream()
                    .filter((MethodSpec mm) -> mm.hasModifier(Modifier.PUBLIC))
                    .forEach((MethodSpec mm) -> {
                        interfaceBuilder.addMethod(MethodSpec.methodBuilder(mm.name)
                                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                .returns(mm.returnType)
                                .addParameters(mm.parameters)
                                .varargs(mm.varargs)
                                .addJavadoc(mm.javadoc).build());
                    });
            TypeSpec.Builder implementingClassBuilder = CopySpec.forType(typeSpecBuilder)
                    .withName(makeName(implementingTypeNamingConvention))
                    .withoutSuperclass()
                    .filterSuperinterfaces(si -> false)
                    .modifyMethods((MethodSpec m) -> m.hasModifier(Modifier.PUBLIC) && !hasOverride(m.annotations)
                    ? m.toBuilder().addAnnotation(Override.class).build()
                    : m)
                    .build()
                    .addSuperinterface(getInterfaceName());
            return new InterfaceAndImplementingType(interfaceBuilder, implementingClassBuilder);
        }
        
        private boolean hasOverride(Collection<AnnotationSpec> annotations) {
            TypeName overrideType = TypeName.get(Override.class);
            return annotations.stream().anyMatch(as -> overrideType.equals(as.type));
        }
    }
    
    public static class InterfaceAndImplementingType {
        
        private final TypeSpec.Builder interfaceBuilder;
        private final TypeSpec.Builder implementingTypeBuilder;
        
        InterfaceAndImplementingType(TypeSpec.Builder interfaceBuilder, TypeSpec.Builder implementingTypeBuilder) {
            this.interfaceBuilder = interfaceBuilder;
            this.implementingTypeBuilder = implementingTypeBuilder;
        }
        
        public TypeSpec.Builder getInterface() {
            return interfaceBuilder;
        }
        
        public TypeSpec.Builder getImplementingType() {
            return implementingTypeBuilder;
        }
        
    }
}
