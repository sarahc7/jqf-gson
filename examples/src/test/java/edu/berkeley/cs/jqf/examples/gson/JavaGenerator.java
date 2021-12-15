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

public class JavaGenerator extends Generator<Pair> {

    private static Set<String> fieldNames;
    private static Set<String> topLevelTypeNames;
    private static Set<String> typeNames;

    private final String BOOLEAN = "Boolean";
    private final String CHARACTER = "Character";
    private final String STRING = "String";
    private final String NUMBER = "Number";
    private final String OBJECT = "Object";
    private final String[] TYPES = {BOOLEAN, CHARACTER, STRING, NUMBER, OBJECT};

    public JavaGenerator() {
        super(Pair.class);
        this.topLevelTypeNames = new HashSet<>();
    }

    @Override
    public Pair generate(SourceOfRandomness random, GenerationStatus __ignore__) {
        this.fieldNames = new HashSet<>();
        this.typeNames = new HashSet<>();

        String className = generateTopLevelTypeName(random);
        TypeSpec.Builder type = TypeSpec.classBuilder(className);
        return new Pair(generateGenerator(random), new Pair(className, generateTypes(random, type).toString()));
    }

    private static class SpecificClassExclusionStrategy implements ExclusionStrategy {
        private final Class<?> excludedThisClass;

        public SpecificClassExclusionStrategy(Class<?> excludedThisClass) {
            this.excludedThisClass = excludedThisClass;
        }

        public boolean shouldSkipClass(Class<?> clazz) {
            return excludedThisClass.equals(clazz);
        }

        public boolean shouldSkipField(FieldAttributes f) {
            return excludedThisClass.equals(f.getDeclaredClass());
        }
    }

    public Gson generateGenerator(SourceOfRandomness random) {
        GsonBuilder gson = new GsonBuilder();
//        gson.addDeserializationExclusionStrategy(ExclusionStrategy strategy);
//        gson.addSerializationExclusionStrategy(ExclusionStrategy strategy);
        if (random.nextBoolean()) gson.disableHtmlEscaping();
        if (random.nextBoolean()) gson.disableInnerClassSerialization();
        if (random.nextBoolean()) gson.enableComplexMapKeySerialization();
//        if (random.nextBoolean()) gson.excludeFieldsWithModifiers(int... modifiers);
        if (random.nextBoolean()) gson.excludeFieldsWithoutExposeAnnotation();
        if (random.nextBoolean()) gson.generateNonExecutableJson();
//        if (random.nextBoolean()) gson.registerTypeAdapter(java.lang.reflect.Type type, java.lang.Object typeAdapter);
//        if (random.nextBoolean()) gson.registerTypeAdapterFactory(TypeAdapterFactory factory);
//        if (random.nextBoolean()) gson.registerTypeHierarchyAdapter(java.lang.Class<?> baseType, java.lang.Object typeAdapter);
        if (random.nextBoolean()) gson.serializeNulls();
        if (random.nextBoolean()) gson.serializeSpecialFloatingPointValues();
        if (random.nextBoolean()) gson.setDateFormat(DateFormat.DEFAULT);
        if (random.nextBoolean()) gson.setDateFormat(DateFormat.DEFAULT, DateFormat.DEFAULT);
        if (random.nextBoolean()) gson.setDateFormat(new SimpleDateFormat().toPattern());
        if (random.nextBoolean()) gson.setExclusionStrategies(new SpecificClassExclusionStrategy(getTypeClass(random.choose(TYPES))));
        if (random.nextBoolean()) {
            gson.setFieldNamingPolicy(random.choose(new HashSet<>(Arrays.asList(
                    FieldNamingPolicy.IDENTITY,
                    FieldNamingPolicy.LOWER_CASE_WITH_DASHES,
                    FieldNamingPolicy.LOWER_CASE_WITH_DOTS,
                    FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES,
                    FieldNamingPolicy.UPPER_CAMEL_CASE,
                    FieldNamingPolicy.UPPER_CAMEL_CASE_WITH_SPACES
            ))));
        }
//        if (random.nextBoolean()) gson.setFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy);
        if (random.nextBoolean()) gson.setLenient();
//        if (random.nextBoolean()) gson.setLongSerializationPolicy(LongSerializationPolicy serializationPolicy);
        if (random.nextBoolean()) gson.setPrettyPrinting();
//        if (random.nextBoolean()) gson.setVersion(double ignoreVersionsAfter);
        return gson.create();
    }

