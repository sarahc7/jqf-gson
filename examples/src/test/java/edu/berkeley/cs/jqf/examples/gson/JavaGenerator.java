import java.lang.reflect.Type;
import java.math.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javafx.util.Pair;

import com.google.gson.*;
import com.google.gson.internal.*;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.squareup.javapoet.*;

import net.openhft.compiler.CompilerUtils;

public class JavaGenerator extends Generator<String> {

    private static Set<String> fieldNames;
    private static Set<String> typeNames;
    private static Map<String, Pair<String, String>> topLevelTypes;
    private static Map<String, Set<String>> enumTypes;
    private static boolean hasCustomTypeConverters;

    private final String INT = "int";
    private final String BOOLEAN = "boolean";
    private final String STRING = "String";
    private final String OBJECT = "Object";

    public JavaGenerator() {
        super(String.class);
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus __ignore__) {
        this.fieldNames = new HashSet<>();
        this.typeNames = new HashSet<>();
        this.topLevelTypes = new HashMap<>();
        this.enumTypes = new HashMap<>();

        TypeSpec.Builder type = TypeSpec.classBuilder("Main");

        generateEnumTypes(random);

        this.hasCustomTypeConverters = random.nextBoolean();
        String main = generateTypes(random, type).toString();

        //return topLevelTypes;

        return String.join("\n", topLevelTypes.values().stream().map(t -> t.getKey()).collect(Collectors.toList()));

//        if (!hasCustomTypeConverters) return new Pair(new Gson(), main);
//        else {
//            GsonBuilder builder = new GsonBuilder()
//                    .setPrettyPrinting()
//                    .serializeNulls()
//                    .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
//                    .enableComplexMapKeySerialization();
//            // System.out.println(typeConverters);
//            for (Pair<String, TypeSpec> p : typeConverters) {
//                try {
//                    String name = p.getKey(), adapter = p.getValue().toString();
////                    System.out.println("name: " + name);
//                    // System.out.println("java: " + topLevelTypes.get(name).toString());
//                    // feed only single adapter class
//                    builder.registerTypeHierarchyAdapter(CompilerUtils.CACHED_COMPILER.loadFromJava(name, topLevelTypes.get(name).toString()),
//                            CompilerUtils.CACHED_COMPILER.loadFromJava(name + "_Adapter", adapter).getDeclaredConstructor().newInstance());
//                } catch (Exception e) {
//                    System.err.println(e);
//                }
//            }
//
//            return new Pair(builder.create(), main);
//        }
    }

    private List<TypeSpec> generateEnumTypes(SourceOfRandomness random) {
        List<TypeSpec> types = new ArrayList<>();
        int numTypes = 1;
        for (int i = 0; i < numTypes; i++) {
            types.add(generateEnumType(random));
        }

        return types;
    }

    private String generateConstant(SourceOfRandomness random) {
        int len = random.nextInt(1, 6);
        String s = "";
        for (int i = 0; i < len; i++)
            s += random.nextChar('A', 'Z');

        return s;
    }

    private TypeSpec generateEnumType(SourceOfRandomness random) {
        String name = generateTypeName(random);
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(name);

        enumTypes.put(name, new HashSet<>());

        int numConstants = random.nextInt(1, 4);
        for (int i = 0; i < numConstants; i++) {
            String enumConstant = generateConstant(random);
            while (enumTypes.get(name).contains(enumConstant)) enumConstant = generateConstant(random);

            enumBuilder.addEnumConstant(enumConstant);
            enumTypes.get(name).add(enumConstant);
        }

        TypeSpec type = enumBuilder.build();
        if (random.nextBoolean()) topLevelTypes.put(name, new Pair(type.toString(), generateTypeAdapter(name).toString()));
        else topLevelTypes.put(name, new Pair(type.toString(), null));

        return type;
    }

    private TypeSpec generateTypes(SourceOfRandomness random, TypeSpec.Builder builder) {
        int numTypes = random.nextInt(3);
        for (int i = 0; i < numTypes; i++) {
            TypeSpec type = generateType(random);
            builder.addType(type);

            FieldSpec f = FieldSpec.builder(ClassName.bestGuess(type.name), generateFieldName(random))
                    .addModifiers(Modifier.PUBLIC)
                    .initializer("new $L()", type.name)
                    .build();
            builder.addField(f);
        }

        TypeSpec type = builder.build();
        topLevelTypes.put("Main", new Pair(type.toString(), null));
        return type;
    }

