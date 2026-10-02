package net.atif.buildnotes.data.template;

import java.util.List;

public record BuildTemplate(
        String id,
        String name,
        List<String> fieldTitles,
        boolean isBuiltIn
) {
    public BuildTemplate {
        // Copy incase the original list is mutated
        fieldTitles = List.copyOf(fieldTitles);
    }
}
