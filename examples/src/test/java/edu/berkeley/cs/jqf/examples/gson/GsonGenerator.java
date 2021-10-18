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

    private Pair<String, String> generateElement(SourceOfRandomness random,
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

    private String generateConstructor(String name, List<Pair<String, String>> fieldValues, List<Pair<String, String>> arrayValues) {
        String constructor = "public " + name + "() {\n";
        for (Pair<String, String> field : fieldValues) {
            constructor += "this." + field.getKey() + " = " + field.getValue() + ";\n";
        }

        Map<String, Integer> indices = new HashMap<>();
        for (Pair<String, String> arrayElement : arrayValues) {
            String arrayName = arrayElement.getKey(), value = arrayElement.getValue();
            if (!indices.containsKey(arrayName)) indices.put(arrayName, 0);

            constructor += "this." + arrayName + "[" + indices.get(arrayName) + "] = " + value + ";\n";
            indices.put(arrayName, indices.get(arrayName) + 1);
        }
        constructor += "}\n";

        return constructor;
    }

    private Pair<String, String> generateObject(SourceOfRandomness random,
                                                String name,
                                                List<Pair<String, String>> fieldValues,
                                                List<Pair<String, String>> arrayValues) {
        String className = generateClassName(random);

        List<Pair<String, String>> innerClassFieldValues = new ArrayList<Pair<String, String>>(), innerClassArrayValues = new ArrayList<Pair<String, String>>();
        Pair<String, String> members = generateMembers(random, innerClassFieldValues, innerClassArrayValues);

        String field = "", modifier = "public";
        if (!name.equals("Main")) {
            field = "public " + className + " " + name + ";\n";
            fieldValues.add(new Pair<String, String>(name, "new " + className + "()"));
            modifier += " static";
        }
        return new Pair<>("{" + members.getKey() + "}", field + modifier + " class " + className + " {\n" +
                members.getValue() + generateConstructor(className, innerClassFieldValues, innerClassArrayValues) + "}");

//        if (name.equals("Main")) return object;
//        else {
//            String field = "public " + className + " " + name + ";\n";
//            fieldValues.add(new Pair<String, String>(name, "new " + className + "();"));
//            return new Pair<>(object.getKey(), field + "static " + object.getValue());
//        }
    }

    private Pair<String, String> generateMembers(SourceOfRandomness random,
                                                 List<Pair<String, String>> fieldValues,
                                                 List<Pair<String, String>> arrayValues) {
        int maxMembers = random.nextInt(4);
        String members = "";
        String javaFields = "";
        for (int i = 0; i < maxMembers; i++) {
            if (i > 0)  members += ", ";

            Pair<String, String> p = generateMember(random, fieldValues, arrayValues);
            members += p.getKey();
            javaFields += p.getValue() + "\n";
        }

        return new Pair<>(members, javaFields);
    }

    private String generateIdentifier(SourceOfRandomness random) {
        String s = random.nextChar('a', 'z') + "_" + identifiers.size();
        identifiers.add(s);
        return s;
    }

    private Pair<String, String> generateMember(SourceOfRandomness random,
                                                List<Pair<String, String>> fieldValues,
                                                List<Pair<String, String>> arrayValues) {
        String name = generateIdentifier(random);
        Pair<String, String> value = generateElement(random, name, fieldValues, arrayValues);

        String member = "\"" + name + "\": " + value.getKey();

        return new Pair<>(member, value.getValue());
    }

    private Pair<String, String> generateArray(SourceOfRandomness random,
                                               String name,
                                               List<Pair<String, String>> fieldValues,
                                               List<Pair<String, String>> arrayValues) {
        String type = random.choose(new String[]{"int", "boolean", "String"}), elements = "";
        int numElements = random.nextInt(4);
        fieldValues.add(new Pair<String, String>(name, "new " + type + "[" + numElements + "]"));

        for (int i = 0; i < numElements; i++) {
            if (i > 0)  elements += ", ";

            String element;
            if (type.equals("int")) {
                element = generateInteger(random, name, new ArrayList<Pair<String, String>>()).getKey();
            } else if (type.equals("boolean")) {
                element = generateBool(random, name, new ArrayList<Pair<String, String>>()).getKey();
            } else {
                element = generateString(random, name, new ArrayList<Pair<String, String>>()).getKey();
            }

            elements += element;
            arrayValues.add(new Pair<String, String>(name, element));
        }

        return new Pair<String, String>("[" + elements + "]", "public " + type + "[] " + name + ";");
    }

    private Pair<String, String> generateString(SourceOfRandomness random,
                                                String name,
                                                List<Pair<String, String>> fieldValues) {
        int len = random.nextInt(6);
        String s = "\"";
        for (int i = 0; i < len; i++)
            s += random.nextChar('a', 'z');
        s += "\"";

        fieldValues.add(new Pair<String, String>(name, s));
        return new Pair<String, String>(s, "public String " + name + ";");
    }

    private Pair<String, String> generateInteger(SourceOfRandomness random,
                                                 String name,
                                                 List<Pair<String, String>> fieldValues) {
        String num = random.choose(Arrays.<Supplier<String>>asList(
                () -> Integer.toString(random.nextInt(10)),
                () -> Integer.toString(random.nextInt(1, 10)) + generateDigits(random),
                () -> "-" + random.nextInt(10),
                () -> "-" + random.nextInt(1, 10) + generateDigits(random)
        )).get();
        fieldValues.add(new Pair<String, String>(name, num));

        return new Pair<>(num, "public int " + name + ";");
    }

    private String generateDigits(SourceOfRandomness random) {
        int maxDigits = random.nextInt(1, 3);
        String digits = "";
        for (int i = 0; i < maxDigits; i++) digits += random.nextInt(10);
        return digits;
    }

    private Pair<String, String> generateBool(SourceOfRandomness random,
                                              String name,
                                              List<Pair<String, String>> fieldValues) {
        String bool = random.choose(Arrays.<Supplier<String>>asList(
                () -> "false",
                () -> "true"
        )).get();
        fieldValues.add(new Pair<String, String>(name, bool));

        return new Pair<>(bool, "public boolean " + name + ";");
    }

    private Pair<String, String> generateNull(SourceOfRandomness random,
                                              String name,
                                              List<Pair<String, String>> fieldValues) {
        fieldValues.add(new Pair<String, String>(name, "null"));
        return new Pair<>("null", "public String " + name + ";");
    }

}
