package megamarket.configuration;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//configuring ISO 8601 for date
@Configuration
@ParametersAreNonnullByDefault
public class JsonAutoConfiguration implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;
    private final DateTimeFormatter datetimeDeserializerFormatter;

    public JsonAutoConfiguration(
            ObjectMapper objectMapper,
            @Value("${json.serializer.datetime.format}") String datetimeDeserializerFormat
    ) {
        this.objectMapper = objectMapper;
        this.datetimeDeserializerFormatter = DateTimeFormatter.ofPattern(datetimeDeserializerFormat);
        setupObjectMapper();
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatterRegistrar jsr310 = new DateTimeFormatterRegistrar();
        jsr310.setDateTimeFormatter(datetimeDeserializerFormatter);
        jsr310.registerFormatters(registry);
    }

    private void setupObjectMapper() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        SimpleModule simpleModule = new SimpleModule(
                "Serializer"
        );
        simpleModule.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(
                    LocalDateTime value, JsonGenerator gen, SerializerProvider serializers
            ) throws IOException {
                String format = value == null ? null : value.format(datetimeDeserializerFormatter);
                gen.writeString(format);
            }
        });
        simpleModule.addDeserializer(LocalDateTime.class, new JsonDeserializer<>() {
            @Override
            public LocalDateTime deserialize(
                    JsonParser p, DeserializationContext ctxt
            ) throws IOException {
                return LocalDateTime.parse(p.readValueAs(String.class), datetimeDeserializerFormatter);
            }
        });

        objectMapper.registerModule(simpleModule);
    }
}