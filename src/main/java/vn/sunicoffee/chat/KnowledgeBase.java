package vn.sunicoffee.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBase {
    public record Entry(String id, String title, String content, List<String> sourceIds, String scope) {}
    public record Source(String id, String title, String location, String locator) {}
    private final List<Entry> entries;
    private final Map<String, Source> sources;
    private static final Set<String> STOP = Set.of("toi", "ban", "cho", "la", "va", "co", "khong", "cua", "thi", "voi", "nao", "nhu", "the", "duoc", "can", "gi", "bao", "nhieu", "hay", "mot", "nhung", "cac", "ve", "de", "khi", "theo", "minh", "giup");

    public KnowledgeBase(ObjectMapper mapper) throws IOException {
        // Explicit allowlist: evaluation/ and arbitrary filesystem paths are never read.
        try (var in = new ClassPathResource("knowledge/knowledge.v1.json").getInputStream()) {
            var root = mapper.readTree(in);
            var parsed = new ArrayList<Entry>();
            for (var e : root.path("items")) {
                var refs = new ArrayList<String>();
                e.path("source_refs").forEach(r -> refs.add(r.asText()));
                parsed.add(new Entry(e.path("id").asText(), e.path("title").asText(), e.path("content").asText(), List.copyOf(refs), e.path("applies_to").asText()));
            }
            entries = List.copyOf(parsed);
        }
        var parsedSources = new LinkedHashMap<String, Source>();
        try (var in = new ClassPathResource("knowledge/sources.v1.json").getInputStream()) {
            for (var s : mapper.readTree(in).path("sources")) {
                var source = new Source(s.path("id").asText(), s.path("title").asText(), s.path("location").asText(), s.path("locator").asText());
                if (parsedSources.put(source.id(), source) != null) throw new IOException("Duplicate knowledge source");
            }
        }
        sources = Collections.unmodifiableMap(parsedSources);
        if (entries.size() < 50 || entries.stream().map(Entry::id).distinct().count() != entries.size()
                || entries.stream().anyMatch(e -> e.content().isBlank() || e.sourceIds().isEmpty() || !sources.keySet().containsAll(e.sourceIds()))) {
            throw new IOException("Invalid knowledge baseline");
        }
    }

    public static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", " ").strip();
    }
    private static Set<String> tokens(String text) {
        return Arrays.stream(normalize(text).split(" +")).filter(t -> t.length() > 1 && !STOP.contains(t)).collect(Collectors.toSet());
    }
    public List<Entry> search(String query) {
        Set<String> terms = tokens(query);
        record Match(Entry entry, double score) {}
        var ranked = entries.stream().map(e -> {
            Set<String> words = tokens(e.title() + " " + e.content());
            Set<String> title = tokens(e.title());
            double score = 0;
            for (String term : terms) if (words.contains(term)) {
                long frequency = entries.stream().filter(x -> tokens(x.title() + " " + x.content()).contains(term)).count();
                score += Math.log(1 + (double) entries.size() / (1 + frequency)) * (title.contains(term) ? 2 : 1);
            }
            return new Match(e, score);
        }).filter(m -> m.score() > 0).sorted(Comparator.comparingDouble(Match::score).reversed()).limit(12).map(Match::entry).toList();
        var selected = new LinkedHashMap<String, Entry>();
        ranked.forEach(e -> selected.put(e.id(), e));
        entries.stream().filter(e -> Set.of("NV21", "NV22", "NV23", "NV24", "NV25").contains(e.id())).forEach(e -> selected.put(e.id(), e));
        return List.copyOf(selected.values());
    }
    public List<ChatReply.Citation> citations(List<String> ids) {
        var result = new LinkedHashMap<String, ChatReply.Citation>();
        for (String id : ids) for (Entry e : entries) if (e.id().equals(id)) for (String ref : e.sourceIds()) {
            var s = sources.get(ref);
            String url = s.location().startsWith("https://") ? s.location() : "/chat/sources/" + s.id();
            result.put(ref, new ChatReply.Citation(s.id(), s.title(), url));
        }
        return List.copyOf(result.values());
    }
    public Optional<Source> source(String id) { return Optional.ofNullable(sources.get(id)); }
    public List<Entry> entriesForSource(String id) { return entries.stream().filter(e -> e.sourceIds().contains(id)).toList(); }
}