    private String generateConstant(SourceOfRandomness random) {
        int len = random.nextInt(1, 6);
        String s = "";
        for (int i = 0; i < len; i++)
            s += random.nextChar('A', 'Z');

        return s;
    }

    private TypeSpec generateTypes(SourceOfRandomness random, TypeSpec.Builder builder) {
        int numTypes = random.nextInt(3);
        String[] types = {"TYPE", "ENUM"};
        for (int i = 0; i < numTypes; i++) {
            String typeName = random.choose(types);

            TypeSpec type;
            FieldSpec f;
            if (typeName.equals("TYPE")) {
                type = generateType(random);
                f = FieldSpec.builder(ClassName.bestGuess(type.name), generateFieldName(random))
                        .addModifiers(Modifier.PUBLIC)
                        .initializer("new $L()", type.name)
                        .build();
            } else {
                Pair<TypeSpec, Set> p = generateEnumType(random);
                type = p.getKey();
                f = FieldSpec.builder(ClassName.bestGuess(type.name), generateFieldName(random))
                        .addModifiers(Modifier.PUBLIC)
                        .initializer("$L.$L", type.name, random.choose(p.getValue()))
                        .build();
            }

            builder.addType(type).addField(f);
        }

        return builder.build();
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

        boolean hasSubtypes = random.nextBoolean();
        if (hasSubtypes) return generateTypes(random, type);

        return type.build();
    }

    private Pair generateEnumType(SourceOfRandomness random) {
        String name = generateTypeName(random);
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(name);

        int numConstants = random.nextInt(1, 4);
        Set<String> enumTypes = new HashSet<>();
        for (int i = 0; i < numConstants; i++) {
            String enumConstant = generateConstant(random);
            while (enumTypes.contains(enumConstant)) enumConstant = generateConstant(random);

            enumBuilder.addEnumConstant(enumConstant);
            enumTypes.add(enumConstant);
        }

        return new Pair(enumBuilder.build(), enumTypes);
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
                () -> generateCollectionField(random, name),
                () -> generateArrayField(random, name),
                () -> generateStringField(random, name),
                () -> generateStringBuilderField(random, name),
                () -> generateStringBufferField(random, name),
                () -> generateBooleanField(random, name),
                () -> generateAtomicBooleanField(random, name),
                () -> generateCharacterField(random, name),
                () -> generateIntegerField(random, name),
                () -> generateAtomicIntegerField(random, name),
                () -> generateAtomicIntegerArrayField(random, name),
                () -> generateNumberField(random, name),
                () -> generateBigDecimalField(random, name),
                () -> generateBigIntegerField(random, name),
                () -> generateURLField(random, name, constructor),
                () -> generateURIField(random, name, constructor),
                () -> generateUUIDField(random, name),
                () -> generateLocaleField(random, name),
                () -> generateInetAddressField(random, name, constructor),
                () -> generateBitSetField(random, name, constructor),
                () -> generateDateField(random, name),
                () -> generateCalendarField(random, name),
                () -> generateCurrencyField(random, name),
                () -> generateJsonElementField(random, name),
                () -> generateJsonArrayField(random, name, constructor),
                () -> generateJsonObjectField(random, name, constructor)
        )).get();
    }

    private Class getTypeClass(String type) {
        if (type.equals(BOOLEAN)) {
            return Boolean.class;
        } else if (type.equals(CHARACTER)) {
            return Character.class;
        } else if (type.equals(STRING)) {
            return String.class;
        } else if (type.equals(NUMBER)) {
            return Number.class;
        } else {
            return Object.class;
        }
    }

    private String getValue(SourceOfRandomness random, String type) {
        if (type.equals(BOOLEAN)) {
            return Boolean.toString(random.nextBoolean());
        } else if (type.equals(CHARACTER)) {
            return "'" + generateCharacter(random) + "'";
        } else if (type.equals(STRING)) {
            return "\"" + generateString(random) + "\"";
        } else if (type.equals(NUMBER)) {
            return generateNumber(random);
        } else {
            return getValue(random, random.choose(TYPES));
        }
    }

    private List<String> generateElements(SourceOfRandomness random, String type) {
        List<String> elements = new ArrayList<>();
        int numElements = random.nextInt(4);
        for (int i = 0; i < numElements; i++) {
            elements.add(getValue(random, type));
        }

        return elements;
    }

    private FieldSpec generateCollectionField(SourceOfRandomness random, String name) {
        String type = random.choose(TYPES);
        String elements = generateElements(random, type).stream().collect(Collectors.joining(", "));

        FieldSpec.Builder collectionField = FieldSpec.builder(ParameterizedTypeName
                .get(Collection.class, getTypeClass(type)), name);

        return collectionField.addModifiers(Modifier.PUBLIC).initializer("com.google.common.collect.ImmutableList.of($L)", elements).build();
    }

    private FieldSpec generateArrayField(SourceOfRandomness random, String name) {
        String type = random.choose(TYPES);
        String elements = generateElements(random, type).stream().collect(Collectors.joining(", "));

        FieldSpec.Builder arrayField;
        if (type.equals(BOOLEAN)) {
            arrayField = FieldSpec.builder(Boolean[].class, name);
        } else if (type.equals(CHARACTER)) {
            arrayField = FieldSpec.builder(Character[].class, name);
        } else if (type.equals(STRING)) {
            arrayField = FieldSpec.builder(String[].class, name);
        } else if (type.equals(NUMBER)) {
            arrayField = FieldSpec.builder(Number[].class, name);
        } else {
            arrayField = FieldSpec.builder(Object[].class, name);
        }

        return arrayField.addModifiers(Modifier.PUBLIC).initializer("{$L}", elements).build();
    }

