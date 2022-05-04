/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI_MP3Player_final.gui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerListener;
import GUI_MP3Player_final.objects.MP3Player;
import GUI_MP3Player_final.objects.MP3;
import GUI_MP3Player_final.utils.FileUtils;
import GUI_MP3Player_final.utils.MP3PlayerFileFilter;
import GUI_MP3Player_final.utils.SkinUtils;

/**
 * @author Tim
 */
public class MP3PlayerGui extends JFrame implements BasicPlayerListener {

    //<editor-fold defaultstate="collapsed" desc="константы">
    private static final String MP3_FILE_EXTENSION = "mp3";
    private static final String MP3_FILE_DESCRIPTION = "Файлы mp3";
    private static final String PLAYLIST_FILE_EXTENSION = "pls";
    private static final String PLAYLIST_FILE_DESCRIPTION = "Файлы плейлиста";
    private static final String EMPTY_STRING = "";
    private static final String INPUT_SONG_NAME = "введите имя песни";
    //</editor-fold>

    private DefaultListModel mp3ListModel = new DefaultListModel();
    private FileFilter mp3FileFilter = new MP3PlayerFileFilter(MP3_FILE_EXTENSION, MP3_FILE_DESCRIPTION);
    private FileFilter playlistFileFilter = new MP3PlayerFileFilter(PLAYLIST_FILE_EXTENSION, PLAYLIST_FILE_DESCRIPTION);
    private MP3Player player = new MP3Player(this);

    //<editor-fold defaultstate="collapsed" desc="переменные для прокрутки песни">
    private long secondsAmount; // сколько секунд прошло с начала проигрывания
    private long duration; // длительность песни в секундах
    private int bytesLen; // размер песни в байтах
    private double posValue = 0.0; // позиция для прокрутки
    // передвигается ли ползунок песни от перетаскивания (или от проигрывания) - используется во время перемотки
    private boolean movingFromJump = false;
    private boolean moveAutomatic = false;// во время проигрывании песни ползунок передвигается, moveAutomatic = true
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="слушатель событий плеера BasicPlayerListener">
    @Override
    public void opened(Object o, Map map) {

        //        еще один вариант определения mp3 тегов
//        AudioFileFormat aff = null;
//        try {
//            aff = AudioSystem.getAudioFileFormat(new File(o.toString()));
//        } catch (UnsupportedAudioFileException ex) {
//            Logger.getLogger(MP3PlayerGui.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(MP3PlayerGui.class.getName()).log(Level.SEVERE, null, ex);
//        }


        duration = Math.round((((Long) map.get("duration")).longValue()) / 1000000); // длительность песни в секундах
        bytesLen = Math.round(((Integer) map.get("mp3.length.bytes")).intValue()); // размер песни в байтах

        String songName = map.get("title") != null ? map.get("title").toString() : FileUtils.getFileNameWithoutExtension(new File(o.toString()).getName());

        // если длинное название - укоротить его
        if (songName.length() > 30) {
            songName = songName.substring(0, 30) + "…";
        }

        labelSongName.setText(songName);

    }

