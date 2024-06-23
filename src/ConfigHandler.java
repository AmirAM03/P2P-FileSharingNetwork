import java.io.*;
import java.util.Scanner;

public class ConfigHandler {

    private static void writeAStringIntoConfFile(String toBeWritten) throws IOException {
        File conf = new File("src/main/resources/Config.json");
        if (conf.exists()) conf.delete();
        conf.createNewFile();

        FileWriter confWriter = new FileWriter(conf.getPath());

        BufferedWriter bufWriter = new BufferedWriter(confWriter);
        bufWriter.write(toBeWritten);
        bufWriter.close();
    }


    private static String readFirstLineOfConfigFile() throws IOException {
        File conf = new File("src/main/resources/Config.json");

        if (conf.exists()) {
            Scanner reader = new Scanner(conf);
            if (reader.hasNextLine()) {
                return reader.nextLine();
            } else {
                System.out.println("Conf file is empty");
                System.exit(0);
            }
        } else {
            System.out.println("Conf file not exists");
            System.exit(0);
        }


        return null;
    }


    public static void resetConfig() throws IOException {
//        JSONObject js = new JSONObject();
//        js.put("RootTrackerAddress", "127.0.0.1:55555");
////        System.out.println(js.toString());
//
//        writeAStringIntoConfFile(js.toString());

        writeAStringIntoConfFile("127.0.0.1:55555");
    }


    public static String queryConfigByKey(String key) throws IOException {
//        JSONObject js = new JSONObject(readFirstLineOfConfigFile());
//        return (String) js.get(key);
////        return js.toString();

        return (String) readFirstLineOfConfigFile();
    }

}
