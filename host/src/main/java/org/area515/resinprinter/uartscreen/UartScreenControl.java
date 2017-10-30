package org.area515.resinprinter.uartscreen;

import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.network.WirelessNetwork;
import org.area515.resinprinter.notification.NotificationManager;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.services.MachineService;
import org.area515.resinprinter.services.PrintableService;
import org.area515.resinprinter.services.PrinterService;
import org.area515.util.BasicUtillities;
import org.area515.util.IOUtilities;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

/**
 * Created by zyd on 2017/8/10.
 * uart screen control
 */

public class UartScreenControl
{
    private String version = "0.1.1";

    //private int Page
    private Thread readThread;
    private Thread writeThread;
    private Thread testThread;
    private volatile boolean isRead_stop = false;
    private volatile boolean isWrite_stop = false;
    private volatile boolean isTest_stop = false;

    private Printer printer;
    private BlockingQueue<byte[]> writeQueue;
    private int cur_file_selected = -1;
    private int cur_file_page = 0;
    private String cur_file_dir = null;

    List<WirelessNetwork> network_list = null;
    private int cur_network_selected = -1;
    private int cur_network_page = 0;
    private String network_ssid;
    private String network_psk = "";

    private int numberOfFirstLayers;
    private int firstLayerTime;
    private int layerTime;
    private double liftDistance;
    private double liftFeedSpeed;
    private double liftRetractSpeed;
    private int delayTimeBeforeSolidify;
    private int delayTimeAfterSolidify;
    private int delayTimeAsLiftedTop;
    private int delayTimeForAirPump;
    private boolean parameterEnabled;
    private boolean detectionEnabled;

    private String update_path = "/udiskdir/update-dlp";
//    private String update_path = "C:\\Users\\zyd\\udiskdir\\update-dlp";
    private Timer shutterTimer;


    /*****************machine status******************/
    private JobStatus machine_status = null;
    private String printFileName = "";
    private long printFileSize = 0;
    private double printProgress = 0;
    private int printCurrentLayer = 0;
    private int printTotalLayers = 0;
    private long printedTime = 0;
    /***********************************/

    /*****************uart screen address******************/
    private int pageLoading = 0;
    private int pageUpdating = 1;
    private int pageUpdated = 2;
    private int pageMain = 7;
    private int pageLocalFile = 8;
    private int pageUdiskFile = 9;
    private int pageSettings = 10;
    private int pageAbout = 11;
    private int pageNetworks = 12;
    private int pageNetworkEdit = 13;
    private int pageAdminSetting = 17;

    private char addr_btn_fileList = 0x0002;
    private char addr_btn_fileSel = 0x0003;
    private char addr_btn_filePaging = 0x0004;
    private char addr_btn_copy_or_delete = 0x0005;
    private char addr_btn_print_ctrl = 0x0006;
    private char addr_btn_network = 0x0007;
    private char addr_btn_material = 0x0008;
    private char addr_btn_control = 0x0009;
    private char addr_btn_parameters = 0x000A;
    private char addr_btn_about = 0x000D;
    private char addr_btn_clear_lifeTime = 0x000E;

    private char[] addr_icon_prog = {0x0100, 0x0101, 0x0102, 0x0103, 0x0104};
    private char addr_icon_pause = 0x0110;
    private char addr_icon_parameter_enabled = 0x0120;
    private char addr_icon_detection_enabled = 0x0121;

    private char addr_txt_ipAddress = 0x1000;
    private char addr_txt_machineStatus = 0x1010;
    private char addr_txt_printFileName = 0x1020;
    private char addr_txt_printFileSize = 0x1040;
    private char addr_txt_printProgress = 0x1050;
    private char addr_txt_layers = 0x1060;
    private char addr_txt_printedTime = 0x1070;
    private char[] addr_txt_fileList = {0x1100, 0x1120, 0x1140, 0x1160, 0x1180};
    private char addr_txt_filePage = 0x11A0;
    private char addr_txt_version = 0x1200;
    private char addr_txt_lifetime_led = 0x1210;
    private char addr_txt_lifetime_screen = 0x1220;
    private char[] addr_txt_network_List = {0x1300, 0x1320, 0x1340, 0x1360, 0x1380};
    private char addr_txt_networkSsid = 0x13A0;
    private char addr_txt_networkPsk = 0x13C0;
    private char addr_txt_material_weight = 0x1400;
    private char addr_txt_led_temperature = 0x1410;
    private char addr_txt_admin_password = 0x1500;
    private char[] addr_txt_parameters = {0x1600, 0x1610, 0x1620, 0x1630, 0x1640, 0x1650, 0x1660, 0x1670, 0x1680, 0x1690};

    private char[] desc_txt_fileList = {0x4003, 0x4023, 0x4043, 0x4063, 0x4083};
    private char[] desc_txt_network_list = {0x4103, 0x4123, 0x4143, 0x4163, 0x4183};
    /***********************************/

    public UartScreenControl(Printer printer)
    {
        this.printer = printer;
        writeQueue = new ArrayBlockingQueue<byte[]>(32);
    }

