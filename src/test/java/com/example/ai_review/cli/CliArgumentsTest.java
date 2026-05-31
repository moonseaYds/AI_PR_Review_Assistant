package com.example.ai_review.cli;

import com.example.ai_review.diff.AnalysisMode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CliArgumentsTest {

    @Test
    void shouldIgnoreNonCliArguments() {
        CliArguments arguments = CliArguments.parse(new String[]{"--server.port=9090"});

        assertThat(arguments.enabled()).isFalse();
        assertThat(arguments.analysisMode()).isEqualTo(AnalysisMode.FAST);
    }

    @Test
    void shouldParsePullRequestMode() {
        CliArguments arguments = CliArguments.parse(new String[]{
                "--cli",
                "--pr=https://github.com/owner/repo/pull/123",
                "--mode=deep"
        });

        assertThat(arguments.enabled()).isTrue();
        assertThat(arguments.prUrl()).isEqualTo("https://github.com/owner/repo/pull/123");
        assertThat(arguments.analysisMode()).isEqualTo(AnalysisMode.DEEP);
    }

    @Test
    void shouldParseDiffFileModeWithDefaults() {
        CliArguments arguments = CliArguments.parse(new String[]{
                "--cli",
                "--diff-file=patch.diff"
        });

        assertThat(arguments.diffFile()).isEqualTo(Path.of("patch.diff"));
        assertThat(arguments.repository()).isEqualTo("local-project");
        assertThat(arguments.baseBranch()).isEqualTo("main");
        assertThat(arguments.headBranch()).isEqualTo("working-tree");
    }

    @Test
    void shouldRejectMultipleInputSources() {
        assertThatThrownBy(() -> CliArguments.parse(new String[]{
                "--cli",
                "--pr=https://github.com/owner/repo/pull/123",
                "--diff-stdin"
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("一次只能选择一种输入");
    }

    @Test
    void shouldAllowHelpWithoutInputSource() {
        CliArguments arguments = CliArguments.parse(new String[]{"--help"});

        assertThat(arguments.enabled()).isTrue();
        assertThat(arguments.help()).isTrue();
    }
}
