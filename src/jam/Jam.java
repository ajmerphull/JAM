/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jam;


import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import java.awt.image.BufferedImage;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;

import java.nio.file.Files;

import java.lang.reflect.InvocationTargetException;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ajmerphull
 */
public class Jam {
    private static SystemTray systemTray;
    private static TrayIcon trayIcon;
    private static final int SC_PHYS_PAGES = 84;
    private static final int SC_PAGESIZE = 30;
    
    private static final Color BLUE = new Color(30, 144, 255);
    private static final Color GREEN = Color.GREEN;
    private static final Color AMBER = Color.ORANGE;
    private static final Color RED = Color.RED;

    private static ServerSocket singleInstanceSocket;
    private static volatile boolean aboutDialogShowing = false;
    private static final String VERSION = "1.1.9";
    private static final String BUILD = "411";
    
    
    
    private static boolean loadAtStartup = true; 
    
    
    
    
    
    
    public static void main(String[] args) {
        // TODO code application logic here
        System.setProperty("apple.awt.UIElement", "true");
    
        if (!acquireInstanceLock()) {
            showAlreadyRunningNotification();
            System.exit(1);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                releaseInstanceLock();
            }
        });
        
        if (!SystemTray.isSupported()) {
            System.err.println("System tray not supported!");
            return;
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
        
        
        
        System.out.println("JVM Used Heap RAM (Bytes): " + getJvmUsedRamBytes());
        System.out.println("JVM Used Heap RAM (MB): " + String.format("%.2f", getJvmUsedRamMB()));

        // Example of allocating some memory to see the change
        System.out.println("\nAllocating some memory...");
        // Create a large array to consume some heap space
        byte[] largeArray = new byte[10 * 1024 * 1024]; // 10 MB
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = (byte) (i % 256); // Fill with some data
        }

        System.out.println("JVM Used Heap RAM (Bytes) after allocation: " + getJvmUsedRamBytes());
        System.out.println("JVM Used Heap RAM (MB) after allocation: " + String.format("%.2f", getJvmUsedRamMB()));

        // Nullifying the reference to allow garbage collection
        largeArray = null;
        // Requesting garbage collection (not guaranteed to run immediately)
        System.gc();

        System.out.println("\nAfter suggesting garbage collection:");
        System.out.println("JVM Used Heap RAM (Bytes): " + getJvmUsedRamBytes());
        System.out.println("JVM Used Heap RAM (MB): " + String.format("%.2f", getJvmUsedRamMB()));
        
        
        
        
        
        
        
        
    }
    
    private static boolean acquireInstanceLock() {
        try {
            
            singleInstanceSocket = new ServerSocket(49276, 1, InetAddress.getLoopbackAddress());
            
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!singleInstanceSocket.isClosed()) {
                            try (Socket socket = singleInstanceSocket.accept()) {
                                BufferedReader in = new BufferedReader(
                                        new InputStreamReader(socket.getInputStream()));
                                String command = in.readLine();
                                if ("show".equals(command)) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (trayIcon != null) {
                                                trayIcon.displayMessage("Jam",
                                                        "Jam is already running",
                                                        TrayIcon.MessageType.INFO);
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    } catch (IOException e) {
                        // Socket closed normally
                    }
                }
            }).start();
            
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    private static void createAndShowGUI() {
        try {
            
            systemTray = SystemTray.getSystemTray();
            
            final PopupMenu popup = new PopupMenu();
            
            MenuItem aboutItem = new MenuItem("About");
            MenuItem gcItem = new MenuItem("Run Garbage Collector");
            
            final CheckboxMenuItem startupItem = new CheckboxMenuItem("Load at Startup");
            startupItem.setState(loadAtStartup);
            
            MenuItem dockVisibilityItem = new MenuItem("Show in App Switcher");
            MenuItem quitItem = new MenuItem("Quit");
            
            startupItem.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    loadAtStartup = startupItem.getState();
                    if (loadAtStartup) {
                        enableStartupLoading();
                    } else {
                        disableStartupLoading();
                    }
                }
            });
            dockVisibilityItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    toggleAppSwitcherVisibility();
                }
            });
            
            
            popup.add(aboutItem);
            popup.addSeparator();
            popup.add(gcItem);
            popup.addSeparator();
            
            popup.add((MenuItem)startupItem);
            
            popup.add(dockVisibilityItem);
            popup.addSeparator();
            popup.add(quitItem);
            
           
            trayIcon = new TrayIcon(createTrayImageWithText(BLUE, "??% ??MB/??MB"));
            trayIcon.setImageAutoSize(true);
            
            
            
            
           
            trayIcon.setPopupMenu(popup);
            
            
            aboutItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAboutDialog(); 
                }
            });
            
            gcItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.gc();
                    updateMemoryStatus();
                }
            });
            
            quitItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            
            
            systemTray.add(trayIcon);
            
            
            updateMemoryStatus();
            
        } catch (AWTException e) {
            System.err.println("TrayIcon could not be added: " + e.getMessage());
        }
    }
    /*
    private static Image createTrayImageWithText(Color color, String text) {
        try {
            // Load the 16x16 icon from your app bundle
            Image icon = Toolkit.getDefaultToolkit().getImage(
                Jam.class.getResource("/jam.iconset/icon_16x16.png")
            );

            // Create temporary graphics for text measurement
            BufferedImage tempImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = tempImg.createGraphics();
            Font font = getSystemFont().deriveFont(Font.PLAIN, 13);
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g2d.dispose();

            // Calculate total width (icon + padding + text)
            int iconWidth = 16; // Matches your icon size
            int padding = 4;
            int totalWidth = iconWidth + padding + textWidth;

            // Create final composite image
            BufferedImage img = new BufferedImage(totalWidth, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Draw the loaded icon
            g.drawImage(icon, 0, 0, 16, 16, null);

            // Draw text
            g.setColor(new Color(30, 30, 30));
            g.setFont(font);
            g.drawString(text, iconWidth + padding, 12);

            g.dispose();
            return img;

        } catch (Exception e) {
            // Fallback to original "M" icon if custom icon fails to load
            System.err.println("Error loading custom icon: " + e.getMessage());

            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(color);
            g.fillRect(0, 0, 16, 16);
            g.setColor(Color.WHITE);
            g.setFont(getSystemFont().deriveFont(Font.BOLD, 12));
            g.drawString("M", 3, 12);
            g.dispose();
            return img;
        }
    }
    */
    private static Image createTrayImageWithText(Color color, String text) {
        
        BufferedImage tempImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempImg.createGraphics();

        // Use same font settings as actual rendering
        Font font = getSystemFont().deriveFont(Font.PLAIN, 13);
        g2d.setFont(font);

        // Calculate text width
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g2d.dispose();

        // Calculate total width (icon + padding + text)
        int iconWidth = 16; // Fixed icon width
        int padding = 4;    // Space between icon and text
        int totalWidth = iconWidth + padding + textWidth;

        // Create final image
        BufferedImage img = new BufferedImage(totalWidth, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw icon background
        g.setColor(color);
        g.fillRect(0, 0, iconWidth, 16);

        // Draw 'M' icon
        g.setColor(Color.WHITE);
        g.setFont(font.deriveFont(Font.BOLD));
        g.drawString("M", 2, 12);

        // Draw text
        g.setColor(new Color(30, 30, 30));
        g.setFont(font);
        g.drawString(text, iconWidth + padding, 12);

        g.dispose();
        return img;
    }
    
    private static void disableStartupLoading() {
        try {
            File plistFile = new File(System.getProperty("user.home"), 
                "Library/LaunchAgents/com.netbridgelimited.jam.plist");
            if (plistFile.exists()) {
                plistFile.delete();
            }
            showNotification("Jam", "Application won't load at startup");
        } catch (Exception e) {
            System.err.println("Error disabling startup: " + e.getMessage());
        }
    }
    
    private static void enableStartupLoading() {
        try {
            // ~/Library/LaunchAgents/
            File launchAgentDir = new File(System.getProperty("user.home"), "Library/LaunchAgents");
            File plistFile = new File(launchAgentDir, "com.netbridgelimited.jam.plist");

            String plistContent = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
                "<plist version=\"1.0\">\n" +
                "<dict>\n" +
                "    <key>Label</key>\n" +
                "    <string>com.netbridgelimited.jam</string>\n" +
                "    <key>ProgramArguments</key>\n" +
                "    <array>\n" +
                "        <string>%s</string>\n" +
                "        <string>-jar</string>\n" +
                "        <string>%s</string>\n" +
                "    </array>\n" +
                "    <key>RunAtLoad</key>\n" +
                "    <true/>\n" +
                "</dict>\n" +
                "</plist>",
                System.getProperty("java.home") + "/bin/java",
                new File("dist/Jam.jar").getAbsolutePath()
            );

           
            Files.write(plistFile.toPath(), plistContent.getBytes());
            showNotification("Jam", "Application will now load at startup");

        } catch (Exception e) {
            System.err.println("Error enabling startup: " + e.getMessage());
        }
    }
    
    private static long getSystemTotalMemory() {
        try {
            // Use macOS system_profiler command
            
            Process p = Runtime.getRuntime().exec("sysctl hw.memsize"); // 34,359,738,368
            BufferedReader reader;
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            if (line != null) {
                return Long.parseLong(line.split(": ")[1].trim());
            }
        } catch (Exception e) {
            System.err.println("Error getting system memory: " + e.getMessage());
        }
        return 0;
    }

    private static long getTotalJVMMemory() {
        long total = 0;
        try {
            // Get memory usage for each Java process
            for (long pid : findJavaPIDs()) {
                String[] cmd = {
                    "/usr/bin/vmmap",
                    "-summary",
                    Long.toString(pid)
                };
                Process proc = Runtime.getRuntime().exec(cmd);
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Java")) {
                        String[] parts = line.split("\\s+");
                        total += Long.parseLong(parts[parts.length-2]) / 1024; // KB to MB
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }
    
    private static List<Long> findJavaPIDs() {
        List<Long> pids = new ArrayList<Long>();
        try {
            // Use macOS ps command to find Java processes
            Process proc = Runtime.getRuntime().exec(
                new String[]{"ps", "-A", "-o", "pid,command"});
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("java") || line.contains("JVM")) {
                    String[] parts = line.trim().split("\\s+", 2);
                    pids.add(Long.parseLong(parts[0]));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pids;
    }

    private static void releaseInstanceLock() {
        try {
            if (singleInstanceSocket != null) {
                singleInstanceSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error releasing instance lock: " + e.getMessage());
        }
    }
    
    private static void showAboutDialog() {
        if (aboutDialogShowing) return;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                aboutDialogShowing = true;

               
                JDialog dialog = new JDialog((Frame)null, "About Jam", true); 
                dialog.setAlwaysOnTop(true);
                
                
                JLabel message = new JLabel("<html><div style='text-align: center;'>"
                                            + "<b>Jam</b> v" + VERSION + " (build " + BUILD + ")<br><br>"
                                            + "Monitors Java Allocated Memory and allows<br>you to deallocate if necessary<br><br>"
                                            + " ©2007 netbridgelimited.com"
                                            + "</div></html>");
                message.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
                dialog.add(message);

                
                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        aboutDialogShowing = false;
                    }
                });

                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setVisible(true);
            }
        });
    }
    
    private static void showAlreadyRunningNotification() {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), 49276); OutputStream out = socket.getOutputStream()) {
          
            out.write("show\n".getBytes());
        } catch (IOException e) {
        
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                try {
                    Runtime.getRuntime().exec(new String[]{"osascript", "-e", 
                        "display notification \"Jam is already running\" " +
                        "with title \"Jam\""});
                } catch (IOException ex) {
                    System.err.println("Jam is already running");
                }
            }
        }
    }
    
    
    
    private static void showNotification(String title, String message) {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            try {
                String script = String.format(
                    "display notification \"%s\" with title \"%s\"",
                    message.replace("\"", "\\\""),
                    title.replace("\"", "\\\"")
                );
                Runtime.getRuntime().exec(new String[]{"osascript", "-e", script});
            } catch (IOException e) {
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
            }
        } else {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }
    private static Font getSystemFont() {
        try {
            return Font.createFont(Font.TRUETYPE_FONT,
                    new File("/System/Library/Fonts/SFPro.ttf"))
                    .deriveFont(Font.PLAIN, 13);
        } catch (Exception e) {
            return new Font("SanFrancisco", Font.PLAIN, 13);
        }
    }
    private static void toggleAppSwitcherVisibility() {
        try {
            Class<?> appClass = Class.forName("com.apple.eawt.Application");
            Object app = appClass.getMethod("getApplication").invoke(null);

            
            Class<?> activationPolicyClass = Class.forName("com.apple.eawt.Application$ActivationPolicy");

            
            //Object[] policies = activationPolicyClass.getEnumConstants();
            @SuppressWarnings("unchecked")
            Enum<?>[] policies = (Enum<?>[]) activationPolicyClass.getEnumConstants();
            Object accessoryPolicy = null;
            for (Object policy : policies) {
                if ("ACCESSORY".equals(policy.toString())) {
                    accessoryPolicy = policy;
                    break;
                }
            }

            if (accessoryPolicy != null) {
                appClass.getMethod("setActivationPolicy", activationPolicyClass)
                       .invoke(app, accessoryPolicy);
            }
        } catch (Exception ex) {
            System.err.println("Couldn't configure app switcher behavior: " + ex.getMessage());
        }
    }
    
    private static void updateMemoryStatus() {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long maxJVM = rt.maxMemory();
        long totalSystemRAM = getSystemTotalMemory();

        // Calculate percentages
        double jvmPercent = (maxJVM > 0) ? (used * 100.0) / maxJVM : 0;
        double systemPercent = (totalSystemRAM > 0) ? (used * 100.0) / totalSystemRAM : 0;

        // Convert to human-readable units
        double usedMB = used / (1024.0 * 1024.0 * 1024.0);
        double maxJVMMB = maxJVM / (1024.0 * 1024.0 * 1024.0);
        double totalSystemMB = totalSystemRAM / (1024.0 * 1024.0 * 1024.0);

        /*
        ************************************************************************
        
        long jvmMem = getTotalJVMMemory();
        long systemMem = getSystemTotalMemory();
        double jvmGB = jvmMem / 1024.0;
        double systemGB = systemMem / (1024.0 * 1024.0 * 1024.0);
        
        String jvmstatus = String.format("JVM: %.2fGB RAM: %.2fGB", jvmGB, systemGB);
        
        
        
        ************************************************************************
        */
        
        String statusText = String.format("JVM %.2f%% (%.2fGB / %.0fGB) RAM %.2f%% (%.2fGB / %.0fGB)", 
                                        jvmPercent, usedMB, maxJVMMB,systemPercent, usedMB, totalSystemMB);
        String tooltipText = String.format(
            "Java Allocated Memory\n" +
            "JVM: %.2f%% (%.0fGB of %.0fGB)\n" +
            "System: %.2f%% (%.0fGB of %.0fGB)",
            jvmPercent, usedMB, maxJVMMB,
            systemPercent, usedMB, totalSystemMB);

        // Determine color based on JVM usage
        Color iconColor;
        if (jvmPercent >= 75) iconColor = RED;
        else if (jvmPercent >= 50) iconColor = AMBER;
        else if (jvmPercent >= 25) iconColor = GREEN;
        else iconColor = BLUE;

        trayIcon.setImage(createTrayImageWithText(iconColor, statusText));
        trayIcon.setToolTip(tooltipText);
        //trayIcon.setImage(createTrayImageWithText(iconColor, jvmstatus));
        //trayIcon.setToolTip(jvmstatus);
    }
    
    
    
    
    
    
    public static long getJvmUsedRamBytes() {
        try {
            // Get the MemoryMXBean, which provides information about the JVM's memory management.
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            // Get the current memory usage of the heap.
            MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();

            // The 'used' field represents the amount of used memory in bytes.
            return heapMemoryUsage.getUsed();
        } catch (Exception e) {
            // In a real-world application, you might want to log this error.
            System.err.println("Error getting JVM used RAM: " + e.getMessage());
            return -1; // Indicate an error
        }
    }
    public static double getJvmUsedRamMB() {
        long usedBytes = getJvmUsedRamBytes();
        if (usedBytes != -1) {
            // Convert bytes to megabytes (1 MB = 1024 * 1024 bytes)
            return (double) usedBytes / (1024.0 * 1024.0);
        }
        return -1.0; // Indicate an error
    }
    
    
    
    
}
