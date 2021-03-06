package home8.utils;

import home8.testguiswing.MP3PlayerGui;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public class SkinUtils {


    /**
     *
     * @param comp компонент
     * @param laf скин библиотеки
     */
    public static void changeSkin(Component comp, LookAndFeel laf) {
        try {
            UIManager.setLookAndFeel(laf);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(MP3PlayerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

        SwingUtilities.updateComponentTreeUI(comp);
        
    }

    /**
     *
     * @param comp компонент
     * @param laf скин в виде строки
     */
    public static void changeSkin(Component comp, String laf) {
        try {
            try {
                UIManager.setLookAndFeel(laf);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(SkinUtils.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(SkinUtils.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(SkinUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(MP3PlayerGui.class.getName()).log(Level.SEVERE, null, ex);
        }

        SwingUtilities.updateComponentTreeUI(comp);
        
    }
}
