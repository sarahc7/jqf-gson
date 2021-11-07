import java.text.*;
import java.util.*;
import javax.lang.model.element.Modifier;
import javafx.util.Pair;
import net.openhft.compiler.CompilerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.*;
import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.*;
import org.junit.runner.RunWith;

import junit.framework.TestCase;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

@RunWith(JQF.class)
public class GsonTest {
    private static final Logger logger = LoggerFactory.getLogger(GsonTest.class);

//    @Fuzz
//    public void testFromGson(@From(GsonGenerator.class) Pair<String, String> jsonAndJavaClass) throws Exception {
//        gson.fromJson(jsonAndJavaClass.getKey(), CompilerUtils.CACHED_COMPILER.loadFromJava("Main", jsonAndJavaClass.getValue()));
//    }

    @Fuzz
    public void testToGson(@From(JavaGenerator.class) String java) throws Exception {
//        GsonBuilder builder = new GsonBuilder()
//                .setPrettyPrinting()
//                .serializeNulls()
//                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
//                .enableComplexMapKeySerialization();
//
//        for (Object obj : classesAndAdapters.keySet()) {
//            String name = (String) obj;
//
//            try {
//                Pair p = (Pair) classesAndAdapters.get(name);
//                String c = (String) p.getKey(), adapter = (String) p.getValue();
//
//                if (adapter == null) continue;
//                //logger.info("c: " + c);
//                //logger.info("adapter: " + adapter);
//                builder.registerTypeHierarchyAdapter(CompilerUtils.CACHED_COMPILER.loadFromJava(name, c),
//                        CompilerUtils.CACHED_COMPILER.loadFromJava(name + "_Adapter", adapter).getDeclaredConstructor().newInstance());
//            } catch (Exception e) {
//                System.err.println(e);
//            }
//        }


        Gson gson = new Gson();
        //String java = (String) ((Pair) classesAndAdapters.get("Main")).getKey();

        String json = gson.toJson(CompilerUtils.CACHED_COMPILER.loadFromJava("Main", java).getDeclaredConstructor().newInstance());
        //logger.info(json);
        gson.fromJson(json, CompilerUtils.CACHED_COMPILER.loadFromJava("Main", java));

        JsonElement rootNode = gson.toJsonTree(CompilerUtils.CACHED_COMPILER.loadFromJava("Main", java).getDeclaredConstructor().newInstance());
        gson.fromJson(rootNode, CompilerUtils.CACHED_COMPILER.loadFromJava("Main", java));
        gson.toJson(rootNode);
    }
}
