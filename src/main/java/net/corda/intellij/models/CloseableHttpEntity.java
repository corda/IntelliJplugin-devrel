package net.corda.intellij.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.http.HttpEntity;

import java.io.Closeable;
import java.io.IOException;

import static org.apache.http.util.EntityUtils.consume;

@AllArgsConstructor
@Getter
public class CloseableHttpEntity implements Closeable {

    private final HttpEntity entity;

    @Override
    public void close() throws IOException {
        consume(entity);
    }
}
