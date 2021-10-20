import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.squareup.javapoet.*;

public class JavaGenerator extends Generator<String> {

    private static Set<String> fieldNames;
    private static Set<String> typeNames;

    public JavaGenerator() {
        super(String.class);
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus __ignore__) {
        this.fieldNames = new HashSet<>();
        this.typeNames = new HashSet<>();

        List<FieldSpec> fields = generateFields(random);
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build();
        TypeSpec.Builder type = TypeSpec.classBuilder("Main")
                .addModifiers(Modifier.PUBLIC)
                .addFields(fields)
                .addMethod(constructor);

        return generateTypes(random, type).toString();
    }

    private TypeSpec generateTypes(SourceOfRandomness random, TypeSpec.Builder outer) {

        int numTypes = random.nextInt(3);
        for (int i = 0; i < numTypes; i++) {
            TypeSpec type = generateType(random);
            outer.addType(type);

            FieldSpec f = FieldSpec.builder(ClassName.bestGuess(type.name), generateFieldName(random))
                    .addModifiers(Modifier.PUBLIC)
                    .initializer("new $L()", type.name)
                    .build();
            outer.addField(f);

//            ClassName outerName = ClassName.get("com.example.project", "Outer");
//            ClassName innerName = outerName.nestedClass("Inner");
//            FieldSpec f = FieldSpec.builder(TypeName.get(type), generateFieldName(random))
//                    .addModifiers(Modifier.PUBLIC)
//                    .initializer("new $L()", type.name)
//                    .build();
//            obj.addField(f);
        }

        return outer.build();
    }

    private TypeSpec generateType(SourceOfRandomness random) {
        String name = generateTypeName(random);
        List<FieldSpec> fields = generateFields(random);

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build();
        TypeSpec.Builder type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addFields(fields)
                .addMethod(constructor);

        boolean hasSubtypes = random.nextBoolean();
        if (hasSubtypes) return generateTypes(random, type);

        return type.build();
    }

    private List<FieldSpec> generateFields(SourceOfRandomness random) {
        int numFields = random.nextInt(4);
        List<FieldSpec> fields = new ArrayList<>();
        for (int i = 0; i < numFields; i++) {
            String name = generateFieldName(random);
            fields.add(generateField(random, name));
        }

        return fields;
    }

    private FieldSpec generateField(SourceOfRandomness random, String name) {
        return random.choose(Arrays.<Supplier<FieldSpec>>asList(
                () -> generateArrayField(random, name),
                () -> generateStringField(random, name),
                () -> generateIntegerField(random, name),
                () -> generateBooleanField(random, name),
                () -> generateNullField(random, name)
        )).get();
    }

    private FieldSpec generateArrayField(SourceOfRandomness random, String name) {
        String type = random.choose(new String[]{"int", "boolean", "String"});

        List<String> elements = new ArrayList<>();
        int numElements = random.nextInt(4);
        for (int i = 0; i < numElements; i++) {
            if (type.equals("int")) {
                elements.add(generateInteger(random));
            } else if (type.equals("boolean")) {
                elements.add(generateBoolean(random));
            } else {
                elements.add(generateString(random));
            }
        }

        String arrayContents = elements.stream().collect(Collectors.joining(", "));

        FieldSpec.Builder arrayField;
        if (type.equals("int")) {
            arrayField = FieldSpec.builder(int[].class, name);
        } else if (type.equals("boolean")) {
            arrayField = FieldSpec.builder(boolean[].class, name);
        } else {
            arrayField = FieldSpec.builder(String[].class, name);
        }
        return arrayField.addModifiers(Modifier.PUBLIC).initializer("{$L}", arrayContents).build();
    }

    private String generateString(SourceOfRandomness random) {
        int len = random.nextInt(6);
        String s = "\"";
        for (int i = 0; i < len; i++)
            s += random.nextChar('a', 'z');
        s += "\"";

        return s;
    }

    private FieldSpec generateStringField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(String.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$L", generateString(random))
                .build();
    }

    private String generateInteger(SourceOfRandomness random) {
        return random.choose(Arrays.<Supplier<String>>asList(
                () -> Integer.toString(random.nextInt(10)),
                () -> Integer.toString(random.nextInt(1, 10)) + generateDigits(random),
                () -> "-" + random.nextInt(10),
                () -> "-" + random.nextInt(1, 10) + generateDigits(random)
        )).get();
    }

    private FieldSpec generateIntegerField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(int.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$L", generateInteger(random))
                .build();
    }

    private String generateDigits(SourceOfRandomness random) {
        int maxDigits = random.nextInt(1, 3);
        String digits = "";
        for (int i = 0; i < maxDigits; i++) digits += random.nextInt(10);
        return digits;
    }

    private String generateBoolean(SourceOfRandomness random) {
        return random.choose(Arrays.<Supplier<String>>asList(
                () -> "false",
                () -> "true"
        )).get();
    }

    private FieldSpec generateBooleanField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(boolean.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$L", generateBoolean(random))
                .build();
    }

    private FieldSpec generateNullField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(String.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("null")
                .build();
    }

    private String generateFieldName(SourceOfRandomness random) {
        String s = random.nextChar('a', 'z') + "_" + fieldNames.size();
        fieldNames.add(s);
        return s;
    }

    private String generateTypeName(SourceOfRandomness random) {
        String s = random.nextChar('A', 'Z') + "_" + typeNames.size();
        typeNames.add(s);
        return s;
    }

}
