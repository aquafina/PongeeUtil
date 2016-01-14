/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pongeeutil;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.swing.JOptionPane;
import sun.text.resources.FormatData;

/**
 *
 * @author IamUsman
 */
public class PongeeUtil {

    /**
     * @param args the command line arguments
     */
    static PreparedStatement ps;
    static ResultSet rs;
    static Connection connAccess;
    static TrayIcon trayIcon;
    static Image image;
    static java.awt.SystemTray st;
    static Image bi;
    static Connection connection;
    static Thread readingThread;
    static String tTime;
    static boolean running;
    static long startLong;
    static long endLong;
    static ArrayList<String> readIds;
    private static float totalTime;

    public static void main(String[] args) {
        readIds = new ArrayList<String>();
        /**STARTING THE MAIN THREAD
         * 
         */
        try {
            tTime = getProperty("threshold_read_time");
            running = true;
            readingThread = new Thread(runnable);
            readingThread.start();
            showTrayIcon();

        } catch (Exception ex) {
            Logger.getLogger(PongeeUtil.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /*TO GET THE CONNECTION AS NAME DESCRIBES
     * 
     */
    private static Connection getConnection() throws Exception {
        String driver = "sun.jdbc.odbc.JdbcOdbcDriver";
        //String url = "jdbc:odbc:TestDS2";
        String pathProp = getProperty("filePath");
        String url2 = "jdbc:odbc:driver={Microsoft Access Driver (*.mdb)};DBQ=" + pathProp;
        String username = "";
        String password = "";
        Class.forName(driver);
        return DriverManager.getConnection(url2, username, password);
    }

    /*GET VALUE FROM THE PROPERTY FILE
     * WHERE THE THREASHHOLD FREQUENCY IS DEFINED
     * 
     */
    static String getProperty(String propName) throws Exception {
        String filepath = "./main.properties";
        FileInputStream file = new FileInputStream(filepath);
        Properties mainProperties = new Properties();
        mainProperties.load(file);
        file.close();
        String pathProp = mainProperties.getProperty(propName);
        return pathProp;
    }

    public static void showTrayIcon() {
        if (java.awt.SystemTray.isSupported()) {
            st = java.awt.SystemTray.getSystemTray();
            image = Toolkit.getDefaultToolkit().getImage(PongeeUtil.class.getResource("beating_1.png"));


            ActionListener listener = new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        int option = JOptionPane.showConfirmDialog(null, "Do you want to kill the process", "Confirm", JOptionPane.OK_CANCEL_OPTION);

                        if (option == JOptionPane.OK_OPTION) {
                            readingThread.stop();
                            running = false;
                            connAccess.close();
                            System.exit(0);
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(PongeeUtil.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };

            PopupMenu popup = new PopupMenu();

            MenuItem defaultItem = new MenuItem("Exit?");
            defaultItem.addActionListener(listener);
            popup.add(defaultItem);

            trayIcon = new TrayIcon(image, "Beating", popup);

            trayIcon.addActionListener(listener);
            try {
                st.add(trayIcon);

            } catch (AWTException e) {
                System.err.println(e);
            }
        }
    }

    static String getFormattedDate() {
        Calendar cal = Calendar.getInstance();
        //cal.setTime(new Date(System.currentTimeMillis()));
        return cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + " " + (cal.get(Calendar.AM_PM) == 1 ? "PM" : "AM");

    }

    static String getLastReadDate() throws Exception {
        File file = new File("lastReadDateIsHere.txt");
        if (file.exists()) {
            BufferedReader br = new BufferedReader(new FileReader(file));
            return br.readLine();
        } else {
            return null;
        }



    }

    static void saveLastReadDate(String dateTime) throws IOException {
        File file = new File("lastReadDateIsHere.txt");
        FileWriter fw = new FileWriter(file);
        fw.write(dateTime);
        fw.close();

    }
    static void logInFile(String log)throws IOException{
        File file = new File ("logs.txt");
        if (!file.exists()) {
          file.createNewFile();
        }
        
        FileWriter fw = new FileWriter(file);
        fw.write(log);
        fw.close();;
    }

    static void WriteToEbs() {
        try {


            //CONNECT TO ACCESS DATABASE
            connAccess = getConnection();

            //CONNECT TO ORACLE DATABASE
            Class.forName("oracle.jdbc.driver.OracleDriver");
//            connection =
//                    DriverManager.getConnection("jdbc:oracle:thin:@192.168.0.31:1522:prod",
//                    "XX_E_PORTAL", "mskiz145");
            connection =
                    DriverManager.getConnection("jdbc:oracle:thin:@192.168.0.31:1522:prod",
                    "XX_E_PORTAL", "mskiz145");



            //GET THE READ_FROM DATE FROM THE FILE
            String readFromDate = getLastReadDate();
            SimpleDateFormat sf = new SimpleDateFormat("yyyy/M/d hh:mm:ss a");
            
            Calendar c = Calendar.getInstance();
            c.setTime(sf.parse(readFromDate));
            c.add(Calendar.SECOND, -(int) totalTime);
            c.add(Calendar.SECOND, -c.get(Calendar.SECOND));
            
            String finalReadFromDate = sf.format(c.getTime());

            System.out.println("Reading From: "+finalReadFromDate+" @ "+new java.util.Date().toString());
            //logInFile("Reading From: "+finalReadFromDate+" @ "+new java.util.Date().toString()+"\n");
            String person_name = getProperty("person_name");

            //GENERATE THE QUERY THAT WILL READ ATTENDANCE DATA FROM ACCESS DATABASE FROM A SPECIFIC DATE
            //HARD COADING SQUID JONES BECAUSE THIS IS THE ONLY EMPLOYEE THAT IS CURRENTLY ON THE HUB
            String readQuery = "select * from Access " + (readFromDate == null ? " where 1=1 and Name in ('Squid Jones','Skunk Jones','Squirrel Jones') " : "where date >= # " + finalReadFromDate + " # " + (person_name.equals("NULL") ? "" : " and EmpID in ('" + person_name + "')"));
            //String readQuery = "select * from Access " + (readFromDate == null ? " where 1=1 and Name in ('Squid Jones','Skunk Jones','Squirrel Jones') " : "where  date >= # " + " 2015/7/28 06:00:00 AM" + " # and date<= # 2015/7/30 11:59:00 PM #" + (person_name.equals("NULL") ? "" : " and name in ('" + person_name + "')"));
            System.out.println(readQuery);
            //logInFile(readQuery+"\n");
            //logInFile("********************************************\n");
            
            ps = connAccess.prepareStatement(readQuery);
            rs = ps.executeQuery();
            startLong = System.currentTimeMillis();
            while (rs.next()) {
                int pongee_serial = rs.getInt("serial");
                String name = rs.getString("Name");
                String id = rs.getString("id");
                String emp_id = rs.getString("EmpID");
                String door_no = rs.getString("DoorNo");
                String door_dsc = rs.getString("DoorDsc");
                String status = rs.getString("status");
                String in_out = rs.getString("in_out");
                Timestamp date = rs.getTimestamp("date");

                //INSERT QUERY TO INSERT DATA TO ORACLE DATABASE
                String insertQuery = "insert into xx_e_portal_raw_emp_atd (attendance_id,pongee_serial,emp_id,emp_name,card_num,door_id,door_description,status,in_out,ATTENDANCE_DATE) select xx_e_portal_sequence.nextval,?,?,?,?,?,?,?,?,? from dual";
                PreparedStatement ps = connection.prepareStatement(insertQuery);
                ps.setInt(1, pongee_serial);
                ps.setString(2, emp_id);
                ps.setString(3, name);
                ps.setString(4, id);
                ps.setString(5, door_no);
                ps.setString(6, door_dsc);
                ps.setString(7, status);
                ps.setString(8, in_out);
                ps.setTimestamp(9, date);
                int insertID = ps.executeUpdate();
                if (insertID != 0) {
                    //ReadWhereType rwt = new ReadWhereType();
                    //SAVE EMPLOYEE ID AND DATE FOR LATER USE
                    readIds.add(emp_id + "#" + date.toString().split(" ")[0]);

                    if (!person_name.equals("NULL")) {
                        System.out.println("Attendance for " + date + " is imported");
                        //logInFile("Attendance for " + date + " is imported\n");
                    }
                }

                ps.close();
                ps = null;
            }

            //CREATE UNIQUE RECORDS FROM ABOVE LIST
            Set<String> set = new HashSet<String>(readIds);
            Iterator<String> itr = set.iterator();
            while (itr.hasNext()) {
                String rwt = itr.next();

                //SELECT THE FORMATTED DATE OF AN EMPLOYEE OF A SPECIFIC DATE
                String formatedDataQuery = "select * from xx_e_portal_emp_atd_v where emp_id = ? and trunc(out_time) = to_Date(?,'YYYY-MM-DD')";
                PreparedStatement pst = connection.prepareStatement(formatedDataQuery);
                //System.out.println(rwt.split("#")[1]);
                pst.setString(1, rwt.split("#")[0]);
                pst.setString(2, rwt.split("#")[1]);
                ResultSet viewRs = pst.executeQuery();
                if (viewRs.next()) {
                    //select xx_e_portal_sequence.nextval
                    //2015/3/28 9:0:0 AM
                    int emp_id = viewRs.getInt("emp_id");
                    String emp_name = viewRs.getString("EMP_NAME");
                    String card_num = viewRs.getString("CARD_NUM");
                    String expected_work_hours = viewRs.getString("EXPECTED_WORK_HOURS");
                    Timestamp in_time = viewRs.getTimestamp("IN_TIME");
                    Timestamp out_time = viewRs.getTimestamp("OUT_TIME");
                    String effective_worked_hours = viewRs.getString("WORKED_HOURS");
                    String max_start_time = viewRs.getString("MAX_START_TIME");
                    String max_end_time = viewRs.getString("MAX_END_TIME");
                    String start_time = viewRs.getString("START_TIME");
                    String end_time = viewRs.getString("END_TIME");
                    String emp_type = viewRs.getString("EMP_TYPE");
                    /*NOW CHECK IF ABOVE SELECTED RECORD EXISTS OR NOT
                     * IF IT EXISTS I AM UPDATING IT
                     * IF NOT I AM INSERTING IT
                     * BELOW IS THE QUERY THAT WILL CHECK THIS RECORD IN THE DATABASE
                     */
                    String insertOrUpdate = "select * from xx_e_portal_emp_atd where emp_id = ? and trunc(ATTENDANCE_DATE) = trunc(?)";
                    PreparedStatement psInsertOrUpdate = connection.prepareStatement(insertOrUpdate);
                    psInsertOrUpdate.setInt(1, emp_id);
                    psInsertOrUpdate.setTimestamp(2, out_time);
                    ResultSet rsInsertOrUpdate = psInsertOrUpdate.executeQuery();
                    /*IT EXISTS. UPDATE IT
                     * 
                     */
                    if (rsInsertOrUpdate.next()) {
                        String updateQuery = "update xx_e_portal_emp_atd set min_in_time = ? , max_out_time = ? ,effective_worked_hours = ?, card_num = ?, EMP_TYPE = ? where emp_id = ? and trunc(ATTENDANCE_DATE) = trunc(?)";
                        PreparedStatement psUpdateQuery = connection.prepareStatement(updateQuery);
                        psUpdateQuery.setTimestamp(1, in_time);
                        psUpdateQuery.setTimestamp(2, out_time);
                        psUpdateQuery.setString(3, effective_worked_hours);
                        psUpdateQuery.setString(4, card_num);
                        if (emp_type ==  null || emp_type.trim().equals("")) {
                            psUpdateQuery.setInt(5, 0);   
                        }else{
                            psUpdateQuery.setInt(5, Integer.parseInt(emp_type));
                        }
                        psUpdateQuery.setInt(6, emp_id);
                        psUpdateQuery.setTimestamp(7, out_time);
                        int id = psUpdateQuery.executeUpdate();
                        if (id > 0) {
                            readIds.clear();
                            /**Save the time*/
                            endLong = System.currentTimeMillis();
                            long elapsedTime = endLong - startLong;
                            totalTime = elapsedTime / 1000f;

                            saveLastReadDate(getFormattedDate());
                        }
                        psUpdateQuery.close();
                    } else {
                        String insertQuery = "INSERT INTO xx_e_portal_emp_atd(emp_atd_id,emp_id,emp_name,card_num,expected_work_hours,min_in_time,max_out_time,effective_worked_hours,MAX_START_TIME,MAX_END_TIME,END_TIME,START_TIME,attendance_date,emp_type) select xx_e_portal_sequence.nextval,?,?,?,?,?,?,?,?,?,?,?,?,? from dual";
                        PreparedStatement psInsert = connection.prepareStatement(insertQuery);
                        psInsert.setInt(1, emp_id);
                        psInsert.setString(2, emp_name);
                        psInsert.setString(3, card_num);
                        psInsert.setString(4, expected_work_hours);
                        psInsert.setTimestamp(5, in_time);
                        psInsert.setTimestamp(6, out_time);
                        psInsert.setString(7, effective_worked_hours);
                        psInsert.setString(8, max_start_time);
                        psInsert.setString(9, max_end_time);
                        psInsert.setString(10, start_time);
                        psInsert.setString(11, end_time);
                        psInsert.setTimestamp(12, out_time);
                        psInsert.setInt(13, Integer.parseInt(emp_type));
                        int id = psInsert.executeUpdate();
                        if (id > 0) {
                            readIds.clear();
                            /**Save the time*/
                            endLong = System.currentTimeMillis();
                            long elapsedTime = endLong - startLong;
                            totalTime = elapsedTime / 1000f;
                          
                            saveLastReadDate(getFormattedDate());
                        }
                        psInsert.close();
                    }
                    psInsertOrUpdate.close();
                }

            }
            System.out.println("Finished Reading, Total time: " + totalTime);
            System.out.println("###############################################################");
            System.out.println("");
//            logInFile("Finished Reading, Total time: " + totalTime+"\n");
//            logInFile("****************************************************\n");




        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                connAccess.close();
                connection.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }


        }
    }

    static class ReadWhereType {

        public String empID;
        public String date;
    }
    static Runnable runnable = new Runnable() {

        @Override
        public void run() {
            while (running) {
                try {
                    WriteToEbs();
                    Thread.sleep(Long.parseLong(tTime));
                } catch (InterruptedException ex) {
                    running = false;
                    System.out.println("************************************************************");
                    System.out.println("Error: " + ex.getMessage());
                    System.out.println("************************************************************");
                }
            }


        }
    };
}
