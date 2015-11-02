package net.unit8.waitt.feature.admin.routes;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.waitt.api.EmbeddedServer;
import net.unit8.waitt.api.dto.ServerMetadata;
import net.unit8.waitt.feature.admin.Route;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.Util;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

import javax.xml.bind.JAXB;
import java.awt.*;
import java.io.IOException;
import java.io.StringWriter;

/**
 * @author kawasima
 */
public class ServerAction implements Route {
    private final EmbeddedServer server;
    private String rrdPath;

    public ServerAction(EmbeddedServer server, String rrdPath) {
        this.server = server;
        this.rrdPath = rrdPath;
    }

    @Override
    public boolean canHandle(HttpExchange exchange) {
        return "GET".equalsIgnoreCase(exchange.getRequestMethod())
                && exchange.getRequestURI().getPath().startsWith("/server");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("/server/cpu.png".equals(exchange.getRequestURI().getPath())) {
            byte[] graph = renderGraph(GraphType.CPU);
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, graph.length);
            exchange.getResponseBody().write(graph);
        } else if ("/server/memory.png".equals(exchange.getRequestURI().getPath())) {
            byte[] graph = renderGraph(GraphType.MEMORY);
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, graph.length);
            exchange.getResponseBody().write(graph);
        } else {
            StringWriter sw = new StringWriter();
            ServerMetadata metadata = new ServerMetadata();
            metadata.setName(server.getName());
            metadata.setStatus(server.getStatus());
            JAXB.marshal(metadata, sw);
            byte[] body = sw.toString().getBytes("UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private byte[] renderGraph(GraphType type) throws IOException {
        RrdGraphDef gDef = new RrdGraphDef();
        long now = Util.getTimestamp();

        gDef.setStartTime(now - 24 * 60 * 60);
        gDef.setEndTime(now);
        gDef.setWidth(500);
        gDef.setHeight(300);
        gDef.setFilename("-");
        gDef.setPoolUsed(true);
        gDef.setImageFormat("png");

        switch (type) {
            case MEMORY:
                renderGraphMemory(gDef);
                break;
            case CPU:
                renderGraphCpu(gDef);
                break;
        }

        return new RrdGraph(gDef).getRrdGraphInfo().getBytes();
    }

    private void renderGraphMemory(RrdGraphDef gDef) {
        gDef.setTitle("Memory usage");
        gDef.setVerticalLabel("bytes");
        gDef.line("Free memory (physical)", Color.BLUE);
        gDef.line("Free memory (swap)", Color.GREEN);
        gDef.datasource("Free memory (physical)", rrdPath, "memory-physical", ConsolFun.AVERAGE);
        gDef.datasource("Free memory (swap)",     rrdPath, "memory-swap",     ConsolFun.AVERAGE);
        gDef.gprint("Free memory (physical)", ConsolFun.AVERAGE, "Free memory (physical) = %.3f%s");
        gDef.gprint("Free memory (swap)", ConsolFun.AVERAGE, "Free memory (swap) = %.3f%s\\c");
    }

    private void renderGraphCpu(RrdGraphDef gDef) {
        gDef.setTitle("Memory usage");
        gDef.setVerticalLabel("bytes");
        gDef.line("Free memory (physical)", Color.BLUE);
        gDef.line("Free memory (swap)", Color.GREEN);
        gDef.datasource("Free memory (physical)", rrdPath, "memory-physical", ConsolFun.AVERAGE);
        gDef.datasource("Free memory (swap)",     rrdPath, "memory-swap",     ConsolFun.AVERAGE);
        gDef.gprint("Free memory (physical)", ConsolFun.AVERAGE, "Free memory (physical) = %.3f%s");
        gDef.gprint("Free memory (swap)", ConsolFun.AVERAGE, "Free memory (swap) = %.3f%s\\c");
    }

    private enum GraphType { MEMORY, CPU }

}