package GUI_MP3Player_part2.utils;


import GUI_MP3Player_part2.gui.MP3PlayerGui;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

// утилита для работы с файлами
public class FileUtils {

    // получить имя файла без расширения
    public static String getFileNameWithoutExtension(String fileName) {
        File file = new File(fileName);
        int index = file.getName().lastIndexOf('.');
        if (index > 0 && index <= file.getName().length() - 2) {
            return file.getName().substring(0, index);
        }
        return "noname";
    }

    // получить расширение файла
    public static String getFileExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    // удалить текущий файл-фильтр и установить новый переданный    
    public static void addFileFilter(JFileChooser jfc, FileFilter filter) {
        jfc.removeChoosableFileFilter(jfc.getFileFilter());
        jfc.setFileFilter(filter);
        jfc.setSelectedFile(new File("")); // удалить последнее имя открыв./сохраняемого файла
    }

    // сохранить объект
    public static void serialize(Object obj, String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            fos.close();
        } catch (IOException e) {
            Logger.getLogger(FileUtils.class.getName()).log(java.util.logging.Level.SEVERE, null, e);

        }
    }

    // открыть объект
    public static Object deserialize(String fileName) {
        try{
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object ts = (Object) ois.readObject();
            fis.close();
            return ts;
        } catch (ClassCastException | IOException | ClassNotFoundException e) {
            Logger.getLogger(FileUtils.class.getName()).log(java.util.logging.Level.SEVERE, null, e);
        }
        return null;
    }
}
