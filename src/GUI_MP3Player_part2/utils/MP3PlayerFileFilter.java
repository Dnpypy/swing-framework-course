package GUI_MP3Player_part2.utils;

import java.io.File;
import javax.swing.filechooser.FileFilter;

// фильтр для возможности выбора файлов только с раширением mp3 для компонента FileChooser
public class MP3PlayerFileFilter extends FileFilter{

    private String fileExtension; //расширение файла
    private String fileDescription; //описание файла

    public MP3PlayerFileFilter(String fileExtension, String fileDescription) {
        this.fileExtension = fileExtension;
        this.fileDescription = fileDescription;
    }

    @Override
    public boolean accept(File file) {
        //разрешить только папки, а также файлы с расширением mp3
        return file.isDirectory()|| file.getAbsolutePath().endsWith(fileExtension);
    }

    @Override
    public String getDescription() {
        //описание для формата mp3 при выборе в диалоговом окне
        return fileDescription + " (*." + fileExtension + ")";
    }
}
