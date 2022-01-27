package org.mvnsearch.http;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@Component
public class HttpFileRunner implements CommandLineRunner, ExitCodeGenerator {
    private final HttpxCommand httpIJCommand;
    private final IFactory factory;
    private int exitCode;

    public HttpFileRunner(HttpxCommand httpIJCommand, IFactory factory) {
        this.httpIJCommand = httpIJCommand;
        this.factory = factory;
    }

    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(httpIJCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
