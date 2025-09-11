package com.nttdata.cards_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Configuration
@Slf4j
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        log.debug("Registrando convertidores de OffsetDateTime <-> Date para MongoDB");
        List<Converter<?, ?>> converters = Arrays.asList(
                new OffsetDateTimeWriteConverter(),
                new OffsetDateTimeReadConverter(),
                new LocalDateWriteConverter(),
                new LocalDateReadConverter()
        );
        return new MongoCustomConversions(converters);
    }

    @WritingConverter
    static class OffsetDateTimeWriteConverter implements Converter<OffsetDateTime, Date> {
        @Override
        public Date convert(OffsetDateTime source) {
            return source == null ? null
                : Date.from(source.toInstant());
        }
    }

    @ReadingConverter
    static class OffsetDateTimeReadConverter implements Converter<Date, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(Date source) {
            return source == null ? null
                : source.toInstant().atOffset(ZoneOffset.UTC);
        }
    }

    @WritingConverter
    static class LocalDateWriteConverter implements Converter<LocalDate, Date> {
        @Override
        public Date convert(LocalDate source) {
            return source == null ? null
                : Date.from(source.atStartOfDay(ZoneOffset.UTC).toInstant());
        }
    }

    @ReadingConverter
    static class LocalDateReadConverter implements Converter<Date, LocalDate> {
        @Override
        public LocalDate convert(Date source) {
            return source == null ? null
                : source.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        }
    }
}