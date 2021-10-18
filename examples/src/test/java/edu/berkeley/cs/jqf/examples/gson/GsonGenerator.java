import java.util.*;
import java.util.function.*;
import javafx.util.Pair;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/* Generates a pair of strings that are syntactically valid Json and Java objects */
public class GsonGenerator extends Generator<Pair> {
    private static Set<String> identifiers;
    private static Set<String> classNames;

    public GsonGenerator() {
        super(Pair.class);
    }

    private static final String[] ESCAPE_TOKENS = {
            "\"", "\\", "/", "b", "f", "n", "r", "t", "!"
    };

    @Override
    public Pair<String, String> generate(SourceOfRandomness random, GenerationStatus __ignore__) {
        this.identifiers = new HashSet<>();
        this.classNames = new HashSet<>();

        return generateObject(random, "Main", new ArrayList<Pair<String, String>>(), new ArrayList<Pair<String, String>>());
    }

    private Pair<String, FieldSpec> generateElement(SourceOfRandomness random,
                                                    String name,
                                                    List<Pair<String, String>> fieldValues,
                                                    List<Pair<String, String>> arrayValues) {
        return random.choose(Arrays.<Supplier<Pair<String, String>>>asList(
                () -> generateObject(random, name, fieldValues, arrayValues),
                () -> generateArray(random, name, fieldValues, arrayValues),
                () -> generateString(random, name, fieldValues),
                () -> generateInteger(random, name, fieldValues),
                () -> generateBool(random, name, fieldValues),
                () -> generateNull(random, name, fieldValues)
        )).get();
    }

    private String generateClassName(SourceOfRandomness random) {
        if (classNames.size() == 0) {
            classNames.add("Main");
            return "Main";
        }

        String s = random.nextChar('A', 'Z') + "_" + classNames.size();
        classNames.add(s);
        return s;
    }

    private MethodSpec generateConstructor(String name, List<Pair<String, String>> fieldValues, List<Pair<String, String>> arrayValues) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        for (Pair<String, String> field : fieldValues) {
            constructor.addStatement("this.$N = $L", field.getKey(), field.getValue());
        }

        Map<String, Integer> indices = new HashMap<>();
        for (Pair<String, String> arrayElement : arrayValues) {
            String arrayName = arrayElement.getKey(), value = arrayElement.getValue();
            if (!indices.containsKey(arrayName)) indices.put(arrayName, 0);

            constructor.addStatement("this.$N[$L] = $L", arrayName, indices.get(arrayName), value);
            // constructor += "this." + arrayName + "[" + indices.get(arrayName) + "] = " + value + ";\n";
            indices.put(arrayName, indices.get(arrayName) + 1);
        }

