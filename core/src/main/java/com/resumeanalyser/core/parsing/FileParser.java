package com.resumeanalyser.core.parsing;

import java.io.IOException;
import java.io.InputStream;

public interface FileParser {
    ParsingResult parse(InputStream inputStream) throws IOException;
}
