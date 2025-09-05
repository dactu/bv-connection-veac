package com.vht.connection;



import com.vht.connection.Objects.BVModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * Giả định bạn đã có sẵn 2 class này trong project:
 *   - BVModule(String veaName, String alias, String ip, int port, double lat, double lon)
 *   - BVManager.getInstance().addBVModule(BVModule m)
 *
 * Nếu tên constructor/manager khác, đổi lại cho khớp.
 */
@Log4j2
@Configuration
@RequiredArgsConstructor
public class ReadConfigFile {

    public static AppConfigProperties.BV bvProps;

    @Bean
    ApplicationRunner initBvModules() {
        return args -> {
            if (bvProps.modules() == null || bvProps.modules().isEmpty()) {
                log.warn("No BV modules configured (prefix 'bv.modules').");
                return;
            }

            log.info("BV modules configured: {}", bvProps.modules().size());

            bvProps.modules().forEach((name, m) -> {
                BVModule module = new BVModule(
                        name,          // veaName = key trong map
                        m.alias(),
                        m.ip(),
                        m.port(),
                        m.lat(),
                        m.lon()
                );

                // Nếu sau này bạn mở rộng BVModule có setter thêm jmName/empName/rangeJammer
                // thì set ở đây.

                log.info("CREATE BV5 {}", module);
                BVManager.getInstance().addBVModule(module);
            });
        };
    }
}

