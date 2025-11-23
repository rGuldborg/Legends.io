package org.example.service.lcu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class ChampionIdMapper {
    private static final String RESOURCE = "/org/example/data/champion-map.json";
    private static final Map<Integer, String> ID_TO_NAME = load();

    private ChampionIdMapper() {
    }

    static String nameForId(int id) {
        return ID_TO_NAME.get(id);
    }

    private static Map<Integer, String> load() {
        try (InputStream stream = ChampionIdMapper.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                System.err.println("[ChampionIdMapper] Missing resource " + RESOURCE);
                return Collections.emptyMap();
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(stream);
            JsonNode data = root.path("data");
            Map<Integer, String> mapping = new HashMap<>();
            data.fields().forEachRemaining(entry -> {
                JsonNode node = entry.getValue();
                int id = node.path("key").asInt(-1);
                String name = node.path("name").asText(entry.getKey());
                if (id > 0) {
                    mapping.put(id, name);
                }
            });
            return Map.copyOf(mapping);
        } catch (Exception ex) {
            System.err.println("[ChampionIdMapper] Failed to load champion metadata: " + ex.getMessage());
            return Collections.emptyMap();
        }
    }
}