    private TypeSpec generateType(SourceOfRandomness random) {
        String name = generateTypeName(random);
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        List<FieldSpec> fields = generateFields(random, constructor);

        TypeSpec.Builder type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addFields(fields)
                .addMethod(constructor.build());

        int hasSubtypes = random.nextInt(4);
        if (hasSubtypes == 0) return generateTypes(random, type);

        return type.build();
    }

    private List<FieldSpec> generateFields(SourceOfRandomness random, MethodSpec.Builder constructor) {
        int numFields = random.nextInt(4);
        List<FieldSpec> fields = new ArrayList<>();
        for (int i = 0; i < numFields; i++) {
            String name = generateFieldName(random);
            fields.add(generateField(random, name, constructor));
        }

        return fields;
    }

    private FieldSpec generateField(SourceOfRandomness random, String name, MethodSpec.Builder constructor) {
        return random.choose(Arrays.<Supplier<FieldSpec>>asList(
                () -> generateEnumField(random, name, constructor)
//                () -> generateCollectionField(random, name),
//                () -> generateArrayField(random, name),
//                () -> generateStringField(random, name),
//                () -> generateStringBuilderField(random, name),
//                () -> generateStringBufferField(random, name),
//                () -> generateBooleanField(random, name),
//                () -> generateAtomicBooleanField(random, name),
//                () -> generateByteField(random, name),
//                () -> generateCharacterField(random, name),
//                () -> generateIntegerField(random, name),
//                () -> generateAtomicIntegerField(random, name),
//                () -> generateAtomicIntegerArrayField(random, name),
//                () -> generateNumberField(random, name),
//                () -> generateBigDecimalField(random, name),
//                () -> generateBigIntegerField(random, name),
//                () -> generateURLField(random, name, constructor),
//                () -> generateURIField(random, name, constructor),
//                () -> generateUUIDField(random, name),
//                () -> generateLocaleField(random, name),
//                () -> generateInetAddressField(random, name, constructor),
//                () -> generateBitSetField(random, name, constructor),
//                () -> generateDateField(random, name),
//                () -> generateCalendarField(random, name),
//                () -> generateCurrencyField(random, name)
        )).get();
    }

    private List<String> generateElements(SourceOfRandomness random, String type) {
        List<String> elements = new ArrayList<>();
        int numElements = random.nextInt(4);
        for (int i = 0; i < numElements; i++) {
            if (type.equals(INT)) {
                elements.add(generateInteger(random));
            } else if (type.equals(BOOLEAN)) {
                elements.add(generateBoolean(random));
            } else if (type.equals(STRING)) {
                elements.add("\"" + generateString(random) + "\"");
            } else {
                elements.add(random.choose(Arrays.<Supplier<String>>asList(
                        () -> generateInteger(random),
                        () -> generateBoolean(random),
                        () -> "\"" + generateString(random) + "\""
                )).get());
            }
        }

        return elements;
    }

