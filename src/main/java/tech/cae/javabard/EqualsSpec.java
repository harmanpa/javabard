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
import com.squareup.javapoet.TypeSpec.Kind;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.lang.model.element.Modifier;

/**
 *
 * @author peter
 */
public class EqualsSpec {

    public static EqualsSpec.Builder forType(TypeSpec.Builder typeSpecBuilder) {
        return new Builder(typeSpecBuilder);
    }

    public static class Builder {

        private final TypeSpec.Builder typeSpecBuilder;
        private final TypeSpec typeSpec;

        Builder(TypeSpec.Builder typeSpecBuilder) {
            this.typeSpecBuilder = typeSpecBuilder;
            this.typeSpec = this.typeSpecBuilder.build();
        }

        MethodSpec makeHashCode(List<FieldSpec> fields, TypeName superclass) {
            MethodSpec.Builder msb = MethodSpec.methodBuilder("hashCode")
                    .returns(TypeName.INT)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class);
            int first = randomPrime();
            int mult = randomPrime();
            msb.addStatement("int hash = $L", first);
            for (FieldSpec field : fields) {
                if (!field.hasModifier(Modifier.STATIC)) {
                    if (TypeName.BOOLEAN.equals(field.type)) {
                        msb.addStatement("hash = $L * hash + (this.$N ? 1 : 0)", mult, field);
                    } else if (TypeName.DOUBLE.equals(field.type)) {
                        msb.addStatement("hash = $L * hash + (int) ($T.doubleToLongBits(this.$N) ^ ($T.doubleToLongBits(this.$N) >>> 32))", mult, Double.class, field, Double.class, field);
                    } else if (TypeName.LONG.equals(field.type)) {
                        msb.addStatement("hash = $L * hash + (int) (this.$N ^ (this.$N >>> 32))", mult, field, field);
                    } else if (TypeName.FLOAT.equals(field.type)) {
                        msb.addStatement("hash = $L * hash + $T.floatToIntBits(this.$N)", mult, Float.class, field);
                    } else if (field.type.isPrimitive()) {
                        msb.addStatement("hash = $L * hash + this.$N", mult, field);
                    } else {
                        msb.addStatement("hash = $L * hash + $T.hashCode(this.$N)", mult, Objects.class, field);
                    }
                }
            }
            if (superclass != null && !TypeName.OBJECT.equals(superclass)) {
                msb.addStatement("hash = $L * hash + super.hashCode()", mult);
            }
            msb.addStatement("return hash");
            return msb.build();
        }

        MethodSpec makeEquals(List<FieldSpec> fields, TypeName superclass) {
            MethodSpec.Builder msb = MethodSpec.methodBuilder("equals")
                    .returns(TypeName.BOOLEAN)
                    .addParameter(TypeName.OBJECT, "obj")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class);
            msb.beginControlFlow("if (this == obj)").addStatement("return true").endControlFlow();
            msb.beginControlFlow("if (obj == null)").addStatement("return false").endControlFlow();
            msb.beginControlFlow("if (obj instanceof $N)", typeSpec);
            msb.addStatement("final $N other = ($N)obj", typeSpec, typeSpec);
            for (FieldSpec field : fields) {
                if (!field.hasModifier(Modifier.STATIC)) {
                    if (TypeName.DOUBLE.equals(field.type)) {
                        msb.beginControlFlow("if ($T.doubleToLongBits(this.$N) != $T.doubleToLongBits(other.$N))", Double.class, field, Double.class, field).addStatement("return false").endControlFlow();
                    } else if (TypeName.FLOAT.equals(field.type)) {
                        msb.beginControlFlow("if ($T.floatToIntBits(this.$N) != $T.floatToIntBits(other.$N))", Float.class, field, Float.class, field).addStatement("return false").endControlFlow();
                    } else if (field.type.isPrimitive()) {
                        msb.beginControlFlow("if (this.$N != other.$N)", field, field).addStatement("return false").endControlFlow();
                    } else {
                        msb.beginControlFlow("if (!$T.equals(this.$N, other.$N))", Objects.class, field, field).addStatement("return false").endControlFlow();
                    }
                }
            }
            if (superclass != null && !TypeName.OBJECT.equals(superclass)) {
                msb.addStatement("return super.equals(obj)");
            } else {
                msb.addStatement("return true");
            }
            msb.endControlFlow();
            msb.addStatement("return false");
            return msb.build();
        }

        public TypeSpec.Builder build() {
            if (this.typeSpec.kind == Kind.CLASS) {
                return this.typeSpecBuilder
                        .addMethod(makeHashCode(this.typeSpec.fieldSpecs, this.typeSpec.superclass))
                        .addMethod(makeEquals(this.typeSpec.fieldSpecs, this.typeSpec.superclass));
            } else {
                return this.typeSpecBuilder;
            }
        }
    }

    private static int randomPrime() {
        Random rand = new Random(); // generate a random number
        int num = rand.nextInt(23) + 1;
        while (!isPrime(num)) {
            num = rand.nextInt(23) + 1;
        }
        return num;
    }

    private static boolean isPrime(int inputNum) {
        if (inputNum <= 3 || inputNum % 2 == 0) {
            return inputNum == 2 || inputNum == 3; //this returns false if number is <=1 & true if number = 2 or 3
        }
        int divisor = 3;
        while ((divisor <= Math.sqrt(inputNum)) && (inputNum % divisor != 0)) {
            divisor += 2; //iterates through all possible divisors
        }
        return inputNum % divisor != 0; //returns true/false
    }
}
