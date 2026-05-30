package com.sibam.graph.storage;

import com.sibam.graph.model.Graph;
import org.springframework.stereotype.Component;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.sibam.graph.model.Graph;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class KryoGraphSerializer implements GraphSerializer {

    private static final String FILE_NAME = "graph.bin";

    private final Kryo kryo;

    public KryoGraphSerializer() {
        this.kryo = new Kryo();
        kryo.register(Graph.class);
    }

    @Override
    public void save(Graph graph) {
        //
    }

    @Override
    public Graph load() {
        return null;
    }

    @Override
    public boolean exists() {
        return false;
    }
}
