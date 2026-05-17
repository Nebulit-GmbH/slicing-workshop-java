package de.eventmodelers.common.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.FieldPredicates;
import org.jeasy.random.randomizers.number.BigDecimalRandomizer;
import org.jeasy.random.randomizers.text.StringRandomizer;

public final class RandomData {

  private RandomData() {}

  public static <T> T newInstance(Class<T> type) {
    return newInstance(type, List.of(), t -> {});
  }

  public static <T> T newInstance(Class<T> type, List<String> fieldsToIgnore, Consumer<T> block) {
    var parameters =
        new EasyRandomParameters()
            .collectionSizeRange(1, 4)
            .randomize(UUID.class, UUID::randomUUID)
            .randomize(BigDecimal.class, new BigDecimalRandomizer(2, RoundingMode.CEILING))
            .randomize(
                CharSequence.class,
                () -> new StringBuilder(new StringRandomizer().getRandomValue()))
            .randomize(
                ByteBuffer.class,
                () -> ByteBuffer.wrap(new StringRandomizer().getRandomValue().getBytes()));

    fieldsToIgnore.forEach(name -> parameters.excludeField(FieldPredicates.named(name)));

    var generator = new EasyRandom(parameters);
    var instance = generator.nextObject(type);
    block.accept(instance);
    return instance;
  }
}
