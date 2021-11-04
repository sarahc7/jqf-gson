import com.google.gson.*;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class GsonGenerator extends Generator<Gson> {

    public GsonGenerator() {
        super(Gson.class);
    }

    @Override
    public Gson generate(SourceOfRandomness random, GenerationStatus __ignore__) {
        if (random.nextBoolean()) return new Gson();
        else return new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    }

}
