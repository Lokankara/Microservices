package com.audio.processor.service;

import com.audio.processor.dto.SongMetadata;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class Mp3MetadataExtractor {

    public SongMetadata extract(byte[] audioData) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(audioData)) {
            Metadata metadata = new Metadata();
            new AutoDetectParser().parse(inputStream, new BodyContentHandler(), metadata);

            SongMetadata result = new SongMetadata();

            String title = coalesce(metadata.get("title"), metadata.get("dc:title"), "Unknown Title");
            result.setName(title);

            String artist = coalesce(metadata.get("artist"), metadata.get("dc:creator"), "Unknown Artist");
            result.setArtist(artist);

            String album = coalesce(metadata.get("album"), metadata.get("dc:subject"), "Unknown Album");
            result.setAlbum(album);

            String rawDuration = metadata.get("xmpDM:duration");
            result.setDuration(formatDuration(rawDuration));

            String year = coalesce(metadata.get("xmpDM:releaseDate"), metadata.get("date"), "2000");
            if (year.length() > 4)
                year = year.substring(year.length() - 4);
            result.setYear(year);

            return result;
        } catch (TikaException | IOException | SAXException e) {
            throw new IllegalArgumentException("Failed to parse MP3 file: " + e.getMessage(), e);
        }
    }

    private String coalesce(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return values[values.length - 1];
    }

    private String formatDuration(String rawMs) {
        if (rawMs == null || rawMs.isBlank())
            return "00:00";
        try {
            long totalSeconds = (long) (Double.parseDouble(rawMs) / 1000);
            return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        } catch (NumberFormatException e) {
            return "00:00";
        }
    }
}
