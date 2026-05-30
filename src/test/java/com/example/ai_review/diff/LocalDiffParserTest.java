package com.example.ai_review.diff;

import com.example.ai_review.github.ChangedFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalDiffParserTest {

    private final LocalDiffParser parser = new LocalDiffParser();

    @Test
    void parsesSingleFileModification() {
        String diff = """
                diff --git a/src/App.java b/src/App.java
                index 123..456 100644
                --- a/src/App.java
                +++ b/src/App.java
                @@ -1,2 +1,3 @@
                -old line
                +new line
                +extra line
                """;

        List<ChangedFile> files = parser.parse(diff);

        assertEquals(1, files.size());
        ChangedFile f = files.get(0);
        assertEquals("src/App.java", f.filename());
        assertEquals("modified", f.status());
        assertEquals(2, f.additions());
        assertEquals(1, f.deletions());
        assertEquals(3, f.changes());
        assertTrue(f.patch().contains("diff --git"));
    }

    @Test
    void parsesMultipleFiles() {
        String diff = """
                diff --git a/A.java b/A.java
                --- a/A.java
                +++ b/A.java
                @@ -1 +1 @@
                -a
                +aa
                diff --git a/B.java b/B.java
                --- a/B.java
                +++ b/B.java
                @@ -1 +1,2 @@
                -b
                +bb
                +bbb
                """;

        List<ChangedFile> files = parser.parse(diff);

        assertEquals(2, files.size());
        assertEquals("A.java", files.get(0).filename());
        assertEquals("B.java", files.get(1).filename());
    }

    @Test
    void parsesNewFile() {
        String diff = """
                diff --git a/New.java b/New.java
                new file mode 100644
                index 0000000..1234567
                --- /dev/null
                +++ b/New.java
                @@ -0,0 +1,3 @@
                +line1
                +line2
                +line3
                """;

        List<ChangedFile> files = parser.parse(diff);

        assertEquals(1, files.size());
        assertEquals("New.java", files.get(0).filename());
        assertEquals("added", files.get(0).status());
        assertEquals(3, files.get(0).additions());
        assertEquals(0, files.get(0).deletions());
    }

    @Test
    void parsesDeletedFile() {
        String diff = """
                diff --git a/Old.java b/Old.java
                deleted file mode 100644
                index 1234567..0000000
                --- a/Old.java
                +++ /dev/null
                @@ -1,3 +0,0 @@
                -line1
                -line2
                -line3
                """;

        List<ChangedFile> files = parser.parse(diff);

        assertEquals(1, files.size());
        assertEquals("Old.java", files.get(0).filename());
        assertEquals("removed", files.get(0).status());
        assertEquals(0, files.get(0).additions());
        assertEquals(3, files.get(0).deletions());
    }

    @Test
    void throwsOnEmptyDiff() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("   "));
    }

    @Test
    void throwsOnNonStandardDiff() {
        assertThrows(IllegalArgumentException.class, () ->
                parser.parse("this is not a git diff"));
        assertThrows(IllegalArgumentException.class, () ->
                parser.parse("just some random text\nwithout diff header"));
    }
}
