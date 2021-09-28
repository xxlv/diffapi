package diff.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class FileUtils {

    private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public static List<String> readFileFromResource(String fileName) {

        readWriteLock.readLock().lock();
        try {
            return Files.readLines(new File(
                    System.getProperty("user.dir") + File.separator + "dataprovider"
                            + File.separator + fileName
            ), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            readWriteLock.readLock().unlock();
        }
        return Collections.emptyList();
    }


    public static Boolean writeTo(String content, String file) {
        // write file
        readWriteLock.writeLock().lock();
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            byte[] bytes = content.getBytes();
            fileOutputStream.write(bytes);
        } catch (IOException e) {
            return false;
        } finally {
            readWriteLock.writeLock().unlock();
        }

        return true;
    }

    public static String readFile(File f) {
        String str = "";
        try {
            FileReader fileReader = new FileReader(f);
            Reader reader = new InputStreamReader(new FileInputStream(f), "utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            str = sb.toString();
            return str;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static List<File> allFiles(String path) {
        List<File> result = new ArrayList<>();
        File file = new File(path);

        if (!file.exists()) {
            return result;
        }
        File[] fs = file.listFiles();

        for (File f : fs) {
            if (!f.isDirectory()) {
                if (f.canRead()) {
                    result.add(f);
                }
            } else {
                result.addAll(allFiles(f.getAbsolutePath()));
            }
        }
        return result;
    }

    public static List<JSONObject> loadJson(String path) {
        List<String> result = new ArrayList<>();
        List<File> files = allFiles(path);
        for (File f : files) {
            if (!f.isDirectory()) {
                if (f.canRead()) {
                    if (f.getName().endsWith(".json")) {
                        result.add(readFile(f));
                    }
                }
            }
        }
        return toJson(result);
    }

    public static List<JSONObject> toJson(List<String> jsons) {
        return jsons.stream().map((e) -> (JSONObject) JSON.parse(e)).collect(Collectors.toList());
    }


}
