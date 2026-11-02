package com.wolinski.adr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkspaceTest {

    private static final Path FIXTURE = Path.of("fixture").toAbsolutePath();

    private static Workspace workspace(Arm arm) throws Exception {
        return Workspace.create(FIXTURE, arm, Files.createTempDirectory("test"));
    }

    @Test
    void theToolsCannotLeaveTheRepository() throws Exception {
        Workspace workspace = workspace(Arm.NONE);

        assertThrows(IllegalArgumentException.class, () -> workspace.read("../../etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> workspace.write("/etc/shop", "x"));
        assertTrue(AgentTools.execute(workspace, "read_file", Map.of("path", "../secret")).isError());
    }

    @Test
    void theToolsDoNotFollowSymbolicLinks() throws Exception {
        Workspace workspace = workspace(Arm.NONE);
        Files.createSymbolicLink(workspace.root.resolve("out"), Path.of("/etc"));

        assertThrows(IllegalArgumentException.class, () -> workspace.read("out/passwd"));
    }

    @Test
    void theArmsDifferOnlyInWhatTheySayAboutDecisions() throws Exception {
        assertFalse(Files.exists(workspace(Arm.NONE).root.resolve("docs/adr")));
        assertFalse(workspace(Arm.NONE).instructions().contains("docs/adr"));

        assertTrue(Files.exists(workspace(Arm.FOLDER).root.resolve("docs/adr/0002-modules-talk-through-their-api.md")));
        assertFalse(workspace(Arm.FOLDER).instructions().contains("docs/adr"));

        assertTrue(workspace(Arm.POINTER).instructions().contains("docs/adr"));
        assertFalse(Files.exists(workspace(Arm.POINTER).root.resolve("src/test/java/com/wolinski/shop/ArchitectureTest.java")));

        assertTrue(Files.exists(workspace(Arm.ENFORCED).root.resolve("src/test/java/com/wolinski/shop/ArchitectureTest.java")));

        assertFalse(Files.exists(workspace(Arm.RULES).root.resolve("docs/adr")));
        assertFalse(workspace(Arm.RULES).instructions().contains("docs/adr"));
        assertTrue(Files.exists(workspace(Arm.RULES).root.resolve("src/test/java/com/wolinski/shop/ArchitectureTest.java")));
    }

    @Test
    void anEditMustMatchExactlyOnce() throws Exception {
        Workspace workspace = workspace(Arm.NONE);
        workspace.write("a.txt", "one two two");

        assertThrows(IllegalArgumentException.class, () -> workspace.edit("a.txt", "three", "x"));
        assertThrows(IllegalArgumentException.class, () -> workspace.edit("a.txt", "two", "x"));
        workspace.edit("a.txt", "one", "uno");
        assertEquals("uno two two", workspace.read("a.txt"));
    }

    @Test
    void theModelsCodeRunsWithoutTheApiKeyInItsEnvironment() throws Exception {
        Workspace workspace = workspace(Arm.NONE);
        workspace.write("src/test/java/com/wolinski/shop/KeyTest.java", """
                package com.wolinski.shop;

                import static org.junit.jupiter.api.Assertions.assertNull;
                import org.junit.jupiter.api.Test;

                class KeyTest {
                    @Test
                    void noKey() {
                        assertNull(System.getenv("ANTHROPIC_API_KEY"));
                    }
                }
                """);
        assertTrue(workspace.runTests().passed());
    }
}
