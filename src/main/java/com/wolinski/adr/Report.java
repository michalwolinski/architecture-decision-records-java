package com.wolinski.adr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Everything is recomputed from results.jsonl. */
public final class Report {

    private Report() {
    }

    static List<JsonNode> rows(Path run) throws IOException {
        ObjectMapper json = new ObjectMapper();
        List<JsonNode> rows = new ArrayList<>();
        Path results = run.resolve("results.jsonl");
        if (false == Files.exists(results)) {
            return rows;
        }
        for (String line : Files.readAllLines(results)) {
            rows.add(json.readTree(line));
        }
        return rows;
    }

    /** An episode followed the decision its ticket tempts it to break, and the feature works. */
    static boolean brokeTarget(JsonNode row) {
        if (row.get("tempts").isNull()) {
            return false;
        }
        for (JsonNode violation : row.get("violations")) {
            if (violation.asText().equals(row.get("tempts").asText())) {
                return true;
            }
        }
        return false;
    }

    /** Wilson 95% score interval, as percentages. */
    static double[] wilson(int hits, int total) {
        if (0 == total) {
            return new double[] {0, 0};
        }
        double z = 1.96;
        double p = (double) hits / total;
        double denominator = 1 + z * z / total;
        double centre = p + z * z / (2 * total);
        double margin = z * Math.sqrt(p * (1 - p) / total + z * z / (4.0 * total * total));
        return new double[] {Math.max(0, 100 * (centre - margin) / denominator), Math.min(100, 100 * (centre + margin) / denominator)};
    }

    public static String render(Path run) throws IOException {
        List<JsonNode> rows = rows(run);
        StringBuilder out = new StringBuilder();
        for (String catalogue : List.of("team", "practice")) {
            List<JsonNode> ofCatalogue = rows.stream()
                    .filter(row -> catalogue.equals(row.path("catalogue").asText()) || "control".equals(row.path("catalogue").asText()))
                    .toList();
            if (ofCatalogue.stream().noneMatch(row -> catalogue.equals(row.path("catalogue").asText()))) {
                continue;
            }
            out.append(String.format("catalogue: %s (with the control ticket in n, functional and read-adr)%n", catalogue));
            out.append(String.format("%-9s %4s %10s %-14s %10s %10s %9s %9s %8s%n", "arm", "n", "broke", "95% CI", "functional",
                    "any-viol", "flagged", "read-adr", "usd"));
            for (String arm : List.of("none", "rules", "folder", "pointer", "enforced")) {
                List<JsonNode> ofArm = ofCatalogue.stream().filter(row -> arm.equals(row.get("arm").asText())).toList();
                if (ofArm.isEmpty()) {
                    continue;
                }
                List<JsonNode> tempting = ofArm.stream().filter(row -> false == row.get("tempts").isNull()).toList();
                int broke = (int) tempting.stream().filter(Report::brokeTarget).count();
                double[] ci = wilson(broke, tempting.size());
                out.append(String.format(Locale.ROOT, "%-9s %4d %6d/%-3d %-14s %6d/%-3d %6d/%-3d %5d/%-3d %5d/%-3d %8.3f%n",
                        arm, ofArm.size(), broke, tempting.size(),
                        String.format(Locale.ROOT, "%.0f-%.0f%%", ci[0], ci[1]),
                        ofArm.stream().filter(row -> row.get("functional").asBoolean()).count(), ofArm.size(),
                        ofArm.stream().filter(row -> false == row.get("violations").isEmpty()).count(), ofArm.size(),
                        ofArm.stream().filter(row -> false == row.get("tampering").isEmpty()).count(), ofArm.size(),
                        ofArm.stream().filter(row -> false == row.get("adrs_read").isEmpty()).count(), ofArm.size(),
                        ofArm.stream().mapToDouble(row -> row.get("usd").asDouble()).sum()));
            }
            out.append('\n');
        }
        out.append(String.format("%nby task: broke its own decision, per arm%n%-20s", "task"));
        List<String> arms = List.of("none", "rules", "folder", "pointer", "enforced");
        arms.forEach(arm -> out.append(String.format("%10s", arm)));
        out.append('\n');
        for (Task task : Task.ALL) {
            if (null == task.tempts()) {
                continue;
            }
            out.append(String.format("%-20s", task.id() + " " + task.tempts().substring(5)));
            for (String arm : arms) {
                List<JsonNode> cell = rows.stream()
                        .filter(row -> arm.equals(row.get("arm").asText()) && task.id().equals(row.get("task").asText()))
                        .toList();
                out.append(String.format("%10s", cell.stream().filter(Report::brokeTarget).count() + "/" + cell.size()));
            }
            out.append('\n');
        }
        return out.toString();
    }
}