//    private FieldSpec generateMapField(SourceOfRandomness random, String name, MethodSpec.Builder constructor) {
//        String t1 = random.choose(TYPES), t2 = random.choose(TYPES);
//
//        int numEntries = random.nextInt(4);
//        for (int i = 0; i < numEntries; i++) {
//            constructor.addStatement("$L.put($L, $L)", name, getValue(random, t1), getValue(random, t2));
//        }
//
//        return FieldSpec.builder(ParameterizedTypeName
//                .get(LinkedTreeMap.class, getTypeClass(t1), getTypeClass(t2)), name)
//                .addModifiers(Modifier.PUBLIC)
//                .initializer("new $T()", LinkedTreeMap.class)
//                .build();
//    }

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

    private FieldSpec generateBooleanField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(Boolean.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$L", random.nextBoolean())
                .build();
    }

    private FieldSpec generateAtomicBooleanField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(AtomicBoolean.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($L)", AtomicBoolean.class, random.nextBoolean())
                .build();
    }

//    private FieldSpec generateByteField(SourceOfRandomness random, String name) {
//        return FieldSpec.builder(Byte.class, name)
//                .addModifiers(Modifier.PUBLIC)
//                .initializer("$L", random.nextBytes(1)[0])
//                .build();
//    }

    private String generateCharacter(SourceOfRandomness random) {
        return Character.toString(random.nextChar('a', 'z'));
    }

    private FieldSpec generateCharacterField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(Character.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("'$L'", generateCharacter(random))
                .build();
    }

    private FieldSpec generateIntegerField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(Integer.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$L", random.nextInt())
                .build();
    }

    private FieldSpec generateAtomicIntegerField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(AtomicInteger.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($L)", AtomicInteger.class, random.nextInt())
                .build();
    }

    private FieldSpec generateAtomicIntegerArrayField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(AtomicIntegerArray.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($L)", AtomicIntegerArray.class, random.nextInt(6))
                .build();
    }

    private String generateNumber(SourceOfRandomness random) {
        return random.choose(Arrays.<Supplier<CodeBlock.Builder>>asList(
                () -> CodeBlock.builder().add("new $T($L)", AtomicInteger.class, random.nextInt()),
                () -> CodeBlock.builder().add("new $T($LL)", AtomicLong.class, random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE)),
                () -> CodeBlock.builder().add("new $T($S)", BigDecimal.class, random.nextInt()),
                () -> CodeBlock.builder().add("new $T($S)", BigInteger.class, random.nextInt()),
                () -> CodeBlock.builder().add("new $T((byte) $L)", Byte.class, random.nextBytes(1)[0]),
                () -> CodeBlock.builder().add("new $T($L)", Double.class, random.nextDouble()),
                () -> CodeBlock.builder().add("new $T($Lf)", Float.class, random.nextFloat(Float.MIN_VALUE, Float.MAX_VALUE)),
                () -> CodeBlock.builder().add("new $T($L)", Integer.class, random.nextInt()),
                () -> CodeBlock.builder().add("new $T($LL)", Long.class, random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE)),
                () -> CodeBlock.builder().add("new $T((short) $L)", Short.class, random.nextShort(Short.MIN_VALUE, Short.MAX_VALUE)),
                () -> CodeBlock.builder().add("new $T($S)", LazilyParsedNumber.class, generateString(random))
        )).get().build().toString();
    }

    private FieldSpec generateNumberField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(Number.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer(generateNumber(random))
                .build();
    }

    private FieldSpec generateBigDecimalField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(BigDecimal.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($S)", BigDecimal.class, random.nextInt())
                .build();
    }

    private FieldSpec generateBigIntegerField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(BigInteger.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($S)", BigInteger.class, random.nextInt())
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
        String[] languages = {"CHINESE", "ENGLISH", "FRENCH", "GERMAN", "JAPANESE", "KOREAN", "SIMPLIFIED_CHINESE", "TRADITIONAL_CHINESE"};

        return FieldSpec.builder(Locale.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$T.$L", Locale.class, random.choose(languages))
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
        return FieldSpec.builder(Date.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($LL)", Date.class, random.nextLong(0, Long.MAX_VALUE))
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

    private FieldSpec generateCurrencyField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(Currency.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$T.getInstance($S)", Currency.class, random.choose(Currency.getAvailableCurrencies()))
                .build();
    }

    private CodeBlock generateJsonPrimitive(SourceOfRandomness random) {
        return CodeBlock.builder().add("new $T($L)", JsonPrimitive.class, getValue(random, random.choose(TYPES))).build();
    }

    private CodeBlock generateJsonElement(SourceOfRandomness random, String name) {
        return random.choose(Arrays.<Supplier<CodeBlock>>asList(
                () -> generateJsonPrimitive(random),
                () -> CodeBlock.builder().add("$T.INSTANCE", JsonNull.class).build()
        )).get();
    }

    private FieldSpec generateJsonElementField(SourceOfRandomness random, String name) {
        return FieldSpec.builder(JsonElement.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer(generateJsonElement(random, name))
                .build();
    }

    private FieldSpec generateJsonArrayField(SourceOfRandomness random, String name, MethodSpec.Builder constructor) {
        int numElements = random.nextInt(4);
        for (int i = 0; i < numElements; i++) {
            constructor.addStatement("$L.add($L)", name, random.choose(Arrays.<Supplier<String>>asList(
                    () -> getValue(random, random.choose(TYPES)),
                    () -> generateJsonPrimitive(random).toString(),
                    () -> CodeBlock.builder().add("$T.INSTANCE", JsonNull.class).build().toString()
            )).get());
        }

        constructor.addStatement("$T $L = $L", JsonElement.class, generateFieldName(random), name);

        return FieldSpec.builder(JsonArray.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T($L)", JsonArray.class, numElements)
                .build();
    }

    private FieldSpec generateJsonObjectField(SourceOfRandomness random, String name, MethodSpec.Builder constructor) {
        int numElements = random.nextInt(4);
        for (int i = 0; i < numElements; i++) {
            constructor.addStatement("$L.addProperty($S, $L)", name, generateFieldName(random), getValue(random, random.choose(TYPES)));
        }

        return FieldSpec.builder(JsonObject.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("new $T()", JsonObject.class)
                .build();
    }

    private String generateFieldName(SourceOfRandomness random) {
        String s = random.nextChar('a', 'z') + "_" + fieldNames.size();
        fieldNames.add(s);
        return s;
    }

    private String generateTopLevelTypeName(SourceOfRandomness random) {
        String s =  "Main_" + random.nextChar('A', 'Z') + "_" + topLevelTypeNames.size();
        topLevelTypeNames.add(s);
        return s;
    }

    private String generateTypeName(SourceOfRandomness random) {
        String s = random.nextChar('A', 'Z') + "_" + typeNames.size();
        typeNames.add(s);
        return s;
    }

}