        return constructor.build();
    }

    private Pair<String, TypeSpec> generateObject(SourceOfRandomness random,
                                                String name,
                                                List<Pair<String, String>> fieldValues,
                                                List<Pair<String, String>> arrayValues) {
        String className = generateClassName(random);

        List<Pair<String, String>> innerClassFieldValues = new ArrayList<Pair<String, String>>(), innerClassArrayValues = new ArrayList<Pair<String, String>>();
        Pair<String, List<FieldSpec>> fields = generateFields(random, innerClassFieldValues, innerClassArrayValues);

        TypeSpec.Builder obj = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addFields(fields);

        // String field = "", modifier = "public";
        if (!name.equals("Main")) {
            obj.addModifiers(Modifier.STATIC);
            obj.addField(ClassName.get("test", className), name, Modifier.PUBLIC);
            // field = "public " + className + " " + name + ";\n";
            fieldValues.add(new Pair<String, String>(name, "new " + className + "()"));
            // modifier += " static";
        }

        obj.addMethod(generateConstructor(className, innerClassFieldValues, innerClassArrayValues));

        return new Pair<>("{" + members.getKey() + "}", obj.build());
    }

    private Pair<String, List<FieldSpec>> generateFields(SourceOfRandomness random,
                                                          List<Pair<String, String>> fieldValues,
                                                          List<Pair<String, String>> arrayValues) {
        int maxMembers = random.nextInt(4);
        String members = "";
        List<FieldSpec> fields = new ArrayList<>();
        for (int i = 0; i < maxMembers; i++) {
            if (i > 0)  members += ", ";

            Pair<String, FieldSpec> p = generateField(random, fieldValues, arrayValues);
            members += p.getKey();
            fields.add(p.getValue());
        }

        return new Pair<>(members, fields);
    }

    private String generateIdentifier(SourceOfRandomness random) {
        String s = random.nextChar('a', 'z') + "_" + identifiers.size();
        identifiers.add(s);
        return s;
    }

    private Pair<String, FieldSpec> generateField(SourceOfRandomness random,
                                                   List<Pair<String, String>> fieldValues,
                                                   List<Pair<String, String>> arrayValues) {
        String name = generateIdentifier(random);
        Pair<String, FieldSpec> value = generateElement(random, name, fieldValues, arrayValues);

        String member = "\"" + name + "\": " + value.getKey();

        return new Pair<>(member, value.getValue());
    }

    private Pair<String, FieldSpec> generateArray(SourceOfRandomness random,
                                               String name,
                                               List<Pair<String, String>> fieldValues,
                                               List<Pair<String, String>> arrayValues) {
        String type = random.choose(new String[]{"int", "bool", "String"});
        List<String> elements = new ArrayList<>();
        int numElements = random.nextInt(4);

        for (int i = 0; i < numElements; i++) {
            if (type.equals("int")) {
                elements.add(generateInteger(random, name, new ArrayList<Pair<String, String>>()).getKey());
            } else if (type.equals("bool")) {
                elements.add(generateBool(random, name, new ArrayList<Pair<String, String>>()).getKey());
            } else {
                elements.add(generateString(random, name, new ArrayList<Pair<String, String>>()).getKey());
            }
        }

        String arrayContents = elements.stream().collect(Collectors.joining(","));

        FieldSpec.Builder arrayField;
        if (type.equals("int")) {
            arrayField = FieldSpec.builder(int[].class, name);
        } else if (type.equals("bool")) {
            arrayField = FieldSpec.builder(bool[].class, name);
        } else {
            arrayField = FieldSpec.builder(String[].class, name);
        }
        return new Pair<String, String>("[" + arrayContents + "]",
                arrayField.addModifiers(Modifier.PUBLIC).initializer("{$L}", arrayContents).build());
    }

    private Pair<String, FieldSpec> generateString(SourceOfRandomness random,
                                                String name,
                                                List<Pair<String, String>> fieldValues) {
        int len = random.nextInt(6);
        String s = "\"";
        for (int i = 0; i < len; i++)
            s += random.nextChar('a', 'z');
        s += "\"";

        FieldSpec stringField = FieldSpec.builder(String.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$S", s)
                .build();
        return new Pair<String, String>(s, stringField);
    }

    private Pair<String, FieldSpec> generateInteger(SourceOfRandomness random,
                                                 String name,
                                                 List<Pair<String, String>> fieldValues) {
        String num = random.choose(Arrays.<Supplier<String>>asList(
                () -> Integer.toString(random.nextInt(10)),
                () -> Integer.toString(random.nextInt(1, 10)) + generateDigits(random),
                () -> "-" + random.nextInt(10),
                () -> "-" + random.nextInt(1, 10) + generateDigits(random)
        )).get();

        FieldSpec intField = FieldSpec.builder(int.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$L", num)
                .build();
        return new Pair<>(num, intField);
    }

    private String generateDigits(SourceOfRandomness random) {
        int maxDigits = random.nextInt(1, 3);
        String digits = "";
        for (int i = 0; i < maxDigits; i++) digits += random.nextInt(10);
        return digits;
    }

    private Pair<String, FieldSpec> generateBool(SourceOfRandomness random,
                                              String name,
                                              List<Pair<String, String>> fieldValues) {
        String bool = random.choose(Arrays.<Supplier<String>>asList(
                () -> "false",
                () -> "true"
        )).get();

        FieldSpec boolField = FieldSpec.builder(bool.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("$L", bool)
                .build();
        return new Pair<>(bool, nullField);
    }

    private Pair<String, FieldSpec> generateNull(SourceOfRandomness random,
                                              String name,
                                              List<Pair<String, String>> fieldValues) {
        FieldSpec nullField = FieldSpec.builder(String.class, name)
                .addModifiers(Modifier.PUBLIC)
                .initializer("null")
                .build();

        return new Pair<>("null", nullField);
    }

}
