import javafx.util.Pair;
import net.openhft.compiler.CompilerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.*;
import org.junit.runner.RunWith;

import junit.framework.TestCase;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

@RunWith(JQF.class)
public class GsonTest {
    private Gson gson = new Gson();
    private static final Logger logger = LoggerFactory.getLogger(GsonTest.class);

    @Fuzz
    public void testFromGson(@From(GsonGenerator.class) Pair<String, String> jsonAndJavaClass) throws Exception {
        gson.fromJson(jsonAndJavaClass.getKey(), CompilerUtils.CACHED_COMPILER.loadFromJava("Main", jsonAndJavaClass.getValue()));
    }

    @Fuzz
    public void testToGson(@From(GsonGenerator.class) Pair<String, String> jsonAndJavaClass) throws Exception {
        gson.toJson(CompilerUtils.CACHED_COMPILER.loadFromJava("Main", jsonAndJavaClass.getValue()).getDeclaredConstructor().newInstance());
    }
}
