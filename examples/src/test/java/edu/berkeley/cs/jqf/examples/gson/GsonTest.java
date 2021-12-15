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
    public void testToGson(@From(JavaGenerator.class) Pair gsonAndJava) throws Exception {
        Gson gson = (Gson) gsonAndJava.getKey();
        String name = (String) ((Pair) gsonAndJava.getValue()).getKey();
        String java = (String) ((Pair) gsonAndJava.getValue()).getValue();

        // logger.info("before created class");

        Class javaClass = CompilerUtils.CACHED_COMPILER.loadFromJava(name, java);
        //logger.info("created class");
        Object obj = javaClass.getDeclaredConstructor().newInstance();

        //logger.info("created obj");

        String json = gson.toJson(obj);
//        logger.info(json);
        gson.fromJson(json, javaClass);

        JsonElement rootNode = gson.toJsonTree(obj);
        // JsonObject jsonObj = rootNode.getAsJsonObject();
        gson.fromJson(rootNode, javaClass);
        gson.toJson(rootNode);

        JsonParser.parseString(json);
    }
}