    @Override
    public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties) {

        float progress = -1.0f;

        if ((bytesread > 0) && ((duration > 0))) {
            progress = bytesread * 1.0f / bytesLen * 1.0f;
        }


        // сколько секунд прошло
        secondsAmount = (long) (duration * progress);

        if (duration != 0) {
            if (movingFromJump == false) {
                slideProgress.setValue(((int) Math.round(secondsAmount * 1000 / duration)));

            }
        }
    }

    @Override
    public void stateUpdated(BasicPlayerEvent bpe) {
        int state = bpe.getCode();

        if (state == BasicPlayerEvent.PLAYING) {
            movingFromJump = false;
        } else if (state == BasicPlayerEvent.SEEKING) {
            movingFromJump = true;
        } else if (state == BasicPlayerEvent.EOM) {
            if (selectNextSong()) {
                playFile();
            }
        }
    }

    @Override
    public void setController(BasicController bc) {
    }
    //</editor-fold>

    /**
     * Creates new form MP3PlayerGui
     */
    public MP3PlayerGui() {
        initComponents();

    }

    private void playFile() {
        int[] indexPlayList = lstPlayList.getSelectedIndices();// получаем выбранные индексы(порядковый номер) песен
        if (indexPlayList.length > 0) {// если выбрали хотя бы одну песню
            MP3 mp3 = (MP3) mp3ListModel.getElementAt(indexPlayList[0]);// находим первую выбранную песню (т.к. несколько песен нельзя проиграть одновременно
            player.play(mp3.getPath());
            player.setVolume(slideVolume.getValue(), slideVolume.getMaximum());
        }

    }

    private boolean selectPrevSong() {
        int nextIndex = lstPlayList.getSelectedIndex() - 1;
        if (nextIndex >= 0) {// если не вышли за пределы плейлиста
            lstPlayList.setSelectedIndex(nextIndex);
            return true;
        }

        return false;
    }

    private boolean selectNextSong() {
        int nextIndex = lstPlayList.getSelectedIndex() + 1;
        if (nextIndex <= lstPlayList.getModel().getSize() - 1) {// если не вышли за пределы плейлиста
            lstPlayList.setSelectedIndex(nextIndex);
            return true;
        }
        return false;
    }

    private void searchSong() {
        String searchStr = txtSearch.getText();

        // если в поиске ничего не ввели - выйти из метода и не производить поиск
        if (searchStr == null || searchStr.trim().equals(EMPTY_STRING)) {
            return;
        }

        // все индексы объектов, найденных по поиску, будут храниться в коллекции
        ArrayList<Integer> mp3FindedIndexes = new ArrayList<Integer>();

        // проходим по коллекции и ищем соответствия имен песен со строкой поиска
        for (int i = 0; i < mp3ListModel.size(); i++) {
            MP3 mp3 = (MP3) mp3ListModel.getElementAt(i);
            // поиск вхождения строки в название песни без учета регистра букв
            if (mp3.getName().toUpperCase().contains(searchStr.toUpperCase())) {
                mp3FindedIndexes.add(i);// найденный индексы добавляем в коллекцию
            }
        }

        // коллекцию индексов сохраняем в массив
        int[] selectIndexes = new int[mp3FindedIndexes.size()];

        if (selectIndexes.length == 0) {// если не найдено ни одной песни, удовлетворяющей условию поиска
            JOptionPane.showMessageDialog(this, "Поиск по строке \'" + searchStr + "\' не дал результатов");
            txtSearch.requestFocus();
            txtSearch.selectAll();
            return;
        }

        // преобразовать коллекцию в массив, т.к. метод для выделения строк в JList работает только с массивом
        for (int i = 0; i < selectIndexes.length; i++) {
            selectIndexes[i] = mp3FindedIndexes.get(i).intValue();
        }

        // выделить в плелисте найдные песни по массиву индексов, найденных ранее
        lstPlayList.setSelectedIndices(selectIndexes);
    }

    private void processSeek(double bytes) {
        try {
            long skipBytes = (long) Math.round(((Integer) bytesLen).intValue() * bytes);
            player.jump(skipBytes);
        } catch (Exception e) {
            e.printStackTrace();
            movingFromJump = false;
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new JFileChooser();
        popupPlaylist = new JPopupMenu();
        popmenuAddSong = new JMenuItem();
        popmenuDeleteSong = new JMenuItem();
        jSeparator1 = new JPopupMenu.Separator();
        popmenuOpenPlaylist = new JMenuItem();
        popmenuClearPlaylist = new JMenuItem();
        jSeparator3 = new JPopupMenu.Separator();
        popmenuPlay = new JMenuItem();
        popmenuStop = new JMenuItem();
        popmenuPause = new JMenuItem();
        panelMain = new JPanel();
        btnStopSong = new JButton();
        btnPauseSong = new JButton();
        btnPlaySong = new JButton();
        btnNextSong = new JButton();
        jScrollPane2 = new JScrollPane();
        lstPlayList = new JList();
        slideVolume = new JSlider();
        tglbtnVolume = new JToggleButton();
        btnPrevSong = new JButton();
        btnAddSong = new JButton();
        btnDeleteSong = new JButton();
        btnSelectNext = new JButton();
        jSeparator2 = new JSeparator();
        btnSelectPrev = new JButton();
        slideProgress = new JSlider();
        labelSongName = new JLabel();
        panelSearch = new JPanel();
        btnSearch = new JButton();
        txtSearch = new JTextField();
        jMenuBar1 = new JMenuBar();
        menuFile = new JMenu();
        menuOpenPlaylist = new JMenuItem();
        menuSavePlaylist = new JMenuItem();
        menuSeparator = new JPopupMenu.Separator();
        menuExit = new JMenuItem();
        menuPrefs = new JMenu();
        menuChangeSkin = new JMenu();
        menuSkin1 = new JMenuItem();
        menuSkin2 = new JMenuItem();

        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setDialogTitle("Выбрать файл");
        fileChooser.setMultiSelectionEnabled(true);

        popmenuAddSong.setIcon(new ImageIcon(getClass().getResource("../images/plus_16.png"))); // NOI18N
        popmenuAddSong.setText("Добавить песню");
        popmenuAddSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuAddSongActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuAddSong);

        popmenuDeleteSong.setIcon(new ImageIcon(getClass().getResource("../images/remove_icon.png"))); // NOI18N
        popmenuDeleteSong.setText("Удалить песню");
        popmenuDeleteSong.setToolTipText("");
        popmenuDeleteSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuDeleteSongActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuDeleteSong);
        popupPlaylist.add(jSeparator1);

        popmenuOpenPlaylist.setIcon(new ImageIcon(getClass().getResource("../images/open-icon.png"))); // NOI18N
        popmenuOpenPlaylist.setText("Открыть плейлист");
        popmenuOpenPlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuOpenPlaylistActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuOpenPlaylist);

        popmenuClearPlaylist.setIcon(new ImageIcon(getClass().getResource("../images/clear.png"))); // NOI18N
        popmenuClearPlaylist.setText("Очистить плейлист");
        popmenuClearPlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuClearPlaylistActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuClearPlaylist);
        popupPlaylist.add(jSeparator3);

        popmenuPlay.setIcon(new ImageIcon(getClass().getResource("../images/Play.png"))); // NOI18N
        popmenuPlay.setText("Play");
        popmenuPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuPlayActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuPlay);

        popmenuStop.setIcon(new ImageIcon(getClass().getResource("../images/stop-red-icon.png"))); // NOI18N
        popmenuStop.setText("Stop");
        popmenuStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuStopActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuStop);

        popmenuPause.setIcon(new ImageIcon(getClass().getResource("../images/Pause-icon.png"))); // NOI18N
        popmenuPause.setText("Pause");
        popmenuPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuPauseActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuPause);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("MP3 плеер");
        setResizable(false);

        panelMain.setBorder(BorderFactory.createTitledBorder(""));

        btnStopSong.setIcon(new ImageIcon(getClass().getResource("../images/stop-red-icon.png"))); // NOI18N
        btnStopSong.setToolTipText("Остановить");
        btnStopSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopSongActionPerformed(evt);
            }
        });

        btnPauseSong.setIcon(new ImageIcon(getClass().getResource("../images/Pause-icon.png"))); // NOI18N
        btnPauseSong.setToolTipText("Пауза");
        btnPauseSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPauseSongActionPerformed(evt);
            }
        });

        btnPlaySong.setIcon(new ImageIcon(getClass().getResource("../images/Play.png"))); // NOI18N
        btnPlaySong.setToolTipText("Воспроизвести");
        btnPlaySong.setName("btnPlay");
        btnPlaySong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPlaySongActionPerformed(evt);
            }
        });

        btnNextSong.setIcon(new ImageIcon(getClass().getResource("../images/next-icon.png"))); // NOI18N
        btnNextSong.setToolTipText("Следующая песня");
        btnNextSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNextSongActionPerformed(evt);
            }
        });

        lstPlayList.setModel(mp3ListModel);
        lstPlayList.setToolTipText("Список песен");
        lstPlayList.setComponentPopupMenu(popupPlaylist);
        lstPlayList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lstPlayListMouseClicked(evt);
            }
        });
        lstPlayList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                lstPlayListKeyPressed(evt);
            }
        });
        jScrollPane2.setViewportView(lstPlayList);

        slideVolume.setMaximum(200);
        slideVolume.setMinorTickSpacing(5);
        slideVolume.setSnapToTicks(true);
        slideVolume.setToolTipText("Изменить громкость");
        slideVolume.setValue(100);
        slideVolume.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                slideVolumeStateChanged(evt);
            }
        });

        tglbtnVolume.setIcon(new ImageIcon(getClass().getResource("../images/speaker.png"))); // NOI18N
        tglbtnVolume.setToolTipText("Выключить звук");
        tglbtnVolume.setSelectedIcon(new ImageIcon(getClass().getResource("../images/mute.png"))); // NOI18N
        tglbtnVolume.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tglbtnVolumeActionPerformed(evt);
            }
        });

        btnPrevSong.setIcon(new ImageIcon(getClass().getResource("../images/prev-icon.png"))); // NOI18N
        btnPrevSong.setToolTipText("Предыдущая песня");
        btnPrevSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrevSongActionPerformed(evt);
            }
        });

        btnAddSong.setIcon(new ImageIcon(getClass().getResource("../images/plus_16.png"))); // NOI18N
        btnAddSong.setToolTipText("Добавить песню");
        btnAddSong.setHorizontalAlignment(SwingConstants.LEFT);
        btnAddSong.setHorizontalTextPosition(SwingConstants.LEFT);
        btnAddSong.setName("btnAddSong");
        btnAddSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddSongActionPerformed(evt);
            }
        });

        btnDeleteSong.setIcon(new ImageIcon(getClass().getResource("../images/remove_icon.png"))); // NOI18N
        btnDeleteSong.setToolTipText("Удалить песню");
        btnDeleteSong.setHorizontalAlignment(SwingConstants.LEFT);
        btnDeleteSong.setHorizontalTextPosition(SwingConstants.LEFT);
        btnDeleteSong.setName("btnAddSong");
        btnDeleteSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteSongActionPerformed(evt);
            }
        });

        btnSelectNext.setIcon(new ImageIcon(getClass().getResource("../images/arrow-down-icon.png"))); // NOI18N
        btnSelectNext.setToolTipText("Выделить следующую песню");
        btnSelectNext.setHorizontalAlignment(SwingConstants.LEFT);
        btnSelectNext.setHorizontalTextPosition(SwingConstants.LEFT);
        btnSelectNext.setName("btnAddSong");
        btnSelectNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectNextActionPerformed(evt);
            }
        });

        jSeparator2.setOrientation(SwingConstants.VERTICAL);

        btnSelectPrev.setIcon(new ImageIcon(getClass().getResource("../images/arrow-up-icon.png"))); // NOI18N
        btnSelectPrev.setToolTipText("Выделить предыдущую песню");
        btnSelectPrev.setHorizontalAlignment(SwingConstants.LEFT);
        btnSelectPrev.setHorizontalTextPosition(SwingConstants.LEFT);
        btnSelectPrev.setName("btnAddSong");
        btnSelectPrev.setVerticalTextPosition(SwingConstants.TOP);
        btnSelectPrev.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectPrevActionPerformed(evt);
            }
        });

        slideProgress.setMaximum(1000);
        slideProgress.setMinorTickSpacing(1);
        slideProgress.setSnapToTicks(true);
        slideProgress.setToolTipText("Изменить громкость");
        slideProgress.setValue(0);
        slideProgress.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                slideProgressStateChanged(evt);
            }
        });

        labelSongName.setText("...");
        labelSongName.setName("");

        GroupLayout panelMainLayout = new GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(
                panelMainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelMainLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelMainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2)
                                        .addGroup(panelMainLayout.createSequentialGroup()
                                                .addComponent(btnAddSong, GroupLayout.PREFERRED_SIZE, 49, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnDeleteSong)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jSeparator2, GroupLayout.PREFERRED_SIZE, 13, GroupLayout.PREFERRED_SIZE)
                                                .addGap(37, 37, 37)
                                                .addComponent(btnSelectNext, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(btnSelectPrev, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(slideProgress, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(panelMainLayout.createSequentialGroup()
                                                .addComponent(tglbtnVolume, GroupLayout.PREFERRED_SIZE, 27, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelMainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addGroup(panelMainLayout.createSequentialGroup()
                                                                .addComponent(btnPrevSong, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnPlaySong, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnPauseSong, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnStopSong, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnNextSong, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                        .addGroup(panelMainLayout.createSequentialGroup()
                                                                .addGap(28, 28, 28)
                                                                .addComponent(slideVolume, GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE))))
                                        .addGroup(panelMainLayout.createSequentialGroup()
                                                .addComponent(labelSongName)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        panelMainLayout.setVerticalGroup(
                panelMainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelMainLayout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(panelMainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(GroupLayout.Alignment.TRAILING, panelMainLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                .addComponent(btnSelectNext, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jSeparator2)
                                                .addComponent(btnSelectPrev, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addComponent(btnAddSong, GroupLayout.Alignment.TRAILING)
                                        .addComponent(btnDeleteSong, GroupLayout.Alignment.TRAILING))
                                .addGap(18, 18, 18)
                                .addComponent(jScrollPane2, GroupLayout.PREFERRED_SIZE, 255, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(labelSongName)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(slideProgress, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelMainLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(slideVolume, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(tglbtnVolume))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(panelMainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(panelMainLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                .addComponent(btnPlaySong, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnPauseSong, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnStopSong, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnNextSong, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(btnPrevSong, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                                .addGap(68, 68, 68))
        );

        panelSearch.setBorder(BorderFactory.createTitledBorder(""));

        btnSearch.setIcon(new ImageIcon(getClass().getResource("../images/search_16.png"))); // NOI18N
        btnSearch.setText("Найти");
        btnSearch.setToolTipText("Найти песню");
        btnSearch.setActionCommand("search");
        btnSearch.setName("btnSearch");
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        txtSearch.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        txtSearch.setForeground(new java.awt.Color(153, 153, 153));
        txtSearch.setText("введите имя песни");
        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtSearchFocusGained(evt);
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                txtSearchFocusLost(evt);
            }
        });
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                txtSearchKeyPressed(evt);
            }
        });

        GroupLayout panelSearchLayout = new GroupLayout(panelSearch);
        panelSearch.setLayout(panelSearchLayout);
        panelSearchLayout.setHorizontalGroup(
                panelSearchLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelSearchLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(txtSearch)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSearch)
                                .addContainerGap())
        );
        panelSearchLayout.setVerticalGroup(
                panelSearchLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelSearchLayout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(panelSearchLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtSearch, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnSearch))
                                .addContainerGap())
        );

        menuFile.setText("Файл");

        menuOpenPlaylist.setIcon(new ImageIcon(getClass().getResource("../images/open-icon.png"))); // NOI18N
        menuOpenPlaylist.setText("Открыть плейлист");
        menuOpenPlaylist.setName("");
        menuOpenPlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOpenPlaylistActionPerformed(evt);
            }
        });
        menuFile.add(menuOpenPlaylist);

        menuSavePlaylist.setIcon(new ImageIcon(getClass().getResource("../images/save_16.png"))); // NOI18N
        menuSavePlaylist.setText("Сохранить плейлист");
        menuSavePlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSavePlaylistActionPerformed(evt);
            }
        });
        menuFile.add(menuSavePlaylist);
        menuFile.add(menuSeparator);

        menuExit.setIcon(new ImageIcon(getClass().getResource("../images/exit.png"))); // NOI18N
        menuExit.setText("Выход");
        menuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExitActionPerformed(evt);
            }
        });
        menuFile.add(menuExit);

        jMenuBar1.add(menuFile);

        menuPrefs.setText("Сервис");

        menuChangeSkin.setIcon(new ImageIcon(getClass().getResource("../images/gear_16.png"))); // NOI18N
        menuChangeSkin.setText("Внешний вид");

        menuSkin1.setText("Скин 1");
        menuSkin1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSkin1ActionPerformed(evt);
            }
        });
        menuChangeSkin.add(menuSkin1);

        menuSkin2.setText("Скин 2");
        menuSkin2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSkin2ActionPerformed(evt);
            }
        });
        menuChangeSkin.add(menuSkin2);

        menuPrefs.add(menuChangeSkin);

        jMenuBar1.add(menuPrefs);

        setJMenuBar(jMenuBar1);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(panelSearch, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelMain, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(5, 5, 5)
                                .addComponent(panelSearch, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelMain, GroupLayout.PREFERRED_SIZE, 475, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(22, Short.MAX_VALUE))
        );

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width - 366) / 2, (screenSize.height - 619) / 2, 366, 619);
    }// </editor-fold>//GEN-END:initComponents

    private void btnPlaySongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPlaySongActionPerformed
        playFile();
    }//GEN-LAST:event_btnPlaySongActionPerformed

    private void btnStopSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopSongActionPerformed
        player.stop();
    }//GEN-LAST:event_btnStopSongActionPerformed

    private void btnPauseSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPauseSongActionPerformed
        player.pause();
    }//GEN-LAST:event_btnPauseSongActionPerformed

    private void slideVolumeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_slideVolumeStateChanged
        player.setVolume(slideVolume.getValue(), slideVolume.getMaximum());

        if (slideVolume.getValue() == 0) {
            tglbtnVolume.setSelected(true);
        } else {
            tglbtnVolume.setSelected(false);
        }
    }//GEN-LAST:event_slideVolumeStateChanged

    private void btnNextSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextSongActionPerformed
        if (selectNextSong()) {
            playFile();
        }
    }//GEN-LAST:event_btnNextSongActionPerformed

    private void btnPrevSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrevSongActionPerformed
        if (selectPrevSong()) {
            playFile();
        }
    }//GEN-LAST:event_btnPrevSongActionPerformed

    private void btnSelectPrevActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSelectPrevActionPerformed
        selectPrevSong();
    }//GEN-LAST:event_btnSelectPrevActionPerformed

    private void btnSelectNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSelectNextActionPerformed
        selectNextSong();
    }//GEN-LAST:event_btnSelectNextActionPerformed

    private void btnAddSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddSongActionPerformed
        FileUtils.addFileFilter(fileChooser, mp3FileFilter);
        int result = fileChooser.showOpenDialog(this);// result хранит результат: выбран файл или нет

        if (result == JFileChooser.APPROVE_OPTION) {// если нажата клавиша OK или YES

            File[] selectedFiles = fileChooser.getSelectedFiles();
            // перебираем все выделенные файлы для добавления в плейлист
            for (File file : selectedFiles) {
                MP3 mp3 = new MP3(file.getName(), file.getPath());

                // если эта песня уже есть в списке - не добавлять ее
                if (!mp3ListModel.contains(mp3)) {
                    mp3ListModel.addElement(mp3);
                }
            }

        }
    }//GEN-LAST:event_btnAddSongActionPerformed

    private void btnDeleteSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteSongActionPerformed
        int[] indexPlayList = lstPlayList.getSelectedIndices();// получаем выбранные индексы(порядковый номер) песен
        if (indexPlayList.length > 0) {// если выбрали хотя бы одну песню
            ArrayList<MP3> mp3ListForRemove = new ArrayList<MP3>();// сначала сохраняем все mp3 для удаления в отдельную коллекцию
            for (int i = 0; i < indexPlayList.length; i++) {// удаляем все выбранные песни из плейлиста
                MP3 mp3 = (MP3) mp3ListModel.getElementAt(indexPlayList[i]);
                mp3ListForRemove.add(mp3);
            }

            // удаляем mp3 в плейлисте
            for (MP3 mp3 : mp3ListForRemove) {
                mp3ListModel.removeElement(mp3);
            }

        }
    }//GEN-LAST:event_btnDeleteSongActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        searchSong();

    }//GEN-LAST:event_btnSearchActionPerformed

    private void txtSearchFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtSearchFocusGained
        if (txtSearch.getText().equals(INPUT_SONG_NAME)) {
            txtSearch.setText(EMPTY_STRING);
        }
    }//GEN-LAST:event_txtSearchFocusGained

    private void txtSearchFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtSearchFocusLost
        if (txtSearch.getText().trim().equals(EMPTY_STRING)) {
            txtSearch.setText(INPUT_SONG_NAME);
        }
    }//GEN-LAST:event_txtSearchFocusLost

    private void menuSavePlaylistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSavePlaylistActionPerformed
        FileUtils.addFileFilter(fileChooser, playlistFileFilter);
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {// если нажата клавиша OK или YES
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.exists()) {// если такой файл уже существует

                int resultOvveride = JOptionPane.showConfirmDialog(this, "Файл существует", "Перезаписать?", JOptionPane.YES_NO_CANCEL_OPTION);
                switch (resultOvveride) {
                    case JOptionPane.NO_OPTION:
                        menuSavePlaylistActionPerformed(evt);// повторно открыть окно сохранения файла
                        return;
                    case JOptionPane.CANCEL_OPTION:
                        fileChooser.cancelSelection();
                        return;
                }
                fileChooser.approveSelection();
            }

            String fileExtension = FileUtils.getFileExtension(selectedFile);

            // имя файла (нужно ли добавлять раширение к имени файлу при сохранении)
            String fileNameForSave = (fileExtension != null && fileExtension.equals(PLAYLIST_FILE_EXTENSION)) ? selectedFile.getPath() : selectedFile.getPath() + "." + PLAYLIST_FILE_EXTENSION;

            FileUtils.serialize(mp3ListModel, fileNameForSave);
        }

    }//GEN-LAST:event_menuSavePlaylistActionPerformed

    private void menuOpenPlaylistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOpenPlaylistActionPerformed
        FileUtils.addFileFilter(fileChooser, playlistFileFilter);
        int result = fileChooser.showOpenDialog(this);// result хранит результат: выбран файл или нет


        if (result == JFileChooser.APPROVE_OPTION) {// если нажата клавиша OK или YES
            File selectedFile = fileChooser.getSelectedFile();//
            DefaultListModel mp3ListModel = (DefaultListModel) FileUtils.deserialize(selectedFile.getPath());
            this.mp3ListModel = mp3ListModel;
            lstPlayList.setModel(mp3ListModel);
        }


    }//GEN-LAST:event_menuOpenPlaylistActionPerformed

    private void menuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_menuExitActionPerformed

    private void menuSkin1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSkin1ActionPerformed
        SkinUtils.changeSkin(this, UIManager.getSystemLookAndFeelClassName());
        fileChooser.updateUI();
    }//GEN-LAST:event_menuSkin1ActionPerformed

    private void menuSkin2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSkin2ActionPerformed
        SkinUtils.changeSkin(this, new NimbusLookAndFeel());
        fileChooser.updateUI();
    }//GEN-LAST:event_menuSkin2ActionPerformed

    private void lstPlayListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstPlayListMouseClicked

        // если нажали левую кнопку мыши 2 раза
        if (evt.getModifiers() == InputEvent.BUTTON1_MASK && evt.getClickCount() == 2) {
            playFile();
        }
    }//GEN-LAST:event_lstPlayListMouseClicked

    private int currentVolumeValue;

    private void tglbtnVolumeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tglbtnVolumeActionPerformed

        if (tglbtnVolume.isSelected()) {
            currentVolumeValue = slideVolume.getValue();
            slideVolume.setValue(0);
        } else {
            slideVolume.setValue(currentVolumeValue);
        }
    }//GEN-LAST:event_tglbtnVolumeActionPerformed

    private void popmenuAddSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuAddSongActionPerformed
        btnAddSongActionPerformed(evt);
    }//GEN-LAST:event_popmenuAddSongActionPerformed

    private void popmenuDeleteSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuDeleteSongActionPerformed
        btnDeleteSongActionPerformed(evt);
    }//GEN-LAST:event_popmenuDeleteSongActionPerformed

    private void popmenuOpenPlaylistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuOpenPlaylistActionPerformed
        menuOpenPlaylistActionPerformed(evt);
    }//GEN-LAST:event_popmenuOpenPlaylistActionPerformed

    private void popmenuClearPlaylistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuClearPlaylistActionPerformed
        mp3ListModel.clear();
    }//GEN-LAST:event_popmenuClearPlaylistActionPerformed

    private void slideProgressStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_slideProgressStateChanged

        if (!slideProgress.getValueIsAdjusting()) {
            if (moveAutomatic) {
                moveAutomatic = false;
                posValue = slideProgress.getValue() * 1.0 / 1000;
                processSeek(posValue);
            }
        } else {
            moveAutomatic = true;
            movingFromJump = true;
        }

    }//GEN-LAST:event_slideProgressStateChanged

    private void popmenuPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuPlayActionPerformed
        btnPlaySongActionPerformed(evt);
    }//GEN-LAST:event_popmenuPlayActionPerformed

    private void popmenuStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuStopActionPerformed
        btnStopSongActionPerformed(evt);
    }//GEN-LAST:event_popmenuStopActionPerformed

    private void popmenuPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuPauseActionPerformed
        btnPauseSongActionPerformed(evt);
    }//GEN-LAST:event_popmenuPauseActionPerformed

    private void lstPlayListKeyPressed(KeyEvent evt) {//GEN-FIRST:event_lstPlayListKeyPressed
        int key = evt.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            playFile();
        }
    }//GEN-LAST:event_lstPlayListKeyPressed

    private void txtSearchKeyPressed(KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyPressed
        int key = evt.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            searchSong();
        }
    }//GEN-LAST:event_txtSearchKeyPressed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;


                }
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MP3PlayerGui.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(MP3PlayerGui.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MP3PlayerGui.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(MP3PlayerGui.class.getName()).log(Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new MP3PlayerGui().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnAddSong;
    private JButton btnDeleteSong;
    private JButton btnNextSong;
    private JButton btnPauseSong;
    private JButton btnPlaySong;
    private JButton btnPrevSong;
    private JButton btnSearch;
    private JButton btnSelectNext;
    private JButton btnSelectPrev;
    private JButton btnStopSong;
    private JFileChooser fileChooser;
    private JMenuBar jMenuBar1;
    private JScrollPane jScrollPane2;
    private JPopupMenu.Separator jSeparator1;
    private JSeparator jSeparator2;
    private JPopupMenu.Separator jSeparator3;
    private JLabel labelSongName;
    private JList lstPlayList;
    private JMenu menuChangeSkin;
    private JMenuItem menuExit;
    private JMenu menuFile;
    private JMenuItem menuOpenPlaylist;
    private JMenu menuPrefs;
    private JMenuItem menuSavePlaylist;
    private JPopupMenu.Separator menuSeparator;
    private JMenuItem menuSkin1;
    private JMenuItem menuSkin2;
    private JPanel panelMain;
    private JPanel panelSearch;
    private JMenuItem popmenuAddSong;
    private JMenuItem popmenuClearPlaylist;
    private JMenuItem popmenuDeleteSong;
    private JMenuItem popmenuOpenPlaylist;
    private JMenuItem popmenuPause;
    private JMenuItem popmenuPlay;
    private JMenuItem popmenuStop;
    private JPopupMenu popupPlaylist;
    public static JSlider slideProgress;
    private JSlider slideVolume;
    private JToggleButton tglbtnVolume;
    private JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
