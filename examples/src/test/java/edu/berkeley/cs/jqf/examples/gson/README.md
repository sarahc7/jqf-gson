# Test Generation for Gson

This example uses JQF to generate test inputs for [Gson](https://github.com/google/gson), a tool that converts Java objects to JSON strings and vice-versa. The following steps walk through how to run JQF and locate generated test inputs and covered methods. 

### Step 0: Build jqf-gson

```
git clone https://github.com/sarahc7/jqf-gson
cd jqf-gson
mvn package
```

### Step 1: Compile classes
```
export GSON=[PATH TO jqf-gson]
cd examples/src/test/java/edu/berkeley/cs/jqf/examples/gson/
javac -cp .:$($GSON/scripts/examples_classpath.sh) GsonTest.java JavaGenerator.java
```

### Step 2: Fuzz with Zest
```
$GSON/bin/jqf-zest -c .:$($GSON/scripts/examples_classpath.sh) GsonTest testGson
```

While running Zest, your screen should look like this:
```
Semantic Fuzzing with Zest
--------------------------

Test name:            GsonTest#testToGson
Results directory:    $GSON/examples/src/test/java/edu/berkeley/cs/jqf/examples/gson/fuzz-results
Elapsed time:         2m 10s (no time limit)
Number of executions: 74 (no trial limit)
Valid inputs:         74 (100.00%)
Cycles completed:     0
Unique failures:      0
Queue size:           74 (0 favored last cycle)
Current parent input: 0 (favored) {73/820 mutations}
Execution speed:      0/sec now | 0/sec overall
Total coverage:       14,233 branches (21.72% of map)
Valid coverage:       14,233 branches (21.72% of map)
```

As the coverage stabilizes, you can exit JQF with `Ctrl-C`.

The corpus of test inputs is located at $GSON/fuzz-results/corpus, and the list of executed methods is in $GSON/fuzz-results/methods.log:

```
com/google/gson/DefaultDateTypeAdapter.<init>:(Ljava/lang/Class;Ljava/lang/String;)V
com/google/gson/DefaultDateTypeAdapter.deserializeToDate:(Ljava/lang/String;)Ljava/util/Date;
com/google/gson/DefaultDateTypeAdapter.read:(Lcom/google/gson/stream/JsonReader;)Ljava/lang/Object;
com/google/gson/DefaultDateTypeAdapter.read:(Lcom/google/gson/stream/JsonReader;)Ljava/util/Date;
com/google/gson/DefaultDateTypeAdapter.verifyDateType:(Ljava/lang/Class;)Ljava/lang/Class;
com/google/gson/DefaultDateTypeAdapter.write:(Lcom/google/gson/stream/JsonWriter;Ljava/lang/Object;)V
com/google/gson/DefaultDateTypeAdapter.write:(Lcom/google/gson/stream/JsonWriter;Ljava/util/Date;)V
com/google/gson/FieldAttributes.<init>:(Ljava/lang/reflect/Field;)V
com/google/gson/FieldNamingPolicy$1.translateName:(Ljava/lang/reflect/Field;)Ljava/lang/String;
com/google/gson/Gson$1.<init>:(Lcom/google/gson/Gson;)V
com/google/gson/Gson$2.<init>:(Lcom/google/gson/Gson;)V
...
```