    private FieldSpec generateEnumField(SourceOfRandomness random, String name, MethodSpec.Builder constructor) {
        if (enumTypes.size() == 0) generateField(random, name, constructor);

        ClassName enumType = ClassName.bestGuess(random.choose(enumTypes.keySet()));
        return FieldSpec.builder(enumType, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$T.$L", enumType, random.choose(enumTypes.get(enumType.simpleName())))
                .build();
    }

    private Type getTypeClass(String type) {
        if (type.equals(INT)) {
            return Integer.class;
        } else if (type.equals(BOOLEAN)) {
            return Boolean.class;
        } else if (type.equals(STRING)) {
            return String.class;
        } else {
            return Object.class;
        }
    }

    private FieldSpec generateCollectionField(SourceOfRandomness random, String name) {
        String type = random.choose(new String[]{INT, BOOLEAN, STRING, OBJECT});
        String elements = generateElements(random, type).stream().collect(Collectors.joining(", "));

        FieldSpec.Builder collectionField = FieldSpec.builder(ParameterizedTypeName
                .get(Collection.class, getTypeClass(type)), name);

        return collectionField.addModifiers(Modifier.PUBLIC).initializer("com.google.common.collect.ImmutableList.of($L)", elements).build();
    }

    private FieldSpec generateArrayField(SourceOfRandomness random, String name) {
        String type = random.choose(new String[]{INT, BOOLEAN, STRING, OBJECT});
        String elements = generateElements(random, type).stream().collect(Collectors.joining(", "));

        FieldSpec.Builder arrayField;
        if (type.equals(INT)) {
            arrayField = FieldSpec.builder(Integer[].class, name);
        } else if (type.equals(BOOLEAN)) {
            arrayField = FieldSpec.builder(Boolean[].class, name);
        } else if (type.equals(STRING)) {
            arrayField = FieldSpec.builder(String[].class, name);
        } else {
            arrayField = FieldSpec.builder(Object[].class, name);
        }

        return arrayField.addModifiers(Modifier.PUBLIC).initializer("{$L}", elements).build();
    }

    private FieldSpec generateMapField(SourceOfRandomness random, String name) {
        String t1 = random.choose(new String[]{INT, BOOLEAN, STRING, OBJECT});
        String t2 = random.choose(new String[]{INT, BOOLEAN, STRING, OBJECT});
        // List<String> e1 = generateElements(random, t1), e2 = generateElements(random, t2);

        return FieldSpec.builder(ParameterizedTypeName
                .get(Map.class, getTypeClass(t1), getTypeClass(t2)), name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T()", HashMap.class)
                .build();
    }

    private String generateString(SourceOfRandomness random) {
        int len = random.nextInt(1, 6);
        String s = "";
        for (int i = 0; i < len; i++)
            s += random.nextChar('a', 'z');

        return s;
    }

    private FieldSpec generateStringField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(String.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$S", random.choose(new String[]{generateString(random), "null"}))
                .build();
    }

    private FieldSpec generateStringBuilderField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(StringBuilder.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($S)", StringBuilder.class, generateString(random))
                .build();
    }

    private FieldSpec generateStringBufferField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(StringBuffer.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($S)", StringBuffer.class, generateString(random))
                .build();
    }

    private String generateBoolean(SourceOfRandomness random) {
        return random.choose(new String[]{"true", "false"});
    }

    private FieldSpec generateBooleanField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(Boolean.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$L", generateBoolean(random))
                .build();
    }

    private FieldSpec generateAtomicBooleanField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(AtomicBoolean.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($L)", AtomicBoolean.class, generateBoolean(random))
                .build();
    }

    private FieldSpec generateByteField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(Byte.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$L", random.nextBytes(1)[0])
                .build();
    }

    private FieldSpec generateCharacterField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(Character.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("'$L'", random.nextChar('a', 'z'))
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
        return FieldSpec.builder(Integer.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$L", generateInteger(random))
                .build();
    }

    private FieldSpec generateAtomicIntegerField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(AtomicInteger.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($L)", AtomicInteger.class, generateInteger(random))
                .build();
    }

    private FieldSpec generateAtomicIntegerArrayField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(AtomicIntegerArray.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($L)", AtomicIntegerArray.class, generateInteger(random))
                .build();
    }

    private FieldSpec generateNumberField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(Number.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($S)", LazilyParsedNumber.class, generateString(random))
                .build();
    }

    private FieldSpec generateBigDecimalField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(BigDecimal.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($S)", BigDecimal.class, generateInteger(random))
                .build();
    }

    private FieldSpec generateBigIntegerField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(BigInteger.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($S)", BigInteger.class, generateInteger(random))
                .build();
    }

    private FieldSpec generateURLField(SourceOfRandomness random, String name, MethodSpec.Builder constructor) {
        constructor.beginControlFlow("try")
                .addStatement("$L = new $T($S)", name, URL.class, "https://" + generateString(random) + ".com")
                .nextControlFlow("catch ($T e)", Exception.class)
                .addStatement("throw new $T(e)", RuntimeException.class)
                .endControlFlow();

        return FieldSpec.builder(URL.class, name)
                .addModifiers(Modifier.PUBLIC)
                .build();
    }

    private FieldSpec generateURIField(SourceOfRandomness random, String name, MethodSpec.Builder constructor) {
        constructor.beginControlFlow("try")
                .addStatement("$L = new $T($S)", name, URI.class, "/" + generateString(random))
                .nextControlFlow("catch ($T e)", Exception.class)
                .addStatement("throw new $T(e)", RuntimeException.class)
                .endControlFlow();

        return FieldSpec.builder(URI.class, name)
                .addModifiers(Modifier.PUBLIC)
                .build();
    }

    private FieldSpec generateUUIDField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(UUID.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($LL, $LL)", UUID.class, random.nextLong(), random.nextLong())
                .build();
    }

    private FieldSpec generateLocaleField(SourceOfRandomness random, String name) {
        String[] languages = {"CHINESE", "ENGLISH", "FRENCH", "GERMAN","JAPANESE", "KOREAN", "SIMPLIFIED_CHINESE", "TRADITIONAL_CHINESE"};

        return FieldSpec.builder(Locale.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$T.$L", Locale.class, random.choose(languages))
                .build();
    }

    private FieldSpec generateCurrencyField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(Currency.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$T.getInstance($S)", Currency.class, random.choose(Currency.getAvailableCurrencies()))
                .build();
    }

    private FieldSpec generateInetAddressField(SourceOfRandomness random, String name, MethodSpec.Builder constructor) {
        constructor.beginControlFlow("try")
                .addStatement("$T.getLocalHost()", InetAddress.class)
                .nextControlFlow("catch ($T e)", Exception.class)
                .addStatement("throw new $T(e)", RuntimeException.class)
                .endControlFlow();

        return FieldSpec.builder(InetAddress.class, name)
                .addModifiers(Modifier.PUBLIC)
                .build();
    }

    private String generateDigits(SourceOfRandomness random) {
        int maxDigits = random.nextInt(1, 3);
        String digits = "";
        for (int i = 0; i < maxDigits; i++) digits += random.nextInt(10);
        return digits;
    }

    private FieldSpec generateBitSetField(SourceOfRandomness random, String name, MethodSpec.Builder constructor) {
        int size = random.nextInt(0, 8);
        for (int i = 0; i < size; i++) {
            if (random.nextBoolean()) constructor.addStatement("$L.flip($L)", name, i);
        }

        return FieldSpec.builder(BitSet.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T()", BitSet.class)
                .build();
    }

    private TypeSpec generateTypeAdapter(String name) {
        MethodSpec serialize = MethodSpec.methodBuilder("serialize")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(JsonElement.class)
                .addParameter(ClassName.bestGuess(name), "src")
                .addParameter(Type.class, "typeOfSrc")
                .addParameter(JsonSerializationContext.class, "context")
                .addStatement("return new $T(src.name())", JsonPrimitive.class)
                .build();

        MethodSpec deserialize = MethodSpec.methodBuilder("deserialize")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.bestGuess(name))
                .addParameter(JsonElement.class, "json")
                .addParameter(Type.class, "typeOfT")
                .addParameter(JsonDeserializationContext.class, "context")
                .addStatement("return $L.valueOf(json.getAsString())", name)
                .build();

        TypeSpec typeConverter = TypeSpec.classBuilder(name + "_Adapter")
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(JsonSerializer.class), ClassName.bestGuess(name)))
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(JsonDeserializer.class), ClassName.bestGuess(name)))
                .addMethod(serialize)
                .addMethod(deserialize)
                .build();

        return typeConverter;
    }

    private FieldSpec generateDateField(SourceOfRandomness random, String name) {
//        String serializeRet = CodeBlock.builder().addStatement("return new $T(src.toString())", JsonPrimitive.class).toString();
//        String deserializeRet = CodeBlock.builder().addStatement("return new $T(json.getAsJsonPrimitive().getAsString())", Date.class).toString();
//        TypeSpec typeConverter = generateTypeAdapter("DateTypeAdapter", Date.class, serializeRet, deserializeRet);
//
//        typeConverters.add(typeConverter);

        return FieldSpec.builder(Date.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T()", Date.class)
                .build();
    }

    private FieldSpec generateCalendarField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(Calendar.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($L, $L, $L, $L, $L, $L)",
                        GregorianCalendar.class,
                        random.nextInt(),
                        random.nextInt(),
                        random.nextInt(),
                        random.nextInt(),
                        random.nextInt(),
                        random.nextInt())
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
