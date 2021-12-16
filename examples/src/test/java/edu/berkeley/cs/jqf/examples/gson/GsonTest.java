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

    @Fuzz
    public void testGson(@From(JavaGenerator.class) Pair gsonAndJava) throws Exception {
        Gson gson = (Gson) gsonAndJava.getKey();
        String name = (String) ((Pair) gsonAndJava.getValue()).getKey();
        String java = (String) ((Pair) gsonAndJava.getValue()).getValue();

        Class javaClass = CompilerUtils.CACHED_COMPILER.loadFromJava(name, java);
        Object javaObj = javaClass.getDeclaredConstructor().newInstance();

        String json = gson.toJson(javaObj);
        gson.fromJson(json, javaClass);
    }
}
