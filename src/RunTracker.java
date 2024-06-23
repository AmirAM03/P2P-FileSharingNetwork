import models.Tracker;

import java.io.IOException;
import java.net.*;


public class RunTracker {
    public static void main(String[] args) throws IOException, URISyntaxException {
//        try {
//            ConfigHandler.resetConfig();
//            System.out.println(ConfigHandler.queryConfigByKey("RootTrackerAddress"));
//
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        Tracker t = new Tracker("127.0.0.1:12345");
        System.out.println(t.isPeerAlive());
    }
}