//import lombok.extern.log4j.Log4j2;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.env.Environment;
//
//@Log4j2
//@Configuration
//public class ReadConfigFile {
//
//    public static int bvNumber;
//
//    public static String veaName1;
//    public static String veaJmName1;
//    public static String veaEmpName1;
//    public static String veaIpAddress1;
//    public static int veaPort1;
//    public static double veaLat1;
//    public static double veaLong1;
//    public static double rangeJammer1;
//
//    public static String veaName2;
//    public static String veaJmName2;
//    public static String veaEmpName2;
//    public static String veaIpAddress2;
//    public static int veaPort2;
//    public static double veaLat2;
//    public static double veaLong2;
//    public static double rangeJammer2;
//
//    public static String veriUsername;
//    public static String veriPassword;
//
//    public static String empGrpcAddress;
//    public static int udpPort;
//    public static double bmsLat;
//    public static double bmsLong;
//
//    public static boolean enableLogBv5Target;
//    public static boolean enableLogBmsTarget;
//    public static boolean enableLogCommand;
//    public static boolean enableLogEmpJammer;
//    public static boolean enableLogDf;
//
//    public static double rangeDf;
//    public static double rangeEMP;
//    public static double rangeSendTarget;
//
//    //add by quyetdd3
//    public static String mainAddress;
//    public static String dp01Address;
//    public static String dp02Address;
//    public static int portHB;
//    public static int timeoutCheckHB;
//    public static boolean turnOffDf;
//    public static double openAngleDf;
//
//    //Coverage
//    public static String empCovName;
//    public static String jmGpsCov01Name; //jm gps bv5.01
//    public static String jmComCov01Name; //jm com bv5.01
//    public static String jmGpsCov02Name; //jm gps bv5.02
//    public static String jmComCov02Name; //jm com bv5.02
//
//    public static String dfCov01Name; //df bv5.01
//    public static String dfCov02Name; //df bv5.02
//
//    //MRS
//    public static double mrsPosLat;
//    public static double mrsPosLon;
//    //SRS01
//    public static double srs01PosLat;
//    public static double srs01PosLon;
//    //SRS02
//    public static double srs02PosLat;
//    public static double srs02PosLon;
//    //SRS03
//    public static double srs03PosLat;
//    public static double srs03PosLon;
//    //U2X
//    public static double u2xPosLat;
//    public static double u2xPosLon;
//    //U3K
//    public static double u3kPosLat;
//    public static double u3kPosLon;
//    //LRA01
//    public static double lra01PosLat;
//    public static double lra01PosLon;
//    //LRA02
//    public static double lra02PosLat;
//    public static double lra02PosLon;
//    //LRA03
//    public static double lra03PosLat;
//    public static double lra03PosLon;
//
//    public static boolean turnOffVq;
//    public static boolean enableLogBv5Plot;
//    // end add by quyetdd3
//
//    public static int moduleStatusPort;
//
//    //add by tunght21 - 1607
//    public static String radarSrs01Name;
//    public static String radarSrs02Name;
//    public static String radarSrs03Name;
//    public static String radarMrsName;
//    public static String radarU2xName;
//    public static String radarU3kName;
//
//    public static String bv5Lra01Name;
//    public static String bv5Lra02Name;
//    public static String srsLra01Name;
//    public static String srsLra02Name;
//    public static String srsLra03Name;
//
//    public static boolean enableLogModuleStatus;
//
//    public static long timeoutSendEmptyStatus;
//
//    public static boolean enableCaptureData;
////    radar_srs_name = SRS-LRA_HL
////            radar_mrs_name = VRS-MRS_d61
////    radar_u2x_name = U2X-BV5.02_HL
////            radar_u3k_name = U3K-Bv5.01_L916
//
////            bv5_01_qdt_name = BV5_LRA.01
////    bv5_02_qdt_name = BV5_LRA.02
////
////    srs_01_qdt_name = SRS_LRA.01
////    srs_02_qdt_name = SRS_LRA.02
////    srs_03_qdt_name = SRS_LRA.03
//
//    //end add by tunght21
//
//    public ReadConfigFile() {
//        //TO DO:
//    }
//
//    @Bean
//    ApplicationRunner restRequestRunner(Environment environment,
//                                        @Value("${num_of_bv5}") int m_bvNumber,
//                                        @Value("${vea_name_1}") String mVeaName1,
//                                        @Value("${vea_ip_address1}") String m_veaIpAddress1,
//                                        @Value("${vea_server_port1}") int m_veaPort1,
//                                        @Value("${vea_ip_lat_1}") double m_veaLat1,
//                                        @Value("${vea_server_long_1}") double m_veaLong1,
//                                        @Value("${config_range_jammer1}") double m_config_range_jammer1,
//                                        @Value("${vea_name_2}") String mVeaName2,
//                                        @Value("${vea_ip_address2}") String m_veaIpAddress2,
//                                        @Value("${vea_server_port2}") int m_veaPort2,
//                                        @Value("${vea_ip_lat_2}") double m_veaLat2,
//                                        @Value("${vea_server_long_2}") double m_veaLong2,
//                                        @Value("${config_range_jammer2}") double m_config_range_jammer2,
//                                        @Value("${connect_verify_username}") String m_veriUsername,
//                                        @Value("${connect_verify_password}") String m_veriPassword,
//                                        @Value("${emp_grpc_address}") String m_empGrpcAddress,
//                                        @Value("${udp_port}") int m_UdpPort,
//                                        @Value("${bms_latitude}") double m_bmsLat,
//                                        @Value("${bms_longitude}") double m_bmsLong,
//                                        @Value("${enable_log_fusion_bv5}") boolean m_enable_log_fusion_bv5,
//                                        @Value("${enable_log_bms_target}") boolean m_enable_log_bms_target,
//                                        @Value("${enable_log_command}") boolean m_enable_log_command,
//                                        @Value("${enable_log_emp_jammer}") boolean m_enable_log_emp_jammer,
//                                        @Value("${enable_log_df}") boolean m_enable_log_df,
//                                        @Value("${config_range_df}") double m_config_range_df,
//                                        @Value("${config_range_emp}") double m_config_range_emp,
//                                        @Value("${config_range_send_target}") double m_config_range_send_target,
//                                        @Value("${ip_main}") String m_ip_main,
//                                        @Value("${ip_dp_01}") String m_ip_dp_01,
//                                        @Value("${ip_dp_02}") String m_ip_dp_02,
//                                        @Value("${grpc_port_connection}") int m_grpc_port_connection,
//                                        @Value("${time_out_check_heartbeat}") int m_time_out_check_heartbeat,
//                                        @Value("${turn_off_df}") boolean m_turn_off_df,
//                                        @Value("${config_open_angle_df}") double m_config_open_angle_df,
//                                        @Value("${vea_emp_name_1}") String m_vea_emp_name_1,
//                                        @Value("${vea_jm_name_1}") String m_vea_jm_name_1,
//                                        @Value("${vea_emp_name_2}") String m_vea_emp_name_2,
//                                        @Value("${vea_jm_name_2}") String m_vea_jm_name_2,
//                                        @Value("${tcp_server_port}") int m_tcp_server_port,
//                                        @Value("${radar_srs_01_name}") String m_radar_srs_01_name,
//                                        @Value("${radar_srs_02_name}") String m_radar_srs_02_name,
//                                        @Value("${radar_srs_03_name}") String m_radar_srs_03_name,
//                                        @Value("${radar_mrs_name}") String m_radar_mrs_name,
//                                        @Value("${radar_u2x_name}") String m_radar_u2x_name,
//                                        @Value("${radar_u3k_name}") String m_radar_u3k_name,
//                                        @Value("${bv5_01_qdt_name}") String m_bv5_01_qdt_name,
//                                        @Value("${bv5_02_qdt_name}") String m_bv5_02_qdt_name,
//                                        @Value("${srs_01_qdt_name}") String m_srs_01_qdt_name,
//                                        @Value("${srs_02_qdt_name}") String m_srs_02_qdt_name,
//                                        @Value("${srs_03_qdt_name}") String m_srs_03_qdt_name,
//                                        @Value("${enable_log_module_status}") boolean m_enable_log_module_status,
//                                        @Value("${timeout_send_empty_status}") long m_timeout_send_empty_status,
//                                        @Value("${enable_capture_data}") boolean m_enable_capture_data,
//                                        @Value("${vea_emp_cov_name}") String m_vea_emp_cov_name,
//                                        @Value("${vea_jm_gps_cov_name_bv5_01}") String m_vea_jm_gps_cov_name_bv5_01,
//                                        @Value("${vea_jm_gps_cov_name_bv5_02}") String m_vea_jm_gps_cov_name_bv5_02,
//                                        @Value("${vea_jm_com_cov_name_bv5_01}") String m_vea_jm_com_cov_name_bv5_01,
//                                        @Value("${vea_jm_com_cov_name_bv5_02}") String m_vea_jm_com_cov_name_bv5_02,
//                                        @Value("${vea_df_cov_name_bv5_01}") String m_vea_df_cov_name_bv5_01,
//                                        @Value("${vea_df_cov_name_bv5_02}") String m_vea_df_cov_name_bv5_02,
//                                        @Value("${mrs_pos_lat}") double m_mrs_pos_lat,
//                                        @Value("${mrs_pos_lon}") double m_mrs_pos_lon,
//                                        @Value("${srs01_pos_lat}") double m_srs01_pos_lat,
//                                        @Value("${srs01_pos_lon}") double m_srs01_pos_lon,
//                                        @Value("${srs02_pos_lat}") double m_srs02_pos_lat,
//                                        @Value("${srs02_pos_lon}") double m_srs02_pos_lon,
//                                        @Value("${srs03_pos_lat}") double m_srs03_pos_lat,
//                                        @Value("${srs03_pos_lon}") double m_srs03_pos_lon,
//                                        @Value("${u2x_pos_lat}") double m_u2x_pos_lat,
//                                        @Value("${u2x_pos_lon}") double m_u2x_pos_lon,
//                                        @Value("${u3k_pos_lat}") double m_u3k_pos_lat,
//                                        @Value("${u3k_pos_lon}") double m_u3k_pos_lon,
//                                        @Value("${lra01_pos_lat}") double m_lra01_pos_lat,
//                                        @Value("${lra01_pos_lon}") double m_lra01_pos_lon,
//                                        @Value("${lra02_pos_lat}") double m_lra02_pos_lat,
//                                        @Value("${lra02_pos_lon}") double m_lra02_pos_lon,
//                                        @Value("${lra03_pos_lat}") double m_lra03_pos_lat,
//                                        @Value("${lra03_pos_lon}") double m_lra03_pos_lon,
//                                        @Value("${turn_off_vq}") boolean m_turn_off_vq,
//                                        @Value("${enable_log_bv5_plot}") boolean m_enable_log_bv5_plot) {
//        return args -> {
//            bvNumber = m_bvNumber;
//
//            veaName1 = mVeaName1;
//            veaIpAddress1 = m_veaIpAddress1;
//            veaPort1 = m_veaPort1;
//            veaLat1 = m_veaLat1;
//            veaLong1 = m_veaLong1;
//            rangeJammer1 = m_config_range_jammer1;
//
//            veaName2 = mVeaName2;
//            veaIpAddress2 = m_veaIpAddress2;
//            veaPort2 = m_veaPort2;
//            veaLat2 = m_veaLat2;
//            veaLong2 = m_veaLong2;
//            rangeJammer2 = m_config_range_jammer2;
//
//            veriUsername = m_veriUsername;
//            veriPassword = m_veriPassword;
//
//            empGrpcAddress = m_empGrpcAddress;
//            udpPort = m_UdpPort;
//            bmsLat = m_bmsLat;
//            bmsLong = m_bmsLong;
//
//            enableLogBv5Target = m_enable_log_fusion_bv5;
//            enableLogBmsTarget = m_enable_log_bms_target;
//            enableLogCommand = m_enable_log_command;
//            enableLogEmpJammer = m_enable_log_emp_jammer;
//
//            // add by quyetdd3
//            rangeDf = m_config_range_df;
//            rangeEMP = m_config_range_emp;
//            rangeSendTarget = m_config_range_send_target;
//            enableLogDf = m_enable_log_df;
//            mainAddress = m_ip_main;
//            dp01Address = m_ip_dp_01;
//            dp02Address = m_ip_dp_02;
//            portHB = m_grpc_port_connection;
//            timeoutCheckHB = m_time_out_check_heartbeat;
//            turnOffDf = m_turn_off_df;
//            openAngleDf = m_config_open_angle_df;
//
//            veaEmpName1 = m_vea_emp_name_1;
//            veaJmName1 = m_vea_jm_name_1;
//            veaEmpName2 = m_vea_emp_name_2;
//            veaJmName2 = m_vea_jm_name_2;
//
//            empCovName = m_vea_emp_cov_name;
//            jmGpsCov01Name = m_vea_jm_gps_cov_name_bv5_01;
//            jmComCov01Name = m_vea_jm_com_cov_name_bv5_01;
//
//            jmGpsCov02Name = m_vea_jm_gps_cov_name_bv5_02;
//            jmComCov02Name = m_vea_jm_com_cov_name_bv5_02;
//
//            dfCov01Name = m_vea_df_cov_name_bv5_01;
//            dfCov02Name = m_vea_df_cov_name_bv5_02;
//
//            mrsPosLat = m_mrs_pos_lat;
//            mrsPosLon = m_mrs_pos_lon;
//
//            srs01PosLat = m_srs01_pos_lat;
//            srs01PosLon = m_srs01_pos_lon;
//
//            srs02PosLat = m_srs02_pos_lat;
//            srs02PosLon = m_srs02_pos_lon;
//
//            srs03PosLat = m_srs03_pos_lat;
//            srs03PosLon = m_srs03_pos_lon;
//
//            u2xPosLat = m_u2x_pos_lat;
//            u2xPosLon = m_u2x_pos_lon;
//
//            u3kPosLat = m_u3k_pos_lat;
//            u3kPosLon = m_u3k_pos_lon;
//
//            lra01PosLat = m_lra01_pos_lat;
//            lra01PosLon = m_lra01_pos_lon;
//
//            lra02PosLat = m_lra02_pos_lat;
//            lra02PosLon = m_lra02_pos_lon;
//
//            lra03PosLat = m_lra03_pos_lat;
//            lra03PosLon = m_lra03_pos_lon;
//
//            turnOffVq = m_turn_off_vq;
//            enableLogBv5Plot = m_enable_log_bv5_plot;
//            // end add by quyetdd3
//
//            moduleStatusPort = m_tcp_server_port;
//
//            radarSrs01Name = m_radar_srs_01_name;
//            radarSrs02Name = m_radar_srs_02_name;
//            radarSrs03Name = m_radar_srs_03_name;
//            radarMrsName = m_radar_mrs_name;
//            radarU2xName = m_radar_u2x_name;
//            radarU3kName = m_radar_u3k_name;
//
//            bv5Lra01Name = m_bv5_01_qdt_name;
//            bv5Lra02Name = m_bv5_02_qdt_name;
//
//            srsLra01Name = m_srs_01_qdt_name;
//            srsLra02Name = m_srs_02_qdt_name;
//            srsLra03Name = m_srs_03_qdt_name;
//
//            enableLogModuleStatus = m_enable_log_module_status;
//
//            timeoutSendEmptyStatus = m_timeout_send_empty_status;
//
//            enableCaptureData = m_enable_capture_data;
//        };
//    }
//}
