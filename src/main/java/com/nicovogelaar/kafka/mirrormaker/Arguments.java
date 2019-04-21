package com.nicovogelaar.kafka.mirrormaker;

import java.util.*;

class Arguments {

    private static final String SOURCE_URL = "sourceUrl";
    private static final String TARGET_URL = "targetUrl";
    private static final String INCLUDE_KEYS = "includeKeys";
    private static final String WHITELIST = "whitelist";

    final String sourceUrl;
    final String targetUrl;
    final boolean includeKeys;
    final List<String> whitelist;

    private static final Collection<String> REQUIRED = Arrays.asList(SOURCE_URL, TARGET_URL);

    private Arguments(final Map<String, String> parsedArgs) {
        sourceUrl = parsedArgs.get(SOURCE_URL);
        targetUrl = parsedArgs.get(TARGET_URL);
        includeKeys = Boolean.parseBoolean(parsedArgs.get(INCLUDE_KEYS));

        String w = parsedArgs.get(WHITELIST);
        if (w == null || w.isEmpty()) {
            whitelist = Collections.emptyList();
        } else {
            whitelist = Arrays.asList(w.split(";"));
        }
    }

    static Arguments parseArgs(final String args) {
        final Map<String, String> parsed = new HashMap<>();
        for (String argPair : args.split(",")) {
            String[] tokens = argPair.split("=");
            if (tokens.length != 2) {
                throw new IllegalArgumentException("expected a comma-delimited list of arguments in k=v form");
            }

            parsed.put(tokens[0], tokens[1]);
        }

        for (String requiredArg : REQUIRED) {
            if (!parsed.containsKey(requiredArg)) {
                throw new IllegalArgumentException(String.format("%s argument was not supplied", requiredArg));
            }
        }

        return new Arguments(parsed);
    }
}