    private void startReadThread()
    {
        readThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                byte[] receive;
                char cmd;

                while (!isRead_stop)
                {
                    try
                    {
                        receive = IOUtilities.read(getPrinter().getUartScreenSerialPort(), 2000, 10);
                        if (receive == null || receive.length < 9)
                            continue;
                        printBytes(receive);

                        cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(receive, 4, 2));
                        if (cmd == addr_btn_fileList)
                            action_file_list(receive);
                        else if (cmd == addr_btn_fileSel)
                            action_file_select(receive);
                        else if (cmd == addr_btn_filePaging)
                            action_file_paging(receive);
                        else if (cmd == addr_btn_copy_or_delete)
                            action_file_copy_or_delete(receive);
                        else if (cmd == addr_btn_print_ctrl)
                            action_print_ctrl(receive);
                        else if (cmd == addr_btn_network)
                            action_network(receive);
                        else if (cmd == addr_btn_material)
                            action_material(receive);
                        else if (cmd == addr_btn_parameters)
                            action_parameters(receive);
                        else if (cmd == addr_btn_control)
                            action_control(receive);
                        else if (cmd == addr_btn_about)
                            action_about(receive);
                        else if (cmd == addr_btn_clear_lifeTime)
                            action_clear_lifeTime(receive);
                        else if (cmd == addr_txt_networkPsk)
                            action_set_network_psk(receive);
                        else if (cmd == addr_txt_admin_password)
                            action_set_admin_password(receive);
                        else if (cmd >= addr_txt_parameters[0] && cmd <= addr_txt_parameters[addr_txt_parameters.length - 1])
                            action_parameters_set(receive);

                    } catch (InterruptedException | IOException e)
                    {
                        System.out.println(e.toString());
                    }
                }
                System.out.println("read thread stop");
            }
        });
        readThread.start();
    }

    private void startWriteThread()
    {
        writeThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                byte[] bytes;
                while (!isWrite_stop)
                {
                    try
                    {
                        bytes = writeQueue.poll(2000, TimeUnit.MILLISECONDS);
                        //bytes = writeQueue.take();
                        if (bytes == null || bytes.length <= 0)
                            continue;
                        getPrinter().getUartScreenSerialPort().write(bytes);
                    } catch (InterruptedException | IOException e)
                    {
                        System.out.println(e.toString());
                    }
                }
                System.out.println("write thread stop");
            }
        });
        writeThread.start();
    }

    private void startTestThread()
    {
        testThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                String response;
//                Main.GLOBAL_EXECUTOR.submit(new Runnable()
//                {
//                    @Override
//                    public void run()
//                    {
//                        System.out.println("executor");
//                    }
//                });
                while (!isTest_stop)
                {
                    try
                    {
                        //response = getPrinter().getGCodeControl().sendGcode("M99");
                        //System.out.println("response 0: "+response);
                        //response = getPrinter().getGCodeControl().sendGcode("M98");
                        //System.out.println("response 1: "+response);
                        //NotificationManager.sendMessage("test");
                        Thread.sleep(1000);
                    } catch (InterruptedException e)
                    {
                        System.out.println(e.toString());
                    }
                }
            }
        });
        testThread.start();
    }

    public void start()
    {
        startReadThread();
        startWriteThread();
        startTestThread();

        Main.GLOBAL_EXECUTOR.submit(new Runnable()
        {
            @Override
            public void run()
            {
                if (check_updatable() && HostProperties.Instance().isEnableUpdate())
                {
                    close();
                    start_update();
                }
                else
                {
                    goPage(pageMain);
                    getIpAddress();
                    setVersion(version);
                }
                return;
            }
        });
    }

    public void close()
    {
        isRead_stop = true;
        isWrite_stop = true;
        isTest_stop = true;
        try
        {
            readThread.join();
            writeThread.join();
        } catch (InterruptedException e)
        {
            System.out.println(e.toString());
        }
    }

    public Printer getPrinter()
    {
        return this.printer;
    }

    private void writeText(char address, byte[] content)
    {
        byte[] bytes;
        int len = content.length + 3;
        bytes = BasicUtillities.bytesCat(new byte[]{0x5A, (byte) 0xA5, (byte) len, (byte) 0x82}, BasicUtillities.charToBytes(address));
        bytes = BasicUtillities.bytesCat(bytes, content);

        try
        {
            writeQueue.put(bytes);
        } catch (InterruptedException e)
        {
            System.out.println(e.toString());
        }
    }

    private void writeKey(byte key)
    {
        byte[] bytes = {0x5A, (byte)0xA5, 0x03, (byte)0x80, 0x4F, key};

        try
        {
            writeQueue.put(bytes);
        } catch (InterruptedException e)
        {
            System.out.println(e.toString());
        }
    }

    private void goPage(int page)
    {
        byte[] bytes;
        bytes = new byte[]{0x5A, (byte) 0xA5, 0x04, (byte) 0x80, 0x03, 0x00, (byte) page};

        try
        {
            writeQueue.put(bytes);
        } catch (InterruptedException e)
        {
            System.out.println(e.toString());
        }
    }

    private void printBytes(byte[] bytes)
    {
        String str = "";
        for (byte b : bytes)
        {
            str += String.format("0x%02x,", b);
        }
        System.out.println(str);
    }

    private List<String> getPrintableList(String whichDir)
    {
        return PrintableService.INSTANCE.getPrintableFiles(whichDir);
    }

    private void filesUpdate(String whichDir, int selected)
    {
        List<String> files = getPrintableList(whichDir);
        String file;

        if (selected < 0)
            selected = 0;

        if (files.size() == 0)
            cur_file_selected = -1;
        else if (selected >= files.size() - 1)
            cur_file_selected = files.size() - 1;
        else
            cur_file_selected = selected;

        if (cur_file_selected < 0)
            cur_file_page = 0;
        else
            cur_file_page = cur_file_selected / 5;

        cur_file_dir = whichDir;

        for (int i = 0; i < 5; i++)
        {
            file = "";
            if (files.size() > i + cur_file_page * 5)
            {
                file = files.get(i + cur_file_page * 5);
            }
            try
            {
                writeText(addr_txt_fileList[i], String.format("%-32s", file).getBytes("GBK"));
            } catch (UnsupportedEncodingException e)
            {
                System.out.println(e.toString());
            }
        }
        clearProgBar();
        showFilePageNumber();

        fileHighLight(cur_file_selected);
    }

    private void fileHighLight(int selected)
    {
        if (selected < 0)
            return;

        selected = selected % 5;

        for (int i = 0; i < 5; i++)
        {
            if (selected == i)
                writeText(desc_txt_fileList[i], new byte[] {(byte)0xF8, 0x00}); //the second param is text's color
            else
                writeText(desc_txt_fileList[i], new byte[] {(byte)0xFF, (byte)0xFF});
        }
    }

    private void clearProgBar()
    {
        for (int i = 0; i < 5; i++)
        {
            writeText(addr_icon_prog[i], new byte[] {0x00, (byte)65});
        }
    }

    private void showFilePageNumber()
    {
        writeText(addr_txt_filePage, new byte[] { (byte) ((cur_file_page >> 8) & 0xFF), (byte) (cur_file_page & 0xFF)});
    }

    private String fileCopy()
    {
        String filename = null;

        List<String> files = getPrintableList("udisk");

        if (files.size() > 0 && cur_file_selected <= files.size() - 1 && cur_file_selected >= 0)
        {
            filename = files.get(cur_file_selected);
            uploadFromUdiskToLocal(filename);
        }
        return filename;
    }

    private void fileDelete()
    {
        List<String> files = getPrintableList("local");

        if (files.size() > 0 && cur_file_selected <= files.size() - 1 && cur_file_selected >= 0)
        {
            deleteLocalFile(files.get(cur_file_selected));
            filesUpdate("local", cur_file_selected);
        }
    }

    private void uploadFromUdiskToLocal(String fileName)
    {
        PrintableService.INSTANCE.uploadViaUdisk(fileName, new ProgressCallback()
        {
            @Override
            public void onProgress(double progress)
            {
                writeText(addr_icon_prog[cur_file_selected % 5], new byte[] {0x00, (byte)(39 + progress / 4)});
                System.out.println(progress);
            }
        });
    }

    private void deleteLocalFile(String fileName)
    {
        PrintableService.INSTANCE.deleteFile(fileName);
    }

    private void jobPrint(String fileName)
    {
        Printer printer = getPrinter();
        if (printer.isStarted() && !printer.isPrintInProgress())
        {
            PrinterService.INSTANCE.print(fileName, printer.getName());
        }
    }

    private JobStatus jobPause()
    {
        Printer printer = getPrinter();
        return printer.togglePause();
    }

    private void jobStop()
    {
        Printer printer = getPrinter();
        if (printer.isPrintActive())
        {
            printer.setStatus(JobStatus.Cancelling);
            setMachineStatus(printer.getStatus(), false, false);
        }
    }

    private void printJob()
    {
        String filename = null;
        if (this.cur_file_dir.equals("udisk"))
        {
            filename = fileCopy();
        }
        else
        {
            List<String> files = getPrintableList("local");
            if (files.size() > 0 && cur_file_selected <= files.size() - 1 && cur_file_selected >= 0)
            {
                filename = files.get(cur_file_selected);
            }
        }

        if (filename != null)
        {
            jobPrint(filename);
            goPage(pageMain);
        }
    }

    private void pauseJob()
    {
        jobPause();
    }

    private void stopJob()
    {
        jobStop();
    }

    private List<WirelessNetwork> getNetworks()
    {
        return MachineService.INSTANCE.getWirelessNetworks();
    }

    private void networksUpdate()
    {
        network_list = getNetworks();
        networkSelect(0);
    }

    private void networkSelect(int selected)
    {
        String network;

        if (selected < 0)
            selected = 0;

        if (network_list == null || network_list.size() == 0)
            cur_network_selected = -1;
        else if (selected >= network_list.size() - 1)
            cur_network_selected = network_list.size() - 1;
        else
            cur_network_selected = selected;

        if (cur_network_selected < 0)
            cur_network_page = 0;
        else
            cur_network_page = cur_network_selected / 5;

        for (int i = 0; i < 5; i++)
        {
            network = "";
            if (network_list != null && network_list.size() > i + cur_network_page * 5)
            {
                network = network_list.get(i + cur_network_page * 5).getSsid();
            }
            try
            {
                writeText(addr_txt_network_List[i], String.format("%-32s", network).getBytes("GBK"));
            } catch (UnsupportedEncodingException e)
            {
                System.out.println(e.toString());
            }
        }

        networkHighLight(cur_network_selected);
        if (cur_network_selected < 0)
            setNetworkSsid("");
        else
            setNetworkSsid(network_list.get(cur_network_selected).getSsid());
    }

    private void networkHighLight(int selected)
    {
        if (selected < 0)
            return;

        selected = selected % 5;

        for (int i = 0; i < 5; i++)
        {
            if (selected == i)
                writeText(desc_txt_network_list[i], new byte[] {(byte)0xF8, 0x00}); //the second param is text's color
            else
                writeText(desc_txt_network_list[i], new byte[] {(byte)0xFF, (byte)0xFF});
        }
    }

    private void connectNetwork(String ssid, String psk)
    {
        for (WirelessNetwork network : getNetworks())
        {
            if (network.getSsid().equals(ssid))
            {
                network.setPassword(psk);
                if (MachineService.INSTANCE.connectToWifiSSID(network))
                {
                    writeKey((byte)0xF1);
                    Main.GLOBAL_EXECUTOR.submit(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                int count = 10;
                                while (count-- > 0)
                                {
                                    getIpAddress();
                                    Thread.sleep(3000);
                                }
                            } catch (InterruptedException e)
                            {
                                System.out.println(e.toString());
                            }
                        }
                    });
                }
                else
                {
                    writeKey((byte)0xF2);
                }
                return;
            }
        }
    }

    private String getIpAddress()
    {
        String ipAddress = MachineService.INSTANCE.getLocalIpAddress("wlan0");
        if (ipAddress != null)
        {
            System.out.println("ip: " + ipAddress);
            writeText(addr_txt_ipAddress, String.format("%-16s", ipAddress).getBytes());
        }
        else
        {
            writeText(addr_txt_ipAddress, String.format("%-16s", "").getBytes());
        }
        return ipAddress;
    }

    private String getNetworkSsid()
    {
        return this.network_ssid;
    }

    private void setNetworkSsid(String ssid)
    {
        this.network_ssid = ssid;
        writeText(addr_txt_networkSsid, String.format("%-32s", ssid).getBytes());
    }

    private String getNetworkPsk()
    {
        return this.network_psk;
    }

    private void setNetworkPsk(String psk)
    {
        this.network_psk = psk;
    }

    private void readParameters()
    {
        numberOfFirstLayers = getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getNumberOfFirstLayers();
        firstLayerTime = getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getFirstLayerExposureTime();
        layerTime = getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getExposureTime();
        liftDistance = getPrinter().getConfiguration().getSlicingProfile().getLiftDistance();
        liftFeedSpeed = getPrinter().getConfiguration().getSlicingProfile().getLiftFeedSpeed();
        liftRetractSpeed = getPrinter().getConfiguration().getSlicingProfile().getLiftRetractSpeed();
        delayTimeBeforeSolidify = getPrinter().getConfiguration().getSlicingProfile().getDelayTimeBeforeSolidify();
        delayTimeAfterSolidify = getPrinter().getConfiguration().getSlicingProfile().getDelayTimeAfterSolidify();
        delayTimeAsLiftedTop = getPrinter().getConfiguration().getSlicingProfile().getDelayTimeAsLiftedTop();
        delayTimeForAirPump = getPrinter().getConfiguration().getSlicingProfile().getDelayTimeForAirPump();
        parameterEnabled = getPrinter().getConfiguration().getSlicingProfile().getParameterEnabled();
        detectionEnabled = getPrinter().getConfiguration().getSlicingProfile().getDetectionEnabled();

        writeText(addr_txt_parameters[0], String.format("%d", numberOfFirstLayers).getBytes());
        writeText(addr_txt_parameters[1], String.format("%d", firstLayerTime).getBytes());
        writeText(addr_txt_parameters[2], String.format("%d", layerTime).getBytes());
        writeText(addr_txt_parameters[3], String.format("%.2f", liftDistance).getBytes());
        writeText(addr_txt_parameters[4], String.format("%.2f", liftFeedSpeed).getBytes());
        writeText(addr_txt_parameters[5], String.format("%.2f", liftRetractSpeed).getBytes());
        writeText(addr_txt_parameters[6], String.format("%d", delayTimeBeforeSolidify).getBytes());
        writeText(addr_txt_parameters[7], String.format("%d", delayTimeAfterSolidify).getBytes());
        writeText(addr_txt_parameters[8], String.format("%d", delayTimeAsLiftedTop).getBytes());
        writeText(addr_txt_parameters[9], String.format("%d", delayTimeForAirPump).getBytes());
        if (parameterEnabled)
            writeText(addr_icon_parameter_enabled, new byte[] {0x00, 67});
        else
            writeText(addr_icon_parameter_enabled, new byte[] {0x00, 66});
        if (detectionEnabled)
            writeText(addr_icon_detection_enabled, new byte[] {0x00, 67});
        else
            writeText(addr_icon_detection_enabled, new byte[] {0x00, 66});
    }

    private void saveParameters()
    {
        getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().setNumberOfFirstLayers(numberOfFirstLayers);
        getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().setFirstLayerExposureTime(firstLayerTime);
        getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().setExposureTime(layerTime);
        getPrinter().getConfiguration().getSlicingProfile().setLiftDistance(liftDistance);
        getPrinter().getConfiguration().getSlicingProfile().setLiftFeedSpeed(liftFeedSpeed);
        getPrinter().getConfiguration().getSlicingProfile().setLiftRetractSpeed(liftRetractSpeed);
        getPrinter().getConfiguration().getSlicingProfile().setDelayTimeBeforeSolidify(delayTimeBeforeSolidify);
        getPrinter().getConfiguration().getSlicingProfile().setDelayTimeAfterSolidify(delayTimeAfterSolidify);
        getPrinter().getConfiguration().getSlicingProfile().setDelayTimeAsLiftedTop(delayTimeAsLiftedTop);
        getPrinter().getConfiguration().getSlicingProfile().setDelayTimeForAirPump(delayTimeForAirPump);
        getPrinter().getConfiguration().getSlicingProfile().setParameterEnabled(parameterEnabled);
        getPrinter().getConfiguration().getSlicingProfile().setDetectionEnabled(detectionEnabled);
        PrinterService.INSTANCE.savePrinter(getPrinter());
    }

    private void setVersion(String version)
    {
        writeText(addr_txt_version, String.format("%-10s", version).getBytes());
    }

    private void setLiftTime()
    {
        String string;

        long ledUsedTime = getPrinter().getLedUsedTime();
        string = String.format("%.1f/%d", ledUsedTime/(60*60*1000.0), 1000);
        writeText(addr_txt_lifetime_led, String.format("%-10s", string).getBytes());

        long screenUsedTime = getPrinter().getScreenUsedTime();
        string = String.format("%.1f/%d", screenUsedTime/(60*60*1000.0), 2000);
        writeText(addr_txt_lifetime_screen, String.format("%-10s", string).getBytes());
    }

    private void loadAdminAccount(String password)
    {
        writeText(addr_txt_admin_password, String.format("%-32s", "").getBytes());
        if (password.equals("123456"))
        {
            goPage(pageAdminSetting);
            setLiftTime();
        }
        else
        {
            writeKey((byte)0xF1);
        }
    }

    private void showImage(String filePath)
    {
        try
        {
            if (filePath != null && BasicUtillities.isExists(filePath))
            {
                IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "sudo xset s off"}, null);
                IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "sudo xset -dpms"}, null);
                IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "sudo xset s noblank"}, null);

                File imageFile = new File(filePath);
                BufferedImage image = ImageIO.read(imageFile);
                getPrinter().showImage(image);
            }
            else
            {
                getPrinter().showBlankImage();
            }
        } catch (IOException e)
        {
            System.out.print(e.toString());
        }
    }

    /****************************notify uartscreen state -start*************************************/
    public void notifyState(Printer printer, PrintJob job)
    {
        setMachineStatus(printer.getStatus(), false, false);

        if (job != null)
        {
            setPrintFileName(job.getJobName(), false, false);
            setPrintFileSize(job.getJobFileSize(), false, false);
            setPrintProgress(job.getJobProgress(), true, false);
            setPrintLayers(job.getCurrentSlice(), job.getTotalSlices(), true, false);
            setPrintedTime(job.getElapsedTime(), true, false);
        }
        else
        {
            setPrintFileName("", false, true);
            setPrintFileSize(0, false, true);
            setPrintProgress(0, false, true);
            setPrintLayers(0, 0, false, true);
            setPrintedTime(0, false, true);
        }
    }

    private void setMachineStatus(JobStatus status, boolean force, boolean hide)
    {
        if (this.machine_status != status)
        {
            this.machine_status = status;
            force = true;
        }

        if (hide)
        {
            writeText(addr_txt_machineStatus, String.format("%-12s", "").getBytes());
        }
        else if (force)
        {
            String string = "";
            try
            {
                if (status == JobStatus.Ready)
                    string = new String(new char[] {0x5C31, 0x7EEA});//就绪
                else if (status == JobStatus.Printing)
                    string = new String(new char[] {0x6253, 0x5370, 0x4E2D});//打印中
                else if (status == JobStatus.Failed)
                    string = new String(new char[] {0x5931, 0x8D25});//失败
                else if (status == JobStatus.Completed)
                    string = new String(new char[] {0x5B8C, 0x6210});//完成
                else if (status == JobStatus.Cancelled)
                    string = new String(new char[] {0x5DF2, 0x53D6, 0x6D88});//已取消
                else if (status == JobStatus.Cancelling)
                    string = new String(new char[] {0x6B63, 0x5728, 0x53D6, 0x6D88});//正在取消
                else if (status == JobStatus.Deleted)
                    string = new String(new char[] {0x5DF2, 0x5220, 0x9664});//已删除
                else if (status == JobStatus.Paused)
                    string = new String(new char[] {0x5DF2, 0x6682, 0x505C});//已暂停
                else if (status == JobStatus.PausedUnconformableMaterial)
                    string = new String(new char[] {0x6811, 0x8102, 0x7C7B, 0x578B, 0x4E0D, 0x7B26});//树脂类型不符
                else if (status == JobStatus.PausedDoorOpened)
                    string = new String(new char[] {0x8231, 0x95E8, 0x6253, 0x5F00});//舱门打开
                else if (status == JobStatus.PausedLedOverTemperature)
                    string = new String(new char[] {0x706F, 0x677F, 0x6E29, 0x5EA6, 0x8FC7, 0x9AD8});//灯板温度过高
                else if (status == JobStatus.PausedGrooveOutOfMaterial)
                    string = new String(new char[] {0x6599, 0x69FD, 0x7F3A, 0x6599});//料槽缺料
                else if (status == JobStatus.PausedBottleOutOfMaterial)
                    string = new String(new char[] {0x6599, 0x74F6, 0x7F3A, 0x6599});//料瓶缺料

                writeText(addr_txt_machineStatus, String.format("%-16s", string).getBytes("GBK"));
                if (status == JobStatus.Printing)
                    writeText(addr_icon_pause, new byte[] {0x00, 39});
                else
                    writeText(addr_icon_pause, new byte[] {0x00, 38});
            } catch (UnsupportedEncodingException e)
            {
                System.out.println(e.toString());
            }
        }
    }

    private void setPrintFileName(String fileName, boolean force, boolean hide)
    {
        if (!this.printFileName.equals(fileName))
        {
            this.printFileName = fileName;
            force = true;
        }

        if (hide)
        {
            writeText(addr_txt_printFileName, String.format("%-32s", "").getBytes());
        }
        else if (force)
        {
            try
            {
                writeText(addr_txt_printFileName, String.format("%-32s", this.printFileName).getBytes("GBK"));
            } catch (UnsupportedEncodingException e)
            {
                System.out.println(e.toString());
            }
        }
    }

    private void setPrintFileSize(long fileSize, boolean force, boolean hide)
    {
        if (this.printFileSize != fileSize)
        {
            this.printFileSize = fileSize;
            force = true;
        }

        if (hide)
        {
            writeText(addr_txt_printFileSize, String.format("%-10s", "").getBytes());
        }
        else if (force)
        {
            String string;
            if (this.printFileSize < 1024)
                string = String.format("%dB", this.printFileSize);
            else if (this.printFileSize < 1024*1024)
                string = String.format("%.1fK", this.printFileSize / 1024.0);
            else
                string = String.format("%.1fM", this.printFileSize / (1024 * 1024.0));
            writeText(addr_txt_printFileSize, String.format("%-10s", string).getBytes());
        }
    }

    private void setPrintProgress(double printProgress, boolean force, boolean hide)
    {
        if (this.printProgress != printProgress)
        {
            this.printProgress = printProgress;
            force = true;
        }

        if (hide)
        {
            writeText(addr_txt_printProgress, String.format("%-10s", "").getBytes());
        }
        else if (force)
        {
            String string = String.format("%.1f%%", printProgress);
            writeText(addr_txt_printProgress, String.format("%-10s", string).getBytes());
        }
    }

    private void setPrintLayers(int current, int total, boolean force, boolean hide)
    {
        if (this.printCurrentLayer != current)
        {
            this.printCurrentLayer = current;
            force = true;
        }
        if (this.printTotalLayers != total)
        {
            this.printTotalLayers = total;
            force = true;
        }

        if (hide)
        {
            writeText(addr_txt_layers, String.format("%-10s", "").getBytes());
        }
        else if (force)
        {
            String string = String.format("%d/%d", this.printCurrentLayer, this.printTotalLayers);
            writeText(addr_txt_layers, String.format("%-10s", string).getBytes());
        }
    }

    private void setPrintedTime(long printedTime, boolean force, boolean hide)
    {
        if (this.printedTime != printedTime)
        {
            this.printedTime = printedTime;
            force = true;
        }

        if (hide)
        {
            writeText(addr_txt_printedTime, String.format("%-10s", "").getBytes());
        }
        else if (force)
        {
            String string = String.format("%d:%02d:%02d",
                    this.printedTime / 3600000,
                    (this.printedTime % 3600000) / 60000,
                    (this.printedTime % 60000) / 1000);
            writeText(addr_txt_printedTime, String.format("%-10s", string).getBytes());
        }
    }
    /****************************notify uartscreen state -end*************************************/

    /***************************action function -start**************************************/
    private void action_file_list(byte[] payload)
    {
        if (payload.length < 9)
            return;
        int key_value = payload[8];

        if (key_value == 0x00)
            filesUpdate("local", 0);
        else if (key_value == 0x01)
            filesUpdate("udisk", 0);
    }

    private void action_file_select(byte[] payload)
    {
        int index;
        index = payload[8];

        filesUpdate(cur_file_dir, cur_file_page * 5 + index);
    }

    private void action_file_paging(byte[] payload)
    {
        if (payload.length < 9)
            return;
        int key_value = payload[8];

        int increment = 0;

        if (key_value == 0x00)
            increment = -5;
        else if (key_value == 0x01)
            increment = -1;
        else if (key_value == 0x02)
            increment = 1;
        else if (key_value == 0x03)
            increment = 5;

        filesUpdate(cur_file_dir, cur_file_selected + increment);
    }

    private void action_file_copy_or_delete(byte[] payload)
    {
        if (getPrinter().getStatus().isPrintInProgress())
            return;

        if (payload.length < 9)
            return;
        int key_value = payload[8];

        if (key_value == 0x00)
            fileCopy();
        else if (key_value == 0x01)
            fileDelete();
    }

    private void action_print_ctrl(byte[] payload)
    {
        if (payload.length < 9)
            return;
        int key_value = payload[8];

        if (key_value == 0x00 && !getPrinter().getStatus().isPrintInProgress())
            printJob();
        else if (key_value == 0x01)
            pauseJob();
        else if (key_value == 0x02)
            stopJob();
    }

    private void action_network(byte[] payload)
    {
        int key_value;
        key_value = payload[8];

        if (key_value >= 0x00 && key_value <= 0x04)
            networkSelect(cur_network_page * 5 + key_value);
        else if (key_value == 0x05)
            networkSelect(cur_network_selected - 5);
        else if (key_value == 0x06)
            networkSelect(cur_network_selected + 5);
        else if (key_value == 0x07)
            networksUpdate();
        else if (key_value == 0x08)
        {
            if (cur_network_selected >= 0)
            {
                goPage(pageNetworkEdit);
            }
        }
        else if (key_value == 0x09)
        {
            String psk = getNetworkPsk();
            if (psk.length() >= 8)
                connectNetwork(getNetworkSsid(), getNetworkPsk());
        }
    }

    private void action_set_network_psk(byte[] payload)
    {
        String psk = new String(BasicUtillities.subBytes(payload, 7));
        setNetworkPsk(psk.replaceAll("[^\\x20-\\x7E]", ""));
    }

    private void action_parameters(byte[] payload)
    {
        int key_value;
        key_value = payload[8];

        if (key_value == 0x00)
        {
            readParameters();
        }
        else if (key_value == 0x01)
        {
            if (getPrinter().getStatus().isPrintInProgress())
            {
                writeKey((byte)0xF2);
                return;
            }
            saveParameters();
            writeKey((byte)0xF1);
        }
        else if (key_value == 0x02)
        {
            parameterEnabled = !parameterEnabled;
            if (parameterEnabled)
                writeText(addr_icon_parameter_enabled, new byte[] {0x00, 67});
            else
                writeText(addr_icon_parameter_enabled, new byte[] {0x00, 66});
        }
        else if (key_value == 0x03)
        {
            detectionEnabled = !detectionEnabled;
            if (detectionEnabled)
                writeText(addr_icon_detection_enabled, new byte[] {0x00, 67});
            else
                writeText(addr_icon_detection_enabled, new byte[] {0x00, 66});
        }
    }

    private void action_parameters_set(byte[] payload)
    {
        if (getPrinter().getStatus().isPrintInProgress())
            return;

        String str;
        char cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 4, 2));

        str = new String(BasicUtillities.subBytes(payload, 7));
        str = str.replaceAll("[^\\x20-\\x7E]", "");
        if (str.length() == 0)
            str = "0";

        if (cmd == addr_txt_parameters[0])
            numberOfFirstLayers = new Integer(str);
        else if (cmd == addr_txt_parameters[1])
            firstLayerTime = new Integer(str);
        else if (cmd == addr_txt_parameters[2])
            layerTime = new Integer(str);
        else if (cmd == addr_txt_parameters[3])
            liftDistance = new Double(str);
        else if (cmd == addr_txt_parameters[4])
            liftFeedSpeed = new Double(str);
        else if (cmd == addr_txt_parameters[5])
            liftRetractSpeed = new Double(str);
        else if (cmd == addr_txt_parameters[6])
            delayTimeBeforeSolidify = new Integer(str);
        else if (cmd == addr_txt_parameters[7])
            delayTimeAfterSolidify = new Integer(str);
        else if (cmd == addr_txt_parameters[8])
            delayTimeAsLiftedTop = new Integer(str);
        else if (cmd == addr_txt_parameters[9])
            delayTimeForAirPump = new Integer(str);
    }

    private void action_material(byte[] payload)
    {
        if (getPrinter().getStatus().isPrintInProgress())
            return;

        int key_value;
        key_value = payload[8];

        if (key_value == 0x01)
        {
            getPrinter().getGCodeControl().executeNetWeight();
        }
        //读取重量
        double materialWeight = 0;
        String receive = getPrinter().getGCodeControl().executeMaterialWeight();
        Pattern GCODE_Weight_PATTERN = Pattern.compile("\\s*Weight:\\s*(-?[\\d\\.]+).*");
        Matcher matcher = GCODE_Weight_PATTERN.matcher(receive);
        if (matcher.find())
        {
            materialWeight = Double.parseDouble(matcher.group(1));
        }
        writeText(addr_txt_material_weight, String.format("%-16s", String.format("%.2f", materialWeight)).getBytes());
    }

    private void action_control(byte[] payload)
    {
        if (getPrinter().getStatus().isPrintInProgress())
            return;

        int key_value;
        key_value = payload[8];

        if (key_value == 0x00) //进入控制页
        {
            double temperature = 0;
            String receive = getPrinter().getGCodeControl().executeQueryTemperature();
            Pattern GCODE_Temperature_PATTERN = Pattern.compile("\\s*T:\\s*(-?[\\d\\.]+).*B:(-?[\\d\\.]+).*");
            Matcher matcher = GCODE_Temperature_PATTERN.matcher(receive);
            if (matcher.find())
            {
                temperature = Double.parseDouble(matcher.group(2));
            }
            writeText(addr_txt_led_temperature, String.format("%-16s", String.format("%.1f", temperature)).getBytes());
        }
        else if (key_value == 0x01) //Z轴上移
        {
            getPrinter().getGCodeControl().executeSetRelativePositioning();
            getPrinter().getGCodeControl().sendGcode("G1 Z1 F1000");
            getPrinter().getGCodeControl().executeSetAbsolutePositioning();
        }
        else if (key_value == 0x02) //Z轴下移
        {
            getPrinter().getGCodeControl().executeSetRelativePositioning();
            getPrinter().getGCodeControl().sendGcode("G1 Z-1 F1000");
            getPrinter().getGCodeControl().executeSetAbsolutePositioning();
        }
        else if (key_value == 0x03) //Z轴归零
        {
            getPrinter().getGCodeControl().executeZHome();
        }
        else if (key_value == 0x04) //Z轴下移到底部
        {
            getPrinter().getGCodeControl().executeSetAbsolutePositioning();
            getPrinter().getGCodeControl().sendGcode("G1 Z0 F1000");
        }
        else if (key_value == 0x05) //图像输出
        {
            showImage("/opt/cwh/3DTALK.png");
        }
        else if (key_value == 0x06) //全屏白色
        {
            showImage("/opt/cwh/WHITE.png");
        }
        else if (key_value == 0x07) //图像关闭
        {
            showImage(null);
        }
        else if (key_value == 0x08)
        {
            getPrinter().getGCodeControl().executeShutterOn();
            if (shutterTimer != null)
            {
                shutterTimer.cancel();
                shutterTimer = null;
            }
            shutterTimer = new Timer();
            shutterTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    getPrinter().getGCodeControl().executeShutterOff();
                }
            }, 40000);
        }
        else if (key_value == 0x09)
        {
            if (shutterTimer != null)
            {
                shutterTimer.cancel();
                shutterTimer = null;
            }
            getPrinter().getGCodeControl().executeShutterOff();
        }
        else if (key_value == 0x0A)
        {
            getPrinter().getGCodeControl().executeWaterPumpOn();
        }
        else if (key_value == 0x0B)
        {
            getPrinter().getGCodeControl().executeWaterPumpOff();
        }
    }

    public void action_about(byte[] payload)
    {
        setLiftTime();
    }

    private void action_set_admin_password(byte[] payload)
    {
        String password = new String(BasicUtillities.subBytes(payload, 7));
        loadAdminAccount(password.replaceAll("[^\\x20-\\x7E]", ""));
    }

    private void action_clear_lifeTime(byte[] payload)
    {
        int key_value;
        key_value = payload[8];

        if (key_value == 0)
        {
            getPrinter().setLedUsedTime(0);
        }
        else if (key_value == 1)
        {
            getPrinter().setScreenUsedTime(0);
        }
        setLiftTime();
    }
    /***************************action function end**************************************/

    private void start_update()
    {
    try
    {
        System.out.println("update started");
        getPrinter().getUartScreenSerialPort().write(new byte[]{0x5A,(byte)0xA5,0x04,(byte)0x80,0x03,0x00,(byte)pageUpdating});
        Thread.sleep(100);
        update_dgus();
        update_filesystem();
        getPrinter().getUartScreenSerialPort().write(new byte[]{0x5A,(byte)0xA5,0x04,(byte)0x80,0x03,0x00,(byte)pageUpdated});
        System.out.println("update completed");
        while (BasicUtillities.isExists(update_path))
        {
            Thread.sleep(1000);
        }
        getPrinter().getUartScreenSerialPort().write(new byte[]{0x5A,(byte)0xA5,0x04,(byte)0x80,0x03,0x00,(byte)pageLoading});
        Thread.sleep(100);
        IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "sudo /etc/init.d/cwhservice restart"}, null);
    } catch (IOException | InterruptedException e)
    {
        System.out.println(e.toString());
    }
}

    private boolean check_updatable()
    {
        String dgus_path = update_path + "/DWIN_SET";
        String filesystem_path = update_path + "/filesystem";
        String version_path = update_path + "/version";

        try
        {
            if (!BasicUtillities.isExists(dgus_path) ||
                    !BasicUtillities.isExists(filesystem_path) ||
                    !BasicUtillities.isExists(version_path))
                return false;

            String version_string = BasicUtillities.readAll(version_path);
            String old_version = version.replace(".", "");
            String new_version = version_string.replace(".", "");
            if (Integer.parseInt(old_version) >= Integer.parseInt(new_version))
                return false;
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
            return false;
        }

        return true;
    }

    private void update_filesystem()
    {
        String updateScript = update_path + "/update.sh";
        if (BasicUtillities.isExists(updateScript))
        {
            IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "sudo " + updateScript}, null);
        }
    }

    private void update_dgus()
    {
        String dwinPath = update_path + "/DWIN_SET";

        File[] files = new File(dwinPath).listFiles();
        if (files == null)
            return;

        for (File file : files)
        {
            if (file.getName().toLowerCase().endsWith(".bmp"))
            {
                System.out.println("update dgus bmp");
                update_dgus_bmp(file);
            }
            if (file.getName().toLowerCase().endsWith(".bin") ||
                    file.getName().toLowerCase().endsWith(".ico") ||
                    file.getName().toLowerCase().endsWith(".hkz"))
            {
                System.out.println("update dgus others");
                update_dgus_others(file);
            }
        }
    }

    private boolean update_dgus_bmp(File file)
    {
        int byteRead;
        byte[] bmp_head;
        byte[] bmp_body;
        byte[] img5r6b6g;
        int bmp_width, bmp_height;
        int b, g, r;
        byte[] receive;
        int bmp_number;
        InputStream inputStream = null;

        String filename = file.getName();
        if (filename.toLowerCase().endsWith(".bmp"))
        {
            try
            {
                bmp_number = Integer.parseInt(filename.replace(".bmp", ""));
                inputStream = new FileInputStream(file);
                bmp_head = new byte[54];
                byteRead = inputStream.read(bmp_head);
                if (byteRead != 54)
                    return false;
                bmp_width = (((bmp_head[21]) & 0xFF) << 24) + (((bmp_head[20]) & 0xFF) << 16) + (((bmp_head[19]) & 0xFF) << 8) + ((bmp_head[18]) & 0xFF);
                bmp_height = (((bmp_head[25]) & 0xFF) << 24) + (((bmp_head[24]) & 0xFF) << 16) + (((bmp_head[23]) & 0xFF) << 8) + ((bmp_head[22]) & 0xFF);
                bmp_body = new byte[bmp_width*bmp_height*3];
                byteRead = inputStream.read(bmp_body);
                if (byteRead != bmp_width*bmp_height*3)
                    return false;
                img5r6b6g = new byte[bmp_width*bmp_height*2];
                for (int i = 0; i < bmp_width*bmp_height; i++)
                {
                    b = (bmp_body[3*i] & 0xF8);
                    g = (bmp_body[3*i + 1] & 0xFC);
                    r = (bmp_body[3*i + 2] & 0xF8);
                    img5r6b6g[2*i] = (byte) ((r + (g >> 5)) & 0xff);
                    img5r6b6g[2*i + 1] = (byte) (((g << 3) + (b >> 3)) & 0xff);
                }

                getPrinter().getUartScreenSerialPort().write(new byte[]{0x5A,(byte)0xA5,0x06,(byte)0x80,(byte)0xF5,0x5A,0x00,0x00,(byte) bmp_number});
                receive = IOUtilities.read(getPrinter().getUartScreenSerialPort(), 2000, 10);
                if (!"OK".equals(new String(receive)))
                    return false;
                for (int i = bmp_height - 1; i >= 0; i--)
                {
                    getPrinter().getUartScreenSerialPort().write(BasicUtillities.subBytes(img5r6b6g, bmp_width*2*i, bmp_width*2));
                }
                Thread.sleep(1000);
                getPrinter().getUartScreenSerialPort().write(new byte[]{0x5A,(byte)0xA5,0x04,(byte)0x80,0x03,0x00,(byte) bmp_number});
                Thread.sleep(100);
            } catch (IOException | InterruptedException e)
            {
                System.out.println(e.toString());
            } finally
            {
                try
                {
                    if (inputStream != null)
                        inputStream.close();
                } catch (IOException e)
                {
                    System.out.println(e.toString());
                }
            }
        }
        else
        {
            return false;
        }
        return true;
    }

    private boolean update_dgus_others(File file)
    {
        InputStream inputStream = null;
        int fileNumber;
        byte[] dgusCommand;
        byte[] fileData;
        byte[] receive;
        int byteRead;

        String filename = file.getName();
        try
        {
            fileNumber = Integer.parseInt(filename.substring(0, filename.lastIndexOf(".")));

            inputStream = new FileInputStream(file);
            fileData = new byte[256*1024];
            while ((byteRead = inputStream.read(fileData)) != -1)
            {
                dgusCommand = new byte[]{0x5A,(byte)0xA5,0x04,(byte)0x80,(byte)0xF3,0x5A,(byte)fileNumber};
                getPrinter().getUartScreenSerialPort().write(dgusCommand);
                receive = IOUtilities.read(getPrinter().getUartScreenSerialPort(), 2000, 10);
                if (!"OK".equals(new String(receive)))
                    return false;
                getPrinter().getUartScreenSerialPort().write(BasicUtillities.subBytes(fileData, 0, byteRead));
                fileNumber++;
                Thread.sleep(1000);
            }

        } catch (IOException | InterruptedException e)
        {
            System.out.println(e.toString());
        } finally
        {
            try
            {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e)
            {
                System.out.println(e.toString());
            }
        }
        return true;
    }

}
